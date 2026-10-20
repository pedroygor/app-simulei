package br.com.simulados.api.questao.infrastructure.storage;

import br.com.simulados.api.questao.application.ImagemIndisponivelException;
import br.com.simulados.api.questao.application.ImagemRejeitadaException;
import br.com.simulados.api.questao.application.ImagemStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Adaptador de disco local — e a fronteira de confiança com a fonte externa.
 *
 * Três garantias que o importador não precisa conhecer:
 *
 * 1. TAMANHO — lê no máximo o teto configurado. Antes o download era
 *    body(byte[].class), que carrega a resposta inteira em memória: uma
 *    resposta grande (ou maliciosa) derrubaria o job com OutOfMemory.
 *
 * 2. FORMATO — allowlist por magic bytes (ver TipoDeImagem). Extensão da URL e
 *    header Content-Type vêm da fonte, então não servem pra validar a fonte.
 *
 * 3. NOME — derivado do hash SHA-256 do CONTEÚDO, nunca da URL. Isso elimina
 *    a classe inteira de path traversal na origem (não há input externo no
 *    caminho), dá deduplicação de graça (o mesmo arquivo em 6 questões vira 1)
 *    e torna honesto o cache immutable: URL diferente = conteúdo diferente.
 */
@Component
public class ImagemStorageLocal implements ImagemStorage {

    private static final Logger log = LoggerFactory.getLogger(ImagemStorageLocal.class);

    private final RestClient restClient;
    private final Path diretorioRaiz;
    private final String baseUrl;
    private final long tamanhoMaximoBytes;

    ImagemStorageLocal(RestClient.Builder builder,
                       @Value("${storage.imagens.diretorio}") String diretorio,
                       @Value("${storage.imagens.base-url}") String baseUrl,
                       @Value("${storage.imagens.tamanho-maximo}") DataSize tamanhoMaximo) {
        // RestClient sem base URL: cada imagem tem host próprio
        this.restClient = builder.build();
        this.diretorioRaiz = Path.of(diretorio).toAbsolutePath().normalize();
        this.baseUrl = baseUrl;
        this.tamanhoMaximoBytes = tamanhoMaximo.toBytes();
    }

    @Override
    public String guardar(String urlOrigem) {
        var conteudo = baixar(urlOrigem);

        var tipo = TipoDeImagem.detectar(conteudo)
                .orElseThrow(() -> new ImagemRejeitadaException(urlOrigem,
                        "formato fora da allowlist (SVG e não-imagens são recusados)"));

        var caminhoRelativo = caminhoDe(conteudo, tipo);
        var destino = diretorioRaiz.resolve(caminhoRelativo);
        try {
            // Content-addressed: se o arquivo existe, o conteúdo é idêntico.
            // Reimportar não rebaixa nada.
            if (!Files.exists(destino)) {
                Files.createDirectories(destino.getParent());
                // Escreve em temporário e move: um kill no meio do job não
                // deixa arquivo truncado com nome de conteúdo válido
                var temporario = Files.createTempFile(destino.getParent(), "parcial-", ".tmp");
                Files.write(temporario, conteudo);
                Files.move(temporario, destino, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            }
        } catch (IOException e) {
            throw new ImagemIndisponivelException(urlOrigem, e);
        }
        return baseUrl + "/" + caminhoRelativo.toString().replace('\\', '/');
    }

    private byte[] baixar(String urlOrigem) {
        try {
            return restClient.get().uri(URI.create(urlOrigem)).exchange((requisicao, resposta) -> {
                if (resposta.getStatusCode().isError()) {
                    throw new ImagemIndisponivelException(urlOrigem,
                            new IOException("HTTP " + resposta.getStatusCode()));
                }
                return lerComLimite(resposta.getBody(), urlOrigem);
            });
        } catch (ImagemIndisponivelException | ImagemRejeitadaException e) {
            throw e;
        } catch (RestClientException | IllegalArgumentException e) {
            throw new ImagemIndisponivelException(urlOrigem, e);
        }
    }

    /**
     * Lê no máximo o teto + 1 byte: se veio o byte extra, a resposta passou do
     * limite e é recusada. Não confiamos no header Content-Length pra isso —
     * ele é declarado por quem está do outro lado.
     */
    private byte[] lerComLimite(InputStream corpo, String urlOrigem) throws IOException {
        try (corpo) {
            var bytes = corpo.readNBytes((int) tamanhoMaximoBytes + 1);
            if (bytes.length > tamanhoMaximoBytes) {
                throw new ImagemRejeitadaException(urlOrigem,
                        "maior que o limite de " + tamanhoMaximoBytes + " bytes");
            }
            if (bytes.length == 0) {
                throw new ImagemIndisponivelException(urlOrigem, new IOException("resposta vazia"));
            }
            return bytes;
        }
    }

    // Layout por prefixo do hash (ab/cd/abcd...png), como object storage faz:
    // evita diretório com dezenas de milhares de arquivos no mesmo nível
    private Path caminhoDe(byte[] conteudo, TipoDeImagem tipo) {
        var hash = sha256(conteudo);
        return Path.of(hash.substring(0, 2), hash.substring(2, 4), hash + tipo.extensao());
    }

    private String sha256(byte[] conteudo) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(conteudo));
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 é obrigatório em toda JVM; se faltar, o ambiente está quebrado
            throw new IllegalStateException(e);
        }
    }
}

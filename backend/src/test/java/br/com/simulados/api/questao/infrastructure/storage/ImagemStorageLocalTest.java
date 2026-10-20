package br.com.simulados.api.questao.infrastructure.storage;

import br.com.simulados.api.questao.application.ImagemIndisponivelException;
import br.com.simulados.api.questao.application.ImagemRejeitadaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.unit.DataSize;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

// Sem rede: o MockRestServiceServer intercepta o download. O disco é um
// @TempDir descartado no fim de cada teste.
class ImagemStorageLocalTest {

    // Magic bytes reais: é assim que o storage decide o que é imagem
    private static final byte[] PNG = concat(
            new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A}, "conteudo-png".getBytes(StandardCharsets.UTF_8));
    private static final byte[] JPEG = concat(
            new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}, "conteudo-jpeg".getBytes(StandardCharsets.UTF_8));
    private static final byte[] SVG =
            "<svg xmlns=\"http://www.w3.org/2000/svg\"><script>alert(1)</script></svg>".getBytes(StandardCharsets.UTF_8);
    private static final byte[] HTML = "<!DOCTYPE html><html><body>oi</body></html>".getBytes(StandardCharsets.UTF_8);

    @TempDir
    Path diretorio;

    private MockRestServiceServer servidor;
    private ImagemStorageLocal storage;

    @BeforeEach
    void setUp() {
        var builder = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(builder).build();
        storage = new ImagemStorageLocal(builder, diretorio.toString(), "/imagens", DataSize.ofKilobytes(64));
    }

    @Test
    void guardaImagemComNomeDerivadoDoConteudo() {
        var url = "https://enem.dev/2023/questions/170/abc123.png";
        servidor.expect(requestTo(url)).andRespond(withSuccess(PNG, MediaType.IMAGE_PNG));

        var urlLocal = storage.guardar(url);

        // O nome é o hash do conteúdo, não o da URL: nenhum pedaço de input
        // externo entra no caminho do arquivo
        assertThat(urlLocal).startsWith("/imagens/").endsWith(".png").doesNotContain("abc123");
        var gravado = diretorio.resolve(urlLocal.substring("/imagens/".length()));
        assertThat(gravado).exists().hasBinaryContent(PNG);
        servidor.verify();
    }

    @Test
    void deduplicaConteudoIdenticoVindoDeUrlsDiferentes() {
        // O broken-image.svg do enem.dev aparecia em 6 questões — com endereço
        // por conteúdo, arquivo repetido é gravado uma vez só
        var url1 = "https://enem.dev/2023/questions/1/a.png";
        var url2 = "https://enem.dev/2023/questions/99/b.png";
        servidor.expect(requestTo(url1)).andRespond(withSuccess(PNG, MediaType.IMAGE_PNG));
        servidor.expect(requestTo(url2)).andRespond(withSuccess(PNG, MediaType.IMAGE_PNG));

        var primeira = storage.guardar(url1);
        var segunda = storage.guardar(url2);

        assertThat(segunda).isEqualTo(primeira);
        assertThat(arquivosGravados()).isEqualTo(1);
    }

    @Test
    void naoRebaixaOQueJaEstaNoDisco() {
        var url = "https://enem.dev/2023/questions/170/abc123.png";
        // once(): se tentar baixar de novo, o mock falha
        servidor.expect(once(), requestTo(url)).andRespond(withSuccess(PNG, MediaType.IMAGE_PNG));

        storage.guardar(url);
        servidor.reset();
        servidor.expect(once(), requestTo(url)).andRespond(withSuccess(PNG, MediaType.IMAGE_PNG));
        storage.guardar(url);

        assertThat(arquivosGravados()).isEqualTo(1);
    }

    @Test
    void recusaSvgAindaQueAFonteDigaQueEImagem() {
        // O vetor real: SVG é XML executável. Servido do NOSSO domínio e aberto
        // direto, o <script> roda no nosso contexto de origem.
        var url = "https://enem.dev/broken-image.svg";
        servidor.expect(requestTo(url))
                .andRespond(withSuccess(SVG, MediaType.valueOf("image/svg+xml")));

        assertThatThrownBy(() -> storage.guardar(url))
                .isInstanceOf(ImagemRejeitadaException.class)
                .hasMessageContaining("allowlist");
        assertThat(arquivosGravados()).isZero();
    }

    @Test
    void recusaArquivoQueNaoEImagemMesmoComContentTypeMentindo() {
        // Content-Type e extensão vêm da fonte; quem manda é o conteúdo
        var url = "https://enem.dev/2023/questions/1/disfarcado.png";
        servidor.expect(requestTo(url)).andRespond(withSuccess(HTML, MediaType.IMAGE_PNG));

        assertThatThrownBy(() -> storage.guardar(url))
                .isInstanceOf(ImagemRejeitadaException.class);
        assertThat(arquivosGravados()).isZero();
    }

    @Test
    void recusaArquivoAcimaDoLimiteSemEstourarMemoria() {
        var url = "https://enem.dev/2023/questions/1/gigante.png";
        var gigante = new byte[(int) DataSize.ofKilobytes(64).toBytes() + 1024];
        System.arraycopy(PNG, 0, gigante, 0, PNG.length);
        servidor.expect(requestTo(url)).andRespond(withSuccess(gigante, MediaType.IMAGE_PNG));

        assertThatThrownBy(() -> storage.guardar(url))
                .isInstanceOf(ImagemRejeitadaException.class)
                .hasMessageContaining("limite");
        assertThat(arquivosGravados()).isZero();
    }

    @Test
    void aceitaJpegAlemDePng() {
        var url = "https://enem.dev/2023/questions/1/foto.jpg";
        servidor.expect(requestTo(url)).andRespond(withSuccess(JPEG, MediaType.IMAGE_JPEG));

        assertThat(storage.guardar(url)).endsWith(".jpg");
    }

    @Test
    void traduzFalhaDeDownloadParaExcecaoPropria() {
        var url = "https://enem.dev/2023/questions/1/quebrada.png";
        servidor.expect(requestTo(url)).andRespond(withServerError());

        assertThatThrownBy(() -> storage.guardar(url))
                .isInstanceOf(ImagemIndisponivelException.class)
                .hasMessageContaining("quebrada.png");
    }

    @Test
    void urlMaliciosaNaoEscapaDoDiretorio() {
        // Com nome derivado do hash, o caminho não depende da URL — o traversal
        // deixa de ser possível por construção, não por higienização
        var url = "https://enem.dev/2023/questions/1/..%2F..%2F..%2Fevil.png";
        servidor.expect(requestTo(url)).andRespond(withSuccess(PNG, MediaType.IMAGE_PNG));

        var urlLocal = storage.guardar(url);

        assertThat(urlLocal).doesNotContain("..").doesNotContain("evil");
        assertThat(arquivosGravados()).isEqualTo(1);
    }

    private long arquivosGravados() {
        try (var arquivos = Files.walk(diretorio)) {
            return arquivos.filter(Files::isRegularFile).count();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] concat(byte[] a, byte[] b) {
        var resultado = new byte[a.length + b.length];
        System.arraycopy(a, 0, resultado, 0, a.length);
        System.arraycopy(b, 0, resultado, a.length, b.length);
        return resultado;
    }
}

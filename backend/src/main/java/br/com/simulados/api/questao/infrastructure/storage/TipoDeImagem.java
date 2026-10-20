package br.com.simulados.api.questao.infrastructure.storage;

import java.util.Arrays;
import java.util.Optional;

/**
 * Allowlist de formatos que aceitamos guardar e servir.
 *
 * A detecção é pelos MAGIC BYTES do conteúdo, não pela extensão da URL nem
 * pelo header Content-Type — os dois vêm da fonte e a fonte é um terceiro que
 * não controlamos. Confiar neles é confiar em quem estamos validando.
 *
 * SVG está deliberadamente FORA: é XML que pode carregar <script>. Um SVG
 * servido do nosso domínio e aberto diretamente executa script no NOSSO
 * contexto de origem — XSS armazenado com o conteúdo vindo de terceiro.
 * Ele não perde nada pro produto: questão do ENEM é imagem rasterizada.
 */
public enum TipoDeImagem {

    PNG("image/png", ".png", new int[]{0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}),
    JPEG("image/jpeg", ".jpg", new int[]{0xFF, 0xD8, 0xFF}),
    GIF("image/gif", ".gif", new int[]{0x47, 0x49, 0x46, 0x38}),
    // WEBP é RIFF????WEBP: os 4 bytes do tamanho no meio variam, então a
    // assinatura é conferida em duas partes (ver detectar)
    WEBP("image/webp", ".webp", new int[]{0x52, 0x49, 0x46, 0x46});

    private static final int[] SUFIXO_WEBP = {0x57, 0x45, 0x42, 0x50};

    private final String contentType;
    private final String extensao;
    private final int[] assinatura;

    TipoDeImagem(String contentType, String extensao, int[] assinatura) {
        this.contentType = contentType;
        this.extensao = extensao;
        this.assinatura = assinatura;
    }

    public String contentType() {
        return contentType;
    }

    public String extensao() {
        return extensao;
    }

    /** Vazio quando o conteúdo não é um dos formatos permitidos. */
    public static Optional<TipoDeImagem> detectar(byte[] conteudo) {
        return Arrays.stream(values())
                .filter(tipo -> tipo.combina(conteudo))
                .findFirst();
    }

    private boolean combina(byte[] conteudo) {
        if (conteudo == null || conteudo.length < assinatura.length) {
            return false;
        }
        for (var i = 0; i < assinatura.length; i++) {
            if ((conteudo[i] & 0xFF) != assinatura[i]) {
                return false;
            }
        }
        if (this != WEBP) {
            return true;
        }
        // RIFF + 4 bytes de tamanho + "WEBP"
        if (conteudo.length < 12) {
            return false;
        }
        for (var i = 0; i < SUFIXO_WEBP.length; i++) {
            if ((conteudo[8 + i] & 0xFF) != SUFIXO_WEBP[i]) {
                return false;
            }
        }
        return true;
    }
}

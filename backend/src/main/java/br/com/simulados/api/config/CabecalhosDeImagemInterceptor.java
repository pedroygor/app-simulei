package br.com.simulados.api.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Endurece as respostas do storage público de imagens.
 *
 * A validação de verdade acontece na INGESTÃO (ver ImagemStorageLocal: só
 * entram formatos raster da allowlist). Estes cabeçalhos são a segunda camada,
 * pro caso de algo entrar por outro caminho — um arquivo colocado no volume à
 * mão, uma migração futura, um bug nosso.
 */
public class CabecalhosDeImagemInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Impede o navegador de "adivinhar" o tipo e reinterpretar um arquivo
        // como HTML/JS. É o cabeçalho que neutraliza content sniffing.
        response.setHeader("X-Content-Type-Options", "nosniff");

        // Se apesar de tudo um conteúdo ativo for servido daqui, ele não
        // carrega nada e não executa nada: sem origem própria, sem scripts.
        response.setHeader("Content-Security-Policy", "default-src 'none'; sandbox; frame-ancestors 'none'");

        // Só o nosso próprio site pode embutir estes arquivos. Corta hotlinking
        // de terceiros (que sairia da NOSSA banda) e isola o recurso.
        // Atenção: se um dia o frontend ficar em domínio registrável diferente
        // do da API, este valor precisa virar cross-origin.
        response.setHeader("Cross-Origin-Resource-Policy", "same-site");
        return true;
    }
}

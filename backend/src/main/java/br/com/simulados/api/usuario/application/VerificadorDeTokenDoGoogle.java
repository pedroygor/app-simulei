package br.com.simulados.api.usuario.application;

import br.com.simulados.api.usuario.domain.CredencialGoogleInvalidaException;
import br.com.simulados.api.usuario.domain.LoginComGoogleIndisponivelException;

/**
 * Confere o ID token que o botão do Google entrega ao front. Interface na camada
 * de aplicação: o caso de uso não sabe o que é JWT nem JWKS, e o teste dele usa
 * um verificador falso, sem rede.
 */
@FunctionalInterface
public interface VerificadorDeTokenDoGoogle {

    /**
     * @throws CredencialGoogleInvalidaException  token forjado, vencido ou de outro site (401)
     * @throws LoginComGoogleIndisponivelException Google inacessível ou login com Google
     *                                             não configurado (503)
     */
    IdentidadeGoogle verificar(String credencial);
}

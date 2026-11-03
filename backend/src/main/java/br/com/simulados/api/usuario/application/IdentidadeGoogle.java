package br.com.simulados.api.usuario.application;

/**
 * Quem o Google diz que a pessoa é, já conferido.
 *
 * @param sub             id da pessoa no Google; não muda nunca (o e-mail muda)
 * @param email           como o Google o mandou, sem normalizar
 * @param emailVerificado se o Google confirmou que a pessoa é dona do e-mail.
 *                        Sem isso, ligar a conta pelo e-mail deixaria alguém tomar
 *                        a conta de outra pessoa
 * @param nome            nome do perfil Google; pode faltar
 */
public record IdentidadeGoogle(String sub, String email, boolean emailVerificado, String nome) {
}

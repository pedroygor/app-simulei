package br.com.simulados.api.usuario.domain;

/**
 * Papel do usuário. Um valor só desde que o PROFESSOR saiu (V13, 15/10/2026), mas
 * o enum fica: o papel viaja no JWT e vira authority, e é por aqui que entra um
 * papel novo (ex.: ADMIN) sem mexer no fluxo de autenticação.
 */
public enum Role {
    ALUNO
}

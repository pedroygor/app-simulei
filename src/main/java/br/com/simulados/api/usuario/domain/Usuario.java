package br.com.simulados.api.usuario.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
public class Usuario {

    // UUID gerado na aplicação (não pelo DEFAULT do banco): assim o id já existe
    // antes do INSERT e o Hibernate não precisa de um round-trip pra descobri-lo
    @Id
    @UuidGenerator
    private UUID id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "senha_hash", nullable = false)
    private String senhaHash;

    // STRING grava o nome do enum ("ALUNO"); o padrão ORDINAL gravaria a posição,
    // que quebra silenciosamente se a ordem dos valores mudar
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Plano plano = Plano.FREE;

    // Preenchido pelo Hibernate no INSERT; updatable=false impede que um
    // UPDATE posterior sobrescreva a data de criação
    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(nullable = false)
    private boolean ativo = true;
}

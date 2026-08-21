-- Tabela de usuários (seção 6 do CLAUDE.md) + coluna role para o Spring Security.
-- gen_random_uuid() é nativo do Postgres 13+; o id nasce no banco.
CREATE TABLE usuarios (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    nome        VARCHAR(120) NOT NULL,
    email       VARCHAR(255) NOT NULL,
    -- 255 e não 60: bcrypt gera 60 chars hoje, mas trocar de algoritmo
    -- no futuro não pode exigir ALTER TABLE
    senha_hash  VARCHAR(255) NOT NULL,
    plano       VARCHAR(10)  NOT NULL DEFAULT 'FREE',
    role        VARCHAR(20)  NOT NULL DEFAULT 'ALUNO',
    criado_em   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    ativo       BOOLEAN      NOT NULL DEFAULT TRUE,

    CONSTRAINT uk_usuarios_email UNIQUE (email),
    -- O CHECK protege contra escrita fora do enum vindo de qualquer origem
    -- (um UPDATE manual, um bug no código), não só da aplicação
    CONSTRAINT ck_usuarios_plano CHECK (plano IN ('FREE', 'PRO')),
    CONSTRAINT ck_usuarios_role  CHECK (role IN ('ALUNO', 'PROFESSOR'))
);

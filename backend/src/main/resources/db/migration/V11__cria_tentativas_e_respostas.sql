-- Tentativa = a EXECUÇÃO de um simulado por um aluno (o Simulado é a definição,
-- V10). Resposta = a alternativa que ele marcou em cada questão.
--
-- Divergências deliberadas da seção 6 do CLAUDE.md (decididas em 02/10/2026):
--   * status sem CRIADA: só tem sentido com turma ("prova atribuída, não aberta",
--     Etapa 5). Hoje iniciar já cria EM_ANDAMENTO; a Etapa 5 amplia o CHECK
--   * sem `nota`: é derivada das respostas e entra com a correção (3c)
--   * questão em branco = SEM linha em respostas, e alternativa_id NOT NULL.
--     Com nulo, "em branco" teria duas representações (sem linha / linha nula)
--   * sem `tempo_gasto_seg`: viria do cliente (não confiável) e nada o usa
--   * sem `id` em respostas: nada referencia uma resposta; a chave natural
--     (tentativa_id, questao_id) é a PK
CREATE TABLE tentativas (
    id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    -- Sem ON DELETE CASCADE de propósito: apagar um simulado nunca pode levar
    -- junto, em silêncio, o resultado de quem já fez a prova
    simulado_id    UUID        NOT NULL REFERENCES simulados (id),
    aluno_id       UUID        NOT NULL REFERENCES usuarios (id),
    status         VARCHAR(20) NOT NULL,
    iniciado_em    TIMESTAMPTZ NOT NULL DEFAULT now(),
    finalizado_em  TIMESTAMPTZ,

    -- "Um aluno faz cada simulado uma vez" (seção 6). Também é o que torna o
    -- iniciar idempotente: a segunda chamada acha a tentativa existente
    CONSTRAINT uk_tentativas_simulado_aluno UNIQUE (simulado_id, aluno_id),
    CONSTRAINT ck_tentativas_status CHECK (status IN ('EM_ANDAMENTO', 'FINALIZADA')),
    -- finalizado_em existe se, e só se, a tentativa foi finalizada
    CONSTRAINT ck_tentativas_finalizacao CHECK ((status = 'FINALIZADA') = (finalizado_em IS NOT NULL))
);

-- Histórico de provas do aluno (3d), da mais nova pra mais velha. A FK de
-- simulado_id já tem índice: é a 1ª coluna do UNIQUE acima
CREATE INDEX idx_tentativas_aluno ON tentativas (aluno_id, iniciado_em DESC);

-- Alvo da FK composta abaixo. `id` sozinho já é único; o par existe pra que o
-- banco consiga provar que a alternativa é DA questão respondida
ALTER TABLE alternativas
    ADD CONSTRAINT uk_alternativas_id_questao UNIQUE (id, questao_id);

CREATE TABLE respostas (
    tentativa_id    UUID        NOT NULL REFERENCES tentativas (id) ON DELETE CASCADE,
    questao_id      BIGINT      NOT NULL,
    alternativa_id  BIGINT      NOT NULL,
    -- Desnormalização deliberada (seção 6): calculada na hora da resposta, pro
    -- desempenho (Etapa 4) agregar sem juntar com o gabarito
    correta         BOOLEAN     NOT NULL,
    -- Hora do SERVIDOR, gravada a cada (re)resposta
    respondida_em   TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT pk_respostas PRIMARY KEY (tentativa_id, questao_id),
    -- A alternativa marcada pertence à questão respondida, garantido pelo banco
    -- (mesma ideia da FK composta do assunto na V4). Também implica que a
    -- questão existe, então não precisa de FK separada pra questoes
    CONSTRAINT fk_respostas_alternativa_da_questao
        FOREIGN KEY (alternativa_id, questao_id) REFERENCES alternativas (id, questao_id)
);

-- Lado que referencia da FK composta (lição da V10) e, de quebra, o caminho do
-- "acerto por assunto" da Etapa 4, que junta respostas com questoes
CREATE INDEX idx_respostas_questao ON respostas (questao_id, alternativa_id);

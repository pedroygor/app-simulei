-- Simulado = a DEFINIÇÃO da prova (quais questões, em que ordem, quanto tempo).
-- A EXECUÇÃO por um aluno é a Tentativa (3b). Separados desde o início: uma
-- prova de turma (Etapa 5) terá N tentativas pra 1 simulado.
--
-- Fora daqui, de propósito, o que a seção 6 do CLAUDE.md prevê pra depois:
-- `turma_id`, `disponivel_de` e `disponivel_ate` só fazem sentido com turma
-- (Etapa 5), e `turmas` ainda nem existe pra FK apontar. Entram numa migration
-- da Etapa 5, sem mexer nesta.
CREATE TABLE simulados (
    -- UUID e não identity: agregado criado por usuário, cujo id aparece na URL
    -- (/simulados/{id}). Sequencial deixaria adivinhar e contar os simulados alheios
    id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    criado_por        UUID         NOT NULL REFERENCES usuarios (id),
    titulo            VARCHAR(120) NOT NULL,
    -- NULL = sem limite de tempo (lista de treino, tarefa de casa)
    tempo_limite_seg  INTEGER,
    criado_em         TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT ck_simulados_titulo CHECK (length(trim(titulo)) > 0),
    -- 1 min a 6 h: um ENEM inteiro (dia de 90 questões) tem 5h30
    CONSTRAINT ck_simulados_tempo CHECK (tempo_limite_seg IS NULL OR tempo_limite_seg BETWEEN 60 AND 21600)
);

-- "Meus simulados" (3d) lista por dono, do mais novo pro mais velho
CREATE INDEX idx_simulados_criado_por ON simulados (criado_por, criado_em DESC);

-- As questões do simulado, na ordem em que aparecem. A ordem é coluna e não a
-- ordem de inserção (lição da V7: o banco não garante ordem de linha)
CREATE TABLE simulado_questoes (
    simulado_id  UUID     NOT NULL REFERENCES simulados (id) ON DELETE CASCADE,
    questao_id   BIGINT   NOT NULL REFERENCES questoes (id),
    ordem        SMALLINT NOT NULL,

    CONSTRAINT pk_simulado_questoes PRIMARY KEY (simulado_id, questao_id),
    -- Duas questões na mesma posição seria prova com numeração quebrada
    CONSTRAINT uk_simulado_questoes_ordem UNIQUE (simulado_id, ordem),
    -- 180 = um ENEM inteiro, o maior simulado que faz sentido
    CONSTRAINT ck_simulado_questoes_ordem CHECK (ordem BETWEEN 1 AND 180)
);

-- "Esta questão está em algum simulado?" — a FK pra questoes precisa de índice
-- do lado que referencia, senão todo DELETE/UPDATE de questão varre a tabela
CREATE INDEX idx_simulado_questoes_questao ON simulado_questoes (questao_id);

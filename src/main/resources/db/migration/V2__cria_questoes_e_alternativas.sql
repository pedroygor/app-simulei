-- Banco de questões importadas da enem.dev (seção 4 do CLAUDE.md).
-- BIGINT identity (não UUID): dado de referência importado em massa,
-- nunca criado por usuário — id compacto e sequencial serve melhor.
CREATE TABLE questoes (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ano         INT          NOT NULL,
    numero      INT          NOT NULL,
    -- NULL = questão comum; preenchido apenas nas questões 1-5 de linguagens,
    -- que existem em duas versões (inglês/espanhol) com o MESMO numero
    idioma      VARCHAR(10),
    area        VARCHAR(20)  NOT NULL,
    -- materia e assunto não vêm da enem.dev: são enriquecimento posterior
    materia     VARCHAR(40),
    assunto     VARCHAR(80),
    -- texto-base em markdown (imagens serão reescritas pro nosso storage no import)
    enunciado   TEXT         NOT NULL,
    -- "alternativesIntroduction" da fonte: a pergunta em si, depois do texto-base
    comando     TEXT,
    imagem_url  TEXT,
    -- VARCHAR(1) e não CHAR(1): o Hibernate mapeia String como varchar e o
    -- ddl-auto=validate reclamaria do tipo bpchar
    gabarito    VARCHAR(1),
    -- Parâmetro B da TRI (microdados INEP, etapa futura)
    dificuldade DOUBLE PRECISION,
    -- CO_HABILIDADE dos microdados (etapa futura)
    habilidade  SMALLINT,
    anulada     BOOLEAN      NOT NULL DEFAULT FALSE,
    fonte       VARCHAR(30)  NOT NULL,
    fonte_id    VARCHAR(60)  NOT NULL,

    -- NULLS NOT DISTINCT (Postgres 15+): sem isso, idioma NULL tornaria a
    -- constraint inútil pra questões comuns (NULL != NULL no padrão SQL)
    CONSTRAINT uk_questoes_ano_numero_idioma UNIQUE NULLS NOT DISTINCT (ano, numero, idioma),
    CONSTRAINT ck_questoes_idioma   CHECK (idioma IN ('INGLES', 'ESPANHOL')),
    CONSTRAINT ck_questoes_area     CHECK (area IN ('LINGUAGENS', 'CIENCIAS_HUMANAS', 'CIENCIAS_NATUREZA', 'MATEMATICA')),
    CONSTRAINT ck_questoes_gabarito CHECK (gabarito IN ('A', 'B', 'C', 'D', 'E')),
    -- Questão válida tem gabarito; anulada pode não ter
    CONSTRAINT ck_questoes_gabarito_obrigatorio CHECK (anulada OR gabarito IS NOT NULL)
);

-- Cobre o filtro mais comum da busca (área + ano, excluindo anuladas)
CREATE INDEX idx_questoes_area_ano ON questoes (area, ano) WHERE NOT anulada;

CREATE TABLE alternativas (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- ON DELETE CASCADE: alternativa não existe sem a questão
    questao_id  BIGINT     NOT NULL REFERENCES questoes (id) ON DELETE CASCADE,
    letra       VARCHAR(1) NOT NULL,
    texto       TEXT,
    imagem_url  TEXT,

    CONSTRAINT uk_alternativas_questao_letra UNIQUE (questao_id, letra),
    CONSTRAINT ck_alternativas_letra CHECK (letra IN ('A', 'B', 'C', 'D', 'E')),
    -- Alternativa pode ser só texto, só imagem, ou ambos — nunca nenhum
    CONSTRAINT ck_alternativas_conteudo CHECK (texto IS NOT NULL OR imagem_url IS NOT NULL)
);

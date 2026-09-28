-- Taxonomia área → matéria → assunto (docs/taxonomia.md, aprovada em 28/09/2026).
-- Estrutura aqui; os 118 assuntos entram na V5. Separadas porque mudam em
-- ritmos diferentes: assunto novo no futuro é uma migration só de INSERT.

-- INTEGER identity e não BIGINT: são ~130 linhas de dado de referência,
-- e o id vai numa FK de cada questão.
CREATE TABLE materias (
    id    INT         GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    area  VARCHAR(20) NOT NULL,
    -- slug é o identificador permanente (URL, CSV da classificação, saída
    -- da IA). O nome pode mudar; o slug não.
    slug  VARCHAR(40) NOT NULL,
    nome  VARCHAR(60) NOT NULL,

    CONSTRAINT uk_materias_slug UNIQUE (slug),
    -- Alvo da FK composta de assuntos: garante que o assunto herda a MESMA
    -- área da matéria dele
    CONSTRAINT uk_materias_id_area UNIQUE (id, area),
    CONSTRAINT ck_materias_area CHECK (area IN ('LINGUAGENS', 'CIENCIAS_HUMANAS', 'CIENCIAS_NATUREZA', 'MATEMATICA'))
);

CREATE TABLE assuntos (
    id            INT          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    materia_id    INT          NOT NULL,
    -- Repete a área da matéria DE PROPÓSITO: é o que permite ao banco
    -- garantir, via FK composta em questoes, que a questão só recebe assunto
    -- da própria área. As duas FKs compostas mantêm a cópia coerente.
    area          VARCHAR(20)  NOT NULL,
    slug          VARCHAR(60)  NOT NULL,
    nome          VARCHAR(120) NOT NULL,
    -- Grupo oficial de objetos de conhecimento da Matriz de Referência do
    -- INEP. Âncora pra conferir contra a habilidade oficial (H1–H30) no futuro
    grupo_matriz  VARCHAR(120) NOT NULL,

    CONSTRAINT uk_assuntos_slug UNIQUE (slug),
    CONSTRAINT uk_assuntos_id_area UNIQUE (id, area),
    CONSTRAINT fk_assuntos_materia FOREIGN KEY (materia_id, area) REFERENCES materias (id, area)
);

CREATE INDEX idx_assuntos_materia ON assuntos (materia_id);

-- materia e assunto eram texto livre desde a V2, reservados pra este
-- enriquecimento, e estão 100% nulos (a enem.dev não fornece). Texto livre
-- deixaria "Probabilidade" e "probabilidade" virarem dois assuntos.
ALTER TABLE questoes
    DROP COLUMN materia,
    DROP COLUMN assunto,
    -- Nullable: questão ainda não classificada. Um assunto PRINCIPAL por
    -- questão (decisão de 25/09/2026); a matéria sai do join, sem duplicar
    ADD COLUMN assunto_id INT,
    -- Quem classificou: a IA ou uma pessoa. A carga da IA nunca sobrescreve
    -- REVISAO_HUMANA
    ADD COLUMN classificacao_origem VARCHAR(20),
    -- Confiança declarada pela IA. Baixa = vai pra revisão
    ADD COLUMN classificacao_confianca VARCHAR(10),
    -- FK composta: assunto de outra área é recusado PELO BANCO. Uma questão
    -- de Matemática não recebe assunto de Química nem se um bug tentar
    ADD CONSTRAINT fk_questoes_assunto FOREIGN KEY (assunto_id, area) REFERENCES assuntos (id, area),
    ADD CONSTRAINT ck_questoes_classificacao_origem CHECK (classificacao_origem IN ('IA', 'REVISAO_HUMANA')),
    ADD CONSTRAINT ck_questoes_classificacao_confianca CHECK (classificacao_confianca IN ('ALTA', 'MEDIA', 'BAIXA')),
    -- Assunto sem origem (ou origem sem assunto) seria dado órfão
    ADD CONSTRAINT ck_questoes_classificacao_coerente CHECK ((assunto_id IS NULL) = (classificacao_origem IS NULL));

-- Cobre o filtro por assunto da busca, no mesmo recorte do índice da V2
CREATE INDEX idx_questoes_assunto ON questoes (assunto_id) WHERE NOT anulada;

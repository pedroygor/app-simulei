-- Prazo da tentativa (3d): até quando o aluno pode responder, se o simulado tem
-- tempo-limite. NULL = sem cronômetro.
--
-- Por que COLUNA e não calculado a partir do simulado a cada uso:
--   * o responder já trava a tentativa (SELECT ... FOR UPDATE): lê o prazo na
--     MESMA linha, sem juntar com simulados
--   * o encerramento automático vira um UPDATE só nesta tabela, com índice
--   * o prazo é um fato da tentativa: fixado ao iniciar, não muda depois
ALTER TABLE tentativas ADD COLUMN prazo TIMESTAMPTZ;

-- As tentativas que já existem ganham o prazo pelo mesmo cálculo do iniciar
UPDATE tentativas t
SET prazo = t.iniciado_em + make_interval(secs => s.tempo_limite_seg)
FROM simulados s
WHERE s.id = t.simulado_id AND s.tempo_limite_seg IS NOT NULL;

ALTER TABLE tentativas
    ADD CONSTRAINT ck_tentativas_prazo CHECK (prazo IS NULL OR prazo > iniciado_em);

-- O job de encerramento procura "em andamento com prazo vencido". Índice
-- PARCIAL: só as linhas que interessam a ele (prova aberta e com cronômetro),
-- que são uma fração mínima da tabela
CREATE INDEX idx_tentativas_prazo_em_andamento
    ON tentativas (prazo)
    WHERE status = 'EM_ANDAMENTO' AND prazo IS NOT NULL;

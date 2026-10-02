-- Questão INCOMPLETA: a fonte (enem.dev) não trouxe o texto-base ou a imagem de
-- que a pergunta depende, e o aluno não teria como responder. Decidido em
-- 01/10/2026: nunca entra em simulado nem aparece pro usuário — como a anulada.
--
-- Marcada, não apagada, pelo mesmo motivo da anulada (ADR de 30/09/2026): a
-- importação pula questão que já existe pela chave natural; apagada, ela voltaria
-- na próxima --importar-questoes. Quais são vem de dados/qualidade/
-- questoes_incompletas.csv, pela carga --marcar-incompletas (numa base nova as
-- questões chegam DEPOIS das migrations).
ALTER TABLE questoes ADD COLUMN incompleta BOOLEAN NOT NULL DEFAULT false;

-- Uma regra só pra "pode ser mostrada e sorteada". Coluna GERADA: o Postgres a
-- calcula a cada escrita e ninguém consegue gravá-la à mão. Toda consulta de
-- usuário (busca, detalhe, contagem por assunto, anos, e o sorteio da 3a) filtra
-- por ela; um motivo novo de exclusão no futuro muda AQUI, e nenhuma consulta
-- fica esquecida com o filtro velho
ALTER TABLE questoes
    ADD COLUMN disponivel BOOLEAN GENERATED ALWAYS AS (NOT anulada AND NOT incompleta) STORED;

-- Os índices parciais da V2 e da V4 eram "WHERE NOT anulada": passam a cobrir
-- exatamente o que as consultas filtram agora
DROP INDEX idx_questoes_area_ano;
CREATE INDEX idx_questoes_area_ano ON questoes (area, ano) WHERE disponivel;
DROP INDEX idx_questoes_assunto;
CREATE INDEX idx_questoes_assunto ON questoes (assunto_id) WHERE disponivel;

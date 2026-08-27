-- Ajuste exigido pelos dados reais da enem.dev: 144 das 2740 questões (5%)
-- não têm texto-base (`context` null) — o enunciado inteiro vive no comando
-- (`alternativesIntroduction`). Ex: 2009 q55, uma questão de história que é
-- só a pergunta, sem texto de apoio.
--
-- Em vez de inventar um enunciado ou descartar 5% do banco, a coluna passa a
-- ser nullable e o CHECK garante que ao menos um dos dois exista: questão sem
-- enunciado E sem comando não é questão.
ALTER TABLE questoes ALTER COLUMN enunciado DROP NOT NULL;

ALTER TABLE questoes ADD CONSTRAINT ck_questoes_tem_texto
    CHECK (enunciado IS NOT NULL OR comando IS NOT NULL);

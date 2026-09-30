-- Ordem explícita da taxonomia (docs/taxonomia.md, na sequência de estudo).
--
-- Por que existe: a V5 insere com INSERT ... SELECT ... FROM (VALUES ...) JOIN,
-- e o SQL não garante a ordem em que essas linhas saem. O id é identity, então
-- reflete a ordem em que o plano de execução produziu as linhas — no banco
-- local os assuntos ficaram INVERTIDOS (Cinemática por último na Física). Ordenar
-- por id era ordenar por acidente. Ordem é dado, então ganha coluna própria.
--
-- A V5 não pode ser corrigida no lugar: o Flyway grava o checksum dela e recusaria
-- qualquer banco onde já rodou. Os ids também ficam como estão (FK de questoes).
--
-- Preenchida por slug, com lista explícita e não derivada do id: derivar do id
-- repetiria o erro, e o resultado dependeria de como cada banco montou a V5.

ALTER TABLE materias ADD COLUMN ordem INT;
ALTER TABLE assuntos ADD COLUMN ordem INT;

-- Posição da matéria na taxonomia (1..14)
UPDATE materias m SET ordem = v.ordem
FROM (VALUES
    ('lingua-portuguesa', 1),
    ('literatura', 2),
    ('ingles', 3),
    ('espanhol', 4),
    ('artes', 5),
    ('educacao-fisica', 6),
    ('matematica', 7),
    ('fisica', 8),
    ('quimica', 9),
    ('biologia', 10),
    ('historia', 11),
    ('geografia', 12),
    ('filosofia', 13),
    ('sociologia', 14)
) AS v (slug, ordem)
WHERE m.slug = v.slug;

-- Posição do assunto DENTRO da matéria (1..n)
UPDATE assuntos a SET ordem = v.ordem
FROM (VALUES
    -- lingua-portuguesa
    ('interpretacao-de-texto', 1),
    ('generos-textuais', 2),
    ('funcoes-da-linguagem', 3),
    ('variacao-linguistica', 4),
    ('figuras-de-linguagem', 5),
    ('coesao-e-coerencia', 6),
    ('gramatica-aplicada', 7),
    ('argumentacao', 8),
    ('intertextualidade', 9),
    ('generos-digitais', 10),
    -- literatura
    ('literatura-colonial-e-arcadismo', 1),
    ('romantismo', 2),
    ('realismo-naturalismo-parnasianismo', 3),
    ('simbolismo-e-pre-modernismo', 4),
    ('modernismo', 5),
    ('literatura-contemporanea', 6),
    ('linguagem-literaria', 7),
    -- ingles
    ('interpretacao-ingles', 1),
    -- espanhol
    ('interpretacao-espanhol', 1),
    -- artes
    ('artes-visuais', 1),
    ('musica-teatro-danca', 2),
    ('arte-cultura-e-sociedade', 3),
    -- educacao-fisica
    ('praticas-corporais-e-saude', 1),
    ('esporte-e-sociedade', 2),
    ('corpo-e-expressao', 3),
    -- matematica
    ('operacoes-e-conjuntos-numericos', 1),
    ('razao-e-proporcao', 2),
    ('porcentagem-e-juros', 3),
    ('progressoes', 4),
    ('analise-combinatoria', 5),
    ('escalas-e-unidades', 6),
    ('geometria-plana', 7),
    ('geometria-espacial', 8),
    ('trigonometria', 9),
    ('visao-espacial', 10),
    ('leitura-de-graficos-e-tabelas', 11),
    ('estatistica', 12),
    ('probabilidade', 13),
    ('funcao-do-1-grau', 14),
    ('funcao-do-2-grau', 15),
    ('exponencial-e-logaritmo', 16),
    ('equacoes-e-inequacoes', 17),
    ('graficos-de-funcoes', 18),
    ('geometria-analitica', 19),
    ('sistemas-lineares', 20),
    -- fisica
    ('cinematica', 1),
    ('dinamica', 2),
    ('energia-trabalho-potencia', 3),
    ('hidrostatica', 4),
    ('gravitacao', 5),
    ('termologia', 6),
    ('termodinamica', 7),
    ('optica', 8),
    ('ondulatoria', 9),
    ('eletrostatica', 10),
    ('eletrodinamica', 11),
    ('eletromagnetismo', 12),
    ('fisica-moderna', 13),
    -- quimica
    ('atomistica-e-tabela-periodica', 1),
    ('ligacoes-e-propriedades', 2),
    ('separacao-de-misturas', 3),
    ('funcoes-inorganicas', 4),
    ('estequiometria', 5),
    ('solucoes', 6),
    ('termoquimica', 7),
    ('cinetica-quimica', 8),
    ('equilibrio-quimico-e-ph', 9),
    ('eletroquimica', 10),
    ('quimica-organica', 11),
    ('reacoes-organicas-e-polimeros', 12),
    ('radioatividade', 13),
    ('quimica-ambiental', 14),
    -- biologia
    ('citologia', 1),
    ('metabolismo-energetico', 2),
    ('genetica', 3),
    ('biotecnologia', 4),
    ('evolucao', 5),
    ('ecologia', 6),
    ('impactos-ambientais', 7),
    ('fisiologia-humana', 8),
    ('saude-e-doencas', 9),
    ('botanica', 10),
    ('zoologia', 11),
    ('reproducao-e-embriologia', 12),
    -- historia
    ('antiguidade', 1),
    ('idade-media', 2),
    ('idade-moderna', 3),
    ('revolucoes-dos-seculos-18-e-19', 4),
    ('brasil-colonia', 5),
    ('brasil-imperio', 6),
    ('brasil-republica', 7),
    ('ditadura-e-redemocratizacao', 8),
    ('seculo-20-mundial', 9),
    ('africa-e-cultura-afro-brasileira', 10),
    ('povos-indigenas', 11),
    ('patrimonio-e-memoria', 12),
    -- geografia
    ('cartografia', 1),
    ('geologia-e-relevo', 2),
    ('clima', 3),
    ('hidrografia', 4),
    ('biomas-e-dominios-naturais', 5),
    ('questoes-ambientais', 6),
    ('urbanizacao', 7),
    ('espaco-agrario', 8),
    ('industria-e-energia', 9),
    ('populacao-e-migracoes', 10),
    ('globalizacao-e-geopolitica', 11),
    -- filosofia
    ('filosofia-antiga', 1),
    ('filosofia-medieval-e-moderna', 2),
    ('filosofia-contemporanea', 3),
    ('etica-e-moral', 4),
    ('filosofia-politica', 5),
    -- sociologia
    ('cultura-e-identidade', 1),
    ('trabalho-e-sociedade', 2),
    ('cidadania-e-movimentos-sociais', 3),
    ('estado-e-democracia', 4),
    ('teoria-sociologica', 5),
    ('desigualdade-e-estratificacao', 6)
) AS v (slug, ordem)
WHERE a.slug = v.slug;

-- Autoconferência: o UPDATE ... FROM deixa sem ordem, em silêncio, qualquer linha
-- cujo slug não esteja na lista. Falha alto aqui, e o Flyway desfaz a transação.
DO $$
DECLARE
    materias_sem_ordem INT := (SELECT count(*) FROM materias WHERE ordem IS NULL);
    assuntos_sem_ordem INT := (SELECT count(*) FROM assuntos WHERE ordem IS NULL);
BEGIN
    IF materias_sem_ordem <> 0 OR assuntos_sem_ordem <> 0 THEN
        RAISE EXCEPTION 'Ordem incompleta: % matérias e % assuntos sem ordem',
            materias_sem_ordem, assuntos_sem_ordem;
    END IF;
END $$;

-- NOT NULL + UNIQUE: assunto novo numa migration futura tem que dizer onde entra,
-- e duas linhas não disputam a mesma posição.
ALTER TABLE materias
    ALTER COLUMN ordem SET NOT NULL,
    ADD CONSTRAINT uk_materias_ordem UNIQUE (ordem);

ALTER TABLE assuntos
    ALTER COLUMN ordem SET NOT NULL,
    ADD CONSTRAINT uk_assuntos_materia_ordem UNIQUE (materia_id, ordem);

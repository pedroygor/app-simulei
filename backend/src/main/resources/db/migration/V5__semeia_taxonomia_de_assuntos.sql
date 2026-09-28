-- Os 118 assuntos de docs/taxonomia.md (aprovada em 28/09/2026).
-- Dado de referência: igual em todo ambiente, por isso é migration e não
-- carga manual. Mudança futura = nova migration (Flyway recusa editar esta
-- depois de aplicada — o checksum dela fica gravado).

INSERT INTO materias (area, slug, nome) VALUES
    ('LINGUAGENS',        'lingua-portuguesa', 'Língua Portuguesa'),
    ('LINGUAGENS',        'literatura',        'Literatura'),
    ('LINGUAGENS',        'ingles',            'Inglês'),
    ('LINGUAGENS',        'espanhol',          'Espanhol'),
    ('LINGUAGENS',        'artes',             'Artes'),
    ('LINGUAGENS',        'educacao-fisica',   'Educação Física'),
    ('MATEMATICA',        'matematica',        'Matemática'),
    ('CIENCIAS_NATUREZA', 'fisica',            'Física'),
    ('CIENCIAS_NATUREZA', 'quimica',           'Química'),
    ('CIENCIAS_NATUREZA', 'biologia',          'Biologia'),
    ('CIENCIAS_HUMANAS',  'historia',          'História'),
    ('CIENCIAS_HUMANAS',  'geografia',         'Geografia'),
    ('CIENCIAS_HUMANAS',  'filosofia',         'Filosofia'),
    ('CIENCIAS_HUMANAS',  'sociologia',        'Sociologia');

-- A área do assunto vem da matéria (m.area), nunca digitada: é o que mantém
-- a cópia de área coerente com a FK composta da V4.
INSERT INTO assuntos (materia_id, area, slug, nome, grupo_matriz)
SELECT m.id, m.area, v.slug, v.nome, v.grupo
FROM (VALUES
    -- Língua Portuguesa
    ('lingua-portuguesa', 'interpretacao-de-texto', 'Interpretação e compreensão de texto', 'Estudo do texto'),
    ('lingua-portuguesa', 'generos-textuais', 'Gêneros textuais e suas finalidades', 'Estudo do texto'),
    ('lingua-portuguesa', 'funcoes-da-linguagem', 'Funções da linguagem', 'Estudo do texto'),
    ('lingua-portuguesa', 'variacao-linguistica', 'Variação linguística e norma', 'Estudo dos aspectos linguísticos'),
    ('lingua-portuguesa', 'figuras-de-linguagem', 'Figuras de linguagem e recursos expressivos', 'Estudo dos aspectos linguísticos'),
    ('lingua-portuguesa', 'coesao-e-coerencia', 'Coesão, coerência e articulação de ideias', 'Estudo dos aspectos linguísticos'),
    ('lingua-portuguesa', 'gramatica-aplicada', 'Gramática no texto (classes, sintaxe, pontuação)', 'Estudo dos aspectos linguísticos'),
    ('lingua-portuguesa', 'argumentacao', 'Argumentação e pontos de vista', 'Estudo do texto argumentativo'),
    ('lingua-portuguesa', 'intertextualidade', 'Intertextualidade', 'Estudo do texto'),
    ('lingua-portuguesa', 'generos-digitais', 'Gêneros digitais e tecnologias da comunicação', 'Estudo dos gêneros digitais'),
    -- Literatura
    ('literatura', 'literatura-colonial-e-arcadismo', 'Quinhentismo, Barroco e Arcadismo', 'Estudo do texto literário'),
    ('literatura', 'romantismo', 'Romantismo', 'Estudo do texto literário'),
    ('literatura', 'realismo-naturalismo-parnasianismo', 'Realismo, Naturalismo e Parnasianismo', 'Estudo do texto literário'),
    ('literatura', 'simbolismo-e-pre-modernismo', 'Simbolismo e Pré-Modernismo', 'Estudo do texto literário'),
    ('literatura', 'modernismo', 'Modernismo', 'Estudo do texto literário'),
    ('literatura', 'literatura-contemporanea', 'Literatura contemporânea', 'Estudo do texto literário'),
    ('literatura', 'linguagem-literaria', 'Linguagem literária e recursos poéticos', 'Estudo do texto literário'),
    -- Inglês e Espanhol
    ('ingles', 'interpretacao-ingles', 'Interpretação de texto em inglês', 'Estudo do texto'),
    ('espanhol', 'interpretacao-espanhol', 'Interpretação de texto em espanhol', 'Estudo do texto'),
    -- Artes
    ('artes', 'artes-visuais', 'Artes visuais e movimentos artísticos', 'Produção e recepção de textos artísticos'),
    ('artes', 'musica-teatro-danca', 'Música, teatro e dança', 'Produção e recepção de textos artísticos'),
    ('artes', 'arte-cultura-e-sociedade', 'Arte, patrimônio e diversidade cultural', 'Produção e recepção de textos artísticos'),
    -- Educação Física
    ('educacao-fisica', 'praticas-corporais-e-saude', 'Exercício físico, saúde e qualidade de vida', 'Estudo das práticas corporais'),
    ('educacao-fisica', 'esporte-e-sociedade', 'Esporte, lazer e sociedade', 'Estudo das práticas corporais'),
    ('educacao-fisica', 'corpo-e-expressao', 'Corpo, dança e expressão cultural', 'Estudo das práticas corporais'),
    -- Matemática
    ('matematica', 'operacoes-e-conjuntos-numericos', 'Operações, conjuntos numéricos, divisibilidade', 'Conhecimentos numéricos'),
    ('matematica', 'razao-e-proporcao', 'Razão, proporção e regra de três', 'Conhecimentos numéricos'),
    ('matematica', 'porcentagem-e-juros', 'Porcentagem e matemática financeira', 'Conhecimentos numéricos'),
    ('matematica', 'progressoes', 'Sequências e progressões (PA e PG)', 'Conhecimentos numéricos'),
    ('matematica', 'analise-combinatoria', 'Análise combinatória e princípio da contagem', 'Conhecimentos numéricos'),
    ('matematica', 'escalas-e-unidades', 'Grandezas, unidades de medida e escalas', 'Conhecimentos geométricos'),
    ('matematica', 'geometria-plana', 'Geometria plana: áreas, perímetros, semelhança', 'Conhecimentos geométricos'),
    ('matematica', 'geometria-espacial', 'Geometria espacial: volumes e sólidos', 'Conhecimentos geométricos'),
    ('matematica', 'trigonometria', 'Trigonometria (triângulo e ciclo)', 'Conhecimentos geométricos'),
    ('matematica', 'visao-espacial', 'Visão espacial, projeções e planificações', 'Conhecimentos geométricos'),
    ('matematica', 'leitura-de-graficos-e-tabelas', 'Leitura e interpretação de gráficos e tabelas', 'Conhecimentos de estatística e probabilidade'),
    ('matematica', 'estatistica', 'Estatística: média, moda, mediana, desvio', 'Conhecimentos de estatística e probabilidade'),
    ('matematica', 'probabilidade', 'Probabilidade', 'Conhecimentos de estatística e probabilidade'),
    ('matematica', 'funcao-do-1-grau', 'Função do 1º grau', 'Conhecimentos algébricos'),
    ('matematica', 'funcao-do-2-grau', 'Função do 2º grau', 'Conhecimentos algébricos'),
    ('matematica', 'exponencial-e-logaritmo', 'Função exponencial e logaritmo', 'Conhecimentos algébricos'),
    ('matematica', 'equacoes-e-inequacoes', 'Equações, inequações e expressões algébricas', 'Conhecimentos algébricos'),
    ('matematica', 'graficos-de-funcoes', 'Gráficos e análise de funções', 'Conhecimentos algébricos'),
    ('matematica', 'geometria-analitica', 'Geometria analítica: plano cartesiano, retas, circunferência', 'Conhecimentos algébricos/geométricos'),
    ('matematica', 'sistemas-lineares', 'Sistemas de equações', 'Conhecimentos algébricos/geométricos'),
    -- Física
    ('fisica', 'cinematica', 'Cinemática: velocidade, aceleração, movimentos', 'O movimento, o equilíbrio e a descoberta de leis físicas'),
    ('fisica', 'dinamica', 'Dinâmica: leis de Newton e forças', 'O movimento, o equilíbrio e a descoberta de leis físicas'),
    ('fisica', 'energia-trabalho-potencia', 'Energia, trabalho, potência e conservação', 'Energia, trabalho e potência'),
    ('fisica', 'hidrostatica', 'Hidrostática: pressão, empuxo', 'O movimento, o equilíbrio e a descoberta de leis físicas'),
    ('fisica', 'gravitacao', 'Gravitação e movimento dos astros', 'A Mecânica e o funcionamento do Universo'),
    ('fisica', 'termologia', 'Calor, temperatura e calorimetria', 'O calor e os fenômenos térmicos'),
    ('fisica', 'termodinamica', 'Termodinâmica e máquinas térmicas', 'O calor e os fenômenos térmicos'),
    ('fisica', 'optica', 'Óptica: reflexão, refração, lentes, espelhos', 'Oscilações, ondas, óptica e radiação'),
    ('fisica', 'ondulatoria', 'Ondas e acústica', 'Oscilações, ondas, óptica e radiação'),
    ('fisica', 'eletrostatica', 'Eletrostática: carga e campo elétrico', 'Fenômenos elétricos e magnéticos'),
    ('fisica', 'eletrodinamica', 'Eletrodinâmica: circuitos, potência e consumo elétrico', 'Fenômenos elétricos e magnéticos'),
    ('fisica', 'eletromagnetismo', 'Magnetismo e indução eletromagnética', 'Fenômenos elétricos e magnéticos'),
    ('fisica', 'fisica-moderna', 'Física moderna e radiações', 'Oscilações, ondas, óptica e radiação'),
    -- Química
    ('quimica', 'atomistica-e-tabela-periodica', 'Modelos atômicos e tabela periódica', 'Transformações químicas'),
    ('quimica', 'ligacoes-e-propriedades', 'Ligações químicas, polaridade e propriedades dos materiais', 'Materiais, suas propriedades e usos'),
    ('quimica', 'separacao-de-misturas', 'Substâncias, misturas e separação', 'Materiais, suas propriedades e usos'),
    ('quimica', 'funcoes-inorganicas', 'Ácidos, bases, sais e óxidos', 'Água'),
    ('quimica', 'estequiometria', 'Reações, balanceamento e estequiometria', 'Representação das transformações químicas'),
    ('quimica', 'solucoes', 'Soluções e concentração', 'Água'),
    ('quimica', 'termoquimica', 'Termoquímica', 'Transformações químicas e energia'),
    ('quimica', 'cinetica-quimica', 'Cinética química', 'Dinâmica das transformações químicas'),
    ('quimica', 'equilibrio-quimico-e-ph', 'Equilíbrio químico, pH e pOH', 'Transformação química e equilíbrio'),
    ('quimica', 'eletroquimica', 'Eletroquímica: pilhas, eletrólise, oxirredução', 'Transformações químicas e energia'),
    ('quimica', 'quimica-organica', 'Funções orgânicas e isomeria', 'Compostos de carbono'),
    ('quimica', 'reacoes-organicas-e-polimeros', 'Reações orgânicas e polímeros', 'Compostos de carbono'),
    ('quimica', 'radioatividade', 'Radioatividade e energia nuclear', 'Energias químicas no cotidiano'),
    ('quimica', 'quimica-ambiental', 'Química ambiental, combustíveis e tratamento de água', 'Relações da química com tecnologias, sociedade e meio ambiente'),
    -- Biologia
    ('biologia', 'citologia', 'Citologia: célula e organelas', 'Moléculas, células e tecidos'),
    ('biologia', 'metabolismo-energetico', 'Fotossíntese, respiração e fermentação', 'Moléculas, células e tecidos'),
    ('biologia', 'genetica', 'Genética e hereditariedade', 'Hereditariedade e diversidade da vida'),
    ('biologia', 'biotecnologia', 'Biotecnologia, DNA e engenharia genética', 'Hereditariedade e diversidade da vida'),
    ('biologia', 'evolucao', 'Evolução e origem da vida', 'Origem e evolução da vida'),
    ('biologia', 'ecologia', 'Ecologia: cadeias, relações e ciclos biogeoquímicos', 'Ecologia e ciências ambientais'),
    ('biologia', 'impactos-ambientais', 'Problemas ambientais e conservação', 'Ecologia e ciências ambientais'),
    ('biologia', 'fisiologia-humana', 'Fisiologia e anatomia humana', 'Identidade dos seres vivos'),
    ('biologia', 'saude-e-doencas', 'Saúde, doenças, vírus, bactérias e parasitas', 'Qualidade de vida das populações humanas'),
    ('biologia', 'botanica', 'Botânica', 'Identidade dos seres vivos'),
    ('biologia', 'zoologia', 'Zoologia e classificação dos seres vivos', 'Identidade dos seres vivos'),
    ('biologia', 'reproducao-e-embriologia', 'Reprodução e embriologia', 'Identidade dos seres vivos'),
    -- História
    ('historia', 'antiguidade', 'Antiguidade: Grécia, Roma e povos antigos', 'Formas de organização social e pensamento político'),
    ('historia', 'idade-media', 'Idade Média', 'Características e transformações das estruturas produtivas'),
    ('historia', 'idade-moderna', 'Renascimento, Reformas, Absolutismo e Iluminismo', 'Formas de organização social e pensamento político'),
    ('historia', 'revolucoes-dos-seculos-18-e-19', 'Revoluções Industrial, Francesa e liberais', 'Características e transformações das estruturas produtivas'),
    ('historia', 'brasil-colonia', 'Brasil Colônia e escravidão', 'Diversidade cultural, conflitos e vida em sociedade'),
    ('historia', 'brasil-imperio', 'Brasil Império', 'Formas de organização social e pensamento político'),
    ('historia', 'brasil-republica', 'Brasil República: da Primeira República à Era Vargas', 'Formas de organização social e pensamento político'),
    ('historia', 'ditadura-e-redemocratizacao', 'Ditadura militar e redemocratização', 'Formas de organização social e pensamento político'),
    ('historia', 'seculo-20-mundial', 'Guerras mundiais, totalitarismos e Guerra Fria', 'Formas de organização social e pensamento político'),
    ('historia', 'africa-e-cultura-afro-brasileira', 'História da África e cultura afro-brasileira', 'Diversidade cultural, conflitos e vida em sociedade'),
    ('historia', 'povos-indigenas', 'Povos indígenas e colonização da América', 'Diversidade cultural, conflitos e vida em sociedade'),
    ('historia', 'patrimonio-e-memoria', 'Patrimônio, memória e cultura material', 'Diversidade cultural, conflitos e vida em sociedade'),
    -- Geografia
    ('geografia', 'cartografia', 'Cartografia e representação espacial', 'Representação espacial'),
    ('geografia', 'geologia-e-relevo', 'Estrutura da Terra, solos e relevo', 'Os domínios naturais e a relação do ser humano com o ambiente'),
    ('geografia', 'clima', 'Clima e fenômenos atmosféricos', 'Os domínios naturais e a relação do ser humano com o ambiente'),
    ('geografia', 'hidrografia', 'Hidrografia e recursos hídricos', 'Os domínios naturais e a relação do ser humano com o ambiente'),
    ('geografia', 'biomas-e-dominios-naturais', 'Biomas e domínios morfoclimáticos', 'Os domínios naturais e a relação do ser humano com o ambiente'),
    ('geografia', 'questoes-ambientais', 'Questões ambientais e sustentabilidade', 'Os domínios naturais e a relação do ser humano com o ambiente'),
    ('geografia', 'urbanizacao', 'Urbanização e questões urbanas', 'Características e transformações das estruturas produtivas'),
    ('geografia', 'espaco-agrario', 'Espaço agrário e questão fundiária', 'Características e transformações das estruturas produtivas'),
    ('geografia', 'industria-e-energia', 'Indústria, energia e transportes', 'Características e transformações das estruturas produtivas'),
    ('geografia', 'populacao-e-migracoes', 'População e migrações', 'Diversidade cultural, conflitos e vida em sociedade'),
    ('geografia', 'globalizacao-e-geopolitica', 'Globalização, geopolítica e conflitos', 'Formas de organização social e pensamento político'),
    -- Filosofia
    ('filosofia', 'filosofia-antiga', 'Filosofia antiga', 'Formas de organização social e pensamento político'),
    ('filosofia', 'filosofia-medieval-e-moderna', 'Filosofia medieval e moderna', 'Formas de organização social e pensamento político'),
    ('filosofia', 'filosofia-contemporanea', 'Filosofia contemporânea', 'Formas de organização social e pensamento político'),
    ('filosofia', 'etica-e-moral', 'Ética e moral', 'Diversidade cultural, conflitos e vida em sociedade'),
    ('filosofia', 'filosofia-politica', 'Filosofia política: Estado, poder e contrato social', 'Formas de organização social e pensamento político'),
    -- Sociologia
    ('sociologia', 'cultura-e-identidade', 'Cultura, identidade e diversidade', 'Diversidade cultural, conflitos e vida em sociedade'),
    ('sociologia', 'trabalho-e-sociedade', 'Trabalho, produção e sociedade', 'Características e transformações das estruturas produtivas'),
    ('sociologia', 'cidadania-e-movimentos-sociais', 'Cidadania, direitos e movimentos sociais', 'Formas de organização social e pensamento político'),
    ('sociologia', 'estado-e-democracia', 'Estado, poder e democracia', 'Formas de organização social e pensamento político'),
    ('sociologia', 'teoria-sociologica', 'Pensadores clássicos: Durkheim, Weber, Marx', 'Formas de organização social e pensamento político'),
    ('sociologia', 'desigualdade-e-estratificacao', 'Desigualdade, violência e exclusão social', 'Diversidade cultural, conflitos e vida em sociedade')
) AS v (materia, slug, nome, grupo)
JOIN materias m ON m.slug = v.materia;

-- Autoconferência. O JOIN acima DESCARTA EM SILÊNCIO qualquer assunto cujo
-- slug de matéria esteja errado — 117 assuntos entrariam e ninguém notaria.
-- Aqui a migration falha alto, e o Flyway desfaz a transação inteira.
DO $$
DECLARE
    total_materias INT := (SELECT count(*) FROM materias);
    total_assuntos INT := (SELECT count(*) FROM assuntos);
BEGIN
    IF total_materias <> 14 OR total_assuntos <> 118 THEN
        RAISE EXCEPTION 'Taxonomia incompleta: % matérias (esperado 14), % assuntos (esperado 118)',
            total_materias, total_assuntos;
    END IF;
END $$;

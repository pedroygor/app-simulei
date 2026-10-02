-- Matriz de Referência do ENEM (INEP): as 30 competências de área e as 120
-- habilidades (H1–H30 em cada uma das 4 áreas), com o texto oficial.
--
-- Por que tabela e não só o número em questoes.habilidade (V2): o número sozinho
-- não diz nada, e nem é único — a H17 de Matemática não é a H17 de Natureza. A
-- chave é (area, codigo), e a FK composta abaixo garante que a questão só aponta
-- pra habilidade da PRÓPRIA área, do mesmo jeito que a V4 faz com o assunto.
--
-- O texto foi tirado do PDF da matriz (microdados 2023, matriz_referencia_enem.pdf)
-- com três correções de extração: H27 de Matemática (a última palavra caiu na
-- quebra de página), H7 e H30 de Humanas (ponto final; o rodapé "ANEXO" colado).
--
-- Aqui entra só a MATRIZ. Qual habilidade cada questão tem é dado importado depois
-- das migrations, então vem pela carga --importar-habilidades, não por UPDATE aqui
-- (numa base nova o UPDATE não acharia questão nenhuma — a lição da V6).

CREATE TABLE competencias (
    area       VARCHAR(20) NOT NULL,
    numero     SMALLINT    NOT NULL,
    descricao  TEXT        NOT NULL,

    CONSTRAINT pk_competencias PRIMARY KEY (area, numero),
    CONSTRAINT ck_competencias_area CHECK (area IN ('LINGUAGENS', 'CIENCIAS_HUMANAS', 'CIENCIAS_NATUREZA', 'MATEMATICA')),
    CONSTRAINT ck_competencias_numero CHECK (numero BETWEEN 1 AND 9)
);

CREATE TABLE habilidades (
    area         VARCHAR(20) NOT NULL,
    codigo       SMALLINT    NOT NULL,
    competencia  SMALLINT    NOT NULL,
    descricao    TEXT        NOT NULL,

    CONSTRAINT pk_habilidades PRIMARY KEY (area, codigo),
    CONSTRAINT ck_habilidades_codigo CHECK (codigo BETWEEN 1 AND 30),
    -- A habilidade pertence a uma competência da MESMA área
    CONSTRAINT fk_habilidades_competencia FOREIGN KEY (area, competencia) REFERENCES competencias (area, numero)
);

INSERT INTO competencias (area, numero, descricao) VALUES
    ('LINGUAGENS', 1, 'Aplicar as tecnologias da comunicação e da informação na escola, no trabalho e em outros contextos relevantes para sua vida.'),
    ('LINGUAGENS', 2, 'Conhecer e usar língua(s) estrangeira(s) moderna(s) como instrumento de acesso a informações e a outras culturas e grupos sociais.'),
    ('LINGUAGENS', 3, 'Compreender e usar a linguagem corporal como relevante para a própria vida, integradora social e formadora da identidade.'),
    ('LINGUAGENS', 4, 'Compreender a arte como saber cultural e estético gerador de significação e integrador da organização do mundo e da própria identidade.'),
    ('LINGUAGENS', 5, 'Analisar, interpretar e aplicar recursos expressivos das linguagens, relacionando textos com seus contextos, mediante a natureza, função, organização, estrutura das manifestações, de acordo com as condições de produção e recepção.'),
    ('LINGUAGENS', 6, 'Compreender e usar os sistemas simbólicos das diferentes linguagens como meios de organização cognitiva da realidade pela constituição de significados, expressão, comunicação e informação.'),
    ('LINGUAGENS', 7, 'Confrontar opiniões e pontos de vista sobre as diferentes linguagens e suas manifestações específicas.'),
    ('LINGUAGENS', 8, 'Compreender e usar a língua portuguesa como língua materna, geradora de significação e integradora da organização do mundo e da própria identidade.'),
    ('LINGUAGENS', 9, 'Entender os princípios, a natureza, a função e o impacto das tecnologias da comunicação e da informação na sua vida pessoal e social, no desenvolvimento do conhecimento, associando-o aos conhecimentos científicos, às linguagens que lhes dão suporte, às demais tecnologias, aos processos de produção e aos problemas que se propõem solucionar.'),
    ('MATEMATICA', 1, 'Construir significados para os números naturais, inteiros, racionais e reais.'),
    ('MATEMATICA', 2, 'Utilizar o conhecimento geométrico para realizar a leitura e a representação da realidade e agir sobre ela.'),
    ('MATEMATICA', 3, 'Construir noções de grandezas e medidas para a compreensão da realidade e a solução de problemas do cotidiano.'),
    ('MATEMATICA', 4, 'Construir noções de variação de grandezas para a compreensão da realidade e a solução de problemas do cotidiano.'),
    ('MATEMATICA', 5, 'Modelar e resolver problemas que envolvem variáveis socioeconômicas ou técnico-científicas, usando representações algébricas.'),
    ('MATEMATICA', 6, 'Interpretar informações de natureza científica e social obtidas da leitura de gráficos e tabelas, realizando previsão de tendência, extrapolação, interpolação e interpretação.'),
    ('MATEMATICA', 7, 'Compreender o caráter aleatório e não-determinístico dos fenômenos naturais e sociais e utilizar instrumentos adequados para medidas, determinação de amostras e cálculos de probabilidade para interpretar informações de variáveis apresentadas em uma distribuição estatística.'),
    ('CIENCIAS_NATUREZA', 1, 'Compreender as ciências naturais e as tecnologias a elas associadas como construções humanas, percebendo seus papéis nos processos de produção e no desenvolvimento econômico e social da humanidade.'),
    ('CIENCIAS_NATUREZA', 2, 'Identificar a presença e aplicar as tecnologias associadas às ciências naturais em diferentes contextos.'),
    ('CIENCIAS_NATUREZA', 3, 'Associar intervenções que resultam em degradação ou conservação ambiental a processos produtivos e sociais e a instrumentos ou ações científico-tecnológicos.'),
    ('CIENCIAS_NATUREZA', 4, 'Compreender interações entre organismos e ambiente, em particular aquelas relacionadas à saúde humana, relacionando conhecimentos científicos, aspectos culturais e características individuais.'),
    ('CIENCIAS_NATUREZA', 5, 'Entender métodos e procedimentos próprios das ciências naturais e aplicá-los em diferentes contextos.'),
    ('CIENCIAS_NATUREZA', 6, 'Apropriar-se de conhecimentos da física para, em situações problema, interpretar, avaliar ou planejar intervenções científico-tecnológicas.'),
    ('CIENCIAS_NATUREZA', 7, 'Apropriar-se de conhecimentos da química para, em situações problema, interpretar, avaliar ou planejar intervenções científico-tecnológicas.'),
    ('CIENCIAS_NATUREZA', 8, 'Apropriar-se de conhecimentos da biologia para, em situações problema, interpretar, avaliar ou planejar intervenções científico-tecnológicas.'),
    ('CIENCIAS_HUMANAS', 1, 'Compreender os elementos culturais que constituem as identidades.'),
    ('CIENCIAS_HUMANAS', 2, 'Compreender as transformações dos espaços geográficos como produto das relações socioeconômicas e culturais de poder.'),
    ('CIENCIAS_HUMANAS', 3, 'Compreender a produção e o papel histórico das instituições sociais, políticas e econômicas, associando-as aos diferentes grupos, conflitos e movimentos sociais.'),
    ('CIENCIAS_HUMANAS', 4, 'Entender as transformações técnicas e tecnológicas e seu impacto nos processos de produção, no desenvolvimento do conhecimento e na vida social.'),
    ('CIENCIAS_HUMANAS', 5, 'Utilizar os conhecimentos históricos para compreender e valorizar os fundamentos da cidadania e da democracia, favorecendo uma atuação consciente do indivíduo na sociedade.'),
    ('CIENCIAS_HUMANAS', 6, 'Compreender a sociedade e a natureza, reconhecendo suas interações no espaço em diferentes contextos históricos e geográficos.');

INSERT INTO habilidades (area, codigo, competencia, descricao) VALUES
    ('LINGUAGENS', 1, 1, 'Identificar as diferentes linguagens e seus recursos expressivos como elementos de caracterização dos sistemas de comunicação.'),
    ('LINGUAGENS', 2, 1, 'Recorrer aos conhecimentos sobre as linguagens dos sistemas de comunicação e informação para resolver problemas sociais.'),
    ('LINGUAGENS', 3, 1, 'Relacionar informações geradas nos sistemas de comunicação e informação, considerando a função social desses sistemas.'),
    ('LINGUAGENS', 4, 1, 'Reconhecer posições críticas aos usos sociais que são feitos das linguagens e dos sistemas de comunicação e informação.'),
    ('LINGUAGENS', 5, 2, 'Associar vocábulos e expressões de um texto em LEM ao seu tema.'),
    ('LINGUAGENS', 6, 2, 'Utilizar os conhecimentos da LEM e de seus mecanismos como meio de ampliar as possibilidades de acesso a informações, tecnologias e culturas.'),
    ('LINGUAGENS', 7, 2, 'Relacionar um texto em LEM, as estruturas linguísticas, sua função e seu uso social.'),
    ('LINGUAGENS', 8, 2, 'Reconhecer a importância da produção cultural em LEM como representação da diversidade cultural e linguística.'),
    ('LINGUAGENS', 9, 3, 'Reconhecer as manifestações corporais de movimento como originárias de necessidades cotidianas de um grupo social.'),
    ('LINGUAGENS', 10, 3, 'Reconhecer a necessidade de transformação de hábitos corporais em função das necessidades cinestésicas.'),
    ('LINGUAGENS', 11, 3, 'Reconhecer a linguagem corporal como meio de interação social, considerando os limites de desempenho e as alternativas de adaptação para diferentes indivíduos.'),
    ('LINGUAGENS', 12, 4, 'Reconhecer diferentes funções da arte, do trabalho da produção dos artistas em seus meios culturais.'),
    ('LINGUAGENS', 13, 4, 'Analisar as diversas produções artísticas como meio de explicar diferentes culturas, padrões de beleza e preconceitos.'),
    ('LINGUAGENS', 14, 4, 'Reconhecer o valor da diversidade artística e das inter-relações de elementos que se apresentam nas manifestações de vários grupos sociais e étnicos.'),
    ('LINGUAGENS', 15, 5, 'Estabelecer relações entre o texto literário e o momento de sua produção, situando aspectos do contexto histórico, social e político.'),
    ('LINGUAGENS', 16, 5, 'Relacionar informações sobre concepções artísticas e procedimentos de construção do texto literário.'),
    ('LINGUAGENS', 17, 5, 'Reconhecer a presença de valores sociais e humanos atualizáveis e permanentes no patrimônio literário nacional.'),
    ('LINGUAGENS', 18, 6, 'Identificar os elementos que concorrem para a progressão temática e para a organização e estruturação de textos de diferentes gêneros e tipos.'),
    ('LINGUAGENS', 19, 6, 'Analisar a função da linguagem predominante nos textos em situações específicas de interlocução.'),
    ('LINGUAGENS', 20, 6, 'Reconhecer a importância do patrimônio linguístico para a preservação da memória e da identidade nacional.'),
    ('LINGUAGENS', 21, 7, 'Reconhecer em textos de diferentes gêneros, recursos verbais e não-verbais utilizados com a finalidade de criar e mudar comportamentos e hábitos.'),
    ('LINGUAGENS', 22, 7, 'Relacionar, em diferentes textos, opiniões, temas, assuntos e recursos linguísticos.'),
    ('LINGUAGENS', 23, 7, 'Inferir em um texto quais são os objetivos de seu produtor e quem é seu público alvo, pela análise dos procedimentos argumentativos utilizados.'),
    ('LINGUAGENS', 24, 7, 'Reconhecer no texto estratégias argumentativas empregadas para o convencimento do público, tais como a intimidação, sedução, comoção, chantagem, entre outras.'),
    ('LINGUAGENS', 25, 8, 'Identificar, em textos de diferentes gêneros, as marcas linguísticas que singularizam as variedades linguísticas sociais, regionais e de registro.'),
    ('LINGUAGENS', 26, 8, 'Relacionar as variedades linguísticas a situações específicas de uso social.'),
    ('LINGUAGENS', 27, 8, 'Reconhecer os usos da norma padrão da língua portuguesa nas diferentes situações de comunicação.'),
    ('LINGUAGENS', 28, 9, 'Reconhecer a função e o impacto social das diferentes tecnologias da comunicação e informação.'),
    ('LINGUAGENS', 29, 9, 'Identificar pela análise de suas linguagens, as tecnologias da comunicação e informação.'),
    ('LINGUAGENS', 30, 9, 'Relacionar as tecnologias de comunicação e informação ao desenvolvimento das sociedades e ao conhecimento que elas produzem.'),
    ('MATEMATICA', 1, 1, 'Reconhecer, no contexto social, diferentes significados e representações dos números e operações - naturais, inteiros, racionais ou reais.'),
    ('MATEMATICA', 2, 1, 'Identificar padrões numéricos ou princípios de contagem.'),
    ('MATEMATICA', 3, 1, 'Resolver situação-problema envolvendo conhecimentos numéricos.'),
    ('MATEMATICA', 4, 1, 'Avaliar a razoabilidade de um resultado numérico na construção de argumentos sobre afirmações quantitativas.'),
    ('MATEMATICA', 5, 1, 'Avaliar propostas de intervenção na realidade utilizando conhecimentos numéricos.'),
    ('MATEMATICA', 6, 2, 'Interpretar a localização e a movimentação de pessoas/objetos no espaço tridimensional e sua representação no espaço bidimensional.'),
    ('MATEMATICA', 7, 2, 'Identificar características de figuras planas ou espaciais.'),
    ('MATEMATICA', 8, 2, 'Resolver situação-problema que envolva conhecimentos geométricos de espaço e forma.'),
    ('MATEMATICA', 9, 2, 'Utilizar conhecimentos geométricos de espaço e forma na seleção de argumentos propostos como solução de problemas do cotidiano.'),
    ('MATEMATICA', 10, 3, 'Identificar relações entre grandezas e unidades de medida.'),
    ('MATEMATICA', 11, 3, 'Utilizar a noção de escalas na leitura de representação de situação do cotidiano.'),
    ('MATEMATICA', 12, 3, 'Resolver situação-problema que envolva medidas de grandezas.'),
    ('MATEMATICA', 13, 3, 'Avaliar o resultado de uma medição na construção de um argumento consistente.'),
    ('MATEMATICA', 14, 3, 'Avaliar proposta de intervenção na realidade utilizando conhecimentos geométricos relacionados a grandezas e medidas.'),
    ('MATEMATICA', 15, 4, 'Identificar a relação de dependência entre grandezas.'),
    ('MATEMATICA', 16, 4, 'Resolver situação-problema envolvendo a variação de grandezas, direta ou inversamente proporcionais.'),
    ('MATEMATICA', 17, 4, 'Analisar informações envolvendo a variação de grandezas como recurso para a construção de argumentação.'),
    ('MATEMATICA', 18, 4, 'Avaliar propostas de intervenção na realidade envolvendo variação de grandezas.'),
    ('MATEMATICA', 19, 5, 'Identificar representações algébricas que expressem a relação entre grandezas.'),
    ('MATEMATICA', 20, 5, 'Interpretar gráfico cartesiano que represente relações entre grandezas.'),
    ('MATEMATICA', 21, 5, 'Resolver situação-problema cuja modelagem envolva conhecimentos algébricos.'),
    ('MATEMATICA', 22, 5, 'Utilizar conhecimentos algébricos/geométricos como recurso para a construção de argumentação.'),
    ('MATEMATICA', 23, 5, 'Avaliar propostas de intervenção na realidade utilizando conhecimentos algébricos.'),
    ('MATEMATICA', 24, 6, 'Utilizar informações expressas em gráficos ou tabelas para fazer inferências.'),
    ('MATEMATICA', 25, 6, 'Resolver problema com dados apresentados em tabelas ou gráficos.'),
    ('MATEMATICA', 26, 6, 'Analisar informações expressas em gráficos ou tabelas como recurso para a construção de argumentos.'),
    ('MATEMATICA', 27, 7, 'Calcular medidas de tendência central ou de dispersão de um conjunto de dados expressos em uma tabela de frequências de dados agrupados (não em classes) ou em gráficos.'),
    ('MATEMATICA', 28, 7, 'Resolver situação-problema que envolva conhecimentos de estatística e probabilidade.'),
    ('MATEMATICA', 29, 7, 'Utilizar conhecimentos de estatística e probabilidade como recurso para a construção de argumentação.'),
    ('MATEMATICA', 30, 7, 'Avaliar propostas de intervenção na realidade utilizando conhecimentos de estatística e probabilidade.'),
    ('CIENCIAS_NATUREZA', 1, 1, 'Reconhecer características ou propriedades de fenômenos ondulatórios ou oscilatórios, relacionando-os a seus usos em diferentes contextos.'),
    ('CIENCIAS_NATUREZA', 2, 1, 'Associar a solução de problemas de comunicação, transporte, saúde ou outro, com o correspondente desenvolvimento científico e tecnológico.'),
    ('CIENCIAS_NATUREZA', 3, 1, 'Confrontar interpretações científicas com interpretações baseadas no senso comum, ao longo do tempo ou em diferentes culturas.'),
    ('CIENCIAS_NATUREZA', 4, 1, 'Avaliar propostas de intervenção no ambiente, considerando a qualidade da vida humana ou medidas de conservação, recuperação ou utilização sustentável da biodiversidade.'),
    ('CIENCIAS_NATUREZA', 5, 2, 'Dimensionar circuitos ou dispositivos elétricos de uso cotidiano.'),
    ('CIENCIAS_NATUREZA', 6, 2, 'Relacionar informações para compreender manuais de instalação ou utilização de aparelhos, ou sistemas tecnológicos de uso comum.'),
    ('CIENCIAS_NATUREZA', 7, 2, 'Selecionar testes de controle, parâmetros ou critérios para a comparação de materiais e produtos, tendo em vista a defesa do consumidor, a saúde do trabalhador ou a qualidade de vida.'),
    ('CIENCIAS_NATUREZA', 8, 3, 'Identificar etapas em processos de obtenção, transformação, utilização ou reciclagem de recursos naturais, energéticos ou matérias-primas, considerando processos biológicos, químicos ou físicos neles envolvidos.'),
    ('CIENCIAS_NATUREZA', 9, 3, 'Compreender a importância dos ciclos biogeoquímicos ou do fluxo energia para a vida, ou da ação de agentes ou fenômenos que podem causar alterações nesses processos.'),
    ('CIENCIAS_NATUREZA', 10, 3, 'Analisar perturbações ambientais, identificando fontes, transporte e(ou) destino dos poluentes ou prevendo efeitos em sistemas naturais, produtivos ou sociais.'),
    ('CIENCIAS_NATUREZA', 11, 3, 'Reconhecer benefícios, limitações e aspectos éticos da biotecnologia, considerando estruturas e processos biológicos envolvidos em produtos biotecnológicos.'),
    ('CIENCIAS_NATUREZA', 12, 3, 'Avaliar impactos em ambientes naturais decorrentes de atividades sociais ou econômicas, considerando interesses contraditórios.'),
    ('CIENCIAS_NATUREZA', 13, 4, 'Reconhecer mecanismos de transmissão da vida, prevendo ou explicando a manifestação de características dos seres vivos.'),
    ('CIENCIAS_NATUREZA', 14, 4, 'Identificar padrões em fenômenos e processos vitais dos organismos, como manutenção do equilíbrio interno, defesa, relações com o ambiente, sexualidade, entre outros.'),
    ('CIENCIAS_NATUREZA', 15, 4, 'Interpretar modelos e experimentos para explicar fenômenos ou processos biológicos em qualquer nível de organização dos sistemas biológicos.'),
    ('CIENCIAS_NATUREZA', 16, 4, 'Compreender o papel da evolução na produção de padrões, processos biológicos ou na organização taxonômica dos seres vivos.'),
    ('CIENCIAS_NATUREZA', 17, 5, 'Relacionar informações apresentadas em diferentes formas de linguagem e representação usadas nas ciências físicas, químicas ou biológicas, como texto discursivo, gráficos, tabelas, relações matemáticas ou linguagem simbólica.'),
    ('CIENCIAS_NATUREZA', 18, 5, 'Relacionar propriedades físicas, químicas ou biológicas de produtos, sistemas ou procedimentos tecnológicos às finalidades a que se destinam.'),
    ('CIENCIAS_NATUREZA', 19, 5, 'Avaliar métodos, processos ou procedimentos das ciências naturais que contribuam para diagnosticar ou solucionar problemas de ordem social, econômica ou ambiental.'),
    ('CIENCIAS_NATUREZA', 20, 6, 'Caracterizar causas ou efeitos dos movimentos de partículas, substâncias, objetos ou corpos celestes.'),
    ('CIENCIAS_NATUREZA', 21, 6, 'Utilizar leis físicas e (ou) químicas para interpretar processos naturais ou tecnológicos inseridos no contexto da termodinâmica e(ou) do eletromagnetismo.'),
    ('CIENCIAS_NATUREZA', 22, 6, 'Compreender fenômenos decorrentes da interação entre a radiação e a matéria em suas manifestações em processos naturais ou tecnológicos, ou em suas implicações biológicas, sociais, econômicas ou ambientais.'),
    ('CIENCIAS_NATUREZA', 23, 6, 'Avaliar possibilidades de geração, uso ou transformação de energia em ambientes específicos, considerando implicações éticas, ambientais, sociais e/ou econômicas.'),
    ('CIENCIAS_NATUREZA', 24, 7, 'Utilizar códigos e nomenclatura da química para caracterizar materiais, substâncias ou transformações químicas.'),
    ('CIENCIAS_NATUREZA', 25, 7, 'Caracterizar materiais ou substâncias, identificando etapas, rendimentos ou implicações biológicas, sociais, econômicas ou ambientais de sua obtenção ou produção.'),
    ('CIENCIAS_NATUREZA', 26, 7, 'Avaliar implicações sociais, ambientais e/ou econômicas na produção ou no consumo de recursos energéticos ou minerais, identificando transformações químicas ou de energia envolvidas nesses processos.'),
    ('CIENCIAS_NATUREZA', 27, 7, 'Avaliar propostas de intervenção no meio ambiente aplicando conhecimentos químicos, observando riscos ou benefícios.'),
    ('CIENCIAS_NATUREZA', 28, 8, 'Associar características adaptativas dos organismos com seu modo de vida ou com seus limites de distribuição em diferentes ambientes, em especial em ambientes brasileiros.'),
    ('CIENCIAS_NATUREZA', 29, 8, 'Interpretar experimentos ou técnicas que utilizam seres vivos, analisando implicações para o ambiente, a saúde, a produção de alimentos, matérias primas ou produtos industriais.'),
    ('CIENCIAS_NATUREZA', 30, 8, 'Avaliar propostas de alcance individual ou coletivo, identificando aquelas que visam à preservação e a implementação da saúde individual, coletiva ou do ambiente.'),
    ('CIENCIAS_HUMANAS', 1, 1, 'Interpretar historicamente e/ou geograficamente fontes documentais acerca de aspectos da cultura.'),
    ('CIENCIAS_HUMANAS', 2, 1, 'Analisar a produção da memória pelas sociedades humanas.'),
    ('CIENCIAS_HUMANAS', 3, 1, 'Associar as manifestações culturais do presente aos seus processos históricos.'),
    ('CIENCIAS_HUMANAS', 4, 1, 'Comparar pontos de vista expressos em diferentes fontes sobre determinado aspecto da cultura.'),
    ('CIENCIAS_HUMANAS', 5, 1, 'Identificar as manifestações ou representações da diversidade do patrimônio cultural e artístico em diferentes sociedades.'),
    ('CIENCIAS_HUMANAS', 6, 2, 'Interpretar diferentes representações gráficas e cartográficas dos espaços geográficos.'),
    ('CIENCIAS_HUMANAS', 7, 2, 'Identificar os significados histórico-geográficos das relações de poder entre as nações.'),
    ('CIENCIAS_HUMANAS', 8, 2, 'Analisar a ação dos estados nacionais no que se refere à dinâmica dos fluxos populacionais e no enfrentamento de problemas de ordem econômico-social.'),
    ('CIENCIAS_HUMANAS', 9, 2, 'Comparar o significado histórico-geográfico das organizações políticas e socioeconômicas em escala local, regional ou mundial.'),
    ('CIENCIAS_HUMANAS', 10, 2, 'Reconhecer a dinâmica da organização dos movimentos sociais e a importância da participação da coletividade na transformação da realidade histórico-geográfica.'),
    ('CIENCIAS_HUMANAS', 11, 3, 'Identificar registros de práticas de grupos sociais no tempo e no espaço.'),
    ('CIENCIAS_HUMANAS', 12, 3, 'Analisar o papel da justiça como instituição na organização das sociedades.'),
    ('CIENCIAS_HUMANAS', 13, 3, 'Analisar a atuação dos movimentos sociais que contribuíram para mudanças ou rupturas em processos de disputa pelo poder.'),
    ('CIENCIAS_HUMANAS', 14, 3, 'Comparar diferentes pontos de vista, presentes em textos analíticos e interpretativos, sobre situação ou fatos de natureza histórico-geográfica acerca das instituições sociais, políticas e econômicas.'),
    ('CIENCIAS_HUMANAS', 15, 3, 'Avaliar criticamente conflitos culturais, sociais, políticos, econômicos ou ambientais ao longo da história.'),
    ('CIENCIAS_HUMANAS', 16, 4, 'Identificar registros sobre o papel das técnicas e tecnologias na organização do trabalho e/ou da vida social.'),
    ('CIENCIAS_HUMANAS', 17, 4, 'Analisar fatores que explicam o impacto das novas tecnologias no processo de territorialização da produção.'),
    ('CIENCIAS_HUMANAS', 18, 4, 'Analisar diferentes processos de produção ou circulação de riquezas e suas implicações sócio-espaciais.'),
    ('CIENCIAS_HUMANAS', 19, 4, 'Reconhecer as transformações técnicas e tecnológicas que determinam as várias formas de uso e apropriação dos espaços rural e urbano.'),
    ('CIENCIAS_HUMANAS', 20, 4, 'Selecionar argumentos favoráveis ou contrários às modificações impostas pelas novas tecnologias à vida social e ao mundo do trabalho.'),
    ('CIENCIAS_HUMANAS', 21, 5, 'Identificar o papel dos meios de comunicação na construção da vida social.'),
    ('CIENCIAS_HUMANAS', 22, 5, 'Analisar as lutas sociais e conquistas obtidas no que se refere às mudanças nas legislações ou nas políticas públicas.'),
    ('CIENCIAS_HUMANAS', 23, 5, 'Analisar a importância dos valores éticos na estruturação política das sociedades.'),
    ('CIENCIAS_HUMANAS', 24, 5, 'Relacionar cidadania e democracia na organização das sociedades.'),
    ('CIENCIAS_HUMANAS', 25, 5, 'Identificar estratégias que promovam formas de inclusão social.'),
    ('CIENCIAS_HUMANAS', 26, 6, 'Identificar em fontes diversas o processo de ocupação dos meios físicos e as relações da vida humana com a paisagem.'),
    ('CIENCIAS_HUMANAS', 27, 6, 'Analisar de maneira crítica as interações da sociedade com o meio físico, levando em consideração aspectos históricos e(ou) geográficos.'),
    ('CIENCIAS_HUMANAS', 28, 6, 'Relacionar o uso das tecnologias com os impactos sócio-ambientais em diferentes contextos histórico-geográficos.'),
    ('CIENCIAS_HUMANAS', 29, 6, 'Reconhecer a função dos recursos naturais na produção do espaço geográfico, relacionando-os com as mudanças provocadas pelas ações humanas.'),
    ('CIENCIAS_HUMANAS', 30, 6, 'Avaliar as relações entre preservação e degradação da vida no planeta nas diferentes escalas.');

-- questoes.habilidade existe desde a V2 e está vazia em toda base: a FK entra
-- sem nada a validar. MATCH SIMPLE (padrão): questão sem habilidade (2009 fora de
-- Matemática, item que o INEP não publicou) não é checada
ALTER TABLE questoes
    ADD CONSTRAINT fk_questoes_habilidade FOREIGN KEY (area, habilidade) REFERENCES habilidades (area, codigo);

-- Autoconferência, como na V5 e na V7: um texto que faltasse passaria em silêncio
DO $$
BEGIN
    IF (SELECT count(*) FROM competencias) <> 30 THEN
        RAISE EXCEPTION 'V8: esperadas 30 competências, há %', (SELECT count(*) FROM competencias);
    END IF;
    IF EXISTS (SELECT area FROM habilidades GROUP BY area HAVING count(*) <> 30)
       OR (SELECT count(DISTINCT area) FROM habilidades) <> 4 THEN
        RAISE EXCEPTION 'V8: cada uma das 4 áreas precisa de exatamente 30 habilidades';
    END IF;
END $$;

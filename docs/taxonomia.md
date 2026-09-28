# Taxonomia de assuntos — Simulei

**Status: APROVADA em 28/09/2026.** Semeada pela migration `V5__semeia_taxonomia_de_assuntos.sql` — é a **única** lista que o classificador pode usar (enum fechado). Mudar agora custa: **nova migration** (a V5 não pode ser editada depois de aplicada) e reclassificação das questões afetadas. Este arquivo e a V5 precisam continuar dizendo a mesma coisa.

## Estrutura

```
área (as 4 do ENEM, já existem no banco)
 └─ matéria (disciplina)
     └─ assunto   ← cada questão recebe UM assunto principal
```

- **Ancorada na Matriz de Referência do ENEM (INEP).** A coluna "Grupo da Matriz" liga cada assunto ao grupo oficial de objetos de conhecimento. É o que permite, no futuro, conferir a classificação contra a habilidade oficial (H1–H30) dos microdados.
- **O `slug` é o identificador permanente**: vai pra URL (`?assunto=probabilidade`), pro CSV da classificação e pra saída da IA. Nome pode mudar, slug não.
- Granularidade escolhida pelo que o aluno procura: "quero treinar probabilidade", não "quero treinar conhecimentos de estatística e probabilidade".

## Regras de classificação

Valem igual pra IA e pra revisão humana. Sem elas, os dois classificariam a mesma questão de jeitos diferentes.

1. **O assunto é o conhecimento necessário pra RESOLVER a questão, não o tema do texto.** Uma questão de Matemática sobre conta de luz que se resolve com regra de três é `razao-e-proporcao`, não "energia".
2. **Um assunto principal.** Se a questão usa dois, vale o que decide a resposta — o passo sem o qual não se chega à alternativa correta.
3. **Matéria dentro da área da questão.** Questão de Ciências Humanas recebe matéria de Humanas, mesmo que o texto seja literário.
4. **Leitura de gráfico/tabela só quando a leitura É a questão.** Se o gráfico só fornece dados pra uma conta de porcentagem, é `porcentagem-e-juros`.
5. **Língua estrangeira é interpretação.** No ENEM, as 5 questões de inglês/espanhol medem compreensão de texto; não há gramática isolada.
6. **Na dúvida real, confiança `baixa`** — a questão vai pra revisão em vez de receber um palpite.

---

## Linguagens, Códigos e suas Tecnologias

### Língua Portuguesa
| slug | assunto | grupo da Matriz |
|---|---|---|
| `interpretacao-de-texto` | Interpretação e compreensão de texto | Estudo do texto |
| `generos-textuais` | Gêneros textuais e suas finalidades | Estudo do texto |
| `funcoes-da-linguagem` | Funções da linguagem | Estudo do texto |
| `variacao-linguistica` | Variação linguística e norma | Estudo dos aspectos linguísticos |
| `figuras-de-linguagem` | Figuras de linguagem e recursos expressivos | Estudo dos aspectos linguísticos |
| `coesao-e-coerencia` | Coesão, coerência e articulação de ideias | Estudo dos aspectos linguísticos |
| `gramatica-aplicada` | Gramática no texto (classes, sintaxe, pontuação) | Estudo dos aspectos linguísticos |
| `argumentacao` | Argumentação e pontos de vista | Estudo do texto argumentativo |
| `intertextualidade` | Intertextualidade | Estudo do texto |
| `generos-digitais` | Gêneros digitais e tecnologias da comunicação | Estudo dos gêneros digitais |

### Literatura
| slug | assunto | grupo da Matriz |
|---|---|---|
| `literatura-colonial-e-arcadismo` | Quinhentismo, Barroco e Arcadismo | Estudo do texto literário |
| `romantismo` | Romantismo | Estudo do texto literário |
| `realismo-naturalismo-parnasianismo` | Realismo, Naturalismo e Parnasianismo | Estudo do texto literário |
| `simbolismo-e-pre-modernismo` | Simbolismo e Pré-Modernismo | Estudo do texto literário |
| `modernismo` | Modernismo | Estudo do texto literário |
| `literatura-contemporanea` | Literatura contemporânea | Estudo do texto literário |
| `linguagem-literaria` | Linguagem literária e recursos poéticos | Estudo do texto literário |

### Inglês
| slug | assunto | grupo da Matriz |
|---|---|---|
| `interpretacao-ingles` | Interpretação de texto em inglês | Estudo do texto |

### Espanhol
| slug | assunto | grupo da Matriz |
|---|---|---|
| `interpretacao-espanhol` | Interpretação de texto em espanhol | Estudo do texto |

### Artes
| slug | assunto | grupo da Matriz |
|---|---|---|
| `artes-visuais` | Artes visuais e movimentos artísticos | Produção e recepção de textos artísticos |
| `musica-teatro-danca` | Música, teatro e dança | Produção e recepção de textos artísticos |
| `arte-cultura-e-sociedade` | Arte, patrimônio e diversidade cultural | Produção e recepção de textos artísticos |

### Educação Física
| slug | assunto | grupo da Matriz |
|---|---|---|
| `praticas-corporais-e-saude` | Exercício físico, saúde e qualidade de vida | Estudo das práticas corporais |
| `esporte-e-sociedade` | Esporte, lazer e sociedade | Estudo das práticas corporais |
| `corpo-e-expressao` | Corpo, dança e expressão cultural | Estudo das práticas corporais |

---

## Matemática e suas Tecnologias

### Matemática
| slug | assunto | grupo da Matriz |
|---|---|---|
| `operacoes-e-conjuntos-numericos` | Operações, conjuntos numéricos, divisibilidade | Conhecimentos numéricos |
| `razao-e-proporcao` | Razão, proporção e regra de três | Conhecimentos numéricos |
| `porcentagem-e-juros` | Porcentagem e matemática financeira | Conhecimentos numéricos |
| `progressoes` | Sequências e progressões (PA e PG) | Conhecimentos numéricos |
| `analise-combinatoria` | Análise combinatória e princípio da contagem | Conhecimentos numéricos |
| `escalas-e-unidades` | Grandezas, unidades de medida e escalas | Conhecimentos geométricos |
| `geometria-plana` | Geometria plana: áreas, perímetros, semelhança | Conhecimentos geométricos |
| `geometria-espacial` | Geometria espacial: volumes e sólidos | Conhecimentos geométricos |
| `trigonometria` | Trigonometria (triângulo e ciclo) | Conhecimentos geométricos |
| `visao-espacial` | Visão espacial, projeções e planificações | Conhecimentos geométricos |
| `leitura-de-graficos-e-tabelas` | Leitura e interpretação de gráficos e tabelas | Conhecimentos de estatística e probabilidade |
| `estatistica` | Estatística: média, moda, mediana, desvio | Conhecimentos de estatística e probabilidade |
| `probabilidade` | Probabilidade | Conhecimentos de estatística e probabilidade |
| `funcao-do-1-grau` | Função do 1º grau | Conhecimentos algébricos |
| `funcao-do-2-grau` | Função do 2º grau | Conhecimentos algébricos |
| `exponencial-e-logaritmo` | Função exponencial e logaritmo | Conhecimentos algébricos |
| `equacoes-e-inequacoes` | Equações, inequações e expressões algébricas | Conhecimentos algébricos |
| `graficos-de-funcoes` | Gráficos e análise de funções | Conhecimentos algébricos |
| `geometria-analitica` | Geometria analítica: plano cartesiano, retas, circunferência | Conhecimentos algébricos/geométricos |
| `sistemas-lineares` | Sistemas de equações | Conhecimentos algébricos/geométricos |

---

## Ciências da Natureza e suas Tecnologias

### Física
| slug | assunto | grupo da Matriz |
|---|---|---|
| `cinematica` | Cinemática: velocidade, aceleração, movimentos | O movimento, o equilíbrio e a descoberta de leis físicas |
| `dinamica` | Dinâmica: leis de Newton e forças | O movimento, o equilíbrio e a descoberta de leis físicas |
| `energia-trabalho-potencia` | Energia, trabalho, potência e conservação | Energia, trabalho e potência |
| `hidrostatica` | Hidrostática: pressão, empuxo | O movimento, o equilíbrio e a descoberta de leis físicas |
| `gravitacao` | Gravitação e movimento dos astros | A Mecânica e o funcionamento do Universo |
| `termologia` | Calor, temperatura e calorimetria | O calor e os fenômenos térmicos |
| `termodinamica` | Termodinâmica e máquinas térmicas | O calor e os fenômenos térmicos |
| `optica` | Óptica: reflexão, refração, lentes, espelhos | Oscilações, ondas, óptica e radiação |
| `ondulatoria` | Ondas e acústica | Oscilações, ondas, óptica e radiação |
| `eletrostatica` | Eletrostática: carga e campo elétrico | Fenômenos elétricos e magnéticos |
| `eletrodinamica` | Eletrodinâmica: circuitos, potência e consumo elétrico | Fenômenos elétricos e magnéticos |
| `eletromagnetismo` | Magnetismo e indução eletromagnética | Fenômenos elétricos e magnéticos |
| `fisica-moderna` | Física moderna e radiações | Oscilações, ondas, óptica e radiação |

### Química
| slug | assunto | grupo da Matriz |
|---|---|---|
| `atomistica-e-tabela-periodica` | Modelos atômicos e tabela periódica | Transformações químicas |
| `ligacoes-e-propriedades` | Ligações químicas, polaridade e propriedades dos materiais | Materiais, suas propriedades e usos |
| `separacao-de-misturas` | Substâncias, misturas e separação | Materiais, suas propriedades e usos |
| `funcoes-inorganicas` | Ácidos, bases, sais e óxidos | Água |
| `estequiometria` | Reações, balanceamento e estequiometria | Representação das transformações químicas |
| `solucoes` | Soluções e concentração | Água |
| `termoquimica` | Termoquímica | Transformações químicas e energia |
| `cinetica-quimica` | Cinética química | Dinâmica das transformações químicas |
| `equilibrio-quimico-e-ph` | Equilíbrio químico, pH e pOH | Transformação química e equilíbrio |
| `eletroquimica` | Eletroquímica: pilhas, eletrólise, oxirredução | Transformações químicas e energia |
| `quimica-organica` | Funções orgânicas e isomeria | Compostos de carbono |
| `reacoes-organicas-e-polimeros` | Reações orgânicas e polímeros | Compostos de carbono |
| `radioatividade` | Radioatividade e energia nuclear | Energias químicas no cotidiano |
| `quimica-ambiental` | Química ambiental, combustíveis e tratamento de água | Relações da química com tecnologias, sociedade e meio ambiente |

### Biologia
| slug | assunto | grupo da Matriz |
|---|---|---|
| `citologia` | Citologia: célula e organelas | Moléculas, células e tecidos |
| `metabolismo-energetico` | Fotossíntese, respiração e fermentação | Moléculas, células e tecidos |
| `genetica` | Genética e hereditariedade | Hereditariedade e diversidade da vida |
| `biotecnologia` | Biotecnologia, DNA e engenharia genética | Hereditariedade e diversidade da vida |
| `evolucao` | Evolução e origem da vida | Origem e evolução da vida |
| `ecologia` | Ecologia: cadeias, relações e ciclos biogeoquímicos | Ecologia e ciências ambientais |
| `impactos-ambientais` | Problemas ambientais e conservação | Ecologia e ciências ambientais |
| `fisiologia-humana` | Fisiologia e anatomia humana | Identidade dos seres vivos |
| `saude-e-doencas` | Saúde, doenças, vírus, bactérias e parasitas | Qualidade de vida das populações humanas |
| `botanica` | Botânica | Identidade dos seres vivos |
| `zoologia` | Zoologia e classificação dos seres vivos | Identidade dos seres vivos |
| `reproducao-e-embriologia` | Reprodução e embriologia | Identidade dos seres vivos |

---

## Ciências Humanas e suas Tecnologias

A Matriz organiza Humanas por **eixos temáticos**, não por disciplina. Aqui a matéria segue a disciplina (é como o aluno estuda), e a coluna "grupo da Matriz" guarda o eixo oficial.

### História
| slug | assunto | grupo da Matriz |
|---|---|---|
| `antiguidade` | Antiguidade: Grécia, Roma e povos antigos | Formas de organização social e pensamento político |
| `idade-media` | Idade Média | Características e transformações das estruturas produtivas |
| `idade-moderna` | Renascimento, Reformas, Absolutismo e Iluminismo | Formas de organização social e pensamento político |
| `revolucoes-dos-seculos-18-e-19` | Revoluções Industrial, Francesa e liberais | Características e transformações das estruturas produtivas |
| `brasil-colonia` | Brasil Colônia e escravidão | Diversidade cultural, conflitos e vida em sociedade |
| `brasil-imperio` | Brasil Império | Formas de organização social e pensamento político |
| `brasil-republica` | Brasil República: da Primeira República à Era Vargas | Formas de organização social e pensamento político |
| `ditadura-e-redemocratizacao` | Ditadura militar e redemocratização | Formas de organização social e pensamento político |
| `seculo-20-mundial` | Guerras mundiais, totalitarismos e Guerra Fria | Formas de organização social e pensamento político |
| `africa-e-cultura-afro-brasileira` | História da África e cultura afro-brasileira | Diversidade cultural, conflitos e vida em sociedade |
| `povos-indigenas` | Povos indígenas e colonização da América | Diversidade cultural, conflitos e vida em sociedade |
| `patrimonio-e-memoria` | Patrimônio, memória e cultura material | Diversidade cultural, conflitos e vida em sociedade |

### Geografia
| slug | assunto | grupo da Matriz |
|---|---|---|
| `cartografia` | Cartografia e representação espacial | Representação espacial |
| `geologia-e-relevo` | Estrutura da Terra, solos e relevo | Os domínios naturais e a relação do ser humano com o ambiente |
| `clima` | Clima e fenômenos atmosféricos | Os domínios naturais e a relação do ser humano com o ambiente |
| `hidrografia` | Hidrografia e recursos hídricos | Os domínios naturais e a relação do ser humano com o ambiente |
| `biomas-e-dominios-naturais` | Biomas e domínios morfoclimáticos | Os domínios naturais e a relação do ser humano com o ambiente |
| `questoes-ambientais` | Questões ambientais e sustentabilidade | Os domínios naturais e a relação do ser humano com o ambiente |
| `urbanizacao` | Urbanização e questões urbanas | Características e transformações das estruturas produtivas |
| `espaco-agrario` | Espaço agrário e questão fundiária | Características e transformações das estruturas produtivas |
| `industria-e-energia` | Indústria, energia e transportes | Características e transformações das estruturas produtivas |
| `populacao-e-migracoes` | População e migrações | Diversidade cultural, conflitos e vida em sociedade |
| `globalizacao-e-geopolitica` | Globalização, geopolítica e conflitos | Formas de organização social e pensamento político |

### Filosofia
| slug | assunto | grupo da Matriz |
|---|---|---|
| `filosofia-antiga` | Filosofia antiga | Formas de organização social e pensamento político |
| `filosofia-medieval-e-moderna` | Filosofia medieval e moderna | Formas de organização social e pensamento político |
| `filosofia-contemporanea` | Filosofia contemporânea | Formas de organização social e pensamento político |
| `etica-e-moral` | Ética e moral | Diversidade cultural, conflitos e vida em sociedade |
| `filosofia-politica` | Filosofia política: Estado, poder e contrato social | Formas de organização social e pensamento político |

### Sociologia
| slug | assunto | grupo da Matriz |
|---|---|---|
| `cultura-e-identidade` | Cultura, identidade e diversidade | Diversidade cultural, conflitos e vida em sociedade |
| `trabalho-e-sociedade` | Trabalho, produção e sociedade | Características e transformações das estruturas produtivas |
| `cidadania-e-movimentos-sociais` | Cidadania, direitos e movimentos sociais | Formas de organização social e pensamento político |
| `estado-e-democracia` | Estado, poder e democracia | Formas de organização social e pensamento político |
| `teoria-sociologica` | Pensadores clássicos: Durkheim, Weber, Marx | Formas de organização social e pensamento político |
| `desigualdade-e-estratificacao` | Desigualdade, violência e exclusão social | Diversidade cultural, conflitos e vida em sociedade |

---

## Resumo

| Área | Matérias | Assuntos |
|---|---|---|
| Linguagens | Língua Portuguesa, Literatura, Inglês, Espanhol, Artes, Educação Física | 25 |
| Matemática | Matemática | 20 |
| Ciências da Natureza | Física, Química, Biologia | 39 |
| Ciências Humanas | História, Geografia, Filosofia, Sociologia | 34 |
| **Total** | **14 matérias** | **118 assuntos** |

## Pontos pra decisão na revisão

- **Humanas por disciplina, não por eixo.** O aluno estuda "História", não "Formas de organização social". O eixo oficial fica guardado na coluna do grupo da Matriz, pra quando precisarmos dele.
- **Inglês e Espanhol como matérias separadas, com um assunto cada.** A prova de língua estrangeira do ENEM é toda interpretação, então subdividir não traria informação.
- **Literatura por escola literária.** É como os cursinhos organizam, e o ENEM cobra reconhecimento de estilo de época.
- **O que ficou de fora de propósito:** assuntos raros no ENEM (matrizes, números complexos, polinômios avançados). Se o piloto mostrar questões que não cabem em nenhum assunto, elas aparecem com confiança baixa e a lista é revista antes da rodada completa.

## Fontes
- Matriz de Referência do ENEM — INEP: https://www.gov.br/inep/pt-br/centrais-de-conteudo/acervo-linha-editorial/publicacoes-institucionais/avaliacoes-e-exames-da-educacao-basica/matrizes-de-referencia-enem
- Objetos de conhecimento por área (reprodução da Matriz): https://blogdoenem.com.br/prepare-se-enem-2015-objetos-de-conhecimento/
- Microdados do ENEM (habilidade por item, sem assunto): https://www.gov.br/inep/pt-br/acesso-a-informacao/dados-abertos/microdados/enem

# Habilidade oficial × assunto (2b.7, Passo B)

Levantado em 30/09/2026. Dados em `dados/conferencia-inep/` (`competencias.csv`,
`compatibilidade.csv`); ferramenta em `ferramentas/inep/cruzar_habilidade.py`.
**Matemática e Linguagens concluídas.** Humanas e Natureza seguem o mesmo método.

## O que este passo faz, e o que não faz

O INEP publica a habilidade (H1–H30) de cada item. Se a IA classificou uma questão
em "estatística" e o INEP diz que ela mede uma habilidade de geometria, um dos dois
errou, e a questão vai pra revisão. É a conferência que a Etapa 4 exige antes de
mostrar diagnóstico a alguém.

**É um detector de erro grosseiro, não de erro fino.** A habilidade descreve o que o
aluno *faz* ("resolver situação-problema", "avaliar proposta de intervenção"), não
sobre *o quê*. Em Matemática, 7 das 30 habilidades são genéricas desse tipo. Por
isso a comparação é por **competência de área** (7 em Matemática), o nível em que a
matriz dá um tema. Consequência: estatística classificada como geometria não passa
despercebida, mas razão e proporção classificada como porcentagem sim — os dois
temas aparecem legitimamente nas mesmas competências.

## Como a tabela foi feita

**Do significado da matriz, não dos nossos dados.** Derivar a tabela da nossa
classificação só aprovaria a própria classificação: seria circular. Os dados serviram
só pra medir se a tabela ficou rígida demais (ver "Resultado").

Cada assunto tem competências **`NUCLEO`** (o tema do assunto é o tema da competência)
e **`TOLERADA`** (o assunto aparece a serviço dela). Fora das duas, a questão vai pra
revisão.

| Competência | Habilidades | Tema |
|---|---|---|
| C1 | H1–H5 | Números: significados, padrões e contagem |
| C2 | H6–H9 | Geometria: localização, figuras planas e espaciais |
| C3 | H10–H14 | Grandezas e medidas, escalas |
| C4 | H15–H18 | Variação de grandezas, proporcionalidade |
| C5 | H19–H23 | Álgebra: modelagem, gráfico cartesiano |
| C6 | H24–H26 | Leitura de gráficos e tabelas |
| C7 | H27–H30 | Estatística e probabilidade |

| Assunto | Núcleo | Tolerada |
|---|---|---|
| operacoes-e-conjuntos-numericos | C1 | C3, C6 |
| porcentagem-e-juros | C1 | C4, C5, C6 |
| razao-e-proporcao | C3, C4 | C1, C5, C6 |
| escalas-e-unidades | C3 | C1, C4, C6 |
| geometria-plana | C2, C3 | C4, C5 |
| geometria-espacial | C2, C3 | C4, C5 |
| visao-espacial | C2 | — |
| geometria-analitica | C2, C5 | C3 |
| trigonometria | C2, C5 | C3 |
| funcao-do-1-grau | C4, C5 | C6 |
| funcao-do-2-grau | C5 | C2, C4, C6 |
| exponencial-e-logaritmo | C5 | C1, C4, C6 |
| graficos-de-funcoes | C4, C5 | C6 |
| equacoes-e-inequacoes | C1, C5 | C3, C4 |
| sistemas-lineares | C5 | C1, C4 |
| progressoes | C1, C5 | C4, C6 |
| analise-combinatoria | C1, C7 | C5 |
| probabilidade | C7 | C1, C6 |
| estatistica | C6, C7 | — |
| leitura-de-graficos-e-tabelas | C6 | C3, C4, C5, C7 |

Escolhas que merecem o seu olho:
- **C6 (gráficos e tabelas) é tolerada em quase todo assunto numérico.** Tabela é
  formato de apresentação: um problema de porcentagem com os dados numa tabela é
  legitimamente H25. Mas **não é tolerada** em geometria, visão espacial nem
  trigonometria, onde ela seria sinal de erro.
- **Análise combinatória é núcleo de C7** além de C1: a habilidade H28
  (probabilidade) usa contagem, e H2 (princípios de contagem) está em C1.
- **Razão e proporção tem dois núcleos (C3 e C4)** e é o assunto-ponte: por isso
  quase nada cai fora dele.

## Resultado

661 questões de Matemática válidas com habilidade. **27 caem fora da tabela (4,1%).**

A validação que importa: a taxa de "fora da tabela" sobe com a incerteza que a
própria classificação declarou.

| Confiança da classificação | Questões | Fora da tabela | Taxa |
|---|---|---|---|
| Alta | 306 | 1 | 0,3% |
| Média | 314 | 21 | 6,7% |
| Baixa | 41 | 5 | 12,2% |

Duas leituras: (1) o método detecta incerteza real; (2) as confianças atribuídas
na classificação significam algo — só 1 das 306 de alta confiança destoa da matriz.

As 27 (assunto · ano/número · habilidade · competência · confiança):

| Assunto | Questões |
|---|---|
| operacoes-e-conjuntos-numericos | 2010/159 H23·C5, 2010/176 H16·C4, 2011/149 H23·C5, 2012/168 H21·C5, 2014/174 H18·C4 (baixa), 2015/163 H17·C4, 2017/145 H17·C4, 2021/160 H17·C4, 2022/152 H18·C4, 2023/153 H17·C4 |
| porcentagem-e-juros | 2013/167 H13·C3, 2014/158 H28·C7, 2015/151 H30·C7 (baixa), 2018/149 H14·C3 |
| geometria-plana | 2013/147 H4·C1, 2018/139 H26·C6 |
| equacoes-e-inequacoes | 2014/150 H9·C2, 2016/140 H29·C7 (baixa) |
| analise-combinatoria | 2015/147 H24·C6, 2017/178 H24·C6 |
| progressoes | 2010/169 H13·C3 |
| escalas-e-unidades | 2014/180 H23·C5 (baixa) |
| geometria-analitica | 2016/149 H26·C6 |
| razao-e-proporcao | 2016/159 H30·C7 (baixa) |
| leitura-de-graficos-e-tabelas | 2017/170 H1·C1 |
| graficos-de-funcoes | 2023/163 H6·C2 |
| trigonometria | 2023/177 H15·C4 |

O aglomerado mais claro é **"operações e conjuntos numéricos" com habilidade de
variação de grandezas (C4)**: 7 das 10. Provável tema: questões de proporção
(consumo, conversão, taxa) que classifiquei como operações. Vale reler.

## Revisão das 27 (30/09/2026)

Reli cada questão com o enunciado, as alternativas, as imagens e o texto da
habilidade oficial. **18 reclassificadas, 9 mantidas**; cada decisão e o motivo
estão em `dados/conferencia-inep/revisoes_habilidade.csv`, e a ferramenta pula as
mantidas enquanto o assunto não mudar.

**Reclassificadas (18):**

| De | Para | Questões |
|---|---|---|
| operações e conjuntos numéricos | razão e proporção | 2010/176, 2014/174*, 2021/160, 2022/152, 2023/153 |
| operações e conjuntos numéricos | equações e inequações | 2010/159, 2011/149 |
| operações e conjuntos numéricos | escalas e unidades | 2015/163 |
| progressões | equações e inequações | 2010/169 |
| porcentagem e juros | razão e proporção | 2018/149 |
| porcentagem e juros | geometria plana | 2013/167 |
| porcentagem e juros | probabilidade | 2014/158 |
| equações e inequações | visão espacial | 2014/150 |
| equações e inequações | estatística | 2016/140* |
| gráficos de funções | visão espacial | 2023/163 |
| trigonometria | gráficos de funções | 2023/177 |
| geometria analítica | gráficos de funções | 2016/149 |
| leitura de gráficos e tabelas | escalas e unidades | 2017/170 |

\* confiança baixa: dados ou regra faltam na fonte.

**O que a revisão mostrou:**
- A hipótese do aglomerado se confirmou em parte: **5 das 10 questões de "operações"
  eram proporção** (variação de grandezas), 3 eram álgebra ou unidades, e 2 eu mantive.
- Os acertos mais claros do INEP: **2014/158** é a *sensibilidade de um teste
  diagnóstico* (probabilidade condicional empírica, não porcentagem); **2023/163**
  pede a *vista superior* de uma trajetória (o gráfico é só o dado); **2014/150**
  exige ler as dimensões numa *planificação*.
- **Mantidas (9)** têm em comum o rótulo de habilidade genérico do INEP descrevendo
  parte da questão e não o conhecimento pedido (ex.: 2012/168 é só potência de
  expoente fracionário; o modelo já vem dado na fórmula). Cinco delas, com dados
  ausentes na fonte, ficam com **confiança baixa**: não dá pra confirmar nem negar.
- 2016/149 era a única questão de **alta confiança** fora da tabela. Era mesmo um caso
  de fronteira (reta e parábola lidas num gráfico) e virou média.

Depois da carga: a ferramenta mostra **0 questões por rever** em Matemática.

## Linguagens (01/10/2026)

Em Linguagens as competências são **temáticas**, e não de uso como em Matemática: C2 é
língua estrangeira (inglês e espanhol batem 100%), C3 é linguagem corporal, C4 é arte,
C5 é texto literário, C8 é língua portuguesa e variação, C9 é tecnologias da
comunicação. Por isso o detector é **mais forte** aqui: uma questão de literatura com
habilidade de língua estrangeira não tem como ser legítima.

| Competência | Habilidades | Tema |
|---|---|---|
| C1 | H1–H4 | Linguagens e sistemas de comunicação; usos sociais |
| C2 | H5–H8 | Língua estrangeira moderna |
| C3 | H9–H11 | Linguagem corporal |
| C4 | H12–H14 | Arte como saber cultural |
| C5 | H15–H17 | Texto literário e contexto |
| C6 | H18–H20 | Organização do texto; função da linguagem; patrimônio linguístico |
| C7 | H21–H24 | Opiniões, argumentação, objetivos do produtor |
| C8 | H25–H27 | Língua portuguesa: variação e norma |
| C9 | H28–H30 | Tecnologias da comunicação e da informação |

Tabela completa em `dados/conferencia-inep/compatibilidade.csv` (25 assuntos). Resumo
das escolhas: a **literatura** (7 assuntos) é `NUCLEO` C5 e tolera C1, C4, C6 e C7;
inglês e espanhol são **só C2**; **Artes** é `NUCLEO` C4 e tolera C1, C5, C7 e **C9**
(arte e tecnologia é tema recorrente: fotografia, cinema, música digital); **Educação
Física** é `NUCLEO` C3.

### Resultado

724 questões de Linguagens válidas com habilidade. **21 caem fora da tabela (2,9%).**
Taxa por confiança: alta 1,5%, média 4,8% (só 4 de baixa têm habilidade, então esse
ponto não diz nada).

| Assunto | Questões fora da tabela |
|---|---|
| variacao-linguistica | 2009/119 H22·C7 (alta), 2009/125 H4·C1, 2009/129 H22·C7 (alta), 2009/131 H4·C1, 2011/125 H22·C7, 2011/132 H28·C9, 2012/129 H23·C7, 2012/130 H22·C7 (alta), 2013/132 H22·C7 (alta), 2014/125 H23·C7, 2015/106 H29·C9, 2015/109 H14·C4, 2017/15 H23·C7, 2017/22 H22·C7, 2018/37 H21·C7 (alta), 2020/20 H29·C9, 2022/14 H22·C7 |
| modernismo | 2012/100 H9·C3, 2023/14 H29·C9 (alta) |
| generos-textuais | 2014/126 H13·C4 |
| realismo-naturalismo-parnasianismo | 2021/44 H29·C9 |

**O aglomerado é "variação linguística": 17 das 79 questões desse assunto (21%).**
Sete têm H21–H23 (C7: opiniões, objetivos do produtor, argumentação), três têm H28/H29
(C9: tecnologias), duas H4 e uma H14. Dois cenários: (a) o assunto virou **depósito**
de questões de interpretação e argumentação que tocam na linguagem coloquial, ou (b)
a tabela está rígida demais pra variação. A leitura das 17 responde.

### Escolhas que merecem o seu olho

1. **Variação linguística tolera só C6 além do núcleo C8.** Tolerar também C7 e C9
   reduziria as 17 pra 3, mas esconderia justamente o aglomerado suspeito. Recomendo
   manter rígido e ler as 17.
2. **C9 tolerada só em Artes**, não em literatura nem em variação. "Internetês" é
   variação legítima em ambiente digital, mas isso é o que a leitura vai dizer caso a
   caso.
3. **C1 (H1–H4) é tolerada em quase tudo**, porque fala de linguagens e sistemas de
   comunicação em geral. É a tolerância mais larga da tabela.

### Revisão das 21 (01/10/2026)

Tabela aprovada pelo desenvolvedor. Reli as 21 com enunciado, alternativas e a habilidade
oficial: **9 reclassificadas, 12 mantidas**, cada uma com o motivo em
`dados/conferencia-inep/revisoes_habilidade.csv`.

| De | Para | Questões |
|---|---|---|
| variação linguística | interpretação de texto | 2011/125, 2012/129 |
| variação linguística | intertextualidade | 2017/22, 2022/14 |
| variação linguística | gêneros digitais | 2011/132, 2015/106 |
| variação linguística | arte, patrimônio e diversidade | 2015/109 |
| modernismo | corpo e expressão | 2012/100 |
| gêneros textuais | música, teatro e dança | 2014/126 |

**O que a revisão mostrou:**
- **Das 17 de "variação linguística", 7 não eram variação (41%)** e 10 eram. Os dois
  cenários se confirmaram em parte: o assunto virou depósito de questões em que a língua
  é o **tema do texto**, mas a tarefa é ler ou relacionar textos. Pela regra 1 da taxonomia
  ("o assunto é o conhecimento necessário, não o tema do texto") esses casos são
  interpretação ou intertextualidade.
- As 10 mantidas são variação legítima (variação diatópica, norma x uso, registro,
  patrimônio linguístico). O rótulo de habilidade do INEP descreve a **tarefa** ("confrontar
  opiniões"), não o conteúdo: por isso manter o assunto rígido na tabela, em vez de
  tolerar C7 e C9 em variação, foi a escolha certa. Tolerar teria escondido os 7.
- **Nenhuma das 6 questões de alta confiança foi reclassificada**: as de alta confiança
  destacadas eram todas legítimas. A calibração se mantém.
- Casos de fronteira decididos pela regra 1: 2012/100 é Drummond, mas a pergunta é sobre
  autoimagem corporal (educação física); 2014/126 é um trecho de Martins Pena, mas a
  pergunta é sobre rubricas na encenação (teatro).

Depois da carga (9 atualizadas, 2ª rodada 0): **0 por rever em Linguagens**, 12 mantidas.

## Natureza (01/10/2026)

Física, Química e Biologia numa área só: 38 assuntos, 622 questões com habilidade.

**Como a tabela foi feita.** Pelo significado da matriz, com um viés a registrar: a tabela
assunto × competência dos nossos dados já tinha sido impressa quando escrevi a tabela, então
ela não é derivada às cegas. Só marquei vínculos que a matriz justifica.
- **C6, C7 e C8 são as competências de cada disciplina** (Física, Química, Biologia). Elas pegam
  o erro grosseiro de disciplina (química classificada como física).
- **C5 (métodos e linguagens das ciências) é transversal**: a H17 diz "físicas, químicas ou
  biológicas". Tolerada em todos os assuntos.
- **C1 e C3 são largas** (senso comum × ciência, tecnologia e saúde; processos de obtenção,
  reciclagem, poluentes) e foram toleradas em quase tudo.
- `termoquimica`, `eletroquimica` e `radioatividade` ficam entre Física e Química: C6 e C7
  são núcleo nos três.

**Resultado.** 11 de 622 (1,8%) fora da tabela; taxa por confiança ALTA 1,5% → MEDIA 2,1% →
BAIXA 2,8%. Sobe, mas bem menos que em Matemática: o detector enxerga pouco aqui, porque
o INEP escreve boa parte das habilidades de Natureza de forma transversal. Ele pega erro de
disciplina, não erro fino dentro de uma disciplina. Isso não mede a qualidade da classificação.

**Revisão das 11 (01/10/2026): 3 reclassificadas, 8 mantidas.**
- 2012/62 evolução → impactos ambientais (híbridos que escapam e substituem a espécie natural).
- 2012/86 soluções → citologia (o que se pede é a osmose na célula humana).
- 2020/108 funções inorgânicas → fisiologia humana (revestimento resistente ao suco gástrico;
  era a alternativa do próprio classificador).
- As 8 mantidas seguem o padrão de Linguagens: o rótulo do INEP descreve a tarefa ou o
  contexto, e o conhecimento pedido é o do assunto atual (polaridade em 2015/80 e 2023/129,
  forças intermoleculares em 2019/112, isomeria em 2021/106, densidade em 2020/122...).
- Das 6 de alta confiança, nenhuma foi reclassificada. Calibração preservada.

**Bug da ferramenta achado aqui.** O `assuntos.csv` mistura CRLF e LF, e o
`registrar_revisao.py` escolhia um separador pro arquivo todo, colando as linhas do outro tipo.
MT e LC passaram por sorte (estavam no trecho CRLF). Agora o fim é preservado linha a linha,
com teste.

## Humanas (01/10/2026)

História, Geografia, Filosofia e Sociologia: 34 assuntos, 629 questões com habilidade.

**Tabela** (pelo significado da matriz; a tabela dos dados já tinha sido impressa antes, como em
Natureza). As competências são mais temáticas que as de Natureza e menos que as de Linguagens:
C1 cultura e memória, C2 espaço e poder, C3 instituições e conflitos, C4 técnica e trabalho,
C5 cidadania e ética, C6 sociedade e natureza. **C6 é o eixo da Geografia física.** História é a
mais permissiva: C1, C2, C3 e C5 cabem em qualquer período. Consequência: o detector pega erro
grosseiro (Geografia física classificada como História) e **não pega erro de período**
(Idade Média classificada como Brasil Colônia).

**Resultado.** 9 de 629 (1,4%) fora da tabela; ALTA 0,3% → MEDIA 2,7% → BAIXA 6,9%.

**Revisão das 9: 3 reclassificadas, 6 mantidas.** Abri as imagens das duas que dependiam delas.
- 2015/15 cartografia → cultura-e-identidade: o mapa é só a ferramenta, a resposta é a valorização
  de identidades de povos tradicionais.
- 2016/22 cultura-e-identidade → seculo-20-mundial: a HQ é Persépolis, sobre a Revolução Islâmica
  de 1979 e o véu obrigatório.
- 2020/75 cartografia → idade-moderna: o objeto é a cartografia, mas o conhecimento é a invenção da
  América pelos europeus e a dominação sobre os saberes ameríndios.
- Mantidas: 2010/7 (coordenadas), 2018/73 (relevo e território), 2019/48 e 2019/75 (filosofia
  moderna e a natureza), 2023/46 (sertão, baixa), 2023/84 (trabalho doméstico).
- **Duas dúvidas.** 2019/75 poderia ser questões ambientais (o texto II é ambiental, mas o que se
  pede é a concepção moderna de natureza). 2023/46 (conceito geográfico de "lugar") não tem assunto
  próprio na taxonomia; ficou em cultura-e-identidade com confiança baixa.

## Balanço do Passo B (as quatro áreas)

| Área | Fora da tabela | Revisadas | Reclassificadas | Mantidas |
|---|---|---|---|---|
| Matemática | 27 (4,1%) | 27 | 18 | 9 |
| Linguagens | 21 (2,9%) | 21 | 9 | 12 |
| Natureza | 11 (1,8%) | 11 | 3 | 8 |
| Humanas | 9 (1,4%) | 9 | 3 | 6 |
| **Total** | **68** | **68** | **33** | **35** |

Em todas as áreas a taxa de destaque sobe de ALTA pra BAIXA, o que valida as confianças. O detector
é forte onde a competência é temática (Matemática, Linguagens) e fraco onde é transversal
(Natureza, Humanas). **Ele mede erro grosseiro, não a acurácia da classificação.** Ainda vale
amostrar à mão antes de mostrar diagnóstico a um cliente.

## Próximos passos

1. Passo B concluído nas quatro áreas. Resta decidir o merge da branch e a Etapa 3.
2. **2009 só tem habilidade pra Matemática.** Nas outras áreas, 134 questões desse ano
   ficam sem conferência; é limite do dado, não do método.

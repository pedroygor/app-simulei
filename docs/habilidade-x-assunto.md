# Habilidade oficial × assunto (2b.7, Passo B) — Matemática

Levantado em 30/09/2026. Dados em `dados/conferencia-inep/` (`competencias.csv`,
`compatibilidade.csv`); ferramenta em `ferramentas/inep/cruzar_habilidade.py`.
**Só Matemática por enquanto.** Linguagens, Humanas e Natureza seguem o mesmo método.

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

## Próximos passos

1. Repetir o método em Linguagens, Humanas e Natureza (30 habilidades e mais
   competências cada; Humanas e Natureza precisam de tabela por **matéria**, não só por
   assunto, porque cada área mistura disciplinas). Cada área: tabela → você revisa →
   eu releio as destacadas.
2. **2009 só tem habilidade pra Matemática.** Nas outras áreas, 134 questões desse ano
   ficam sem conferência; é limite do dado, não do método.

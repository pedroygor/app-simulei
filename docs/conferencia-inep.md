# Conferência com o INEP (2b.7 — Passo A: gabarito e anulações)

Levantado em 30/09/2026 medindo os 15 anos (2009–2023). Ferramentas em
`ferramentas/inep/`; correções em `dados/conferencia-inep/correcoes.csv`.

## Por que isso importa

A enem.dev é a nossa fonte das **questões**, mas o INEP é a fonte oficial do
**gabarito**. Um gabarito errado corrige a prova do aluno ao contrário; uma
questão anulada num simulado dá nota errada. E a taxonomia/diagnóstico da
Etapa 4 herda qualquer erro daqui.

## Achados

| Achado | Tamanho | Evidência |
|---|---|---|
| **Gabarito da enem.dev diverge do INEP** | 14 de 2.732 questões casadas (0,5%) | 11 confirmadas no PDF oficial; 3 só no CSV (2010 q112, q139; 2011 q81) |
| **Anuladas guardadas com letra de gabarito** | 6 (2018 q163, 2020 q135 e q157, 2021 q138, 2022 q157, 2023 q164) | as 6 confirmadas no PDF ("Anulado") |
| Questões de idioma sem `idioma` na fonte | 9 (2011 q91–95, 2012 q94, 2015 q95, 2016 q93, 2020 q5) | ver abaixo |
| Itens do INEP que o banco não tem | 38 (19 são a variante de idioma que falta; os demais, questões que o importador descartou por dado inválido, como a 2023 q132) | |
| Habilidade oficial (H1–H30) disponível | 2.597 de 2.741 (95%); 2010–2023 ≈ 100%; **2009 só Matemática** | insumo da 2b.7 Passo B |

Metade das divergências (7 de 14) está em **2021**. O padrão do erro é o da fonte,
não do nosso mapeamento: nas 700 comparações entre o CSV do INEP e o PDF oficial
(2012, 2018, 2021, 2022) as duas fontes **nunca discordaram**.

### `IN_ITEM_ABAN=1` NÃO é anulação

O INEP marca "abandonado" por dois motivos completamente diferentes:

- **Abandono estatístico** (40 itens entre os casados): motivo "não permitiu a
  convergência do algoritmo de estimação da TRI", "Bilog não converge". O item
  foi aplicado, vale, tem gabarito normal. Só ficou fora do cálculo da nota.
  **Não é anulada.** Marcar como anulada tiraria questão boa do banco.
- **Anulação pedagógica**: motivo "Anulado", "anulado pedagogicamente",
  "Exclusão pedagógica", e **`TX_GABARITO = X`**. Estas são as anuladas.

A regra usada é `TX_GABARITO = X` **no caderno de referência**. Anulação é por
caderno: em 2021 o item 84880 é `X` num caderno e `D` em outro.

### Questões de idioma sem `idioma` (9)

Comparando o gabarito do banco com as duas variantes do INEP: 2011 q91, q92, q94,
2015 q95 e 2016 q93 batem com **inglês**; 2011 q95 e 2020 q5 batem com
**espanhol**; 2011 q93 e 2012 q94 são ambíguas (as duas variantes têm o mesmo
gabarito). **Não se propõe corrigir o `idioma`**: ele compõe a chave natural
`(ano, numero, idioma)`, e mudá-lo faz o importador (idempotente por essa chave)
criar duplicata na próxima importação. A taxonomia já separa por assunto
(`interpretacao-ingles` / `-espanhol`), o que limita o dano.

## Como o casamento funciona

O `CO_POSICAO` do INEP **não é** o número da questão. `ferramentas/inep/microdados.py`
explica; o resumo:

1. Há um `CO_PROVA` por área e por cor. A numeração da enem.dev é a do caderno
   **AZUL da 1ª aplicação** — descoberto pelo gabarito, não suposto.
2. Em 2016 e 2017 a posição é local; a ordem dos blocos de área varia por ano
   (a mesma do V6 e do `CadernoDoEnem` — confirmada por uma segunda fonte).
3. Numera-se por **posição relativa**, não por rank: área com item faltando
   deslocaria tudo o que vem depois.
4. Nenhum mapeamento é aceito com menos de 90% de gabarito igual.

Resultado: 2.732 de 2.741 questões casadas, 99,3% de gabarito igual (as 20
diferenças são as 14 divergências + as 6 anuladas).

## Carga aplicada (30/09/2026)

Decisões do desenvolvedor: a carga foi **aprovada**, e **o INEP prevalece sobre a
enem.dev** no gabarito e na anulação. Comando `--aplicar-conferencia-inep`, no
mesmo desenho da carga da classificação:

- **`GABARITO`**: atualiza só se o gabarito atual ainda for o `valor_banco` do
  CSV. Se alguém já mexeu à mão, não sobrescreve: a linha vai pra `divergentes` e
  é mostrada no log pra decisão humana.
- **`ANULADA`**: `anulada = true` e `gabarito = NULL` (o CHECK da V2 permite, e
  o CLAUDE.md já diz que anulada não tem gabarito). **Marcada, não apagada**: a
  `--importar-questoes` pula o que já existe pela chave natural, então apagar
  faria a questão voltar na próxima importação.
- Valida o arquivo inteiro antes de gravar (tipo, letra A–E, anulação com `X`,
  questão repetida); idempotente (2ª rodada = 0 alterações).
- Vive na **carga**, não em migration: numa base nova as questões são importadas
  DEPOIS das migrations, então um UPDATE por migration não acharia nada (foi o
  caso da V6). Ordem: `--importar-questoes` → `--importar-classificacao` →
  `--aplicar-conferencia-inep`.
- **O `correcoes.csv` é a fonte permanente**, e `conferir_gabarito.py` só
  acrescenta linhas, nunca remove. Depois da carga o banco concorda com o INEP e
  a conferência deixa de enxergar as correções; regravar só o que ainda diverge
  esvaziaria o arquivo versionado.

Resultado no banco local: 14 gabaritos corrigidos, 6 questões anuladas, **2.735
questões válidas** (de 2.741). As 6 anuladas saem da busca, do detalhe e da
contagem por assunto (já filtravam `anulada = false`) e sairão do sorteio dos
simulados na 3a. Segunda rodada: 0 alterações, 20 inalteradas.

## Pendências

- **2b.7 Passo B:** cruzar a habilidade oficial com o assunto (exige tabela
  habilidade → assuntos compatíveis, a curar a partir do `matriz_referencia_enem.pdf`).
- **Questões que a enem.dev não tem** (~19 comuns): avaliar se vale reimportar
  com tratamento de dado inválido.

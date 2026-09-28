# Identidade visual — Simulei

Documento de referência para qualquer trabalho de interface neste projeto. Ler antes de criar tela nova.

---

## Conceito

Dark minimalista com referência ao mundo da prova. Três decisões carregam a identidade:

1. **Grafite, não preto.** A base é um cinza-azulado que remete a grafite de lápis. Preto puro (`#000`) é o default de todo produto dark e não diz nada sobre o produto — além de ser mais cansativo de ler no celular à noite.
2. **Marca-texto como acento.** Amarelo-limão de marca-texto, usado com a mesma disciplina de um marca-texto de verdade: só no que importa. É a aposta estética do projeto e o que diferencia o Simulei de qualquer SaaS dark genérico.
3. **A bolha do cartão-resposta é a assinatura.** A alternativa não é radio button. É bolha que preenche. O progresso é fileira de bolhas. O painel do professor é grade de bolhas. É o elemento pelo qual o produto é lembrado.

Público: aluno de 16–18 anos no celular (uso principal) e professor/dono de cursinho no desktop (quem paga).

---

## Paleta

### Base — tema escuro (padrão)

| Papel | Hex | Uso | Contraste |
|---|---|---|---|
| grafite | `#0F1216` | fundo da página | — |
| carta | `#171C23` | superfície de card, alternativa não selecionada | — |
| borda | `#2A313A` | hairline decorativo | 1.43 (decoração, não precisa passar) |
| **contorno** | `#626F7F` | **contorno de controle** (bolha vazia, checkbox), hover | 3.67 ✓ |
| papel | `#ECEEF1` | texto primário | 16.15 ✓ |
| texto secundário | `#B8C0CA` | texto de alternativa, apoio | 9.32 ✓ |
| lápis | `#7C8794` | metadados, legendas, placeholder | 4.69 ✓ |
| marca-texto | `#D4F03C` | acento: seleção, ação primária, questão atual | 14.60 ✓ |
| sobre-acento | `#1F2812` | texto e ícone sobre o marca-texto | 11.91 ✓ |

**Duas correções aplicadas em 27/08/2026** depois de medir a paleta contra o WCAG:

- `borda forte #3A424D` (1.85) **falhava** o SC 1.4.11 como contorno de controle: a bolha vazia é elemento interativo e precisa de 3:1, senão o aluno com baixa visão não enxerga onde tocar. Virou `contorno #626F7F` (3.67). O papel decorativo (hairline de card) continua com `borda #2A313A`, que não precisa passar no critério.
- `lápis #77828F` sobre carta dava 4.38, abaixo dos 4.5 para texto pequeno. Clareado para `#7C8794` (4.69).

São **dois tokens distintos** porque têm exigências distintas: `--border` (hairline) e `--contorno` (controle).

### Áreas do ENEM

Cores que **codificam informação** (a área da questão), nunca decoração. Não usar para outra finalidade.

| Área | Hex |
|---|---|
| Linguagens | `#6EA8FE` |
| Humanas | `#F0A93C` |
| Natureza | `#4FD1A5` |
| Matemática | `#E86A8C` |

Expostas como tokens (`--area-linguagens` etc.), nunca hardcoded em componente.

### Tema claro

Obrigatório, não opcional. O professor projeta o painel no datashow da sala e imprime relatório — dark em projetor fica ilegível.

O tema claro é derivado dos **mesmos papéis semânticos**, não é uma segunda paleta inventada. Definir os dois desde o início: retrofitar tema depois significa reescrever componente por componente.

| Papel | Hex | Contraste |
|---|---|---|
| fundo da página | `#FBFCFD` | — |
| card | `#FFFFFF` | — |
| texto primário | `#0F1216` | 19.4 ✓ |
| lápis (apoio) | `#5A6675` | 5.85 ✓ |
| borda (hairline) | `#E1E5EA` | — |
| contorno de controle | `#7C8794` | 3.56 ✓ |
| **ação primária** | `#5F6E0F` | 5.49 ✓ (texto `#ECEEF1` em cima: 4.85 ✓) |
| marca-texto (destaque) | `#D4F03C` | com texto `#1F2812`: 11.91 ✓ |

**A ação primária muda de tom entre os temas, e isso é deliberado.** O `#D4F03C` sobre fundo claro dá contraste **1.25** — um botão que desaparece no papel. A primária do tema claro é a mesma matiz limão, fechada até passar 3:1, com texto claro por cima.

O **marca-texto puro continua idêntico nos dois temas**, no papel natural dele: fundo de destaque com texto escuro em cima — que é literalmente como marca-texto funciona sobre papel.

**Áreas no tema claro.** As cores do tema escuro ficam em ~2:1 sobre fundo claro; as versões fechadas são:

| Área | Escuro | Claro |
|---|---|---|
| Linguagens | `#6EA8FE` | `#1F5FD0` (5.67) |
| Humanas | `#F0A93C` | `#8A5A00` (5.77) |
| Natureza | `#4FD1A5` | `#0C7A5A` (5.18) |
| Matemática | `#E86A8C` | `#B01F45` (6.54) |

---

## Tipografia

Três papéis, sem exceção. Se um texto não se encaixa em nenhum deles, o problema é o texto.

| Papel | Fonte | Uso |
|---|---|---|
| Display | Bricolage Grotesque | título de tela, nota final, número grande. **Uso restrito.** |
| Corpo | Geist Sans | todo o resto do conteúdo |
| Dados | Geist Mono | tempo, nota, percentual, ano da questão |

**Por que essas.** Inter é a fonte default de todo projeto novo e não tem personalidade. Bricolage Grotesque tem irregularidade proposital que dá cara jovem sem virar fonte decorativa, e é variável (controle fino de peso e largura). Geist vem junto com o Next.js via `next/font` — zero configuração, zero request extra, e acentuação portuguesa bem desenhada.

**Números sempre com `font-variant-numeric: tabular-nums`.** Sem isso o cronômetro "dança" a cada segundo e a coluna de notas desalinha.

---

## Densidade por público

Não são duas identidades. São os **mesmos tokens com densidade diferente**.

**Aluno** — uma coisa por tela, alvo de toque generoso, respiro amplo. O marca-texto aparece em momentos de progresso e conquista.

**Professor** — tabela densa, muita informação por tela. O marca-texto aparece só em alerta e ação primária. A grade de bolhas (aluno × questão, preenchida = acertou) é o resumo que ele lê em três segundos e mostra para o pai do aluno.

---

## Acessibilidade — requisitos, não sugestões

- **Cor nunca é o único sinal.** Alternativa selecionada preenche com marca-texto **e** muda de forma. Cerca de 8% dos meninos têm alguma deficiência de visão de cores, e esse é o público.
- **Texto sobre marca-texto é o grafite `#1F2812`**, nunca preto puro.
- **Alvo de toque mínimo 44px** em qualquer controle de aluno.
- **Foco visível** em toda navegação por teclado.
- **Bolhas de alternativa têm semântica de radio group** — não são `div` com `onClick`.
- **`prefers-reduced-motion` respeitado.**
- Dark é o padrão (aluno estudando de madrugada no celular é o caso de uso real), mas `prefers-color-scheme` é lido e o toggle é acessível.

---

## Regras de implementação

Complementam a seção 8.5 do `CLAUDE.md`.

- Cor, espaçamento e raio vêm sempre de token. **Nenhum valor mágico** — sem `bg-[#D4F03C]` espalhado pelo código.
- Variante de componente com CVA, nunca ternário aninhado em template string.
- Componentes do shadcn/ui instalados pela CLI antes de escrever qualquer coisa do zero.
- **Mobile-first:** classes base são para 360px; `md:`/`lg:` são exceção.
- Utilitários Tailwind vivem dentro de `components/ui/`. Telas consomem componentes com API de props.

---

## Componentes-assinatura

**`AlternativaBolha`** — a alternativa de questão. Estados: `default`, `hover`, `selecionada`, `correta`, `incorreta`, `desabilitada`.

**`ProgressoBolhas`** — fileira de marcadores: respondidas (papel), atual (marca-texto), pendentes (borda).

**`GradeCartaoResposta`** — grade aluno × questão no painel do professor. Preenchida = acertou.

---

## Decisões descartadas (e por quê)

| Descartado | Motivo |
|---|---|
| Preto puro + verde neon | Default de todo produto dark de 2025. Bonito na primeira olhada, não diz nada sobre o produto. |
| Inter em tudo | Fonte padrão de qualquer projeto novo. Zero personalidade. |
| Duas identidades (aluno jovem / professor sóbrio) | Dobra o custo de manutenção. Densidade resolve. |
| Só tema escuro | Inviabiliza datashow e impressão — os dois usos reais do professor. |
| Radio button padrão nas alternativas | Descarta o artefato mais reconhecível do universo da prova brasileira. |
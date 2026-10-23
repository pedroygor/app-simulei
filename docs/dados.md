# Dados

De onde vêm as questões, o que o Simulei acrescenta a elas e como tudo é carregado. Licenças e créditos: [NOTICE](../NOTICE).

## As fontes

### enem.dev: as questões
A [enem.dev](https://enem.dev) ([código](https://github.com/yunger7/enem-api), GPL-2.0) é uma API pública da comunidade com as provas do ENEM de 2009 a 2023.

O Simulei **não consulta a enem.dev durante o uso**. Um comando de importação puxa as questões uma vez e grava no Postgres, inclusive **baixando as imagens** para o próprio armazenamento. Assim a aplicação não depende da disponibilidade de terceiros, responde rápido e pode enriquecer cada questão com dados próprios.

Detalhes da fonte que moldaram o modelo:
- **Questões de língua estrangeira têm o mesmo número no mesmo ano** (a de inglês e a de espanhol). A chave natural é `(ano, numero, idioma)`
- Enunciado e alternativas vêm em **markdown com imagens hospedadas lá**
- **Assunto e dificuldade não vêm da fonte**: são acréscimos do Simulei

### Microdados do INEP: gabarito, habilidade e dificuldade
O INEP publica, por item, o gabarito oficial, a habilidade da Matriz de Referência e os parâmetros da TRI (`ITENS_PROVA_{ano}.csv`). O cruzamento com a numeração da enem.dev é feito por scripts Python em `ferramentas/inep/`, fora do backend; o resultado vira CSV versionado em `dados/conferencia-inep/`, aplicado por uma carga idempotente.

Entram: **gabarito e anulação** (o INEP prevalece quando diverge), **habilidade oficial** (mostrada no detalhe da questão) e **dificuldade** (parâmetro B da TRI, gravada e ainda não exibida). Método e números em [conferencia-inep.md](conferencia-inep.md).

## O que aprendemos medindo a enem.dev

Fatos que não estão na documentação dela e que custaram bugs para descobrir:

- **Limite de 10 requisições a cada 10 segundos** (cabeçalhos `x-ratelimit-*`). O cliente espaça as chamadas em 1,1 s e recua exponencialmente no 429. O host das imagens não tem limite
- **`limit` máximo de 50** por página (100 devolve 400)
- **A paginação repete um item:** `offset=50` devolve os índices de 50 a 100, ou seja, 51 itens, repetindo a última questão da página anterior. Sem deduplicação, entraria uma questão duplicada por página
- **`language` só entrega uma variante por vez**, então cada ano é varrido duas vezes (inglês e espanhol). As variantes disponíveis variam por prova e estão declaradas em `GET /v1/exams`: **2009 não tem nenhuma, 2011 só tem espanhol**. Pedir uma variante que a prova não tem devolve 400
- Em 2011, as questões de língua estrangeira vêm **sem `language`**
- **A área está errada em 147 das 2.741 questões (5,4%)**, por exemplo química marcada como Ciências Humanas. A pista: Humanas tinha de 47 a 79 questões por ano, e cada área tem 45. A verdade é a **posição no caderno**, em blocos de 45 (2009: Natureza, Humanas, Linguagens, Matemática; 2010–2016: Humanas, Natureza, Linguagens, Matemática; 2017 em diante: Linguagens, Humanas, Natureza, Matemática). O importador usa a posição e avisa a cada divergência
- **A fonte não marca questões anuladas** e erra o gabarito de 14 questões (0,5%). Os dois são corrigidos pela conferência com o INEP
- **Qualidade:** 144 questões sem texto-base (5%), 7 com 4 alternativas, uma com alternativas em branco (2023, questão 132). 77 questões dependem de um texto ou imagem que a fonte não entregou; elas são marcadas como `incompleta` e nunca aparecem

E do lado do INEP:
- **`IN_ITEM_ABAN=1` não é anulação.** Indica abandono estatístico na TRI; o item vale e tem gabarito. A anulação é o gabarito `X`, e é por caderno
- **`CO_POSICAO` não é o número da questão:** há um `CO_PROVA` por área e por cor de caderno, e o caderno da enem.dev é o azul da 1ª aplicação
- O `ITENS_PROVA_2011.csv` mistura latin-1 e UTF-8 no mesmo arquivo
- O servidor do INEP **não envia o certificado intermediário** na conexão TLS. Em vez de desligar a verificação, o intermediário público está versionado em `ferramentas/inep/` e é confiado só nas requisições ao INEP

## O que o Simulei acrescenta (pasta `dados/`, CC BY-SA 4.0)

| Arquivo | Conteúdo |
|---|---|
| `classificacao/assuntos.csv` | o assunto de cada questão, com confiança e justificativa |
| `classificacao/amostra_revisao_2026-10.csv` | amostra aleatória revisada à mão, para medir o acerto da classificação |
| `classificacao/habilidades_baixa_confianca.csv` | 12 habilidades estimadas (2009, sem dado do INEP) que **não vão para o banco**: o palpite acertou só 36% da habilidade exata contra as oficiais |
| `conferencia-inep/correcoes.csv` | gabaritos corrigidos e anulações, segundo o INEP |
| `conferencia-inep/habilidades.csv` | habilidade oficial e dificuldade (TRI) de cada questão |
| `conferencia-inep/competencias.csv`, `compatibilidade.csv`, `revisoes_habilidade.csv` | a conferência "habilidade × assunto" e as revisões feitas a partir dela |
| `qualidade/questoes_incompletas.csv` | as questões sem texto-base, com o motivo |

A taxonomia (118 assuntos em 14 matérias, ancorada na Matriz de Referência do ENEM) está em [taxonomia.md](taxonomia.md). A conferência entre habilidade e assunto, em [habilidade-x-assunto.md](habilidade-x-assunto.md).

## As cargas

Todas rodam de `backend/` e são **idempotentes**: repetir não duplica nem desfaz nada. Cada uma sobe a aplicação sem servidor web, então pode rodar com a API no ar. Numa base nova, **nesta ordem**:

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="--importar-questoes"        # da enem.dev, com as imagens (--anos=2023 para um ano só)
./mvnw spring-boot:run -Dspring-boot.run.arguments="--importar-classificacao"   # dados/classificacao/assuntos.csv
./mvnw spring-boot:run -Dspring-boot.run.arguments="--aplicar-conferencia-inep" # dados/conferencia-inep/correcoes.csv
./mvnw spring-boot:run -Dspring-boot.run.arguments="--importar-habilidades"     # dados/conferencia-inep/habilidades.csv
./mvnw spring-boot:run -Dspring-boot.run.arguments="--marcar-incompletas"       # dados/qualidade/questoes_incompletas.csv
```

Garantias comuns às cargas: o arquivo **inteiro** é validado antes de gravar qualquer linha (carga pela metade não existe), e as escritas são em lote, com um número fixo de consultas. A carga da classificação **nunca sobrescreve uma revisão humana**, e a do gabarito só troca um valor se o banco ainda tiver o que o arquivo esperava.

Questão anulada ou incompleta é **marcada, não apagada**: a importação pula o que já existe pela chave natural, então apagar faria a questão voltar na próxima importação.

## As ferramentas Python (`ferramentas/`)

Só biblioteca padrão, sem instalar nada. Rodam da raiz.

```bash
python3 ferramentas/inep/baixar_itens_prova.py     # ITENS_PROVA de 2009 a 2023 (~2 MB, lendo só o arquivo necessário de cada zip)
python3 ferramentas/inep/conferir_gabarito.py      # gera dados/conferencia-inep/correcoes.csv (só acrescenta, nunca remove)
python3 ferramentas/inep/gerar_habilidades.py      # gera dados/conferencia-inep/habilidades.csv
python3 -m unittest discover -s ferramentas/inep    # testes
```

Em `ferramentas/classificacao/` ficam o exportador de lotes para classificação, o validador do `assuntos.csv` e a ferramenta de amostra para revisão.

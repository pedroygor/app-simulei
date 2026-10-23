# Arquitetura

Como o Simulei é organizado e as regras que o código segue. O porquê das escolhas maiores está em [decisoes.md](decisoes.md); de onde vêm os dados e como são carregados, em [dados.md](dados.md).

## Stack

### Backend
- **Java 21** e **Spring Boot 4.1**: Spring Web (API REST), Spring Data JPA com Hibernate, Spring Security com JWT (via OAuth2 Resource Server, sem filtro próprio)
- **Flyway** para o schema. Nunca `ddl-auto=update`: toda mudança é uma migration em `backend/src/main/resources/db/migration/`, no padrão `V{n}__{descricao}.sql`
- **Bean Validation** nos DTOs de entrada, **Lombok** e **MapStruct**
- **Maven**, com o wrapper (`./mvnw`)

**Armadilha conhecida:** Lombok e MapStruct exigem a ordem certa em `annotationProcessorPaths` (`lombok` → `lombok-mapstruct-binding` → `mapstruct-processor`). Se o mapper gerado vier vazio, é isso.

### Banco
**PostgreSQL 16**, local via Docker Compose. Recursos do Postgres são usados de propósito (`UNIQUE NULLS NOT DISTINCT`, coluna gerada, `date_trunc` com fuso), por isso os testes não usam H2.

### Frontend
- **Next.js 16** (App Router), **React 19**, **TypeScript** em modo estrito
- **Tailwind CSS 4** sem `tailwind.config.js`: os tokens do tema ficam em `@theme`, no `globals.css`
- **shadcn/ui** (componentes copiados para `components/ui/`, não instalados como dependência), **CVA** para variantes e o helper **`cn()`** (clsx + tailwind-merge)
- **TanStack Query** para o estado do servidor; `useState`/`useReducer` para o estado local. Sem Redux
- **react-hook-form + zod** nos formulários, **lucide-react** nos ícones, **next-themes** para o tema sem piscar

**Next 16 mudou coisas que pegam:** `params` e `searchParams` são assíncronos (`await searchParams`), o Turbopack é o padrão em dev e no build, e `middleware` virou `proxy`. A documentação da versão instalada fica em `frontend/node_modules/next/dist/docs/`.

**Tema:** `:root` é o tema claro e `.dark` o escuro, porque é o que os componentes do shadcn esperam. Quem faz do escuro o padrão é o `ThemeProvider` (`defaultTheme="dark"`), não o CSS.

**Frontend e API em origens diferentes** (`localhost:3000` → `localhost:8080`), com o refresh token num cookie `httpOnly`: exige `credentials: 'include'` no fetch, `allowCredentials(true)` com a origem explícita no CORS (variável `CORS_ORIGENS`) e atenção ao `SameSite`. Não é bug, é configuração.

### Testes
JUnit 5 e Mockito nos unitários, **Testcontainers** (Postgres de verdade) na integração, Vitest no frontend.

## Organização do backend

Pacotes **por domínio**, não por camada técnica:

```
br.com.simulados.api/
├── config/          segurança, CORS, agendamento, relógio
├── shared/          exceções e tratamento de erro, segurança (JWT)
├── usuario/         cadastro, login, sessão
├── questao/         questões, alternativas, taxonomia, importação e cargas
├── simulado/        montagem (Simulado), execução (Tentativa), respostas, resultado
└── desempenho/      acerto por área e por assunto, evolução por semana
```

Dentro de cada um:
- `api/`: o controller recebe, valida, delega e devolve um DTO. **Nenhuma regra de negócio.**
- `application/`: os services, com a regra de negócio e o `@Transactional`
- `infrastructure/`: repositórios e acesso a dados
- `domain/`: entidades e regras do domínio

**Entidade JPA nunca sai pela API:** sempre um DTO de entrada (`...Request`) e um de saída (`...Response`).

## Modelo de dados

**Decisão central:** `Simulado` é a definição da prova; `Tentativa` é a execução dela por um aluno. Hoje cada simulado tem uma tentativa (o aluno monta a própria prova).

Schema atual (V1–V13). **A fonte da verdade são as migrations**: se este bloco e elas discordarem, vale a migration.

```
usuarios                                                    (V1, V13)
  id (UUID), nome, email (unique), senha_hash,
  role (só ALUNO desde a V13), plano (sem uso), criado_em, ativo

materias                                                    (V4, V7)
  id, area, slug (unique), nome, ordem (unique)
  UNIQUE (id, area)                       -- alvo da FK composta

assuntos                                                    (V4, V7)
  id, materia_id, area, slug (unique), nome, grupo_matriz, ordem
  FK (materia_id, area) -> materias; UNIQUE (materia_id, ordem); UNIQUE (id, area)

competencias (area, numero 1..9, descricao)  PK (area, numero)          (V8)
habilidades  (area, codigo 1..30, competencia, descricao)  PK (area, codigo)
  FK (area, competencia) -> competencias

questoes                                                    (V2, V3, V4, V8, V9)
  id (bigint identity), ano, numero, idioma (nullable: INGLES|ESPANHOL),
  area (LINGUAGENS|CIENCIAS_HUMANAS|CIENCIAS_NATUREZA|MATEMATICA),
  enunciado (nullable, markdown), comando (nullable), imagem_url (nullable),
  gabarito (nullable; CHECK anulada OR gabarito NOT NULL),
  dificuldade (nullable — parâmetro B da TRI), habilidade (nullable, 1..30),
  anulada (bool), incompleta (bool),
  disponivel (GERADA: NOT anulada AND NOT incompleta),
  assunto_id (nullable), classificacao_origem (IA|REVISAO_HUMANA),
  classificacao_confianca (ALTA|MEDIA|BAIXA), fonte, fonte_id
  UNIQUE NULLS NOT DISTINCT (ano, numero, idioma)
  CHECK (enunciado IS NOT NULL OR comando IS NOT NULL)
  FK (assunto_id, area) -> assuntos       -- assunto de outra área é recusado
  FK (area, habilidade) -> habilidades

alternativas                                                (V2, V11)
  id (bigint identity), questao_id, letra (A..E), texto (nullable), imagem_url (nullable)
  UNIQUE (questao_id, letra); UNIQUE (id, questao_id)   -- alvo da FK de respostas
  CHECK (texto IS NOT NULL OR imagem_url IS NOT NULL)

simulados                                                   (V10)
  id (UUID), criado_por -> usuarios, titulo (não vazio),
  tempo_limite_seg (nullable, 60..21600), criado_em

simulado_questoes                                           (V10)
  simulado_id (ON DELETE CASCADE), questao_id, ordem (1..180)
  PK (simulado_id, questao_id); UNIQUE (simulado_id, ordem)

tentativas                                                  (V11, V12)
  id (UUID), simulado_id (SEM cascade), aluno_id -> usuarios,
  status (EM_ANDAMENTO|FINALIZADA), iniciado_em,
  finalizado_em (preenchido sse FINALIZADA),
  prazo (nullable = sem cronômetro; CHECK prazo > iniciado_em)
  UNIQUE (simulado_id, aluno_id)
  índice parcial (prazo) WHERE status = 'EM_ANDAMENTO' AND prazo IS NOT NULL

respostas                                                   (V11)
  tentativa_id (ON DELETE CASCADE), questao_id, alternativa_id (NOT NULL),
  correta (bool), respondida_em
  PK (tentativa_id, questao_id)
  FK (alternativa_id, questao_id) -> alternativas   -- alternativa de outra questão é recusada
```

**Notas:**
- `questoes` e `alternativas` usam **bigint identity**: são dado de referência importado em massa, e o id compacto deixa índices e FKs menores. UUID fica para o que o usuário cria (`usuarios`, `simulados`, `tentativas`).
- O `UNIQUE (ano, numero, idioma)` precisa de **`NULLS NOT DISTINCT`**: a maioria das questões tem `idioma` nulo, e no SQL padrão `NULL != NULL`, então sem a cláusula a constraint não impediria duplicata nenhuma.
- **`disponivel` é a única regra de "pode ser mostrada ou sorteada".** Consulta nova filtra por ela, nunca só por `anulada`.
- **Resposta em branco = nenhuma linha** em `respostas` (por isso `alternativa_id NOT NULL`), e a PK é `(tentativa_id, questao_id)`.
- `respostas.correta` é uma desnormalização deliberada, para agregar desempenho sem juntar com o gabarito. É calculada na hora da resposta e **nunca aparece para o aluno com a prova aberta**: revelaria o gabarito.
- **Não existe coluna de nota.** Ela é calculada a cada consulta, para acompanhar `disponivel`: uma questão anulada depois da prova muda o denominador.
- O tempo que vale é o `prazo`, imposto pelo servidor. Não existe tempo por questão (viria do relógio do celular, que não é confiável).
- `tentativas.simulado_id` **sem CASCADE**: apagar um simulado nunca leva resultado de aluno junto.
- `UNIQUE (simulado_id, aluno_id)` é a regra "cada simulado se faz uma vez". Um futuro "refazer" vira uma coluna `numero_tentativa`, sem remover a constraint.
- A ordem das questões é por simulado (`simulado_questoes.ordem`), igual para todos.

## Convenções

### Código
- Domínio em português, infraestrutura em inglês. Mensagens ao usuário e comentários em português
- Entidades JPA com `@Getter`/`@Setter`/`@NoArgsConstructor`, **nunca `@Data`** (quebra `equals`/`hashCode` com proxies do Hibernate)
- DTOs como `record`
- `@Transactional` no service, não no controller nem no repositório
- Senha sempre com `BCryptPasswordEncoder`
- Commits em [Conventional Commits](https://www.conventionalcommits.org/), em português (`feat: ...`, `fix: ...`)

### Validação de entrada
**Nenhum endpoint aceita entrada sem validação.**
- DTO de entrada é um `record` com anotações de Bean Validation, e o controller usa `@Valid`
- Query params entram como um record de filtro com `@Valid` (por exemplo, `FiltroDaEvolucao`)
- Formato e obrigatoriedade ficam no DTO; regra de negócio que depende do banco fica no service, que lança exceção
- Em listas, a anotação vai **dentro** do tipo genérico: `List<@Valid Escolha>` e `List<@Min(2009) Integer>`. Sem isso, a lista é validada, mas cada item não

### Erros
**Erro é exceção, nunca valor de retorno.**
- O service lança exceção de domínio e não conhece HTTP
- Um único `@RestControllerAdvice` traduz exceção em status, a partir de bases semânticas: `RecursoNaoEncontradoException` (404), `ConflitoException` (409), `RequisicaoInvalidaException` (400), `MuitasRequisicoesException` (429) e as de regra de negócio (422)
- Formato único de erro: `{ "timestamp", "status", "erro", "mensagem", "path", "campos"? }`, inclusive nos 401 e 403 do Spring Security

### Consultas
- **Nunca consultar dentro de laço.** Dados de N entidades vêm numa query só
- Estatística é **agregação no banco** (`GROUP BY`), não laço em Java
- Sorteio de questões numa query só (`row_number() OVER (PARTITION BY ... ORDER BY random())`), nunca "busca tudo e sorteia em Java"
- Relacionamentos `LAZY`, com `JOIN FETCH` quando preciso
- Toda listagem é paginada (teto de 100 por página)
- Percentual sem divisão por zero: sem base, o resultado é `null`, nunca 0%
- **Testes contam as queries** (`hibernate.generate_statistics`) nos pontos mais propensos a N+1. Se alguém introduzir um, o build quebra

### Segurança
- **Nenhum segredo no código.** Tudo vem de variável de ambiente; o `application.yml` só referencia (`${JWT_SECRET}`). O `.env` está no `.gitignore` e há um `.env.example` com valores fictícios
- Token de acesso curto (30 min), em memória no frontend; refresh token em cookie `httpOnly`
- Nenhuma resposta da API carrega `senha_hash`
- CORS restrito às origens configuradas, nunca `*`
- **Autorização por dono:** toda consulta com dado de aluno filtra pelo usuário do token. Recurso de outra pessoa responde 404, como se não existisse

### Frontend
- Toda tela é desenhada para **360px primeiro**; `md:`/`lg:` são a exceção
- JSX de página não acumula utilitários: com mais de ~5 classes num elemento, vira componente
- Variantes com CVA, nunca ternário dentro de string de classe; sobrescrita pontual com `cn()`
- Cores, espaçamentos e raios vêm dos tokens do tema, sem valor mágico
- Antes de escrever um componente, ver se o shadcn/ui já tem
- **Testa-se a lógica que quebra em silêncio** (cliente HTTP, cronômetro, regras de desempenho), não componente de apresentação

### Conteúdo de terceiros (imagens das questões)
As imagens são servidas sem autenticação, de propósito: prova do ENEM é pública. O risco real é servir do próprio domínio um arquivo que um terceiro entregou.
- **Na ingestão** (`ImagemStorageLocal`): allowlist por magic bytes (PNG, JPEG, GIF, WEBP; SVG fora, porque pode carregar script), teto de tamanho lido do stream, nome igual ao SHA-256 do conteúdo e escrita atômica. Conteúdo recusado mantém a URL da fonte
- **Ao servir:** `X-Content-Type-Options: nosniff`, `Content-Security-Policy: default-src 'none'; sandbox` e `Cross-Origin-Resource-Policy: same-site`
- **O markdown das questões** é renderizado sem HTML cru e com allowlist de elementos: a fonte chega a entregar `<SCRIPT>` literal no texto

### Definição de pronto
1. `cd backend && ./mvnw test` passa
2. As migrations rodam limpas num banco zerado
3. Endpoint novo tem validação de entrada e pelo menos um teste
4. Com mudança no frontend: `npm test && npx tsc --noEmit && npx eslint .` passam
5. Tela nova foi vista em largura de celular

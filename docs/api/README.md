# Coleção de API do Simulei

Requests prontos pra testar a API na mão, no [Bruno](https://www.usebruno.com/) (`bruno/`, arquivos `.bru` em texto plano, versionados com o código).

A coleção percorre o fluxo inteiro do aluno, na ordem das pastas: cadastro → login → banco de questões → montar prova → fazer a prova → resultado → desempenho. Cada request explica, no próprio `docs`, o que o endpoint faz e por quê. Os ids passam de um request pro outro por variável de ambiente (o simulado montado é o que a prova inicia, e assim por diante).

## Antes de rodar

API no ar e banco com as questões carregadas: veja o [README da raiz](../../README.md#rodando-localmente). As pastas **Questoes**, **Simulados**, **Tentativas** e **Desempenho** precisam das questões no banco.

## No aplicativo

Não precisa importar: o Bruno lê a pasta direto.

1. **Open Collection** → aponte pra `docs/api/bruno`
2. Selecione o ambiente **local** no canto superior direito
3. Rode as pastas na ordem: **Usuarios** → **Auth** → **Questoes** → **Simulados** → **Tentativas** → **Desempenho**

O `Auth/01 - Login` guarda `accessToken` e `refreshToken` no ambiente, e os requests protegidos funcionam em seguida.

## Pela linha de comando

```bash
cd docs/api/bruno
npx @usebruno/cli run --env local --disable-cookies
```

Deve dar **47 requests, 13 testes e 91 asserções, PASS**. A coleção pode ser repetida: na 2ª rodada o cadastro dá 409 (esperado) e cada rodada monta uma prova nova, porque cada aluno faz cada simulado uma vez só.

A pasta **enem.dev (fonte externa)** consulta a API pública da fonte das questões, pra explorar o formato dela. Precisa de internet e não depende da nossa API.

Pra rodar contra outra porta: `--env-var baseUrl=http://localhost:8081`.

## O cookie jar atrapalha os testes negativos

O refresh token vem em cookie `httpOnly` + `Secure`, com `Path=/auth/refresh`. O Bruno tem cookie jar próprio, e o cookie guardado **sobrescreve** o header `Cookie` que os requests mandam explicitamente.

- **Fluxo feliz** (login → refresh): funciona com o jar ligado ou desligado.
- **Cenário negativo** (`Auth/05 - Refresh com token invalido`): com o jar ligado, o cookie válido entra no lugar do inválido e a resposta vira 200. Pra ver o 401, limpe os cookies ou use `--disable-cookies`.

No navegador isso não acontece desse jeito: o cookie `Secure` é aceito em `localhost` (contexto seguro), e o frontend funciona com `credentials: 'include'` no fetch.

## Sobre credenciais

O ambiente `bruno/environments/local.bru` tem o e-mail e a senha de um aluno de teste, criado pela própria coleção no Postgres local. **Não é segredo**: é dado descartável de desenvolvimento.

Os tokens (`accessToken`, `refreshToken`) são `vars:secret`: o Bruno os guarda fora do arquivo e eles **não entram no git**. Sem isso, cada rodada gravaria um JWT válido num arquivo versionado.

Pra apontar a coleção pra outro ambiente, **não edite o `local.bru`**: crie outro arquivo em `environments/` e coloque-o no `.gitignore`.

## Endpoints cobertos

Rota pública = sem token. As demais exigem `Authorization: Bearer`, e tudo o que é do aluno (simulados, tentativas, desempenho) é filtrado pelo aluno do token: o de outro aluno responde 404, como se não existisse.

| Método | Rota | Auth | Cenários |
|---|---|---|---|
| POST | `/usuarios` | pública | 201 / `role` enviado é ignorado / 400 inválido / 409 duplicado / 400 JSON quebrado |
| POST | `/auth/login` | pública | 200 com tokens / 401 senha errada |
| POST | `/auth/refresh` | cookie | 200 com rotação / 401 token inválido |
| POST | `/auth/logout` | pública | 204, expira o cookie |
| GET | `/usuarios/me` | Bearer | 200 / 401 sem token |
| GET | `/questoes` | Bearer | paginado / filtros por área, ano, idioma, matéria e assunto / 400 ordenação inválida |
| GET | `/questoes/{id}` | Bearer | 200 com alternativas e gabarito |
| GET | `/questoes/anos` | Bearer | os anos que têm questão |
| GET | `/taxonomia` | Bearer | área → matéria → assunto, com quantas questões há em cada assunto |
| GET | `/imagens/**` | pública | imagem da questão |
| POST | `/simulados` | Bearer | 201 / 422 escolhas sobrepostas / 400 com o campo de cada item |
| GET | `/simulados` | Bearer | histórico paginado ("Minhas provas") |
| GET | `/simulados/{id}` | Bearer | composição / 404 alheio ou inexistente |
| POST | `/simulados/{id}/tentativas` | Bearer | 201 inicia / 200 a mesma tentativa (idempotente) |
| GET | `/tentativas/{id}` | Bearer | a prova, sem gabarito / 404 alheia ou inexistente |
| PUT | `/tentativas/{id}/respostas/{questaoId}` | Bearer | 204 / 422 alternativa de outra questão / 422 prova entregue |
| DELETE | `/tentativas/{id}/respostas/{questaoId}` | Bearer | 204, idempotente |
| GET | `/tentativas/em-andamento` | Bearer | até 3 provas abertas, sem acertos |
| POST | `/tentativas/{id}/finalizar` | Bearer | 200 entrega |
| GET | `/tentativas/{id}/resultado` | Bearer | nota, por área e assunto, gabarito / 422 antes de entregar |
| GET | `/desempenho` | Bearer | acerto geral, por área e por assunto, pontos fracos |
| GET | `/desempenho/evolucao` | Bearer | por semana / de um assunto / 404 assunto inexistente / 400 filtro inválido |

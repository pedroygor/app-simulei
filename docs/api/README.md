# Collections de API — Simulei

Requests prontos pra testar a API na mão, em duas versões equivalentes:

- **Bruno** — `bruno/` (arquivos `.bru`, versionados em texto plano)
- **Postman** — `simulei.postman_collection.json` (schema v2.1)

Ambas cobrem os 11 requests da nossa API (Etapa 1) mais 3 da fonte externa
enem.dev, úteis pra explorar o formato antes do job de importação.

## Antes de rodar

```bash
docker compose up -d          # Postgres
set -a; source .env; set +a   # variáveis de ambiente (inclusive JWT_SECRET)
./mvnw spring-boot:run        # API em http://localhost:8080
```

## Bruno

Não precisa importar: o Bruno lê a pasta direto.

1. **Open Collection** → aponte pra `docs/api/bruno`
2. Selecione o ambiente **local** no seletor do canto superior direito
3. Rode na ordem: pasta **Usuarios** (cria os usuários) → pasta **Auth**

O request `Auth/01 - Login` guarda `accessToken` e `refreshToken` no ambiente
via script pós-resposta, então os requests protegidos já funcionam em seguida.

### Como smoke test pela linha de comando

```bash
cd docs/api/bruno
npx @usebruno/cli run --env local --disable-cookies
```

Deve dar **20 requests, 25 assertions, PASS** (com 2023 importado). O `--disable-cookies` é
necessário pros cenários negativos — veja a seção sobre o cookie jar abaixo.

## Postman

**Import** → arraste o arquivo `simulei.postman_collection.json`. As variáveis
(`baseUrl`, `email`, `senha`, `accessToken`, `refreshToken`) já vêm embutidas
na collection, não precisa criar environment.

Mesma ordem: pasta **Usuarios** primeiro, depois **Auth**.

Pela linha de comando: `npx newman run docs/api/simulei.postman_collection.json`

## O cookie jar atrapalha os testes negativos

O refresh token vem em cookie `httpOnly` + `Secure`, com `Path=/auth/refresh`.
Tanto o Bruno quanto o Postman têm cookie jar próprio, e o cookie guardado
**sobrescreve** o header `Cookie` que os requests mandam explicitamente.

Na prática:

- **Fluxo feliz** (login → refresh): funciona de qualquer jeito, com jar
  ligado ou desligado.
- **Cenário negativo** (`05 - Refresh com token invalido`): com o jar ligado
  o cookie válido entra no lugar do inválido e a resposta vira 200. Pra ver o
  401 de verdade, limpe os cookies do cliente ou use `--disable-cookies`.

Detalhe que só vale pra ferramenta de API: no **navegador** isso não acontece
desse jeito — o cookie `Secure` é aceito em `localhost` (contexto seguro), e o
frontend vai funcionar com `credentials: 'include'` no fetch.

## Sobre credenciais

O ambiente `bruno/environments/local.bru` tem e-mail e senha de um usuário de
teste criado no Postgres local do docker-compose. **Não é segredo** — é dado
descartável de desenvolvimento.

Os tokens (`accessToken`, `refreshToken`) estão declarados como
`vars:secret`, então o Bruno os guarda fora do arquivo e eles **não entram no
git**. Isso importa: sem isso, cada rodada da CLI gravaria um JWT válido
dentro de um arquivo versionado.

Se um dia apontar a collection pra staging/produção, **não edite o
`local.bru`**: crie um `environments/prod.bru` e coloque no `.gitignore`.

## Endpoints cobertos

| Método | Rota | Auth | Cenário |
|---|---|---|---|
| POST | `/usuarios` | pública | 201 professor / 201 aluno / 400 inválido / 409 duplicado / 400 JSON quebrado |
| POST | `/auth/login` | pública | 200 com tokens / 401 senha errada |
| POST | `/auth/refresh` | cookie | 200 rotação / 401 token inválido |
| GET | `/usuarios/me` | Bearer | 200 perfil / 401 sem token |
| GET | `/questoes` | Bearer | 200 paginado / filtros / 400 validação / 400 sort inválido |
| GET | `/questoes/{id}` | Bearer | 200 com alternativas / 404 anulada ou inexistente |
| GET | `/questoes/anos` | Bearer | 200 com os anos que têm questão (vêm dos dados, não de constante) |
| GET | `/imagens/**` | pública | 200 imagem da questão |

A pasta **Questões** precisa das questões importadas no banco:

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="--importar-questoes --anos=2023"
```

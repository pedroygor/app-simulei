# Simulei

Simulados gratuitos do ENEM. Você monta a prova com questões de edições anteriores, responde pelo celular e vê onde erra, por área e por assunto, para saber o que treinar.

> **Estado:** em desenvolvimento, ainda sem versão hospedada. O código é aberto para quem quiser estudar ou rodar localmente.

## O que dá para fazer

- **Montar uma prova** escolhendo áreas, matérias ou assuntos (por exemplo, "Probabilidade + Função do 1º grau"), os anos e quantas questões quer. O sorteio prioriza questões que você ainda não viu.
- **Responder pelo celular**, com ou sem cronômetro. As respostas são salvas na hora, e o tempo é controlado pelo servidor.
- **Ver o resultado** com gabarito, acerto por área e por assunto, e a revisão questão a questão.
- **Acompanhar o desempenho**: os assuntos em que você menos acerta (só a partir de 5 respostas, para não confundir sorte com conhecimento), a evolução por semana e um atalho para treinar o assunto mais fraco.

## De onde vêm as questões

As questões são das provas do ENEM (INEP), importadas da API pública [enem.dev](https://enem.dev). Elas **não estão neste repositório**: são baixadas na instalação.

Por cima delas, o Simulei acrescenta dados próprios, que estão em [`dados/`](dados/):

- a **classificação de cada questão por assunto**, numa taxonomia de 118 assuntos ancorada na Matriz de Referência do ENEM;
- o **gabarito e as anulações conferidos com os microdados do INEP**, que prevalecem quando a fonte diverge;
- a **habilidade oficial** e a **dificuldade (TRI)** de cada questão.

Detalhes em [`docs/taxonomia.md`](docs/taxonomia.md), [`docs/conferencia-inep.md`](docs/conferencia-inep.md) e [`docs/habilidade-x-assunto.md`](docs/habilidade-x-assunto.md).

## Stack

- **Backend:** Java 21, Spring Boot 4.1, Spring Data JPA, Spring Security com JWT, Flyway, PostgreSQL 16
- **Frontend:** Next.js 16 (App Router), React 19, TypeScript, Tailwind CSS 4, shadcn/ui, TanStack Query
- **Testes:** JUnit 5 e Testcontainers (Postgres de verdade, sem H2) no backend; Vitest no frontend
- **Ferramentas de dados:** Python 3, só biblioteca padrão, em [`ferramentas/`](ferramentas/)

## Rodando localmente

**Requisitos:** Docker (com Docker Compose), Java 21, Node.js 20.9 ou mais novo. Python 3 só se for mexer nas ferramentas de dados.

### 1. Configuração

```bash
git clone <este repositório> simulei
cd simulei
cp .env.example .env
```

No `.env`, troque as duas senhas do banco e gere o segredo do JWT (mínimo de 32 bytes):

```bash
openssl rand -base64 64 | tr -d '\n'
```

O `.env` nunca deve ser commitado (já está no `.gitignore`).

### 2. Banco e API

```bash
docker compose up -d          # Postgres 16 na porta 5433 do host
cd backend
./mvnw spring-boot:run        # API em http://localhost:8080; as migrations rodam sozinhas
```

### 3. Questões e dados

Ainda dentro de `backend/`, **nesta ordem**. Cada comando sobe a aplicação só para a carga, sem servidor web, então pode rodar com a API no ar:

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="--importar-questoes"        # da enem.dev, com as imagens
./mvnw spring-boot:run -Dspring-boot.run.arguments="--importar-classificacao"   # assunto de cada questão
./mvnw spring-boot:run -Dspring-boot.run.arguments="--aplicar-conferencia-inep" # gabaritos e anulações do INEP
./mvnw spring-boot:run -Dspring-boot.run.arguments="--importar-habilidades"     # habilidade oficial e dificuldade
./mvnw spring-boot:run -Dspring-boot.run.arguments="--marcar-incompletas"       # questões sem o texto-base
```

A importação das questões leva alguns minutos: a enem.dev limita as requisições, e as imagens são baixadas para `storage/imagens/`. Para importar só um ano, use `--importar-questoes --anos=2023`. Todas as cargas podem ser repetidas sem duplicar nada.

### 4. Frontend

```bash
cd frontend
cp .env.example .env.local
npm ci
npm run dev                   # http://localhost:3000
```

Depois é criar uma conta pela tela de cadastro.

## Testes

```bash
cd backend && ./mvnw test                                    # precisa do Docker (Testcontainers)
cd frontend && npm test && npx tsc --noEmit && npx eslint .
python3 -m unittest discover -s ferramentas/inep             # ferramentas de dados
```

## Estrutura

```
backend/      API em Spring Boot, organizada por domínio (questao, simulado, desempenho, usuario)
frontend/     Next.js, mobile-first (toda tela é desenhada para 360px primeiro)
dados/        dados produzidos pelo projeto (classificação, conferência com o INEP), CC BY-SA 4.0
ferramentas/  scripts Python de classificação e de conferência com os microdados do INEP
docs/         documentação: taxonomia, conferência com o INEP, identidade visual, coleções da API
```

## Contribuindo

Por enquanto o projeto não aceita pull requests, mas issues são muito bem-vindas, principalmente para apontar **questão classificada no assunto errado**. Veja o [CONTRIBUTING.md](CONTRIBUTING.md).

## Licença

- **Código:** [GNU Affero General Public License v3.0](LICENSE). Quem hospedar uma versão modificada precisa publicar o código dela.
- **Dados em [`dados/`](dados/):** [Creative Commons Atribuição-CompartilhaIgual 4.0](dados/LICENSE).
- **Créditos e conteúdo de terceiros:** veja o [NOTICE](NOTICE).

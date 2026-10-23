# Decisões de projeto

Registro curto das decisões que não são óbvias olhando o código, e do porquê de cada uma. Ordem de data. Decisões substituídas continuam aqui, marcadas, porque o motivo da troca também é informação.

Detalhes técnicos de cada uma estão em [arquitetura.md](arquitetura.md) e [dados.md](dados.md).

---

## Agosto de 2026

### `Simulado` separado de `Tentativa` desde o início
O simulado é a definição da prova; a tentativa é a execução dela por um aluno. Separar desde o começo permite, no futuro, "refazer" ou uma mesma prova feita por várias pessoas sem reescrever o domínio.

### A resposta guarda a alternativa, não a letra
`respostas.alternativa_id` aponta para a alternativa. A letra deixa de significar alguma coisa no dia em que as alternativas forem embaralhadas.

### Spring Boot no backend
Java e Spring são a stack em que o projeto quer investir. Spring Boot 4.1 e não 3.x (21/08/2026): o start.spring.io já não oferecia 3.x, e começar um projeto novo numa versão fora de suporte seria pior.

### Domínio em português, infraestrutura em inglês
O vocabulário do ENEM (questão, prova, tentativa, assunto) é mais claro em português para quem desenvolve e para quem usa. Classes técnicas genéricas (`JwtService`, `GlobalExceptionHandler`) ficam em inglês.

### Testcontainers, não H2
Os testes de integração rodam contra um Postgres de verdade. O H2 diverge do Postgres e esconderia justamente os bugs de SQL que mais importam aqui (`NULLS NOT DISTINCT`, colunas geradas, `date_trunc` com fuso).

### Imagens das questões públicas, com a defesa na ingestão (26/08/2026)
Prova do ENEM é pública, e exigir login para ver uma imagem custaria fetch autenticado sem proteger nada. O risco real é outro: o Simulei baixa arquivos de um terceiro e os serve do próprio domínio. A importação inicial trouxe 6 SVGs, e SVG pode carregar script. A defesa foi para onde o risco mora: allowlist por magic bytes (PNG, JPEG, GIF e WEBP, sem SVG), teto de tamanho e cabeçalhos que impedem execução ao servir.

### Nome do arquivo de imagem = hash SHA-256 do conteúdo (26/08/2026)
Path traversal deixa de ser possível por construção (nada que venha de fora entra no caminho), conteúdo repetido é gravado uma vez só, e o `Cache-Control: immutable` fica honesto: URL diferente significa bytes diferentes.

### ~~Cursinhos e professores como clientes pagantes; o aluno como usuário~~
**Substituída em 15/10/2026.** O plano inicial era vender para cursinhos.

### ~~Online pelo celular antes de papel com correção por foto~~
**Em parte substituída em 15/10/2026.** O "celular primeiro" continua: todo estudante tem celular, mesmo onde não tem computador. O papel com correção por foto saiu dos planos, porque servia a cursinho aplicando prova.

---

## Setembro de 2026

### ~~Dois produtos (escola e aluno) num código só~~ (25/09/2026)
**Substituída em 15/10/2026**, junto com o modelo pago.

### Assunto classificado por IA, com taxonomia fechada e revisão (25/09/2026)
O INEP publica a habilidade (H1–H30) e os parâmetros da TRI de cada questão, mas **não o assunto**: "função do 1º grau" é taxonomia de cursinho. Classificar 2.741 questões à mão levaria semanas. O modelo só escolhe entre os 118 assuntos de uma lista aprovada, declara a confiança, e um piloto revisado mede o acerto antes da rodada completa. Um assunto principal por questão, para que filtros e estatísticas não contem a mesma questão duas vezes.

### Classificação feita sem custo de API (29/09/2026)
A classificação foi feita pelo Claude dentro do Claude Code, sem gasto por token: scripts Python exportam lotes em texto (com a taxonomia permitida e as imagens), o modelo classifica, e um validador confere slug, área e duplicatas antes da carga. Mais lento, e retomável entre sessões.

### A área da questão vem da posição no caderno, não da fonte (29/09/2026)
A enem.dev erra a área de 5,4% das questões (147 de 2.741). A posição no caderno é fato do exame, em blocos de 45. Tinha que ser corrigido antes da classificação: o banco recusa assunto de outra área, e o diagnóstico por área herdaria o erro.

### O INEP prevalece sobre a enem.dev no gabarito e na anulação (30/09/2026)
Comparando os 15 anos, a enem.dev divergiu do INEP em 14 gabaritos (0,5%) e guardava com letra 6 questões anuladas. Gabarito errado corrige a prova do aluno ao contrário. As correções vivem num CSV versionado, aplicado por carga idempotente, e a questão anulada é marcada, não apagada, para a importação não recriá-la.

---

## Outubro de 2026

### Questão sem texto-base é marcada `incompleta` e some de tudo (01/10/2026)
A fonte entregou 77 questões sem o texto ou a imagem de que a pergunta depende: ninguém consegue respondê-las, e o erro viraria diagnóstico falso. A regra "pode ser mostrada ou sorteada" virou **uma coluna gerada pelo banco** (`disponivel = NOT anulada AND NOT incompleta`), para que nenhuma consulta nova esqueça um dos motivos de exclusão.

### Questão anulada depois da montagem sai da prova, sem substituta (02/10/2026)
Se uma carga futura do INEP anular uma questão que já está num simulado, ela não aparece mais, não aceita resposta e não conta na correção: a prova encolhe e é renumerada. Trocar por outra mudaria a prova de quem já começou.

### ~~Gabarito no detalhe da questão só para professor~~ (01/10/2026)
**Substituída em 15/10/2026** (ver abaixo).

### Simulei gratuito, para a comunidade, sustentado por doação; foco só no aluno (15/10/2026)
Substitui o modelo de venda para cursinhos. Saem os planos pagos, turma e professor, prova em papel e embaralhamento por aluno (serviam a cursinho) e a resolução comentada por IA (custo por uso sem receita). Fica tudo o que é do aluno. Consequências: o público é praticamente todo adolescente, então a LGPD (art. 14) é requisito antes de qualquer uso real; e a hospedagem é paga do bolso ou por doações.

### Sem a rodada de entrevistas com cursinhos (15/10/2026)
Ela existia para validar preço e uso em sala, perguntas de um produto vendido a escolas. O que ainda importa (o aluno usa no celular? papel faz falta?) passa a ser respondido pelo uso real. Risco aceito conscientemente.

### Código aberto sob AGPL-3.0; dados sob CC BY-SA 4.0 (15/10/2026)
Doação depende de confiança, e código aberto mostra para onde vai o apoio; também abre espaço para contribuição, como a revisão de classificação. **AGPL e não MIT:** com MIT, qualquer um poderia fechar o código e vendê-lo, que é o modelo descartado. A AGPL obriga quem hospedar uma versão modificada a publicar o código dela. Ela não impede cópia idêntica nem uso comercial; proibir uso comercial deixaria de ser código aberto.

### Perfil de professor removido; gabarito no detalhe da questão para todo aluno (15/10/2026)
Consequência do produto só do aluno. As contas de professor existentes viraram contas de aluno, e o cadastro não escolhe mais o papel (o servidor decide). O mecanismo de papel continua, para um papel futuro (por exemplo, administração). O gabarito no banco de questões é treino, e treinar sem conferir não serve; a **prova** continua sem gabarito até a entrega.

### O diagnóstico por assunto conta só as questões respondidas (16/10/2026)
Questão em branco é ambígua: pode ser "não sei", "acabou o tempo" ou "pulei". O tempo acabando castiga as últimas questões, e numa prova agrupada por assunto o último levaria a culpa do relógio. A nota da prova continua contando a em branco como zero, que é a regra do ENEM; o diagnóstico mostra a em branco à parte. Também não se força resposta: chute não se distingue de resposta e polui o diagnóstico. Em vez disso, a entrega lembra que errar não tira ponto e leva à primeira em branco.

### A evolução do desempenho é por semana, no fuso de Brasília (20/10/2026)
Por prova enganaria: cada prova tem conteúdo diferente, e uma prova mais fácil pareceria melhora. Na semana, várias provas se somam. A semana vai de segunda a domingo no fuso de Brasília (o servidor roda em UTC), e a prova conta na semana em que foi entregue. Semana sem prova é um buraco no gráfico, nunca 0%. Janela padrão de 4 semanas.

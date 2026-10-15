-- O perfil PROFESSOR sai do produto (decidido em 15/10/2026: Simulei gratuito e
-- só do aluno; turma e professor fora dos planos — ver ADR).
--
-- Os usuários que se cadastraram como professor viram alunos, em vez de serem
-- apagados: a conta, os simulados e as tentativas continuam deles.
UPDATE usuarios SET role = 'ALUNO' WHERE role = 'PROFESSOR';

-- O CHECK passa a aceitar só ALUNO: sem isto, um INSERT esquecido (carga, script)
-- ainda poderia criar professor por fora da API. A COLUNA fica: o mecanismo de
-- papel (claim no JWT -> authority -> @PreAuthorize) é o que um papel futuro, como
-- um ADMIN pra revisar classificação, vai usar
ALTER TABLE usuarios DROP CONSTRAINT ck_usuarios_role;
ALTER TABLE usuarios ADD CONSTRAINT ck_usuarios_role CHECK (role IN ('ALUNO'));

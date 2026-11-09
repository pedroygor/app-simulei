-- O e-mail passa a ser guardado sempre em minúsculas (a aplicação normaliza no
-- cadastro e no login, em Emails.normalizar). Antes, "Ana@x.com" e "ana@x.com"
-- viravam duas contas, e o login com a outra grafia falhava. O login com Google,
-- que manda o e-mail em minúsculas, depende disso pra achar a conta existente.

-- Duas contas que só diferem na maiúscula não são juntadas aqui: escolher qual
-- fica (e o que fazer com as provas da outra) é decisão humana. A migration
-- para, em vez de decidir sozinha ou falhar na UNIQUE com uma mensagem obscura
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM usuarios GROUP BY lower(email) HAVING count(*) > 1) THEN
        RAISE EXCEPTION 'Há contas cujo e-mail só difere em maiúsculas e minúsculas. '
            'Resolva à mão (SELECT lower(email), count(*) FROM usuarios GROUP BY 1 HAVING count(*) > 1) '
            'antes de aplicar esta migration';
    END IF;
END $$;

UPDATE usuarios SET email = lower(email) WHERE email <> lower(email);

-- Com tudo em minúsculas, a UNIQUE (email) que já existe passa a valer sem
-- diferenciar maiúscula. O CHECK garante que continue assim mesmo numa escrita
-- por fora da API (script, carga, SQL à mão)
ALTER TABLE usuarios ADD CONSTRAINT ck_usuarios_email_minusculo CHECK (email = lower(email));

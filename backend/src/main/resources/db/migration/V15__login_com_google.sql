-- Login com Google (Etapa 6). Uma conta passa a poder existir sem senha: quem
-- entra só pelo Google não tem senha_hash.
ALTER TABLE usuarios ALTER COLUMN senha_hash DROP NOT NULL;

-- O "sub" do ID token: o identificador da pessoa no Google, que não muda nunca
-- (o e-mail muda). É ele que liga a conta ao Google, não o e-mail. O Google
-- garante no máximo 255 caracteres
ALTER TABLE usuarios ADD COLUMN google_sub VARCHAR(255);

-- Uma conta Google liga a uma conta Simulei só. NULL não conflita com NULL no
-- UNIQUE do Postgres, então as contas sem Google convivem à vontade
ALTER TABLE usuarios ADD CONSTRAINT uk_usuarios_google_sub UNIQUE (google_sub);

-- Nenhuma conta fica sem jeito de entrar: tirar a senha de quem não tem Google
-- (ou o contrário) é recusado pelo banco, venha a escrita de onde vier
ALTER TABLE usuarios ADD CONSTRAINT ck_usuarios_forma_de_entrar
    CHECK (senha_hash IS NOT NULL OR google_sub IS NOT NULL);

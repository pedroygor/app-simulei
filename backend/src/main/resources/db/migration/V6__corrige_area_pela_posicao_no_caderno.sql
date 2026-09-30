-- A enem.dev erra o "discipline" de 147 das 2.741 questões (5,4%): química e
-- matemática marcadas como Ciências Humanas, por exemplo (2022 q108, ácido
-- tartárico; 2012 q164, dosagem de remédio). A área de uma questão do ENEM é
-- a POSIÇÃO dela na prova, em blocos fixos de 45:
--
--   2009       Natureza | Humanas  | Linguagens | Matemática
--   2010-2016  Humanas  | Natureza | Linguagens | Matemática
--   2017+      Linguagens | Humanas | Natureza  | Matemática
--
-- A mesma regra vive em CadernoDoEnem.java, que o importador usa daqui pra
-- frente. Esta migration conserta o que já foi importado.

DO $$
BEGIN
    -- Mudar a área de uma questão JÁ classificada violaria a FK composta
    -- (assunto_id, area): o assunto é de uma área só. Hoje nenhuma está
    -- classificada; se um dia estiver, é decisão humana, não UPDATE em massa
    IF EXISTS (SELECT 1 FROM questoes WHERE assunto_id IS NOT NULL) THEN
        RAISE EXCEPTION 'V6: há questões classificadas por assunto; corrigir área agora quebraria a classificação';
    END IF;
    IF EXISTS (SELECT 1 FROM questoes WHERE numero NOT BETWEEN 1 AND 180) THEN
        RAISE EXCEPTION 'V6: há questão com número fora de 1..180; a regra da posição não se aplica';
    END IF;
END $$;

UPDATE questoes q
SET area = p.area
FROM (
    SELECT id,
           CASE
               WHEN ano <= 2009 THEN (ARRAY['CIENCIAS_NATUREZA', 'CIENCIAS_HUMANAS', 'LINGUAGENS', 'MATEMATICA'])[(numero - 1) / 45 + 1]
               WHEN ano <= 2016 THEN (ARRAY['CIENCIAS_HUMANAS', 'CIENCIAS_NATUREZA', 'LINGUAGENS', 'MATEMATICA'])[(numero - 1) / 45 + 1]
               ELSE                  (ARRAY['LINGUAGENS', 'CIENCIAS_HUMANAS', 'CIENCIAS_NATUREZA', 'MATEMATICA'])[(numero - 1) / 45 + 1]
           END AS area
    FROM questoes
) p
WHERE q.id = p.id
  AND q.area <> p.area;

-- Autoconferência: depois da correção, nenhuma área de nenhum ano pode ter
-- mais de 45 questões por variante de idioma. Foi esta régua que revelou o
-- problema (Humanas tinha de 47 a 79 por ano)
DO $$
DECLARE
    estourou RECORD;
BEGIN
    SELECT ano, area, count(*) AS total INTO estourou
    FROM questoes
    GROUP BY ano, area, idioma
    HAVING count(*) > 45
    LIMIT 1;
    IF FOUND THEN
        RAISE EXCEPTION 'V6: % tem % questões de % numa mesma variante (máximo 45)',
            estourou.ano, estourou.total, estourou.area;
    END IF;
END $$;

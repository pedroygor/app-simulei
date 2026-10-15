"""Testes das partes puras da amostra (sem banco).

Uso (da raiz): python3 -m unittest discover -s ferramentas/classificacao
"""

import unittest

from amostra_revisao import conferir_folha, contar, sortear, wilson

TAXONOMIA = {
    "cinematica": {"area": "CIENCIAS_NATUREZA", "materia": "Física"},
    "dinamica": {"area": "CIENCIAS_NATUREZA", "materia": "Física"},
    "genetica": {"area": "CIENCIAS_NATUREZA", "materia": "Biologia"},
    "idade-media": {"area": "CIENCIAS_HUMANAS", "materia": "História"},
}


def linha(atribuido="cinematica", veredito="CERTO", correto="", area="CIENCIAS_NATUREZA", confianca="alta"):
    return {"ano": "2020", "numero": "100", "idioma": "", "area": area, "assunto_atribuido": atribuido,
            "veredito": veredito, "assunto_correto": correto, "obs": "", "confianca": confianca}


class SortearTest(unittest.TestCase):
    def test_pula_excluidas_e_completa_com_as_seguintes(self):
        candidatas = [{"ano": 2020, "numero": n, "idioma": None, "area": "CIENCIAS_NATUREZA"} for n in (1, 2, 3, 4)]
        amostra = sortear(candidatas, {(2020, 2, "")}, 2)
        self.assertEqual([q["numero"] for q in amostra], [1, 3])

    def test_limita_por_area_e_ordena_natureza_antes_de_humanas(self):
        candidatas = [{"ano": 2020, "numero": n, "idioma": None, "area": a}
                      for n, a in ((1, "CIENCIAS_HUMANAS"), (2, "CIENCIAS_NATUREZA"), (3, "CIENCIAS_NATUREZA"))]
        amostra = sortear(candidatas, set(), 1)
        self.assertEqual([q["numero"] for q in amostra], [2, 1])


class WilsonTest(unittest.TestCase):
    def test_acerto_total_nao_passa_de_100_nem_encolhe_a_zero(self):
        baixo, alto = wilson(60, 60)
        self.assertAlmostEqual(alto, 1.0)
        self.assertLess(baixo, 0.95)

    def test_valor_conhecido(self):
        baixo, alto = wilson(57, 60)
        self.assertAlmostEqual(baixo, 0.863, places=3)
        self.assertAlmostEqual(alto, 0.983, places=3)


class ConferirFolhaTest(unittest.TestCase):
    def test_folha_valida(self):
        linhas = [linha(), linha(veredito="errado", correto="dinamica")]
        self.assertEqual(conferir_folha(linhas, TAXONOMIA), [])

    def test_veredito_em_branco_e_problema(self):
        self.assertEqual(len(conferir_folha([linha(veredito="")], TAXONOMIA)), 1)

    def test_errado_exige_assunto_existente_da_mesma_area_e_diferente(self):
        casos = [linha(veredito="ERRADO"),
                 linha(veredito="ERRADO", correto="nao-existe"),
                 linha(veredito="ERRADO", correto="idade-media"),
                 linha(veredito="ERRADO", correto="cinematica")]
        for caso in casos:
            self.assertEqual(len(conferir_folha([caso], TAXONOMIA)), 1, caso)

    def test_certo_com_correcao_e_contraditorio(self):
        self.assertEqual(len(conferir_folha([linha(correto="dinamica")], TAXONOMIA)), 1)


class ContarTest(unittest.TestCase):
    def test_assunto_errado_na_mesma_materia_conta_como_materia_certa(self):
        linhas = [linha(),
                  linha(veredito="ERRADO", correto="dinamica", confianca="media"),
                  linha(veredito="ERRADO", correto="genetica", confianca="media")]
        grupos = contar(linhas, TAXONOMIA)
        self.assertEqual(grupos[("CIENCIAS_NATUREZA", "todas")], [3, 1, 2])
        self.assertEqual(grupos[("CIENCIAS_NATUREZA", "media")], [2, 0, 1])


if __name__ == "__main__":
    unittest.main()

"""
Testes do gerador do habilidades.csv. O arquivo vai pro banco e pra tela como dado
OFICIAL, então valor fora do domínio tem que parar o script, não ser gravado.

    python3 -m unittest discover -s ferramentas/inep -v
"""

import unittest

import gerar_habilidades as g
from microdados import ItemDoInep


def item(**campos):
    base = dict(ano=2020, numero=100, idioma="", area="CN", co_item="1", gabarito="A",
                abandonado=False, motivo_do_abandono="", co_prova="1", cor="Azul",
                habilidade="17", dificuldade="0.53")
    return ItemDoInep(**{**base, **campos})


class LinhasDoCsvTest(unittest.TestCase):
    def test_converte_habilidade_e_mantem_a_dificuldade_como_veio(self):
        [linha] = g.linhas_do_csv([item()])
        self.assertEqual(linha["habilidade"], 17)
        self.assertEqual(linha["dificuldade"], "0.53")

    def test_item_sem_habilidade_nem_dificuldade_fica_de_fora(self):
        self.assertEqual(g.linhas_do_csv([item(habilidade="", dificuldade="")]), [])

    def test_so_dificuldade_entra_com_habilidade_vazia(self):
        # 2009 fora de Matemática: o INEP publica o parâmetro B mas não a habilidade
        [linha] = g.linhas_do_csv([item(habilidade="")])
        self.assertEqual(linha["habilidade"], "")

    def test_recusa_habilidade_fora_de_1_a_30(self):
        for ruim in ("0", "31", "H5"):
            with self.assertRaises(ValueError, msg=ruim):
                g.linhas_do_csv([item(habilidade=ruim)])

    def test_recusa_dificuldade_que_nao_e_numero(self):
        with self.assertRaises(ValueError):
            g.linhas_do_csv([item(dificuldade="nan")])

    def test_recusa_questao_repetida(self):
        with self.assertRaises(ValueError):
            g.linhas_do_csv([item(co_item="1"), item(co_item="2")])

    def test_idioma_faz_parte_da_chave(self):
        linhas = g.linhas_do_csv([item(idioma="INGLES"), item(idioma="ESPANHOL", co_item="2")])
        self.assertEqual([l["idioma"] for l in linhas], ["ESPANHOL", "INGLES"])


if __name__ == "__main__":
    unittest.main()

"""
Testes da numeração de microdados.numerar — a parte que quebra em silêncio: um
deslocamento errado não dá erro, dá "divergências" falsas e correção errada.

Cada caso reproduz um layout REAL do ITENS_PROVA descoberto medindo os 15 anos.

    python3 -m unittest discover -s ferramentas/inep -v
"""

import unittest

import microdados


def item(pos, gabarito="A", lingua=""):
    return {"CO_POSICAO": str(pos), "TX_GABARITO": gabarito, "TP_LINGUA": lingua, "CO_ITEM": f"i{pos}"}


class NumerarTest(unittest.TestCase):
    def test_2016_humanas_vem_em_46_a_90_mas_e_a_primeira_area_da_prova(self):
        # 2010–2016: Humanas abre a prova (1–45), mas o INEP guarda 46–90
        itens = [item(p) for p in range(46, 91)]

        numeradas = microdados.numerar(2016, "CH", itens)

        self.assertEqual(min(n for n, _ in numeradas), 1)
        self.assertEqual(max(n for n, _ in numeradas), 45)

    def test_2017_posicao_e_local_por_caderno_e_o_bloco_define_o_numero(self):
        # 2017: cada área recomeça em 1; Natureza é o 3º bloco (91–135)
        numeradas = microdados.numerar(2017, "CN", [item(p) for p in range(1, 46)])

        self.assertEqual(min(n for n, _ in numeradas), 91)
        self.assertEqual(max(n for n, _ in numeradas), 135)

    def test_2017_linguagens_ingles_1_a_5_espanhol_6_a_10_comuns_11_a_50(self):
        itens = (
            [item(p, lingua="0") for p in range(1, 6)]
            + [item(p, lingua="1") for p in range(6, 11)]
            + [item(p) for p in range(11, 51)]
        )

        numeradas = microdados.numerar(2017, "LC", itens)

        # Idioma: posição 1 (inglês) e posição 6 (espanhol) são a MESMA questão 1
        self.assertIn((1, "INGLES"), numeradas)
        self.assertIn((1, "ESPANHOL"), numeradas)
        self.assertEqual(numeradas[(1, "ESPANHOL")]["CO_POSICAO"], "6")
        self.assertEqual(numeradas[(5, "ESPANHOL")]["CO_POSICAO"], "10")
        # Comuns: posição 11 do INEP é a questão 6 do nosso banco, e vão até a 45
        self.assertEqual(numeradas[(6, "")]["CO_POSICAO"], "11")
        self.assertEqual(max(n for n, i in numeradas if i == ""), 45)

    def test_linguagens_de_2023_tem_os_dois_idiomas_na_mesma_posicao(self):
        itens = (
            [item(p, lingua="0") for p in range(1, 6)]
            + [item(p, lingua="1") for p in range(1, 6)]
            + [item(p) for p in range(6, 46)]
        )

        numeradas = microdados.numerar(2023, "LC", itens)

        self.assertEqual(numeradas[(3, "INGLES")]["TP_LINGUA"], "0")
        self.assertEqual(numeradas[(3, "ESPANHOL")]["TP_LINGUA"], "1")
        self.assertEqual(numeradas[(6, "")]["CO_POSICAO"], "6")

    def test_2009_linguagens_nao_tem_idioma_e_nao_ganha_deslocamento_de_5(self):
        # Bug real da 1ª versão: somar 5 sempre errava todo o LC de 2009 (14/44)
        itens = [item(p) for p in range(91, 136)]  # sem coluna TP_LINGUA

        numeradas = microdados.numerar(2009, "LC", itens)

        self.assertEqual(min(n for n, _ in numeradas), 91)
        self.assertEqual(max(n for n, _ in numeradas), 135)

    def test_item_faltando_nao_desloca_os_seguintes(self):
        # 2021 Matemática lista 44 itens. Numerar por rank empurraria tudo depois
        # do buraco; por posição, o buraco fica no lugar
        itens = [item(p) for p in range(136, 181) if p != 150]

        numeradas = microdados.numerar(2021, "MT", itens)

        self.assertNotIn((150, ""), numeradas)
        self.assertEqual(numeradas[(151, "")]["CO_POSICAO"], "151")
        self.assertEqual(len(numeradas), 44)

    def test_ordem_dos_blocos_de_area_por_ano(self):
        # Mesma ordem do V6/CadernoDoEnem — segunda fonte independente do mesmo fato
        self.assertEqual([microdados.inicio_do_bloco(2009, a) for a in ("CN", "CH", "LC", "MT")], [1, 46, 91, 136])
        self.assertEqual([microdados.inicio_do_bloco(2012, a) for a in ("CH", "CN", "LC", "MT")], [1, 46, 91, 136])
        self.assertEqual([microdados.inicio_do_bloco(2019, a) for a in ("LC", "CH", "CN", "MT")], [1, 46, 91, 136])


class CasarTest(unittest.TestCase):
    def test_recusa_mapeamento_que_nao_bate_com_o_banco(self):
        # Se nenhum caderno concorda com o gabarito do banco, casar tem que falhar
        # alto: aceitar geraria "correções" para questões que estão certas
        cadernos = {("MT", "1"): [item(p, "A") for p in range(136, 181)]}
        banco = {(2020, n, ""): "B" for n in range(136, 181)}

        with self.assertRaises(RuntimeError):
            microdados._escolher_caderno(2020, "MT", cadernos, banco)


if __name__ == "__main__":
    unittest.main()

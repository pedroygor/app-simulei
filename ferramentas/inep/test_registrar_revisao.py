"""
Testes do registro de revisão. Mexe no assuntos.csv (fonte da classificação) por
linha, então um erro aqui corromperia dado em silêncio: por isso é testado.

    python3 -m unittest discover -s ferramentas/inep -v
"""

import unittest

import registrar_revisao as r


def decisao(**campos):
    base = {"ano": "2020", "numero": "20", "idioma": "", "habilidade": "29", "assunto_anterior": "a",
            "assunto_atual": "b", "confianca": "media", "decisao": "RECLASSIFICADA", "motivo": "porque"}
    return {**base, **campos}


LINHAS = [
    "ano,numero,idioma,assunto,confianca,justificativa",
    "2020,20,,a,media,antiga",
    "2020,21,,x,alta,\"com vírgula, aspas\"",
    "2023,1,INGLES,interpretacao-ingles,alta,idioma",
]


class AplicarEmLinhasTest(unittest.TestCase):
    def test_troca_so_a_linha_da_decisao(self):
        linhas = list(LINHAS)

        alteradas = r.aplicar_em_linhas(linhas, [decisao()])

        self.assertEqual(alteradas, 1)
        self.assertEqual(linhas[0], LINHAS[0])
        self.assertEqual(linhas[2:], LINHAS[2:])  # as outras, byte a byte
        # A justificativa leva uma vírgula ("INEP, H29"), então vem entre aspas
        self.assertEqual(linhas[1], '2020,20,,b,media,"porque [conferida com o INEP, H29: reclassificada]"')

    def test_motivo_com_virgula_fica_entre_aspas(self):
        linhas = list(LINHAS)

        r.aplicar_em_linhas(linhas, [decisao(motivo="texto, com vírgula")])

        self.assertIn('"texto, com vírgula [conferida', linhas[1])

    def test_chave_inclui_o_idioma(self):
        linhas = list(LINHAS)

        r.aplicar_em_linhas(linhas, [decisao(ano="2023", numero="1", idioma="INGLES",
                                             assunto_anterior="interpretacao-ingles", assunto_atual="outro")])

        self.assertIn(",outro,", linhas[3])

    def test_recusa_se_o_assunto_anterior_nao_bate(self):
        # A revisão foi feita sobre outro estado do arquivo: melhor parar do que sobrescrever
        with self.assertRaises(ValueError):
            r.aplicar_em_linhas(list(LINHAS), [decisao(assunto_anterior="nao-era-esse")])

    def test_recusa_questao_inexistente(self):
        with self.assertRaises(KeyError):
            r.aplicar_em_linhas(list(LINHAS), [decisao(ano="1999")])

    def test_decisao_igual_ao_que_ja_esta_nao_conta_como_alteracao(self):
        linhas = list(LINHAS)
        r.aplicar_em_linhas(linhas, [decisao()])

        # mesma decisão com o estado de partida já aplicado: 'a' -> 'b' falharia; aqui MANTIDA em 'b'
        alteradas = r.aplicar_em_linhas(linhas, [decisao(decisao="MANTIDA", assunto_anterior="b", assunto_atual="b")])

        self.assertEqual(alteradas, 1)  # só a justificativa muda (de reclassificada pra mantida)


class FimDeLinhaTest(unittest.TestCase):
    def test_arquivo_com_crlf_e_lf_misturados_volta_identico(self):
        texto = "a,b\r\n2020,20,,a,media,x\n2020,21,,x,alta,y\r\n"

        linhas, fins = r.dividir_linhas(texto)

        self.assertEqual(linhas[1], "2020,20,,a,media,x")
        self.assertEqual(r.juntar_linhas(linhas, fins), texto)

    def test_acha_a_questao_em_trecho_lf_de_arquivo_com_crlf(self):
        linhas, fins = r.dividir_linhas("a,b\r\n2020,20,,a,media,x\n2020,21,,x,alta,y\r\n")

        r.aplicar_em_linhas(linhas, [decisao()])  # 2020/20 está na linha LF

        self.assertIn(",b,", r.juntar_linhas(linhas, fins).split("\n")[1])


class MesclarRevisoesTest(unittest.TestCase):
    def test_decisao_nova_substitui_a_da_mesma_questao(self):
        existentes = [{c: "x" for c in r.COLUNAS_REVISAO} | {"ano": "2020", "numero": "20", "idioma": ""}]

        registro = r.mesclar_revisoes(existentes, [decisao()])

        self.assertEqual(len(registro), 1)
        self.assertEqual(registro[0]["assunto_atual"], "b")

    def test_acumula_e_ordena(self):
        registro = r.mesclar_revisoes([], [decisao(ano="2021", numero="5"), decisao(ano="2009", numero="9")])

        self.assertEqual([x["ano"] for x in registro], ["2009", "2021"])


class LerDecisoesTest(unittest.TestCase):
    def _arquivo(self, linhas):
        import tempfile
        import pathlib
        d = pathlib.Path(tempfile.mkdtemp()) / "d.csv"
        d.write_text("\n".join([",".join(r.COLUNAS_DECISAO)] + linhas) + "\n", encoding="utf-8")
        return d

    def test_mantida_precisa_ter_o_mesmo_assunto(self):
        with self.assertRaises(ValueError):
            r.ler_decisoes(self._arquivo(["2020,20,,29,a,b,media,MANTIDA,x"]))

    def test_reclassificada_precisa_mudar_de_assunto(self):
        with self.assertRaises(ValueError):
            r.ler_decisoes(self._arquivo(["2020,20,,29,a,a,media,RECLASSIFICADA,x"]))

    def test_recusa_questao_repetida(self):
        with self.assertRaises(ValueError):
            r.ler_decisoes(self._arquivo(["2020,20,,29,a,b,media,RECLASSIFICADA,x", "2020,20,,29,a,c,media,RECLASSIFICADA,y"]))

    def test_aceita_decisao_valida(self):
        self.assertEqual(len(r.ler_decisoes(self._arquivo(["2020,20,,29,a,b,media,RECLASSIFICADA,x"]))), 1)


if __name__ == "__main__":
    unittest.main()

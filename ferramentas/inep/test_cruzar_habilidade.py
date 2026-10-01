"""
Testes da lógica pura do cruzamento habilidade × assunto e dos dados versionados.

    python3 -m unittest discover -s ferramentas/inep -v
"""

import csv
import unittest

import cruzar_habilidade as c

FAIXAS_MT = [(1, 5, 1), (6, 9, 2), (10, 14, 3), (15, 18, 4), (19, 23, 5), (24, 26, 6), (27, 30, 7)]


class CompetenciaDaTest(unittest.TestCase):
    def test_limites_de_cada_faixa(self):
        # Os limites são onde um off-by-one esconderia erro: H5/H6, H14/H15, H26/H27
        esperado = {1: 1, 5: 1, 6: 2, 9: 2, 10: 3, 14: 3, 15: 4, 18: 4, 19: 5, 23: 5, 24: 6, 26: 6, 27: 7, 30: 7}
        for habilidade, competencia in esperado.items():
            self.assertEqual(c.competencia_da(habilidade, FAIXAS_MT), competencia, f"H{habilidade}")

    def test_habilidade_fora_de_qualquer_faixa_e_erro_nao_silencio(self):
        with self.assertRaises(ValueError):
            c.competencia_da(31, FAIXAS_MT)


class ForaDaTabelaTest(unittest.TestCase):
    faixas = {"MT": FAIXAS_MT}
    tabela = {("MT", "estatistica"): {7: "NUCLEO", 6: "NUCLEO"}}

    def test_competencia_compativel_passa(self):
        self.assertIsNone(c.fora_da_tabela("MT", "estatistica", 27, self.faixas, self.tabela))

    def test_competencia_incompativel_devolve_qual_foi(self):
        # estatística com habilidade de geometria (H7 -> C2)
        self.assertEqual(c.fora_da_tabela("MT", "estatistica", 7, self.faixas, self.tabela), 2)

    def test_assunto_fora_da_tabela_levanta_em_vez_de_aprovar(self):
        # Tabela incompleta não pode virar "tudo compatível" em silêncio
        with self.assertRaises(KeyError):
            c.fora_da_tabela("MT", "assunto-novo", 7, self.faixas, self.tabela)


class JaRevisadaTest(unittest.TestCase):
    revisoes = {(2012, 168, ""): ("operacoes-e-conjuntos-numericos", "MANTIDA"),
                (2010, 159, ""): ("equacoes-e-inequacoes", "RECLASSIFICADA")}

    def test_mantida_com_o_mesmo_assunto_e_pulada(self):
        self.assertTrue(c.ja_revisada((2012, 168, ""), "operacoes-e-conjuntos-numericos", self.revisoes))

    def test_mantida_volta_a_ser_conferida_se_o_assunto_mudou(self):
        # A decisão de manter era sobre AQUELE assunto, não sobre a questão pra sempre
        self.assertFalse(c.ja_revisada((2012, 168, ""), "razao-e-proporcao", self.revisoes))

    def test_reclassificada_nao_e_pulada(self):
        # O assunto novo tem que ser compatível; se não for, é o que a conferência deve mostrar
        self.assertFalse(c.ja_revisada((2010, 159, ""), "equacoes-e-inequacoes", self.revisoes))

    def test_questao_nunca_revista_nao_e_pulada(self):
        self.assertFalse(c.ja_revisada((2020, 1, ""), "estatistica", self.revisoes))


class DadosVersionadosTest(unittest.TestCase):
    """Os CSVs são a tabela de verdade: um erro de digitação aqui não quebra nada
    visível, só deixa de conferir. Por isso são testados."""

    def test_faixas_de_matematica_cobrem_h1_a_h30_sem_buraco_nem_sobreposicao(self):
        faixas = sorted(c.ler_faixas()["MT"])
        self.assertEqual(faixas[0][0], 1)
        self.assertEqual(faixas[-1][1], 30)
        for (_, fim_anterior, _), (inicio, _, _) in zip(faixas, faixas[1:]):
            self.assertEqual(inicio, fim_anterior + 1)

    def test_todo_assunto_de_matematica_da_taxonomia_esta_na_tabela(self):
        import re
        import microdados
        v5 = next((microdados.RAIZ / "backend/src/main/resources/db/migration").glob("V5__*.sql")).read_text()
        slugs = set(re.findall(r"\('matematica', '([a-z0-9-]+)'", v5))
        na_tabela = {assunto for (area, assunto) in c.ler_compatibilidade() if area == "MT"}
        self.assertEqual(len(slugs), 20)
        self.assertEqual(slugs - na_tabela, set(), "assunto da taxonomia sem linha em compatibilidade.csv")
        self.assertEqual(na_tabela - slugs, set(), "linha em compatibilidade.csv de assunto que não existe")

    def test_papeis_validos_e_competencias_existentes(self):
        faixas = {comp for _, _, comp in c.ler_faixas()["MT"]}
        with (c.PASTA / "compatibilidade.csv").open(newline="", encoding="utf-8") as arquivo:
            for linha in csv.DictReader(arquivo):
                self.assertIn(linha["papel"], {"NUCLEO", "TOLERADA"}, linha)
                self.assertIn(int(linha["competencia"]), faixas, linha)

    def test_revisoes_apontam_pra_assuntos_e_decisoes_validos(self):
        tabela = c.ler_compatibilidade()
        assuntos = {a for (_, a) in tabela}
        for (ano, numero, _), (assunto, decisao) in c.ler_revisoes().items():
            self.assertIn(decisao, {"MANTIDA", "RECLASSIFICADA"}, (ano, numero))
            self.assertIn(assunto, assuntos, (ano, numero))

    def test_todo_assunto_tem_ao_menos_um_nucleo(self):
        # Assunto sem núcleo é assunto sem tema: a tabela estaria dizendo que ele
        # só existe "a serviço" de outra coisa
        for (area, assunto), competencias in c.ler_compatibilidade().items():
            self.assertIn("NUCLEO", competencias.values(), f"{area}/{assunto}")


if __name__ == "__main__":
    unittest.main()

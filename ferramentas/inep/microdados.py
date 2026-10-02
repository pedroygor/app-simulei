"""
Casa as questões do nosso banco com os itens do ITENS_PROVA do INEP.

É a base de tudo que cruza com o INEP (conferência de gabarito, questões
anuladas, e depois a habilidade oficial da 2b.7).

Como o casamento funciona — descoberto medindo os 15 anos (30/09/2026):

1. O INEP publica um `CO_PROVA` por ÁREA e por COR de caderno, e cada caderno
   põe os mesmos itens em ordens diferentes. A numeração da enem.dev é a de UM
   caderno. Qual? O que tiver o gabarito mais parecido com o do nosso banco: é
   sempre o AZUL da 1ª aplicação, e o teste de gabarito é o que prova isso,
   ano a ano, em vez de eu supor.

2. O `CO_POSICAO` do INEP NÃO é o número da questão:
   - 2016 e 2017 guardam posição LOCAL (2017: cada área recomeça em 1);
   - a ordem dos blocos de área muda (a mesma que o V6 e o `CadernoDoEnem`
     usam: 2009 Natureza primeiro; 2010–2016 Humanas primeiro; 2017+ Linguagens
     primeiro);
   - em Linguagens os 5 itens de idioma vêm em posições próprias (em 2017:
     inglês 1–5, espanhol 6–10, os 40 comuns em 11–50).
   Por isso numera-se por POSIÇÃO RELATIVA ao início do bloco, e não por rank:
   área com item faltando (2021 tem só 44 em Matemática) deslocaria tudo o que
   vem depois se a conta fosse por ordem de chegada.

3. `TP_LINGUA`: 0 = inglês, 1 = espanhol. Só existe a partir de 2010.

O casamento nunca é aceito no escuro: se nenhum caderno concordar com o banco
em pelo menos LIMITE_DE_CONFIANCA das questões, levanta erro. Um mapeamento
errado geraria "divergências" falsas e correções erradas — pior que nenhuma.
"""

import csv
from collections import defaultdict
from dataclasses import dataclass
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[2]
PASTA = RAIZ / "dados" / "inep"

AREAS = ("LC", "CH", "CN", "MT")
LIMITE_DE_CONFIANCA = 0.9
IDIOMA_POR_TP_LINGUA = {"0": "INGLES", "1": "ESPANHOL"}


@dataclass(frozen=True)
class ItemDoInep:
    ano: int
    numero: int
    idioma: str  # "" quando a questão não tem variante de idioma
    area: str  # LC | CH | CN | MT
    co_item: str
    gabarito: str  # A–E, ou X = anulada
    abandonado: bool
    motivo_do_abandono: str
    co_prova: str
    cor: str
    habilidade: str  # "" quando o INEP não publicou
    # Parâmetro B da TRI (dificuldade calibrada); "" quando o item foi abandonado
    # pela TRI ou o INEP não publicou. Default pra não quebrar quem monta à mão
    dificuldade: str = ""


def ler_itens(ano: int) -> list[dict]:
    # O INEP não é consistente: quase todos os anos são latin-1, mas em 2011 há
    # campos em UTF-8 no meio de um arquivo latin-1 (o motivo do abandono), então
    # nenhuma decodificação do arquivo inteiro acerta. Linha a linha: texto
    # latin-1 com acento quase nunca é UTF-8 válido, e a tentativa distingue
    def decodificar(linha: bytes) -> str:
        try:
            return linha.decode("utf-8")
        except UnicodeDecodeError:
            return linha.decode("latin-1")

    dados = (PASTA / f"ITENS_PROVA_{ano}.csv").read_bytes()
    texto = "\n".join(decodificar(l) for l in dados.splitlines())
    return list(csv.DictReader(texto.splitlines(), delimiter=";"))


def inicio_do_bloco(ano: int, area: str) -> int:
    if ano == 2009:
        ordem = ["CN", "CH", "LC", "MT"]
    elif ano <= 2016:
        ordem = ["CH", "CN", "LC", "MT"]
    else:
        ordem = ["LC", "CH", "CN", "MT"]
    return ordem.index(area) * 45 + 1


def _tp_lingua(linha: dict) -> str:
    # 2009 não tem a coluna
    return (linha.get("TP_LINGUA") or "").strip()


def numerar(ano: int, area: str, itens: list[dict]) -> dict[tuple[int, str], dict]:
    """{(numero, idioma): linha} de UM caderno, numerando por posição relativa."""
    inicio = inicio_do_bloco(ano, area)
    de_idioma = [l for l in itens if _tp_lingua(l)]
    comuns = [l for l in itens if not _tp_lingua(l)]
    primeira = min(int(l["CO_POSICAO"]) for l in comuns)
    # Os 5 itens de idioma ocupam as 5 primeiras posições do bloco de Linguagens
    base = inicio + (5 if de_idioma else 0)

    numeradas = {(base + int(l["CO_POSICAO"]) - primeira, ""): l for l in comuns}
    for l in de_idioma:
        numeradas[(inicio + (int(l["CO_POSICAO"]) - 1) % 5, IDIOMA_POR_TP_LINGUA[_tp_lingua(l)])] = l
    return numeradas


def _escolher_caderno(ano, area, cadernos, gabaritos_do_banco):
    melhor = None
    for (a, prova), itens in cadernos.items():
        if a != area:
            continue
        numeradas = numerar(ano, area, itens)
        comparaveis = [(k, l) for k, l in numeradas.items() if (ano, *k) in gabaritos_do_banco]
        if not comparaveis:
            continue
        iguais = sum(1 for k, l in comparaveis if gabaritos_do_banco[(ano, *k)] == l["TX_GABARITO"])
        taxa = iguais / len(comparaveis)
        if melhor is None or taxa > melhor[0]:
            melhor = (taxa, prova, numeradas)
    if melhor is None or melhor[0] < LIMITE_DE_CONFIANCA:
        taxa = "nenhum caderno" if melhor is None else f"melhor caderno só {melhor[0]:.0%}"
        raise RuntimeError(f"{ano} {area}: não achei o caderno de referência ({taxa}); o mapeamento não é confiável")
    return melhor[1], melhor[2]


def casar(gabaritos_do_banco: dict[tuple[int, int, str], str], anos=range(2009, 2024)) -> list[ItemDoInep]:
    """Itens do INEP na numeração do nosso banco.

    `gabaritos_do_banco`: {(ano, numero, idioma): gabarito}, usado SÓ pra achar o
    caderno de referência. Não há mais nenhuma decisão baseada nele depois.
    """
    resultado = []
    for ano in anos:
        linhas = ler_itens(ano)
        cadernos = defaultdict(list)
        for l in linhas:
            cadernos[(l["SG_AREA"], l["CO_PROVA"])].append(l)
        # Mesmo item = mesma habilidade em todos os cadernos (medido: 0 conflitos
        # em 15 anos), então vale a de qualquer caderno que a tenha
        habilidade_do_item = {}
        dificuldade_do_item = {}
        for l in linhas:
            h = (l["CO_HABILIDADE"] or "").strip()
            if h:
                habilidade_do_item[l["CO_ITEM"]] = h
            b = (l.get("NU_PARAM_B") or "").strip()
            if b:
                dificuldade_do_item[l["CO_ITEM"]] = b

        for area in AREAS:
            prova, numeradas = _escolher_caderno(ano, area, cadernos, gabaritos_do_banco)
            for (numero, idioma), l in sorted(numeradas.items()):
                resultado.append(ItemDoInep(
                    ano=ano, numero=numero, idioma=idioma, area=area,
                    co_item=l["CO_ITEM"], gabarito=l["TX_GABARITO"],
                    abandonado=l["IN_ITEM_ABAN"] == "1",
                    motivo_do_abandono=(l["TX_MOTIVO_ABAN"] or "").strip(),
                    co_prova=prova, cor=l["TX_COR"],
                    habilidade=habilidade_do_item.get(l["CO_ITEM"], ""),
                    dificuldade=dificuldade_do_item.get(l["CO_ITEM"], ""),
                ))
    return resultado

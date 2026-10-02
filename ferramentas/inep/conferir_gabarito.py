"""
Confere o gabarito e as anulações do nosso banco contra o INEP (Passo A da 2b.7).

Por que existe: a enem.dev é a nossa fonte das questões, mas o INEP é a fonte
oficial do gabarito. Medindo os 15 anos (30/09/2026), o banco divergiu do INEP
em 14 gabaritos e guardava com letra 6 questões que o INEP anulou. Um gabarito
errado corrige a prova do aluno ao contrário; uma anulada num simulado dá nota
errada. Ver docs/conferencia-inep.md.

O que faz:
  1. lê o gabarito do banco (psql do container, como as outras ferramentas);
  2. casa cada questão com o item do INEP (microdados.py);
  3. imprime o relatório;
  4. ACRESCENTA ao dados/conferencia-inep/correcoes.csv (versionado) o que ainda
     não estiver lá. Nunca remove: depois que a carga aplica uma correção, o banco
     concorda com o INEP e este script deixa de vê-la, mas ela tem que continuar no
     arquivo pra valer numa base nova. Este script NÃO altera o banco; quem aplica
     é `--aplicar-conferencia-inep` (backend).

Regras:
  - ANULADA: o INEP tem gabarito `X` no caderno de referência. `X` é anulação
    PEDAGÓGICA. O `IN_ITEM_ABAN=1` sozinho NÃO é anulação: a maioria é abandono
    ESTATÍSTICO ("não permitiu a convergência do algoritmo da TRI"), item que
    valeu e tem gabarito normal — só ficou fora do cálculo da nota.
  - GABARITO: letra do banco diferente da do INEP (A–E).

Uso (da raiz; precisa do Postgres no ar e dos ITENS_PROVA baixados):
    python3 ferramentas/inep/conferir_gabarito.py
"""

import csv
import sys
from collections import Counter
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "classificacao"))
from exportar_lotes import consultar_json  # noqa: E402  (reaproveita o acesso ao banco)

import microdados  # noqa: E402

SAIDA = microdados.RAIZ / "dados" / "conferencia-inep" / "correcoes.csv"
COLUNAS = ["tipo", "ano", "numero", "idioma", "valor_banco", "valor_inep", "evidencia", "detalhe"]

MICRODADOS = "microdados INEP (ITENS_PROVA)"

# Casos em que, além do CSV, o gabarito oficial em PDF foi lido e concorda
# (baixado com extrair_do_zip.py; caderno AZUL, 1ª aplicação). Em 700
# comparações CSV x PDF (2012, 2018, 2021, 2022) as duas fontes nunca
# discordaram — por isso o CSV sozinho é evidência suficiente onde não há PDF
# legível: 2010 (PDF escaneado) e 2011 (o zip só traz a 2ª aplicação).
CONFIRMADO_NO_PDF = {
    (2009, 120): "ENEM_2009_GAB_DIA_2.pdf (coluna Gab_Azul)",
    (2012, 165): "gabarito_dia2_azul.pdf",
    (2018, 15): "ENEM_2018_P1_GAB_01_DIA_1_AZUL.pdf",
    (2018, 163): "ENEM_2018_P1_GAB_07_DIA_2_AZUL.pdf",
    (2020, 135): "ENEM_2020_P1_GAB_07_DIA_2_AZUL.pdf",
    (2020, 157): "ENEM_2020_P1_GAB_07_DIA_2_AZUL.pdf",
    (2021, 15): "ENEM_2021_P1_GAB_01_DIA_1_AZUL.pdf",
    (2021, 25): "ENEM_2021_P1_GAB_01_DIA_1_AZUL.pdf",
    (2021, 38): "ENEM_2021_P1_GAB_01_DIA_1_AZUL.pdf",
    (2021, 53): "ENEM_2021_P1_GAB_01_DIA_1_AZUL.pdf",
    (2021, 138): "ENEM_2021_P1_GAB_07_DIA_2_AZUL.pdf",
    (2021, 139): "ENEM_2021_P1_GAB_07_DIA_2_AZUL.pdf",
    (2021, 145): "ENEM_2021_P1_GAB_07_DIA_2_AZUL.pdf",
    (2021, 152): "ENEM_2021_P1_GAB_07_DIA_2_AZUL.pdf",
    (2022, 143): "ENEM_2022_P1_GAB_07_DIA_2_AZUL.pdf",
    (2022, 157): "ENEM_2022_P1_GAB_07_DIA_2_AZUL.pdf",
    (2023, 164): "ENEM_2023_P1_GAB_07_DIA_2_AZUL.pdf",
}


def evidencia(ano: int, numero: int) -> str:
    pdf = CONFIRMADO_NO_PDF.get((ano, numero))
    return f"{MICRODADOS} + PDF oficial {pdf}" if pdf else MICRODADOS


def ler_banco() -> list[dict]:
    return consultar_json(
        "SELECT coalesce(json_agg(t ORDER BY ano, numero, idioma), '[]'::json) FROM "
        "(SELECT ano, numero, coalesce(idioma, '') AS idioma, gabarito, anulada FROM questoes) t"
    )


def chave_da_linha(linha: dict) -> tuple:
    return (linha["tipo"], int(linha["ano"]), int(linha["numero"]), linha["idioma"])


def ler_existentes() -> dict:
    if not SAIDA.exists():
        return {}
    with SAIDA.open(newline="", encoding="utf-8") as arquivo:
        return {chave_da_linha(l): l for l in csv.DictReader(arquivo)}


def main() -> int:
    banco ={(q["ano"], q["numero"], q["idioma"]): q for q in ler_banco()}
    itens = microdados.casar({k: q["gabarito"] for k, q in banco.items() if q["gabarito"]})
    por_chave = {(i.ano, i.numero, i.idioma): i for i in itens}

    correcoes = []
    estatisticos = 0
    for chave, questao in sorted(banco.items()):
        item = por_chave.get(chave)
        if item is None:
            continue
        ano, numero, idioma = chave
        if item.gabarito == "X":
            if not questao["anulada"]:
                # valor_banco = a letra que o banco tinha; valor_inep = X, como o INEP
                # escreve. Sem vírgula nos 6 primeiros campos: o leitor Java lê por split
                correcoes.append({
                    "tipo": "ANULADA", "ano": ano, "numero": numero, "idioma": idioma,
                    "valor_banco": questao["gabarito"] or "", "valor_inep": "X",
                    "evidencia": evidencia(ano, numero), "detalhe": item.motivo_do_abandono,
                })
        elif item.abandonado:
            estatisticos += 1  # válida; só saiu do cálculo da nota TRI
        if item.gabarito in "ABCDE" and questao["gabarito"] and questao["gabarito"] != item.gabarito:
            correcoes.append({
                "tipo": "GABARITO", "ano": ano, "numero": numero, "idioma": idioma,
                "valor_banco": questao["gabarito"], "valor_inep": item.gabarito,
                "evidencia": evidencia(ano, numero), "detalhe": f"item INEP {item.co_item}",
            })

    sem_par = sorted(k for k in banco if k not in por_chave)
    omitidas = sorted((i.ano, i.numero, i.gabarito) for k, i in por_chave.items() if k not in banco)
    omitidas_anuladas = [o for o in omitidas if o[2] == "X"]

    print(f"Banco: {len(banco)} questões | casadas com o INEP: {len(banco) - len(sem_par)}")
    print(f"Correções propostas: {Counter(c['tipo'] for c in correcoes)}")
    print(f"Abandonos ESTATÍSTICOS (válidas, gabarito ok, nada a fazer): {estatisticos}")
    print(f"Questões do banco sem item no INEP ({len(sem_par)}): "
          + ", ".join(f"{a}/{n}{'/' + i[:2] if i else ''}" for a, n, i in sem_par))
    print(f"Itens do INEP que o banco não tem: {len(omitidas)}, dos quais anulados: "
          f"{len(omitidas_anuladas)} (a enem.dev já omitiu essas)")

    # SÓ ACRESCENTA, nunca remove. Depois que a carga aplica uma correção, o banco
    # passa a concordar com o INEP e esta conferência deixa de enxergá-la: regravar
    # só o que ainda diverge esvaziaria o arquivo versionado, e numa base nova
    # (questões reimportadas da enem.dev) as correções se perderiam
    existentes = ler_existentes()
    novas = [c for c in correcoes if chave_da_linha(c) not in existentes]
    resultado = {**existentes, **{chave_da_linha(c): c for c in novas}}

    SAIDA.parent.mkdir(parents=True, exist_ok=True)
    with SAIDA.open("w", newline="", encoding="utf-8") as arquivo:
        escritor = csv.DictWriter(arquivo, fieldnames=COLUNAS)
        escritor.writeheader()
        escritor.writerows(sorted(resultado.values(), key=lambda c: (int(c["ano"]), int(c["numero"]), c["tipo"])))
    print(f"\nGravado: {SAIDA.relative_to(microdados.RAIZ)} ({len(resultado)} linhas: "
          f"{len(existentes)} que já estavam, {len(novas)} novas)")
    return 0


if __name__ == "__main__":
    sys.exit(main())

"""
Gera dados/conferencia-inep/habilidades.csv: a habilidade oficial (H1–H30) e a
dificuldade TRI (parâmetro B) de cada questão, tiradas dos microdados do INEP.

Por que um CSV versionado e não ler os microdados no backend: os ITENS_PROVA são
dado de terceiro, ficam fora do git (dados/inep/), e o casamento com a nossa
numeração é trabalho do microdados.py. O CSV é o resultado já casado; a carga
`--importar-habilidades` (backend) o aplica, numa base nova inclusive.

Ao contrário do correcoes.csv, este arquivo é REGRAVADO inteiro: não guarda uma
decisão nossa, é só a cópia de um dado oficial, e regerar dá sempre o mesmo
resultado. A guarda é outra: se a nova versão tiver MENOS linhas que a anterior,
o script para (sinal de microdados faltando ou casamento quebrado).

Só entra o que o INEP publicou. Habilidade estimada por nós (ver
dados/classificacao/habilidades_baixa_confianca.csv) não entra aqui: a tela
mostra a habilidade como oficial.

Uso (da raiz; precisa do Postgres no ar e dos ITENS_PROVA baixados):
    python3 ferramentas/inep/gerar_habilidades.py [--forcar]
"""

import csv
import math
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "classificacao"))
from exportar_lotes import consultar_json  # noqa: E402

import microdados  # noqa: E402

SAIDA = microdados.RAIZ / "dados" / "conferencia-inep" / "habilidades.csv"
COLUNAS = ["ano", "numero", "idioma", "area", "habilidade", "dificuldade", "item_inep"]


def linhas_do_csv(itens) -> list[dict]:
    """Uma linha por questão com habilidade OU dificuldade publicada; ordenada.

    Recusa valor fora do domínio em vez de gravar lixo: habilidade é 1–30 e a
    dificuldade é um número finito (o INEP escreve com ponto decimal)."""
    linhas = []
    for i in itens:
        if not i.habilidade and not i.dificuldade:
            continue
        if i.habilidade and not (i.habilidade.isdigit() and 1 <= int(i.habilidade) <= 30):
            raise ValueError(f"{i.ano}/{i.numero}: habilidade '{i.habilidade}' fora de 1–30")
        if i.dificuldade and not math.isfinite(float(i.dificuldade)):
            raise ValueError(f"{i.ano}/{i.numero}: dificuldade '{i.dificuldade}' não é número")
        linhas.append({"ano": i.ano, "numero": i.numero, "idioma": i.idioma, "area": i.area,
                       "habilidade": int(i.habilidade) if i.habilidade else "",
                       "dificuldade": i.dificuldade, "item_inep": i.co_item})
    linhas.sort(key=lambda l: (l["ano"], l["numero"], l["idioma"]))
    chaves = [(l["ano"], l["numero"], l["idioma"]) for l in linhas]
    if len(chaves) != len(set(chaves)):
        raise ValueError("casamento devolveu a mesma questão duas vezes")
    return linhas


def main() -> int:
    banco = consultar_json(
        "SELECT coalesce(json_agg(t), '[]'::json) FROM "
        "(SELECT ano, numero, coalesce(idioma, '') AS idioma, gabarito FROM questoes) t")
    # O gabarito serve SÓ pra achar o caderno de referência (microdados.casar).
    # Anulada tem gabarito NULL depois da conferência e fica de fora disso
    gabaritos = {(q["ano"], q["numero"], q["idioma"]): q["gabarito"] for q in banco if q["gabarito"]}
    no_banco = {(q["ano"], q["numero"], q["idioma"]) for q in banco}

    # Só as questões que existem no banco: as ~19 que o importador descartou
    # não teriam onde ser aplicadas
    linhas = [l for l in linhas_do_csv(microdados.casar(gabaritos))
              if (l["ano"], l["numero"], l["idioma"]) in no_banco]

    if SAIDA.exists() and "--forcar" not in sys.argv:
        with SAIDA.open(newline="", encoding="utf-8") as arquivo:
            anteriores = sum(1 for _ in csv.DictReader(arquivo))
        if len(linhas) < anteriores:
            print(f"ERRO: {len(linhas)} linhas, menos que as {anteriores} do arquivo atual. "
                  "Microdados faltando? Use --forcar se for de propósito.")
            return 1

    with SAIDA.open("w", newline="", encoding="utf-8") as arquivo:
        escritor = csv.DictWriter(arquivo, fieldnames=COLUNAS, lineterminator="\n")
        escritor.writeheader()
        escritor.writerows(linhas)

    com_h = sum(1 for l in linhas if l["habilidade"] != "")
    com_b = sum(1 for l in linhas if l["dificuldade"])
    print(f"{SAIDA.relative_to(microdados.RAIZ)}: {len(linhas)} linhas | "
          f"com habilidade {com_h} | com dificuldade {com_b} | banco tem {len(no_banco)} questões")
    return 0


if __name__ == "__main__":
    sys.exit(main())

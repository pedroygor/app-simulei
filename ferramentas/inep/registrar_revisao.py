"""
Registra uma rodada de revisão das questões que a conferência de habilidade destacou
(2b.7, Passo B): aplica as decisões no dados/classificacao/assuntos.csv e acrescenta
o registro em dados/conferencia-inep/revisoes_habilidade.csv.

Uma lista só alimenta os dois arquivos, pra eles não divergirem. O registro é o que faz
o cruzar_habilidade.py pular as questões MANTIDAS (enquanto o assunto não mudar).

Entrada: um CSV de decisões, com cabeçalho
    ano,numero,idioma,habilidade,assunto_anterior,assunto_atual,confianca,decisao,motivo
onde decisao é RECLASSIFICADA ou MANTIDA e confianca é alta|media|baixa. O `motivo`
vai pra justificativa do assuntos.csv e pro registro.

Depois: validar_classificacao.py e `--importar-classificacao` (origem IA).

Uso (da raiz):
    python3 ferramentas/inep/registrar_revisao.py decisoes.csv
Seguro de repetir: o que já está registrado é substituído, não duplicado.
"""

import csv
import io
import sys
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[2]
ASSUNTOS = RAIZ / "dados" / "classificacao" / "assuntos.csv"
REVISOES = RAIZ / "dados" / "conferencia-inep" / "revisoes_habilidade.csv"

COLUNAS_DECISAO = ["ano", "numero", "idioma", "habilidade", "assunto_anterior", "assunto_atual", "confianca", "decisao", "motivo"]
COLUNAS_REVISAO = ["ano", "numero", "idioma", "habilidade", "assunto_anterior", "assunto_atual", "decisao", "motivo"]
DECISOES_VALIDAS = {"RECLASSIFICADA", "MANTIDA"}
CONFIANCAS = {"alta", "media", "baixa"}


def ler_decisoes(caminho: Path) -> list[dict]:
    with caminho.open(newline="", encoding="utf-8") as arquivo:
        leitor = csv.DictReader(arquivo)
        if leitor.fieldnames != COLUNAS_DECISAO:
            raise ValueError(f"cabeçalho esperado: {','.join(COLUNAS_DECISAO)}")
        decisoes = list(leitor)
    problemas = []
    vistas = set()
    for n, d in enumerate(decisoes, start=2):
        if d["decisao"] not in DECISOES_VALIDAS:
            problemas.append(f"linha {n}: decisao '{d['decisao']}' (esperado RECLASSIFICADA ou MANTIDA)")
        if d["confianca"] not in CONFIANCAS:
            problemas.append(f"linha {n}: confianca '{d['confianca']}'")
        if d["decisao"] == "MANTIDA" and d["assunto_anterior"] != d["assunto_atual"]:
            problemas.append(f"linha {n}: MANTIDA com assunto diferente do anterior")
        if d["decisao"] == "RECLASSIFICADA" and d["assunto_anterior"] == d["assunto_atual"]:
            problemas.append(f"linha {n}: RECLASSIFICADA com o mesmo assunto")
        chave = (d["ano"], d["numero"], d["idioma"])
        if chave in vistas:
            problemas.append(f"linha {n}: questão repetida")
        vistas.add(chave)
    if problemas:
        raise ValueError("decisões inválidas:\n  " + "\n  ".join(problemas))
    return decisoes


def _linha_csv(campos) -> str:
    saida = io.StringIO()
    csv.writer(saida, lineterminator="").writerow(campos)
    return saida.getvalue()


def aplicar_em_linhas(linhas: list[str], decisoes: list[dict]) -> int:
    """Troca, em `linhas` (as do assuntos.csv, sem o fim de linha), a de cada decisão.

    Por linha e não pelo csv.writer do arquivo inteiro: regravar tudo poderia mudar
    aspas e fim de linha em milhares de linhas que não tiveram nada a ver com a revisão.
    Recusa se o assunto anterior não bater: a revisão foi feita sobre OUTRO estado."""
    indice = {}
    for i, linha in enumerate(linhas):
        partes = linha.split(",", 3)
        if len(partes) >= 3 and partes[0].isdigit():
            indice[(partes[0], partes[1], partes[2])] = i

    alteradas = 0
    for d in decisoes:
        chave = (d["ano"], d["numero"], d["idioma"])
        if chave not in indice:
            raise KeyError(f"{chave} não está no assuntos.csv")
        i = indice[chave]
        atual = next(csv.reader([linhas[i]]))
        if atual[3] != d["assunto_anterior"]:
            raise ValueError(f"{chave}: o assuntos.csv tem '{atual[3]}', a decisão partiu de '{d['assunto_anterior']}'")
        rotulo = "reclassificada" if d["decisao"] == "RECLASSIFICADA" else "mantida"
        justificativa = f"{d['motivo']} [conferida com o INEP, H{d['habilidade']}: {rotulo}]"
        nova = _linha_csv([d["ano"], d["numero"], d["idioma"], d["assunto_atual"], d["confianca"], justificativa])
        if nova != linhas[i]:
            linhas[i] = nova
            alteradas += 1
    return alteradas


def dividir_linhas(texto: str) -> tuple[list[str], list[str]]:
    """Separa o texto em linhas e guarda o fim de cada uma ('\\r' ou ''), por linha.

    O assuntos.csv mistura CRLF e LF (veio de várias rodadas). Escolher um separador pro
    arquivo inteiro cola as linhas do outro tipo numa só e a questão "some". Preservar o
    fim de cada linha também mantém o diff só nas linhas revisadas."""
    brutas = texto.split("\n")
    fins = ["\r" if b.endswith("\r") else "" for b in brutas]
    return [b[:-1] if f else b for b, f in zip(brutas, fins)], fins


def juntar_linhas(linhas: list[str], fins: list[str]) -> str:
    return "\n".join(l + f for l, f in zip(linhas, fins))


def mesclar_revisoes(existentes: list[dict], decisoes: list[dict]) -> list[dict]:
    """Registro acumulado: as decisões novas substituem as da mesma questão."""
    por_chave = {(r["ano"], r["numero"], r["idioma"]): r for r in existentes}
    for d in decisoes:
        por_chave[(d["ano"], d["numero"], d["idioma"])] = {c: d[c] for c in COLUNAS_REVISAO}
    return sorted(por_chave.values(), key=lambda r: (int(r["ano"]), int(r["numero"]), r["idioma"]))


def main() -> int:
    if len(sys.argv) != 2:
        print(__doc__)
        return 2
    decisoes = ler_decisoes(Path(sys.argv[1]))

    texto = ASSUNTOS.read_bytes().decode("utf-8")
    linhas, fins = dividir_linhas(texto)
    alteradas = aplicar_em_linhas(linhas, decisoes)
    ASSUNTOS.write_bytes(juntar_linhas(linhas, fins).encode("utf-8"))

    existentes = []
    if REVISOES.exists():
        with REVISOES.open(newline="", encoding="utf-8") as arquivo:
            existentes = list(csv.DictReader(arquivo))
    registro = mesclar_revisoes(existentes, decisoes)
    with REVISOES.open("w", newline="", encoding="utf-8") as arquivo:
        escritor = csv.DictWriter(arquivo, fieldnames=COLUNAS_REVISAO, lineterminator="\n")
        escritor.writeheader()
        escritor.writerows(registro)

    reclassificadas = sum(1 for d in decisoes if d["decisao"] == "RECLASSIFICADA")
    print(f"assuntos.csv: {alteradas} linhas alteradas | revisoes: {len(decisoes)} registradas "
          f"({reclassificadas} reclassificadas, {len(decisoes) - reclassificadas} mantidas); "
          f"registro acumulado: {len(registro)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())

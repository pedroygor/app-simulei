"""
Confere dados/classificacao/assuntos.csv ANTES de qualquer carga no banco.

Existe pra pegar erro de quem classificou — no caso, o Claude escrevendo o CSV
à mão. Um slug com letra trocada seria recusado pela FK na carga (2b.5), mas é
melhor saber agora, apontando a linha, do que no meio de uma carga.

Confere:
  - cabeçalho esperado
  - slug existe na taxonomia (lida da TABELA, a mesma que a carga vai usar)
  - assunto é da MESMA ÁREA da questão (a regra da FK composta da V4)
  - questão existe no banco (anulada é tolerada: ver abaixo)
  - chave (ano, numero, idioma) sem repetição
  - confiança em alta|media|baixa; justificativa preenchida

Uso (da raiz):
    python3 ferramentas/classificacao/validar_classificacao.py
Sai com código 1 se houver qualquer problema.
"""

import csv
import sys
from collections import Counter

from exportar_lotes import CSV_CLASSIFICACAO, NOME_DA_AREA, consultar_json

CABECALHO = ["ano", "numero", "idioma", "assunto", "confianca", "justificativa"]
CONFIANCAS = {"alta", "media", "baixa"}


def main() -> int:
    if not CSV_CLASSIFICACAO.exists():
        print(f"Nada a validar: {CSV_CLASSIFICACAO} não existe.")
        return 0

    area_do_assunto = {l["slug"]: l["area"] for l in consultar_json(
        "SELECT json_agg(json_build_object('slug', slug, 'area', area)) FROM assuntos;")}
    questoes = {(q["ano"], q["numero"], q["idioma"] or ""): q for q in consultar_json(
        "SELECT json_agg(json_build_object('ano', ano, 'numero', numero, 'idioma', idioma,"
        " 'area', area, 'anulada', anulada)) FROM questoes;")}

    problemas = []
    anuladas = []
    chaves = Counter()
    with CSV_CLASSIFICACAO.open(newline="", encoding="utf-8") as arquivo:
        leitor = csv.DictReader(arquivo)
        if leitor.fieldnames != CABECALHO:
            print(f"Cabeçalho inválido: {leitor.fieldnames} (esperado {CABECALHO})")
            return 1
        linhas = list(leitor)

    for n, linha in enumerate(linhas, start=2):  # linha 1 é o cabeçalho
        chave = (int(linha["ano"]), int(linha["numero"]), linha["idioma"])
        chaves[chave] += 1
        questao = questoes.get(chave)
        slug = linha["assunto"]
        if questao is None:
            problemas.append(f"linha {n}: questão {chave} não existe no banco")
            continue
        if questao["anulada"]:
            # NÃO é erro: a anulação pelo INEP (--aplicar-conferencia-inep) veio DEPOIS
            # da classificação. A linha é histórico e a carga a aplica sem dano. O
            # exportador já não oferece anulada pra classificar
            anuladas.append(chave)
        if slug not in area_do_assunto:
            problemas.append(f"linha {n}: assunto '{slug}' não existe na taxonomia")
        elif area_do_assunto[slug] != questao["area"]:
            problemas.append(f"linha {n}: '{slug}' é de {NOME_DA_AREA[area_do_assunto[slug]]},"
                             f" mas a questão {chave} é de {NOME_DA_AREA[questao['area']]}")
        if linha["confianca"] not in CONFIANCAS:
            problemas.append(f"linha {n}: confiança '{linha['confianca']}' inválida")
        if not linha["justificativa"].strip():
            problemas.append(f"linha {n}: justificativa vazia")

    problemas += [f"chave {c} aparece {vezes}x" for c, vezes in chaves.items() if vezes > 1]

    por_confianca = Counter(l["confianca"] for l in linhas)
    print(f"{len(linhas)} classificações | confiança: {dict(por_confianca)}")
    if problemas:
        print(f"\n{len(problemas)} problema(s):")
        print("\n".join(f"  - {p}" for p in problemas))
        return 1
    print("OK: todas as linhas válidas")
    if anuladas:
        print(f"(aviso: {len(anuladas)} já anuladas pelo INEP, classificadas antes da anulação — histórico, sem dano)")
    return 0


if __name__ == "__main__":
    sys.exit(main())

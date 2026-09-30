"""
Exporta questões do banco em LOTES de texto pra classificação por assunto.

Quem classifica é o Claude, dentro do Claude Code (ADR de 29/09/2026): o
plano não permite gasto na API. Os lotes são o "prompt" — cada questão com
enunciado, comando, alternativas e os caminhos das imagens no storage, mais a
lista de assuntos permitidos da área.

Retomável: questões que já estão em dados/classificacao/assuntos.csv são
puladas. A classificação atravessa várias sessões sem refazer nada.

Só biblioteca padrão. Fala com o banco pelo psql do container (docker compose),
então não precisa instalar driver nem criar venv.

Uso (da raiz do repositório):
    python3 ferramentas/classificacao/exportar_lotes.py --piloto
    python3 ferramentas/classificacao/exportar_lotes.py --todas --tamanho 40
"""

import argparse
import csv
import json
import subprocess
import sys
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[2]
CSV_CLASSIFICACAO = RAIZ / "dados" / "classificacao" / "assuntos.csv"
PASTA_LOTES = Path(__file__).resolve().parent / "lotes"
QUESTOES_POR_AREA_NO_PILOTO = 25

NOME_DA_AREA = {
    "LINGUAGENS": "Linguagens",
    "MATEMATICA": "Matemática",
    "CIENCIAS_NATUREZA": "Ciências da Natureza",
    "CIENCIAS_HUMANAS": "Ciências Humanas",
}


def ler_env() -> dict:
    """Lê o .env da raiz só pra saber usuário e banco do Postgres.

    Nada daqui é impresso nem gravado: é a mesma regra 8.1 do CLAUDE.md que
    vale pro backend.
    """
    env = {}
    for linha in (RAIZ / ".env").read_text().splitlines():
        linha = linha.strip()
        if linha and not linha.startswith("#") and "=" in linha:
            chave, valor = linha.split("=", 1)
            env[chave.strip()] = valor.strip().strip('"').strip("'")
    return env


def consultar_json(sql: str) -> list:
    """Roda uma consulta que devolve UMA linha com um JSON e a desserializa.

    json_agg no banco em vez de linhas soltas: texto de questão tem vírgula,
    aspas e quebra de linha, e CSV de psql com isso é pedir problema.
    """
    env = ler_env()
    resultado = subprocess.run(
        ["docker", "compose", "exec", "-T", "postgres", "psql",
         "-U", env["POSTGRES_USER"], "-d", env["POSTGRES_DB"], "-tA", "-c", sql],
        cwd=RAIZ, capture_output=True, text=True, check=True,
    )
    return json.loads(resultado.stdout.strip() or "[]")


def ja_classificadas() -> set:
    if not CSV_CLASSIFICACAO.exists():
        return set()
    with CSV_CLASSIFICACAO.open(newline="", encoding="utf-8") as arquivo:
        return {(int(l["ano"]), int(l["numero"]), l["idioma"]) for l in csv.DictReader(arquivo)}


def buscar_questoes(piloto: bool) -> list:
    # Piloto: 25 por área, embaralhadas de forma DETERMINÍSTICA (md5 do id
    # com um sal fixo). random() daria um piloto diferente a cada execução,
    # e a revisão do desenvolvedor perderia a referência.
    ordem = "md5(q.id::text || 'simulei-piloto')" if piloto else "q.ano DESC, q.numero, q.idioma NULLS FIRST"
    limite = f"WHERE posicao <= {QUESTOES_POR_AREA_NO_PILOTO}" if piloto else ""
    sql = f"""
        WITH base AS (
            SELECT q.id, q.ano, q.numero, q.idioma, q.area, q.enunciado, q.comando,
                   row_number() OVER (PARTITION BY q.area ORDER BY {ordem}) AS posicao,
                   (SELECT json_agg(json_build_object('letra', a.letra, 'texto', a.texto,
                                                      'imagem', a.imagem_url) ORDER BY a.letra)
                      FROM alternativas a WHERE a.questao_id = q.id) AS alternativas
            FROM questoes q
            WHERE NOT q.anulada AND q.assunto_id IS NULL
        )
        SELECT coalesce(json_agg(b ORDER BY b.area, b.posicao), '[]') FROM base b {limite};
    """
    return consultar_json(sql)


def buscar_assuntos_por_area() -> dict:
    # Da TABELA, não do docs/taxonomia.md: só vale slug que a carga (2b.5)
    # vai aceitar
    linhas = consultar_json("""
        SELECT json_agg(json_build_object('area', a.area, 'materia', m.nome,
                                          'slug', a.slug, 'nome', a.nome) ORDER BY m.id, a.id)
        FROM assuntos a JOIN materias m ON m.id = a.materia_id;
    """)
    por_area = {}
    for linha in linhas:
        por_area.setdefault(linha["area"], []).append(linha)
    return por_area


def caminho_da_imagem(url: str | None) -> str | None:
    """/imagens/ab/cd/x.png -> storage/imagens/ab/cd/x.png (relativo à raiz).

    URL externa é o SVG de "imagem quebrada" que o allowlist recusou (seção
    8.6): não tem conteúdo, então não vale abrir.
    """
    if not url:
        return None
    if url.startswith("/imagens/"):
        return "storage" + url
    return None


def imagens_do_markdown(texto: str | None) -> list:
    import re
    return re.findall(r"!\[[^\]]*\]\(([^)\s]+)", texto or "")


def formatar_questao(q: dict) -> str:
    chave = f"{q['ano']}/{q['numero']}" + (f"/{q['idioma']}" if q["idioma"] else "")
    partes = [f"### {chave} · id {q['id']} · {NOME_DA_AREA[q['area']]}", ""]
    if q["enunciado"]:
        partes += ["**Enunciado:**", q["enunciado"].strip(), ""]
    if q["comando"]:
        partes += ["**Comando:**", q["comando"].strip(), ""]
    partes.append("**Alternativas:**")
    for alt in q["alternativas"] or []:
        if alt["imagem"]:
            local = caminho_da_imagem(alt["imagem"])
            partes.append(f"- {alt['letra']}) [imagem: {local or 'externa, sem conteúdo'}]")
        else:
            partes.append(f"- {alt['letra']}) {(alt['texto'] or '').strip()}")
    imagens = [caminho_da_imagem(u) for u in imagens_do_markdown(q["enunciado"])]
    imagens = [i for i in imagens if i]
    if imagens:
        partes += ["", "**Imagens do enunciado:** " + ", ".join(imagens)]
    return "\n".join(partes)


def formatar_lote(questoes: list, assuntos_por_area: dict, nome: str) -> str:
    areas = sorted({q["area"] for q in questoes}, key=list(NOME_DA_AREA).index)
    partes = [f"# Lote {nome} — {len(questoes)} questões", ""]
    for area in areas:
        partes += [f"## Assuntos permitidos — {NOME_DA_AREA[area]}", ""]
        materia_atual = None
        for a in assuntos_por_area[area]:
            if a["materia"] != materia_atual:
                materia_atual = a["materia"]
                partes.append(f"- **{materia_atual}**")
            partes.append(f"  - `{a['slug']}` — {a['nome']}")
        partes.append("")
    partes += ["## Questões", ""]
    partes += [formatar_questao(q) + "\n\n---\n" for q in questoes]
    return "\n".join(partes)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    modo = parser.add_mutually_exclusive_group(required=True)
    modo.add_argument("--piloto", action="store_true", help="100 questões, 25 por área")
    modo.add_argument("--todas", action="store_true", help="todas as não classificadas")
    parser.add_argument("--tamanho", type=int, default=35, help="questões por arquivo de lote")
    args = parser.parse_args()

    feitas = ja_classificadas()
    questoes = [q for q in buscar_questoes(args.piloto)
                if (q["ano"], q["numero"], q["idioma"] or "") not in feitas]
    if not questoes:
        print("Nada a exportar: tudo já está classificado.")
        return

    assuntos_por_area = buscar_assuntos_por_area()
    PASTA_LOTES.mkdir(exist_ok=True)
    prefixo = "piloto" if args.piloto else "lote"
    for i in range(0, len(questoes), args.tamanho):
        pedaco = questoes[i:i + args.tamanho]
        nome = f"{prefixo}-{i // args.tamanho + 1:03d}"
        (PASTA_LOTES / f"{nome}.md").write_text(formatar_lote(pedaco, assuntos_por_area, nome), encoding="utf-8")

    total_lotes = (len(questoes) + args.tamanho - 1) // args.tamanho
    print(f"{len(questoes)} questões em {total_lotes} lote(s) em {PASTA_LOTES.relative_to(RAIZ)}/"
          f" ({len(feitas)} já classificadas foram puladas)")


if __name__ == "__main__":
    sys.exit(main())

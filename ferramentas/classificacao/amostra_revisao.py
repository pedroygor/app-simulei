"""
Amostra aleatória da classificação por assunto em Natureza e Humanas, pra
revisão À MÃO — e a medição do acerto depois de revisada (auditoria da Etapa 3,
pendência F2).

Por que existe: o Passo B da 2b.7 só olhou as questões que o detector de
habilidade destacou, e em Natureza e Humanas o detector é fraco (competências
transversais). Nenhuma amostra ALEATÓRIA dessas áreas foi conferida, então não
há medida do acerto ali — e a Etapa 4 transforma a classificação em "pontos
fracos" do aluno.

Dois modos:
  --gerar  sorteia 60 questões por área e escreve:
           - ferramentas/classificacao/lotes/amostra-revisao.md (questões completas,
             pra ler; gitignored, regenerável)
           - dados/classificacao/amostra_revisao_2026-10.csv (a FOLHA, versionada)
  --medir  lê a folha preenchida e mede acerto no assunto e na matéria, por área
           e por confiança, com intervalo de 95% (Wilson), contra a meta do piloto

Revisão às cegas: o .md e a folha mostram o assunto atribuído, mas NÃO a
confiança nem a justificativa da IA (ancorariam o julgamento). A confiança só
entra na medição, lida do assuntos.csv — por isso, medir ANTES de aplicar as
correções.

Sorteio determinístico (md5 do id com sal fixo, como no piloto): rodar de novo
sorteia as mesmas questões. Ficam de fora as 25 por área do piloto (já revisadas)
e as revisadas no Passo B (já corrigidas) — o que deixa a estimativa um pouco
conservadora.

Uso (da raiz):
    python3 ferramentas/classificacao/amostra_revisao.py --gerar
    python3 ferramentas/classificacao/amostra_revisao.py --medir
"""

import argparse
import csv
import re
import sys
from collections import defaultdict
from math import sqrt
from pathlib import Path

from exportar_lotes import CSV_CLASSIFICACAO, NOME_DA_AREA, PASTA_LOTES, RAIZ, consultar_json

AREAS = ("CIENCIAS_NATUREZA", "CIENCIAS_HUMANAS")
POR_AREA = 60
SAL_DA_AMOSTRA = "simulei-amostra-2026-10"
# O mesmo sal e o mesmo tamanho do piloto (exportar_lotes.py), pra reconstruí-lo
SAL_DO_PILOTO = "simulei-piloto"
TAMANHO_DO_PILOTO = 25
FOLHA = RAIZ / "dados" / "classificacao" / "amostra_revisao_2026-10.csv"
LEITURA = PASTA_LOTES / "amostra-revisao.md"
REVISOES_PASSO_B = RAIZ / "dados" / "conferencia-inep" / "revisoes_habilidade.csv"

COLUNAS_DA_FOLHA = ["ano", "numero", "idioma", "area", "assunto_atribuido", "veredito", "assunto_correto", "obs"]
VEREDITOS = {"CERTO", "ERRADO"}
META_ASSUNTO = 0.90
META_MATERIA = 0.98


# --- sorteio ---

def chaves_do_passo_b() -> set:
    with REVISOES_PASSO_B.open(newline="", encoding="utf-8") as arquivo:
        return {(int(l["ano"]), int(l["numero"]), l["idioma"]) for l in csv.DictReader(arquivo)}


def sortear(candidatas: list, excluidas: set, por_area: int) -> list:
    """Pega as primeiras `por_area` de cada área, na ordem já sorteada, pulando as excluídas.

    O filtro é em Python (e não no SQL) porque a lista do Passo B vive num CSV
    versionado, não no banco.
    """
    escolhidas = defaultdict(list)
    for q in candidatas:
        chave = (q["ano"], q["numero"], q["idioma"] or "")
        if chave not in excluidas and len(escolhidas[q["area"]]) < por_area:
            escolhidas[q["area"]].append(q)
    return [q for area in AREAS for q in escolhidas[area]]


def buscar_candidatas() -> list:
    # Piloto reconstruído com a regra de 29/09: as 25 primeiras por área no md5
    # sobre TODAS as questões (naquele dia nenhuma estava anulada nem incompleta,
    # então nada de filtro aqui — filtrar deslocaria o ranking)
    return consultar_json(f"""
        WITH piloto AS (
            SELECT id, row_number() OVER (PARTITION BY area ORDER BY md5(id::text || '{SAL_DO_PILOTO}')) AS posicao
            FROM questoes
        )
        SELECT coalesce(json_agg(json_build_object('id', q.id, 'ano', q.ano, 'numero', q.numero,
                                                   'idioma', q.idioma, 'area', q.area)
                                 ORDER BY q.area, md5(q.id::text || '{SAL_DA_AMOSTRA}')), '[]')
        FROM questoes q JOIN piloto p ON p.id = q.id
        WHERE q.disponivel AND q.assunto_id IS NOT NULL
          AND q.area IN ({", ".join(f"'{a}'" for a in AREAS)})
          AND p.posicao > {TAMANHO_DO_PILOTO};
    """)


def buscar_conteudo(ids: list) -> dict:
    lista = ", ".join(str(int(i)) for i in ids)
    linhas = consultar_json(f"""
        SELECT json_agg(json_build_object(
            'id', q.id, 'enunciado', q.enunciado, 'comando', q.comando,
            'assunto', a.slug, 'assunto_nome', a.nome, 'materia', m.nome,
            'alternativas', (SELECT json_agg(json_build_object('letra', al.letra, 'texto', al.texto,
                                                               'imagem', al.imagem_url) ORDER BY al.letra)
                             FROM alternativas al WHERE al.questao_id = q.id)))
        FROM questoes q JOIN assuntos a ON a.id = q.assunto_id JOIN materias m ON m.id = a.materia_id
        WHERE q.id IN ({lista});
    """)
    return {l["id"]: l for l in linhas}


def buscar_taxonomia() -> list:
    # Da TABELA (como o exportador): só vale slug que a carga aceita
    return consultar_json("""
        SELECT json_agg(json_build_object('area', a.area, 'materia', m.nome, 'slug', a.slug, 'nome', a.nome)
                        ORDER BY m.ordem, a.ordem)
        FROM assuntos a JOIN materias m ON m.id = a.materia_id;
    """)


# --- leitura da amostra ---

def imagem_local(url: str | None) -> str | None:
    """/imagens/ab/cd/x.png -> caminho relativo ao .md, pra o preview do editor mostrar."""
    if url and url.startswith("/imagens/"):
        return "../../../storage" + url
    return None


def com_imagens_locais(markdown: str | None) -> str:
    return re.sub(r"\]\((/imagens/[^)\s]+)", lambda m: "](" + imagem_local(m.group(1)) + ")", markdown or "")


def formatar_leitura(amostra: list, conteudo: dict, taxonomia: list) -> str:
    partes = ["# Amostra de revisão da classificação — Natureza e Humanas", "",
              f"{len(amostra)} questões. Para cada uma: o assunto atribuído responde ao conhecimento",
              "necessário pra resolver (regra 1 da taxonomia), não ao tema do texto? Anote na folha",
              f"`{FOLHA.relative_to(RAIZ)}`: `CERTO`, ou `ERRADO` com o slug certo em `assunto_correto`.", ""]
    for area in AREAS:
        partes += [f"## Assuntos — {NOME_DA_AREA[area]}", ""]
        materia = None
        for a in (t for t in taxonomia if t["area"] == area):
            if a["materia"] != materia:
                materia = a["materia"]
                partes.append(f"- **{materia}**")
            partes.append(f"  - `{a['slug']}` — {a['nome']}")
        partes.append("")
    partes += ["## Questões", ""]
    for n, q in enumerate(amostra, start=1):
        c = conteudo[q["id"]]
        chave = f"{q['ano']}/{q['numero']}" + (f"/{q['idioma']}" if q["idioma"] else "")
        partes += [f"### {n}. {chave} · {NOME_DA_AREA[q['area']]}", "",
                   f"> **Atribuído:** {c['materia']} · {c['assunto_nome']} (`{c['assunto']}`)", ""]
        if c["enunciado"]:
            partes += [com_imagens_locais(c["enunciado"]).strip(), ""]
        if c["comando"]:
            partes += [f"**{com_imagens_locais(c['comando']).strip()}**", ""]
        for alt in c["alternativas"] or []:
            local = imagem_local(alt["imagem"])
            texto = f"![alternativa {alt['letra']}]({local})" if local else (alt["texto"] or "[imagem externa, sem conteúdo]").strip()
            partes.append(f"- **{alt['letra']})** {texto}")
        partes += ["", "---", ""]
    return "\n".join(partes)


def gerar(forcar: bool) -> int:
    if FOLHA.exists() and not forcar:
        # Regravar a folha apagaria a revisão já feita
        print(f"{FOLHA.relative_to(RAIZ)} já existe; use --forcar pra regerar (apaga a revisão anotada).")
        return 1
    amostra = sortear(buscar_candidatas(), chaves_do_passo_b(), POR_AREA)
    conteudo = buscar_conteudo([q["id"] for q in amostra])
    PASTA_LOTES.mkdir(exist_ok=True)
    LEITURA.write_text(formatar_leitura(amostra, conteudo, buscar_taxonomia()), encoding="utf-8")
    with FOLHA.open("w", newline="", encoding="utf-8") as arquivo:
        escritor = csv.writer(arquivo)
        escritor.writerow(COLUNAS_DA_FOLHA)
        for q in amostra:
            escritor.writerow([q["ano"], q["numero"], q["idioma"] or "", q["area"],
                               conteudo[q["id"]]["assunto"], "", "", ""])
    por_area = {a: sum(q["area"] == a for q in amostra) for a in AREAS}
    print(f"{len(amostra)} questões {por_area}\n  ler:      {LEITURA.relative_to(RAIZ)}\n"
          f"  anotar:   {FOLHA.relative_to(RAIZ)}")
    return 0


# --- medição ---

def wilson(acertos: int, n: int, z: float = 1.96) -> tuple[float, float]:
    """Intervalo de 95% pra uma proporção. Wilson e não p ± 1,96·erro: com acerto
    perto de 100% e n pequeno, o intervalo ingênuo passa de 100% e encolhe pra zero."""
    if n == 0:
        return (0.0, 1.0)
    p = acertos / n
    centro = p + z * z / (2 * n)
    margem = z * sqrt(p * (1 - p) / n + z * z / (4 * n * n))
    divisor = 1 + z * z / n
    return ((centro - margem) / divisor, (centro + margem) / divisor)


def conferir_folha(linhas: list, taxonomia: dict) -> list:
    """Problemas de preenchimento. Linha sem veredito é problema: revisão pela metade
    não vira medida (e "em branco = certo" seria o aceite geral do piloto de novo)."""
    problemas = []
    for n, l in enumerate(linhas, start=2):
        veredito = l["veredito"].strip().upper()
        correto = l["assunto_correto"].strip()
        if veredito not in VEREDITOS:
            problemas.append(f"linha {n}: veredito '{l['veredito']}' (esperado CERTO ou ERRADO)")
        elif veredito == "ERRADO":
            if not correto:
                problemas.append(f"linha {n}: ERRADO sem assunto_correto")
            elif correto not in taxonomia:
                problemas.append(f"linha {n}: assunto '{correto}' não existe na taxonomia")
            elif taxonomia[correto]["area"] != l["area"]:
                problemas.append(f"linha {n}: '{correto}' é de outra área")
            elif correto == l["assunto_atribuido"]:
                problemas.append(f"linha {n}: ERRADO com o mesmo assunto atribuído")
        elif correto:
            problemas.append(f"linha {n}: CERTO com assunto_correto preenchido")
    return problemas


def contar(linhas: list, taxonomia: dict) -> dict:
    """(n, acertos no assunto, acertos na matéria) por área e por confiança."""
    grupos = defaultdict(lambda: [0, 0, 0])
    for l in linhas:
        certo = l["veredito"].strip().upper() == "CERTO"
        correto = l["assunto_atribuido"] if certo else l["assunto_correto"].strip()
        materia_certa = taxonomia[correto]["materia"] == taxonomia[l["assunto_atribuido"]]["materia"]
        for grupo in ((l["area"], "todas"), (l["area"], l["confianca"])):
            grupos[grupo][0] += 1
            grupos[grupo][1] += certo
            grupos[grupo][2] += materia_certa
    return grupos


def confiancas_da_ia() -> dict:
    with CSV_CLASSIFICACAO.open(newline="", encoding="utf-8") as arquivo:
        return {(l["ano"], l["numero"], l["idioma"]): l["confianca"] for l in csv.DictReader(arquivo)}


def medir() -> int:
    with FOLHA.open(newline="", encoding="utf-8") as arquivo:
        leitor = csv.DictReader(arquivo)
        if leitor.fieldnames != COLUNAS_DA_FOLHA:
            print(f"Cabeçalho inválido: {leitor.fieldnames}")
            return 1
        linhas = list(leitor)
    taxonomia = {t["slug"]: t for t in buscar_taxonomia()}
    problemas = conferir_folha(linhas, taxonomia)
    if problemas:
        print(f"Folha incompleta ou inválida ({len(problemas)}):\n  " + "\n  ".join(problemas))
        return 1
    confiancas = confiancas_da_ia()
    for l in linhas:
        l["confianca"] = confiancas.get((l["ano"], l["numero"], l["idioma"]), "?")

    grupos = contar(linhas, taxonomia)
    aprovado = True
    for area in AREAS:
        print(f"\n{NOME_DA_AREA[area]}")
        for confianca in ("todas", "alta", "media", "baixa"):
            n, assunto, materia = grupos.get((area, confianca), (0, 0, 0))
            if n == 0:
                continue
            baixo, alto = wilson(assunto, n)
            print(f"  {confianca:6} n={n:3}  assunto {assunto:3}/{n} = {assunto / n:6.1%}"
                  f" (95%: {baixo:.0%}–{alto:.0%})   matéria {materia}/{n} = {materia / n:.1%}")
        n, assunto, materia = grupos[(area, "todas")]
        aprovado &= assunto / n >= META_ASSUNTO and materia / n >= META_MATERIA
    print(f"\nMeta (a do piloto): assunto ≥ {META_ASSUNTO:.0%} e matéria ≥ {META_MATERIA:.0%} em cada área"
          f" → {'ATINGIDA' if aprovado else 'NÃO ATINGIDA'}")
    print("Com 60 por área a margem é larga: o acerto observado passa na meta, mas o intervalo diz quanto se sabe.")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    modo = parser.add_mutually_exclusive_group(required=True)
    modo.add_argument("--gerar", action="store_true")
    modo.add_argument("--medir", action="store_true")
    parser.add_argument("--forcar", action="store_true", help="regera a folha mesmo que já exista")
    args = parser.parse_args()
    return gerar(args.forcar) if args.gerar else medir()


if __name__ == "__main__":
    sys.exit(main())

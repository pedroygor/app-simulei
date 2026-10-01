"""
Cruza a habilidade oficial do INEP com o assunto que demos a cada questão
(2b.7, Passo B) e lista as que caem FORA da tabela de compatibilidade.

Por que é por COMPETÊNCIA e não por habilidade: a habilidade descreve o que o
aluno FAZ ("resolver situação-problema...", "avaliar proposta de intervenção..."),
não sobre o QUÊ. Em Matemática metade das 30 é genérica. A competência de área
(7 em Matemática) é o nível em que a matriz dá um tema.

A tabela (dados/conferencia-inep/compatibilidade.csv) sai do SIGNIFICADO da matriz
e não dos nossos dados: derivá-la da nossa classificação só aprovaria a própria
classificação. NUCLEO = o tema do assunto é o tema da competência; TOLERADA = o
assunto aparece legitimamente a serviço dela. O que cai fora vai pra revisão.

Isto é um detector de erro GROSSEIRO, não de erro fino. Assunto-ponte
(razão e proporção, geometria plana) se espalha legitimamente por várias
competências, então quase nada cai fora deles; já estatística classificada como
geometria não passa despercebida. Ver docs/habilidade-x-assunto.md.

Uso (da raiz; precisa do Postgres no ar e dos ITENS_PROVA baixados):
    python3 ferramentas/inep/cruzar_habilidade.py
    python3 ferramentas/inep/cruzar_habilidade.py --area MT
"""

import argparse
import csv
import sys
from collections import Counter
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "classificacao"))
from exportar_lotes import consultar_json  # noqa: E402

import microdados  # noqa: E402

PASTA = microdados.RAIZ / "dados" / "conferencia-inep"
AREA_DO_BANCO = {"MT": "MATEMATICA", "LC": "LINGUAGENS", "CH": "CIENCIAS_HUMANAS", "CN": "CIENCIAS_NATUREZA"}


def ler_faixas(caminho: Path = PASTA / "competencias.csv") -> dict[str, list[tuple[int, int, int]]]:
    """{area: [(habilidade_inicial, habilidade_final, competencia), ...]}"""
    faixas: dict[str, list] = {}
    with caminho.open(newline="", encoding="utf-8") as arquivo:
        for l in csv.DictReader(arquivo):
            faixas.setdefault(l["area"], []).append(
                (int(l["habilidade_inicial"]), int(l["habilidade_final"]), int(l["competencia"])))
    return faixas


def ler_compatibilidade(caminho: Path = PASTA / "compatibilidade.csv") -> dict[tuple[str, str], dict[int, str]]:
    """{(area, assunto): {competencia: papel}}"""
    tabela: dict[tuple[str, str], dict[int, str]] = {}
    with caminho.open(newline="", encoding="utf-8") as arquivo:
        for l in csv.DictReader(arquivo):
            tabela.setdefault((l["area"], l["assunto"]), {})[int(l["competencia"])] = l["papel"]
    return tabela


def ler_revisoes(caminho: Path = PASTA / "revisoes_habilidade.csv") -> dict[tuple[int, int, str], tuple[str, str]]:
    """{(ano, numero, idioma): (assunto_atual, decisao)} — o que já foi revisto"""
    if not caminho.exists():
        return {}
    with caminho.open(newline="", encoding="utf-8") as arquivo:
        return {(int(l["ano"]), int(l["numero"]), l["idioma"]): (l["assunto_atual"], l["decisao"])
                for l in csv.DictReader(arquivo)}


def ja_revisada(chave: tuple[int, int, str], assunto: str, revisoes) -> bool:
    """Uma revisão MANTIDA vale enquanto o assunto for o mesmo que foi revisto.

    Se alguém reclassificar depois, a questão volta a ser conferida: a decisão
    de manter era sobre aquele assunto, não sobre a questão pra sempre. Uma
    RECLASSIFICADA não precisa ser pulada: o assunto novo já é compatível, e se
    não for, é justamente o que a conferência tem que mostrar."""
    revisao = revisoes.get(chave)
    return revisao is not None and revisao[1] == "MANTIDA" and revisao[0] == assunto


def competencia_da(habilidade: int, faixas_da_area: list[tuple[int, int, int]]) -> int:
    for inicio, fim, competencia in faixas_da_area:
        if inicio <= habilidade <= fim:
            return competencia
    raise ValueError(f"habilidade H{habilidade} não pertence a nenhuma competência da tabela")


def fora_da_tabela(area: str, assunto: str, habilidade: int, faixas, tabela) -> int | None:
    """A competência, se ela NÃO for compatível com o assunto; None se estiver ok.

    Assunto que não está na tabela levanta erro em vez de passar: tabela
    incompleta tem que aparecer, não virar "tudo compatível" em silêncio."""
    compativeis = tabela.get((area, assunto))
    if compativeis is None:
        raise KeyError(f"assunto '{assunto}' ({area}) não está em compatibilidade.csv")
    competencia = competencia_da(habilidade, faixas[area])
    return None if competencia in compativeis else competencia


def tabela_cruzada(registros, faixas_da_area):
    """Linhas de assunto × competência, pra montar a tabela de compatibilidade.

    `registros`: iterável de (assunto, habilidade). Devolve
    [(assunto, {competencia: n}, total)], do assunto mais frequente ao menos.
    É material de TRABALHO: mostra o que a classificação e o INEP dizem, não o que
    a tabela deve dizer. A tabela sai do significado da matriz."""
    contagem: dict[str, Counter] = {}
    for assunto, habilidade in registros:
        contagem.setdefault(assunto, Counter())[competencia_da(int(habilidade), faixas_da_area)] += 1
    return sorted(((a, dict(c), sum(c.values())) for a, c in contagem.items()), key=lambda x: -x[2])


def texto_das_habilidades(area: str) -> dict[int, str]:
    """{habilidade: texto} lido da matriz de referência do INEP (PDF do zip de 2023).

    Opcional: sem o PDF (extrair_do_zip.py --padrao=matriz_referencia) ou sem o
    pdftotext, devolve vazio e o lote mostra só o número da habilidade."""
    import re
    import subprocess
    pdf = microdados.PASTA / "extraidos" / "2023" / "matriz_referencia_enem.pdf"
    titulo = {"MT": "Matemática e suas", "LC": "Linguagens, Códigos", "CN": "Ciências da Natureza", "CH": "Ciências Humanas"}[area]
    try:
        texto = subprocess.run(["pdftotext", "-layout", str(pdf), "-"], capture_output=True, text=True, check=True).stdout
    except (OSError, subprocess.CalledProcessError):
        return {}
    secoes = re.split(r"Matriz de Referência de ", texto)
    secao = next((x for x in secoes if x.startswith(titulo)), "")
    # (?=\n\s*\n|\n\s*Compet): o texto de uma habilidade acaba na linha em branco OU no
    # começo da próxima competência (que nem sempre vem separada por linha em branco)
    achados = re.findall(r"H(\d+)\s*[-–]\s*(.*?)(?=\n\s*\n|\n\s*Compet|\n\s*H\d+\s*[-–]|\Z)", secao, re.S)
    return {int(h): " ".join(t.split()) for h, t in achados}


def imprimir_lote(destacadas, area: str, faixas) -> None:
    """Material de revisão: cada destacada com tudo que é preciso pra decidir."""
    import csv as _csv
    habilidades = texto_das_habilidades(area)
    descricao = {c: d for (a, c, d) in _descricoes() if a == area}
    justificativas = {}
    with (microdados.RAIZ / "dados" / "classificacao" / "assuntos.csv").open(newline="", encoding="utf-8") as f:
        for l in _csv.DictReader(f):
            justificativas[(int(l["ano"]), int(l["numero"]), l["idioma"])] = l["justificativa"]
    ids = ",".join(f"({a},{n})" for _, _, (a, n, _), _, _, _ in destacadas) or "(0,0)"
    dados = {(q["ano"], q["numero"], q["idioma"]): q for q in consultar_json(
        "SELECT json_agg(t) FROM (SELECT q.ano, q.numero, coalesce(q.idioma,'') AS idioma, q.id, q.gabarito, "
        "q.enunciado, q.comando, (SELECT json_agg(json_build_object('l', l.letra, 't', l.texto, 'i', l.imagem_url) "
        "ORDER BY l.letra) FROM alternativas l WHERE l.questao_id = q.id) AS alts FROM questoes q "
        f"WHERE (q.ano, q.numero) IN ({ids})) t")}
    for sigla, assunto, chave, h, c, conf in destacadas:
        q = dados[chave]
        print(f"##### {chave[0]}/{chave[1]}{'/' + chave[2] if chave[2] else ''} · id {q['id']} · gabarito {q['gabarito']}")
        print(f"ATUAL: {assunto} [{conf}] — {justificativas.get(chave, '')}")
        print(f"INEP: H{h} (C{c}: {descricao.get(c, '')}) — {habilidades.get(h, '')}")
        print("ENUNCIADO: " + (q["enunciado"] or "(sem texto-base)"))
        print("COMANDO: " + (q["comando"] or ""))
        for a in q["alts"] or []:
            print(f"  {a['l']}) {a['t'] or ''} {('[IMG ' + a['i'] + ']') if a['i'] else ''}")
        print()


def _descricoes():
    with (PASTA / "competencias.csv").open(newline="", encoding="utf-8") as arquivo:
        return [(l["area"], int(l["competencia"]), l["descricao"]) for l in csv.DictReader(arquivo)]


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--area", choices=sorted(AREA_DO_BANCO), help="uma área só (padrão: as que estão na tabela)")
    parser.add_argument("--lote", action="store_true",
                        help="imprime as destacadas da --area com enunciado, alternativas e a habilidade oficial, pra revisão")
    parser.add_argument("--cruzada", action="store_true",
                        help="imprime assunto × competência da --area (material pra montar a tabela) e sai")
    args = parser.parse_args()

    faixas, tabela, revisoes = ler_faixas(), ler_compatibilidade(), ler_revisoes()
    areas = [args.area] if args.area else sorted(faixas)

    questoes = consultar_json(
        "SELECT json_agg(t) FROM (SELECT q.ano, q.numero, coalesce(q.idioma,'') AS idioma, q.gabarito, q.area, "
        "a.slug AS assunto, q.classificacao_confianca AS confianca FROM questoes q "
        "JOIN assuntos a ON a.id = q.assunto_id WHERE NOT q.anulada) t")
    banco = {(q["ano"], q["numero"], q["idioma"]): q for q in questoes}
    # O gabarito do banco serve SÓ pra achar o caderno de referência (microdados.casar)
    itens = {(i.ano, i.numero, i.idioma): i for i in microdados.casar(
        {k: q["gabarito"] for k, q in banco.items() if q["gabarito"]})}

    if args.cruzada:
        if not args.area:
            parser.error("--cruzada precisa de --area")
        registros = [(q["assunto"], int(itens[k].habilidade)) for k, q in sorted(banco.items())
                     if q["area"] == AREA_DO_BANCO[args.area] and k in itens and itens[k].habilidade]
        competencias = sorted({c for _, _, c in faixas[args.area]})
        print(f"{args.area}: {len(registros)} questões com habilidade\n")
        print(f"{'assunto':<36}" + "".join(f"  C{c}" for c in competencias) + "   total")
        for assunto, por_competencia, n in tabela_cruzada(registros, faixas[args.area]):
            print(f"{assunto:<36}" + "".join(f"{por_competencia.get(c, 0):>4}" for c in competencias) + f"   {n:>4}")
        return 0

    fora, total, mantidas = [], Counter(), Counter()
    total_por_confianca, fora_por_confianca = Counter(), Counter()
    for chave, q in sorted(banco.items()):
        sigla = next((s for s, a in AREA_DO_BANCO.items() if a == q["area"]), None)
        item = itens.get(chave)
        if sigla not in areas or item is None or not item.habilidade:
            continue
        total[sigla] += 1
        total_por_confianca[q["confianca"]] += 1
        competencia_fora = fora_da_tabela(sigla, q["assunto"], int(item.habilidade), faixas, tabela)
        if competencia_fora is not None and ja_revisada(chave, q["assunto"], revisoes):
            mantidas[sigla] += 1  # destacada antes, revista por uma pessoa e mantida
            continue
        competencia = competencia_fora
        if competencia is not None:
            fora.append((sigla, q["assunto"], chave, int(item.habilidade), competencia, q["confianca"]))
            fora_por_confianca[q["confianca"]] += 1

    if args.lote:
        if not args.area:
            parser.error("--lote precisa de --area")
        imprimir_lote([f for f in fora if f[0] == args.area], args.area, faixas)
        return 0

    for sigla in areas:
        n_fora = sum(1 for f in fora if f[0] == sigla)
        print(f"{sigla}: {total[sigla]} questões com habilidade | fora da tabela e ainda por rever: {n_fora} "
              f"({n_fora / total[sigla]:.1%}) | destacadas, revistas e mantidas: {mantidas[sigla]}"
              if total[sigla] else f"{sigla}: sem questões")
    print("\nTaxa fora da tabela por confiança da classificação (deve subir de ALTA pra BAIXA):")
    for c in ("ALTA", "MEDIA", "BAIXA"):
        if total_por_confianca[c]:
            print(f"  {c:<6} {fora_por_confianca[c]:>3}/{total_por_confianca[c]:<4} {fora_por_confianca[c] / total_por_confianca[c]:.1%}")

    print("\nassunto | ano/número | habilidade | competência | confiança")
    for sigla, assunto, (ano, numero, idioma), h, c, conf in fora:
        print(f"  {assunto:<34} {ano}/{numero}{'/' + idioma[:2] if idioma else '':<3} H{h:<3} C{c}  {conf}")
    return 0


if __name__ == "__main__":
    sys.exit(main())

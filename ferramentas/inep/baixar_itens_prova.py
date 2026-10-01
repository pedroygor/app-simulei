"""
Baixa o ITENS_PROVA dos microdados do ENEM (INEP) SEM baixar o zip inteiro.

Por que existe (2b.7): o INEP publica, por questão, a habilidade oficial
(CO_HABILIDADE, H1–H30) e os parâmetros TRI. Cruzar com o assunto que a IA deu
é a conferência que a Etapa 4 exige antes de mostrar diagnóstico a alguém.

Por que não é só um curl: o ITENS_PROVA é um CSV de poucas centenas de KB
dentro de um zip de 550 MB a 1 GB (uns 10 GB nos 15 anos). O servidor aceita
`Range`, e o zip guarda o índice no FIM do arquivo — então dá pra ler o índice
e só os bytes do arquivo que interessa. Transferido: alguns KB por ano.

Segurança (TLS):
  O servidor do INEP entrega só o certificado dele, sem o intermediário
  "RNP ICPEdu GR46 OV TLS CA 2025". Por isso um curl/Python comum falha com
  "unable to get local issuer certificate". NÃO se desliga a verificação: o
  intermediário (certificado público, emitido pela GlobalSign Root R46, que o
  sistema já confia) está em ferramentas/inep/ e é acrescentado à confiança
  SÓ deste script. Se o INEP consertar a cadeia, continua funcionando; se
  trocar de emissor, o script falha alto em vez de baixar sem verificar.

O que é gravado:
  dados/inep/ITENS_PROVA_{ano}.csv, byte a byte como veio (latin-1, separador
  ';'). Conversão de encoding é problema do cruzamento, não do download.
  dados/inep/ é dado de terceiro: fica fora do git; o script refaz.

Uso (da raiz):
    python3 ferramentas/inep/baixar_itens_prova.py --listar --anos=2023
    python3 ferramentas/inep/baixar_itens_prova.py --anos=2023
    python3 ferramentas/inep/baixar_itens_prova.py             (2009–2023)
Só biblioteca padrão.
"""

import argparse
import re
import ssl
import sys
import zipfile
from pathlib import Path

from zip_remoto import TAMANHO_MAXIMO, URL, ArquivoRemoto, abrir_zip, contexto_tls

RAIZ = Path(__file__).resolve().parents[2]
DESTINO = RAIZ / "dados" / "inep"

ANOS_PADRAO = range(2009, 2024)


def listar(ano: int, contexto: ssl.SSLContext) -> list[zipfile.ZipInfo]:
    z, _ = abrir_zip(ano, contexto)
    with z:
        return z.infolist()


def eh_itens_prova(nome: str) -> bool:
    return re.search(r"itens_prova.*\.csv$", nome, re.IGNORECASE) is not None


def baixar(ano: int, contexto: ssl.SSLContext) -> Path | None:
    z, remoto = abrir_zip(ano, contexto)
    with z:
        candidatos = [i for i in z.infolist() if eh_itens_prova(i.filename)]
        if not candidatos:
            print(f"  {ano}: NÃO há ITENS_PROVA neste zip — use --listar pra ver o conteúdo")
            return None
        if len(candidatos) > 1:
            # Melhor parar do que escolher um no palpite: seria dado errado em silêncio
            nomes = ", ".join(c.filename for c in candidatos)
            raise RuntimeError(f"{ano}: mais de um candidato a ITENS_PROVA: {nomes}")

        item = candidatos[0]
        if item.file_size > TAMANHO_MAXIMO:
            raise RuntimeError(f"{ano}: {item.filename} tem {item.file_size} bytes (teto {TAMANHO_MAXIMO})")

        # z.read confere o CRC-32 ao chegar no fim: arquivo corrompido em
        # trânsito vira BadZipFile em vez de CSV truncado
        conteudo = z.read(item)

    DESTINO.mkdir(parents=True, exist_ok=True)
    saida = DESTINO / f"ITENS_PROVA_{ano}.csv"
    # Escrita atômica: o mesmo cuidado do storage de imagens. Interrompido no
    # meio, não deixa CSV truncado com nome de arquivo válido
    temporario = saida.with_suffix(".csv.tmp")
    temporario.write_bytes(conteudo)
    temporario.replace(saida)

    print(
        f"  {ano}: {item.filename} -> {saida.relative_to(RAIZ)} "
        f"({len(conteudo):,} bytes; transferidos {remoto.bytes_baixados / 1024:,.0f} KB "
        f"em {remoto.requisicoes} requisições, de um zip de {remoto.tamanho / 1e6:,.0f} MB)"
    )
    return saida


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--anos", help="lista separada por vírgula (padrão: 2009–2023)")
    parser.add_argument("--listar", action="store_true", help="só lista o conteúdo dos zips, sem gravar nada")
    args = parser.parse_args()

    anos = [int(a) for a in args.anos.split(",")] if args.anos else list(ANOS_PADRAO)
    contexto = contexto_tls()
    falhas = 0

    for ano in anos:
        try:
            if args.listar:
                print(f"== {ano}")
                for info in listar(ano, contexto):
                    print(f"  {info.file_size:>14,}  {info.filename}")
            else:
                baixar(ano, contexto)
        except (RuntimeError, OSError, zipfile.BadZipFile) as erro:
            falhas += 1
            print(f"  {ano}: FALHOU — {erro}", file=sys.stderr)

    return 1 if falhas else 0


if __name__ == "__main__":
    sys.exit(main())

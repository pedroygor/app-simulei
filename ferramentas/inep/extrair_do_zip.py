"""
Extrai do zip de microdados do INEP os arquivos cujo nome casa com uma regex.

Serve pra buscar documentos que acompanham os microdados — gabaritos oficiais
em PDF, a matriz de referência — sem baixar o zip de 500 MB a 1 GB. Mesmo
mecanismo e mesmos cuidados de TLS do baixar_itens_prova.py.

Uso (da raiz):
    python3 ferramentas/inep/extrair_do_zip.py --anos=2021 --padrao='P1_GAB_0[17]_.*AZUL\\.pdf'
    python3 ferramentas/inep/extrair_do_zip.py --anos=2023 --padrao='matriz_referencia'
Grava em dados/inep/extraidos/{ano}/ (fora do git).
"""

import argparse
import sys
from pathlib import Path

from zip_remoto import contexto_tls, extrair

RAIZ = Path(__file__).resolve().parents[2]


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--anos", required=True, help="lista separada por vírgula")
    parser.add_argument("--padrao", required=True, help="regex sobre o caminho do arquivo no zip")
    args = parser.parse_args()

    contexto = contexto_tls()
    falhas = 0
    for ano in (int(a) for a in args.anos.split(",")):
        try:
            gravados = extrair(ano, args.padrao, RAIZ / "dados" / "inep" / "extraidos" / str(ano), contexto)
        except (RuntimeError, OSError) as erro:
            falhas += 1
            print(f"  {ano}: FALHOU — {erro}", file=sys.stderr)
            continue
        if not gravados:
            print(f"  {ano}: nenhum arquivo casa com a regex")
        for caminho in gravados:
            print(f"  {ano}: {caminho.relative_to(RAIZ)} ({caminho.stat().st_size:,} bytes)")
    return 1 if falhas else 0


if __name__ == "__main__":
    sys.exit(main())

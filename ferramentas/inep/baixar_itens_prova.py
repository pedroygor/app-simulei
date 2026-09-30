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
import io
import re
import ssl
import sys
import time
import urllib.error
import urllib.request
import zipfile
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[2]
DESTINO = RAIZ / "dados" / "inep"
INTERMEDIARIO = Path(__file__).resolve().parent / "rnp-icpedu-gr46-ov-tls-ca-2025.pem"

URL = "https://download.inep.gov.br/microdados/microdados_enem_{ano}.zip"
ANOS_PADRAO = range(2009, 2024)

# Bloco lido por requisição. O zipfile lê de 4 KB em 4 KB; sem isto seriam
# centenas de requisições por arquivo
BLOCO = 1024 * 1024
TENTATIVAS = 4
# ITENS_PROVA real tem < 1 MB. O teto protege contra zip malformado ou hostil
# (arquivo que "descomprime" pra gigabytes)
TAMANHO_MAXIMO = 20 * 1024 * 1024


def contexto_tls() -> ssl.SSLContext:
    """Verificação COMPLETA, com o intermediário faltante do INEP acrescentado."""
    contexto = ssl.create_default_context()
    contexto.load_verify_locations(cafile=str(INTERMEDIARIO))
    return contexto


class ArquivoRemoto(io.RawIOBase):
    """
    Arquivo somente-leitura sobre HTTP com `Range`, o suficiente pro zipfile.

    Guarda o último bloco lido: o zipfile faz muitas leituras pequenas e
    vizinhas (cabeçalho, nome, dados), que caem quase todas no mesmo bloco.
    """

    def __init__(self, url: str, contexto: ssl.SSLContext):
        self.url = url
        self.contexto = contexto
        self.posicao = 0
        self.bloco_inicio = 0
        self.bloco = b""
        self.requisicoes = 0
        self.bytes_baixados = 0
        self.tamanho = self._descobrir_tamanho()

    def _abrir(self, requisicao):
        # Servidor de governo cai e oscila (anotado no CLAUDE.md): tenta de novo
        # com espera crescente. Erro 4xx é resposta definitiva, não se repete
        for tentativa in range(1, TENTATIVAS + 1):
            try:
                return urllib.request.urlopen(requisicao, context=self.contexto, timeout=60)
            except urllib.error.HTTPError as e:
                if e.code < 500 or tentativa == TENTATIVAS:
                    raise
            except (urllib.error.URLError, TimeoutError):
                if tentativa == TENTATIVAS:
                    raise
            time.sleep(2**tentativa)

    def _descobrir_tamanho(self) -> int:
        with self._abrir(urllib.request.Request(self.url, method="HEAD")) as r:
            if r.headers.get("Accept-Ranges") != "bytes":
                raise RuntimeError("o servidor não aceita Range: baixar o zip inteiro seria o único jeito")
            return int(r.headers["Content-Length"])

    def readable(self):
        return True

    def seekable(self):
        return True

    def tell(self):
        return self.posicao

    def seek(self, deslocamento, referencia=io.SEEK_SET):
        if referencia == io.SEEK_SET:
            self.posicao = deslocamento
        elif referencia == io.SEEK_CUR:
            self.posicao += deslocamento
        else:
            self.posicao = self.tamanho + deslocamento
        return self.posicao

    def _carregar(self, inicio: int) -> None:
        fim = min(inicio + BLOCO, self.tamanho) - 1
        req = urllib.request.Request(self.url, headers={"Range": f"bytes={inicio}-{fim}"})
        with self._abrir(req) as r:
            if r.status != 206:
                # 200 = ignorou o Range e mandaria o zip inteiro. Melhor parar
                raise RuntimeError(f"esperava 206 Partial Content, veio {r.status}")
            dados = r.read()
        self.requisicoes += 1
        self.bytes_baixados += len(dados)
        self.bloco_inicio, self.bloco = inicio, dados

    def readinto(self, destino):
        if self.posicao >= self.tamanho:
            return 0
        fim_bloco = self.bloco_inicio + len(self.bloco)
        if not (self.bloco_inicio <= self.posicao < fim_bloco):
            # Leitura perto do fim do arquivo (o índice do zip): recua o bloco
            # pra que ele termine no fim, em vez de pedir um bloco quase vazio
            self._carregar(min(self.posicao, max(0, self.tamanho - BLOCO)))
        deslocamento = self.posicao - self.bloco_inicio
        pedaco = self.bloco[deslocamento : deslocamento + len(destino)]
        destino[: len(pedaco)] = pedaco
        self.posicao += len(pedaco)
        return len(pedaco)


def listar(ano: int, contexto: ssl.SSLContext) -> list[zipfile.ZipInfo]:
    remoto = ArquivoRemoto(URL.format(ano=ano), contexto)
    with zipfile.ZipFile(io.BufferedReader(remoto)) as z:
        return z.infolist()


def eh_itens_prova(nome: str) -> bool:
    return re.search(r"itens_prova.*\.csv$", nome, re.IGNORECASE) is not None


def baixar(ano: int, contexto: ssl.SSLContext) -> Path | None:
    remoto = ArquivoRemoto(URL.format(ano=ano), contexto)
    with zipfile.ZipFile(io.BufferedReader(remoto)) as z:
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

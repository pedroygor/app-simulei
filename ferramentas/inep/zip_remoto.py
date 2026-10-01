"""
Leitura de zips do INEP por HTTP `Range`, sem baixar o arquivo inteiro.

Compartilhado por baixar_itens_prova.py e extrair_do_zip.py. O motivo do
`Range` e o cuidado com o TLS estão explicados em baixar_itens_prova.py.
"""

import io
import re
import ssl
import time
import urllib.error
import urllib.request
import zipfile
from pathlib import Path

INTERMEDIARIO = Path(__file__).resolve().parent / "rnp-icpedu-gr46-ov-tls-ca-2025.pem"

URL = "https://download.inep.gov.br/microdados/microdados_enem_{ano}.zip"
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


def abrir_zip(ano: int, contexto: ssl.SSLContext | None = None) -> tuple[zipfile.ZipFile, "ArquivoRemoto"]:
    remoto = ArquivoRemoto(URL.format(ano=ano), contexto or contexto_tls())
    return zipfile.ZipFile(io.BufferedReader(remoto)), remoto


def extrair(ano: int, padrao: str, destino: Path, contexto: ssl.SSLContext | None = None) -> list[Path]:
    """Grava em `destino` os membros do zip do ano cujo caminho casa com `padrao`
    (regex, sem diferenciar maiúsculas). Só o nome-base é usado no disco: o nome
    vem do zip, que é entrada não confiável, e um caminho com `../` sairia do
    diretório (zip slip)."""
    gravados = []
    z, _ = abrir_zip(ano, contexto)
    with z:
        for info in z.infolist():
            if info.is_dir() or not re.search(padrao, info.filename, re.IGNORECASE):
                continue
            if info.file_size > TAMANHO_MAXIMO:
                raise RuntimeError(f"{ano}: {info.filename} tem {info.file_size} bytes (teto {TAMANHO_MAXIMO})")
            destino.mkdir(parents=True, exist_ok=True)
            saida = destino / Path(info.filename).name
            temporario = saida.with_name(saida.name + ".tmp")
            temporario.write_bytes(z.read(info))   # confere o CRC-32 ao ler
            temporario.replace(saida)
            gravados.append(saida)
    return gravados

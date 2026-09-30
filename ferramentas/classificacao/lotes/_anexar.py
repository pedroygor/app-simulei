# Auxiliar descartável da sessão: anexa linhas "ano|numero|idioma|assunto|confianca|justificativa"
# lidas da entrada padrão ao CSV, com o quoting certo
import csv, sys
linhas = [l.rstrip('\n').split('|') for l in sys.stdin if l.strip()]
assert all(len(l) == 6 for l in linhas), [l for l in linhas if len(l) != 6]
with open('dados/classificacao/assuntos.csv', 'a', newline='', encoding='utf-8') as f:
    csv.writer(f).writerows(linhas)
print(len(linhas), 'linhas anexadas')

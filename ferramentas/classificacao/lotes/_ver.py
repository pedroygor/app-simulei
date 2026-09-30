# Auxiliar descartável da sessão: mostra um lote de forma compacta
import re, sys
texto = open(sys.argv[1], encoding='utf-8').read().split('## Questões', 1)[1]
for bloco in texto.split('\n---\n'):
    b = bloco.strip()
    if not b:
        continue
    b = re.sub(r'\*\*Imagens do enunciado:\*\*.*', '', b)
    b = re.sub(r'!\[[^\]]*\]\((/imagens/[^)]+)\)', r'[IMG storage\1]', b)
    b = re.sub(r'!\[[^\]]*\]\(https?://[^)]+\)', '[IMG quebrada]', b)
    partes = re.split(r'\*\*(Enunciado|Comando|Alternativas):\*\*', b)
    cab = partes[0].strip().replace('### ', '')
    campos = dict(zip(partes[1::2], partes[2::2]))
    enun = re.sub(r'\s+', ' ', campos.get('Enunciado', '')).strip()
    if len(enun) > 700:
        enun = enun[:700] + ' […]'
    com = re.sub(r'\s+', ' ', campos.get('Comando', '')).strip()
    alts = ' / '.join(re.sub(r'\s+', ' ', a).strip()[:110] for a in campos.get('Alternativas', '').strip().split('\n- ') if a.strip())
    print(f'## {cab}\nE: {enun}\nC: {com}\nA: {alts}\n')

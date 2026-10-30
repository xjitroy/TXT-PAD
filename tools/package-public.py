#!/usr/bin/env python3
"""Package public source/store material, refusing private signing material."""
from pathlib import Path
import argparse
import zipfile

p=argparse.ArgumentParser();p.add_argument('--out',default='dist');a=p.parse_args()
root=Path(__file__).resolve().parents[1]
out=Path(a.out).resolve();out.mkdir(parents=True,exist_ok=True)
private_suffixes={'.p12','.jks','.keystore','.key','.password'}
ignore={'.git','.gradle','.kotlin','.idea','build','__pycache__','dist'}
files=[]
for f in root.rglob('*'):
 if not f.is_file() or any(part in ignore for part in f.relative_to(root).parts) or f.is_relative_to(out):continue
 assert f.suffix not in private_suffixes and not f.name.startswith('TXT-private') and 'keystore-password' not in f.name,f'Private signing file found: {f.name}'
 if f.name=='local.properties' or f.suffix=='.pyc': continue
 assert not f.name.startswith('.env'),f'Environment credentials found: {f.name}'
 assert (b'PRIVATE'+b' KEY-----') not in f.read_bytes(),f'Private key content found: {f.name}'
 files.append(f)
with zipfile.ZipFile(out/'TXT-source.zip','w',zipfile.ZIP_DEFLATED) as z:
 for f in sorted(files):z.write(f,'TXT-PAD/'+str(f.relative_to(root)))
store=list((root/'store').rglob('*'))+[root/'PRIVACY.md',root/'docs/PLAY_STORE.md',root/'docs/RELEASING.md']
with zipfile.ZipFile(out/'TXT-store-assets.zip','w',zipfile.ZIP_DEFLATED) as z:
 for f in sorted(store):
  if f.is_file():z.write(f,str(f.relative_to(root)))
print('Created public source and store packages; private signing material excluded.')

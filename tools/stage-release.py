#!/usr/bin/env python3
"""Stage already-signed PUBLIC artifacts as Git blobs; never sends signing keys."""
import argparse,base64,gzip,hashlib,json,pathlib,re,subprocess
p=argparse.ArgumentParser()
p.add_argument('--dir',required=True);p.add_argument('--source',required=True);p.add_argument('--run',required=True,type=int)
p.add_argument('--tag',required=True);p.add_argument('--repo',default='xjitroy/TXT-PAD')
p.add_argument('--out',default='release/publish-request.json');a=p.parse_args()
root=pathlib.Path(a.dir)
assert re.fullmatch(r'[0-9a-f]{40}',a.source)
assert re.fullmatch(r'v[0-9]+\.[0-9]+\.[0-9]+',a.tag)
assert not any(f.suffix in {'.p12','.jks','.keystore','.key','.password'} for f in root.iterdir()),'Private material cannot be in the public artifact directory'
required=['TXT.apk','TXT.aab','SHA256SUMS','release-metadata.json','TXT-source.zip','TXT-store-assets.zip','VERIFICATION.json','mapping.txt']
assert all((root/f).is_file() for f in required),'Prepare all public artifacts first'
metadata=json.loads((root/'release-metadata.json').read_text())
assert a.tag=='v'+metadata['versionName']
(root/'mapping.txt.gz').write_bytes(gzip.compress((root/'mapping.txt').read_bytes(),mtime=0))
files={}
for name in required[:-1]+['mapping.txt.gz']:
    data=(root/name).read_bytes()
    response=subprocess.check_output(['gh','api','--method','POST',f'repos/{a.repo}/git/blobs','--input','-'],
        input=json.dumps({'encoding':'base64','content':base64.b64encode(data).decode()}).encode())
    sha=json.loads(response)['sha']
    files[name]={'blob':sha,'sha256':hashlib.sha256(data).hexdigest(),'bytes':len(data)}
    print('Staged public artifact:',name)
request={'tag':a.tag,'source_commit':a.source,'verification_run':a.run,'files':files,
         'mapping_sha256':hashlib.sha256((root/'mapping.txt').read_bytes()).hexdigest()}
pathlib.Path(a.out).write_text(json.dumps(request,indent=2)+'\n')
print('Commit',a.out,'to trigger signed artifact verification and publication.')

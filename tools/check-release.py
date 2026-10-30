#!/usr/bin/env python3
"""Verify signatures, offline permissions, SDK values and 16 KiB native ELF alignment."""
import argparse
import hashlib
import json
import os
import pathlib
import re
import struct
import subprocess
import zipfile

p=argparse.ArgumentParser()
p.add_argument('apk');p.add_argument('aab');p.add_argument('--out',default='release-metadata.json')
a=p.parse_args()
build=pathlib.Path(os.environ['TXT_BUILD_TOOLS'])
def run(*args): return subprocess.check_output([str(x) for x in args],stderr=subprocess.STDOUT).decode()
cert=pathlib.Path(__file__).resolve().parents[1]/'release/signing-certificate.pem'
cert_der=subprocess.check_output(['openssl','x509','-in',str(cert),'-outform','DER'])
expected=hashlib.sha256(cert_der).hexdigest()
verified=run(build/'apksigner','verify','--verbose','--print-certs',a.apk)
actual=re.search(r'Signer #1 certificate SHA-256 digest: ([0-9a-f]+)',verified).group(1)
assert actual==expected,'APK signing certificate differs from the pinned public certificate'
run(build/'zipalign','-c','-P','16','4',a.apk)
assert 'jar verified' in run('jarsigner','-verify',a.aab)
bundle_certificate=run('keytool','-J-Duser.language=en','-printcert','-jarfile',a.aab)
bundle_digest=re.search(r'SHA256: ([0-9A-F:]+)',bundle_certificate).group(1).replace(':','').lower()
assert bundle_digest==expected,'AAB signing certificate differs from the pinned public certificate'
badging=run(build/'aapt2','dump','badging',a.apk)
permissions=run(build/'aapt2','dump','permissions',a.apk)
assert 'android.permission.INTERNET' not in permissions
assert 'android.permission.ACCESS_NETWORK_STATE' not in permissions
version=re.search(r"package: name='([^']+)' versionCode='([^']+)' versionName='([^']+)'",badging)
assert version and version.group(1)=='com.secure.textpad'
assert re.search(r"sdkVersion:'26'",badging)
assert re.search(r"targetSdkVersion:'36'",badging)
native=[]
with zipfile.ZipFile(a.apk) as z:
    assert z.testzip() is None
    for name in z.namelist():
        if not name.endswith('.so'): continue
        data=z.read(name)
        native.append(name)
        if '/arm64-v8a/' not in name and '/x86_64/' not in name: continue
        assert data[:5]==b'\x7fELF\x02',f'Expected ELF64 in {name}'
        endian='<' if data[5]==1 else '>'
        offset=struct.unpack_from(endian+'Q',data,32)[0]
        entry,count=struct.unpack_from(endian+'HH',data,54)
        for i in range(count):
            pos=offset+i*entry
            if struct.unpack_from(endian+'I',data,pos)[0]==1:
                alignment=struct.unpack_from(endian+'Q',data,pos+48)[0]
                assert alignment>=16384,f'{name} needs 16 KiB LOAD alignment, got {alignment}'
with zipfile.ZipFile(a.aab) as z:
    assert z.testzip() is None
    assert 'base/manifest/AndroidManifest.xml' in z.namelist()
metadata={'package':version.group(1),'versionCode':int(version.group(2)),'versionName':version.group(3),
          'minSdk':26,'targetSdk':36,'signingCertificateSha256':expected,
          'networkPermissions':False,'apkSignature':'verified','aabSignature':'verified',
          'native64Bit16KiBAlignment':'verified','nativeLibraries':native,
          'files':{pathlib.Path(f).name:{'bytes':pathlib.Path(f).stat().st_size,'sha256':hashlib.sha256(pathlib.Path(f).read_bytes()).hexdigest()} for f in [a.apk,a.aab]}}
pathlib.Path(a.out).write_text(json.dumps(metadata,indent=2)+'\n')
print(json.dumps(metadata,indent=2))

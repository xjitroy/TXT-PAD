#!/usr/bin/env python3
"""Build a separate, test-only clipboard receiver; never uses the TXT signing key."""
import argparse
import os
import pathlib
import subprocess
import zipfile

p=argparse.ArgumentParser();p.add_argument('--out',required=True);a=p.parse_args()
out=pathlib.Path(a.out).resolve();out.mkdir(parents=True,exist_ok=True)
root=pathlib.Path(__file__).resolve().parent/'qa-input'
sdk_name=os.environ.get('ANDROID_HOME') or os.environ.get('ANDROID_SDK_ROOT')
if not sdk_name and os.environ.get('TXT_BUILD_TOOLS'):sdk_name=str(pathlib.Path(os.environ['TXT_BUILD_TOOLS']).parent.parent)
assert sdk_name,'Set ANDROID_HOME or TXT_BUILD_TOOLS to the Android SDK location'
sdk=pathlib.Path(sdk_name)
android=sdk/'platforms/android-36/android.jar';build=sdk/'build-tools/36.0.0'
assert android.is_file() and (build/'d8').is_file(),'Install SDK platform and build-tools 36 first'
classes=out/'classes';classes.mkdir(exist_ok=True);dex=out/'dex';dex.mkdir(exist_ok=True)
def run(*command):subprocess.run([str(c) for c in command],check=True,stdout=subprocess.DEVNULL,timeout=90)
run('javac','-source','8','-target','8','-classpath',android,'-d',classes,root/'ClipboardInput.java')
run(build/'d8','--lib',android,'--min-api','26','--output',dex,*classes.rglob('*.class'))
unsigned=out/'unsigned.apk'
run(build/'aapt2','link','-o',unsigned,'--manifest',root/'AndroidManifest.xml','-I',android,'--min-sdk-version','26','--target-sdk-version','36')
with zipfile.ZipFile(unsigned,'a',zipfile.ZIP_DEFLATED) as z:z.write(dex/'classes.dex','classes.dex')
key=out/'test-only.p12'
if not key.exists():run('keytool','-genkeypair','-keystore',key,'-storetype','PKCS12','-storepass','android','-alias','qa-input','-keyalg','RSA','-keysize','2048','-validity','2','-dname','CN=TXT synthetic QA helper')
aligned=out/'aligned.apk';run(build/'zipalign','-f','4',unsigned,aligned)
run(build/'apksigner','sign','--ks',key,'--ks-key-alias','qa-input','--ks-pass','pass:android','--out',out/'TXT-qa-input.apk',aligned)
print(out/'TXT-qa-input.apk')

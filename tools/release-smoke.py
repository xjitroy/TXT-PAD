#!/usr/bin/env python3
"""Drive the real, minified signed APK. All taps come from UI XML bounds."""
import argparse
import pathlib
import re
import subprocess
import tempfile
import time
import urllib.parse
import xml.etree.ElementTree as ET

parser = argparse.ArgumentParser()
parser.add_argument('--apk', required=True)
parser.add_argument('--out', default='release-proof')
args = parser.parse_args()
out = pathlib.Path(args.out); out.mkdir(parents=True, exist_ok=True)

def adb(*parts, binary=False):
    result = subprocess.run(['adb', *parts], capture_output=True, timeout=40)
    if result.returncode:
        print(result.stdout.decode('utf-8',errors='replace'),result.stderr.decode('utf-8',errors='replace'),flush=True)
        result.check_returncode()
    return result.stdout if binary else result.stdout.decode('utf-8', errors='replace')

def tree():
    adb('shell', 'uiautomator', 'dump', '/sdcard/txt-ui.xml')
    xml = adb('exec-out', 'cat', '/sdcard/txt-ui.xml')
    return ET.fromstring(xml[xml.index('<?xml'):])

def bounds(node):
    return tuple(map(int, re.findall(r'\d+', node.attrib['bounds'])))

def locate(label=None, *, edit=False, timeout=25, scroll=False, resource=None):
    deadline = time.monotonic() + timeout
    swiped = False
    while time.monotonic() < deadline:
        root = tree()
        nodes = [n for n in root.iter('node') if
                 n.get('enabled') != 'false' and
                 (resource is None or n.get('resource-id', '').endswith('/'+resource)) and
                 (not edit or n.get('class') == 'android.widget.EditText') and
                 (label is None or label.casefold() in ((n.get('text') or '').casefold(), (n.get('content-desc') or '').casefold()))]
        nodes = [n for n in nodes if bounds(n)[2] > bounds(n)[0] and bounds(n)[3] > bounds(n)[1]]
        if nodes:
            return nodes[0]
        if scroll and not swiped:
            scrolls = [n for n in root.iter('node') if n.get('scrollable') == 'true']
            if scrolls:
                x1, y1, x2, y2 = bounds(max(scrolls, key=lambda n: bounds(n)[3]-bounds(n)[1]))
                adb('shell', 'input', 'swipe', str((x1+x2)//2), str((y1+3*y2)//4), str((3*y1+y2)//4), '350')
                swiped = True
        time.sleep(.3)
    failed=ET.tostring(tree(), encoding='unicode')
    (out/'failed-ui.xml').write_text(failed)
    print('Synthetic release-smoke UI at failure:',failed,flush=True)
    raise AssertionError(f'UI element not found: {label!r}, edit={edit}')

def tap(label=None, *, node=None, **options):
    n = node if node is not None else locate(label, **options)
    x1,y1,x2,y2 = bounds(n)
    adb('shell', 'input', 'tap', str((x1+x2)//2), str((y1+y2)//2))
    return n

def text(value):
    assert re.fullmatch(r'[A-Za-z0-9 ._-]+', value)
    adb('shell', 'input', 'text', value.replace(' ', '%s'))

def hide_keyboard():
    state = adb('shell', 'dumpsys', 'input_method')
    if re.search(r'(?:mInputShown|mIsInputViewShown|mInputViewShown)=true', state):
        adb('shell','input','keyevent','KEYCODE_BACK')
        time.sleep(.3)

def screenshot(name):
    time.sleep(.5)
    (out/f'{name}.png').write_bytes(adb('exec-out', 'screencap', '-p', binary=True))
    (out/f'{name}.xml').write_text(ET.tostring(tree(), encoding='unicode'))

def external():
    folder=urllib.parse.quote('primary:Documents/TXT-QA',safe='')
    document=urllib.parse.quote('primary:Documents/TXT-QA/TXT-release-check.txt',safe='')
    uri='content://com.android.externalstorage.documents/tree/'+folder+'/document/'+document
    adb('shell', 'am', 'start', '-W', '-a', 'android.intent.action.VIEW', '-d', uri,
        '-n', 'com.secure.textpad/.MainActivity')

def wait_saved():
    locate('Saved on this device', timeout=45)
    time.sleep(.3)

def check_raw(predicate, timeout=45):
    deadline=time.monotonic()+timeout
    while time.monotonic()<deadline:
        data=adb('exec-out','cat','/sdcard/Documents/TXT-QA/TXT-release-check.txt',binary=True)
        if predicate(data): return data
        time.sleep(.4)
    raise AssertionError('Saved document bytes did not reach the expected state')

adb('logcat','-c')
adb('install','-r',args.apk)
adb('shell','settings','put','secure','show_ime_with_hard_keyboard','1')
adb('shell','cmd','uimode','night','yes')
adb('shell','am','start','-W','-n','com.secure.textpad/.MainActivity')
locate('Choose workspace folder',timeout=45)
screenshot('release-setup-dark')
with tempfile.TemporaryDirectory() as tmp:
    fixture=pathlib.Path(tmp)/'TXT-release-check.txt'
    fixture.write_text('Release check\nA clear page for your ideas.\nEverything stays on this device.\n',encoding='utf-8')
    adb('shell','mkdir','-p','/sdcard/Documents/TXT-QA')
    adb('push',str(fixture),'/sdcard/Documents/TXT-QA/TXT-release-check.txt')
# Select the real workspace through SAF; no shell-granted URI permission shortcut.
tap('Choose workspace folder')
tap('Documents',scroll=True,timeout=45)
tap('TXT-QA',scroll=True,timeout=45)
locate('TXT-QA',resource='breadcrumb_text')
tap('Use this folder')
tap('Allow',timeout=25)
locate('New File',timeout=45)
external()
locate('TXT-release-check.txt')
field=locate(edit=True)
tap(node=field)
adb('shell','input','keyevent','KEYCODE_MOVE_END')
text('Release typing')
wait_saved()
check_raw(lambda b:b'Release typing' in b)
screenshot('release-editor-keyboard')
tap('Undo');wait_saved();check_raw(lambda b:b'Release typing' not in b)
tap('Redo');wait_saved();check_raw(lambda b:b'Release typing' in b)
hide_keyboard()
# Native password inputs and authenticated save in the minified build.
tap('Lock')
locate('Lock this document')
for index in range(2):
    fields=[n for n in tree().iter('node') if n.get('class')=='android.widget.EditText']
    assert len(fields)==2
    tap(node=fields[index]);text('release-check-password')
tap('Continue')
wait_saved();check_raw(lambda b:b.startswith(b'STXT'))
# Close the editor using its named Back control, then require a password on re-open.
tap('Back to files')
external();locate('Unlock TXT-release-check.txt')
tap(edit=True);text('release-check-password');tap('Continue')
wait_saved()
assert 'Release typing' in ET.tostring(tree(),encoding='unicode')
hide_keyboard()
tap('Tools ⋯',scroll=True)
tap('Isolate from other apps',scroll=True)
locate('Moved to private vault.');tap('OK')
tap('Back to files')
locate('TXT-release-check.txt')
screenshot('release-home-dark')
# Preserve encrypted bytes through the real Trash and Restore controls.
tap('File options: TXT-release-check.txt',scroll=True)
tap('Move to Trash',scroll=True);tap('Move to Trash')
tap('Trash')
locate('TXT-release-check.txt');tap('Restore')
locate('Restored TXT-release-check.txt.');tap('OK')
tap('‹ Back to documents')
locate('TXT-release-check.txt')
# Exercise the minified ZIP/JSON backup path through Android's destination picker.
# A separate plain workspace file verifies both placement flags in a full backup.
fixture=out/'workspace-check.txt';fixture.write_text('Workspace restore check',encoding='utf-8')
adb('push',str(fixture),'/sdcard/Documents/TXT-QA/workspace-check.txt')
tap('Backup');tap('Create full offline backup')
locate(edit=True)
picker_name=locate(edit=True).get('text')
assert picker_name.startswith('TXT_Backup_') and picker_name.endswith('.tpbak')
tap('Save')
locate('Backup master password')
for index in range(2):
    fields=[n for n in tree().iter('node') if n.get('class')=='android.widget.EditText']
    assert len(fields)==2
    tap(node=fields[index]);text('master-release-password')
tap('Continue')
locate('Offline backup created. Keep it and its master password safe.',timeout=60);tap('OK')
paths=adb('shell','find','/sdcard/Download','/sdcard/Documents','-name',picker_name).strip().splitlines()
assert len(paths)==1
assert adb('exec-out','cat',paths[0],binary=True).startswith(b'TPBK')
tap('Backup');tap('Restore offline backup')
try:
    tap(picker_name,timeout=8,scroll=True)
except AssertionError:
    tap('Show roots');tap('Downloads');tap(picker_name,scroll=True)
locate('Restore offline backup');tap('Enter master password')
tap(edit=True);text('master-release-password');tap('Continue')
locate('Restored 2 files. Skipped 0.',timeout=60);tap('OK')
with tempfile.TemporaryDirectory() as tmp:
    copy=pathlib.Path(tmp)/'workspace-restored.txt'
    adb('pull','/sdcard/Documents/TXT-QA/workspace-check (1).txt',str(copy))
    assert copy.read_text()=='Workspace restore check'
locate('TXT-release-check (1).txt',scroll=True)
tap('TXT-release-check (1).txt')
locate('Unlock TXT-release-check (1).txt')
tap(edit=True);text('release-check-password');tap('Continue')
wait_saved()
assert 'Release typing' in ET.tostring(tree(),encoding='unicode')
tap('Back to files')
adb('shell','cmd','uimode','night','no')
locate('TXT-release-check.txt')
screenshot('release-home-light')
adb('shell','cmd','uimode','night','yes')
locate('TXT-release-check.txt')
# Record actual interaction feedback and the moving home backdrop.
record=subprocess.Popen(['adb','shell','screenrecord','--time-limit','12','--size','540x960','/sdcard/TXT-release-motion.mp4'],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
try:
    time.sleep(1)
    tap('New File')
    tap(edit=True);text('A fresh page')
    time.sleep(2)
    tap('Undo');time.sleep(.5);tap('Redo')
    time.sleep(1)
    hide_keyboard()
    tap('Back to files')
    record.wait(timeout=25)
finally:
    if record.poll() is None: record.terminate()
adb('pull','/sdcard/TXT-release-motion.mp4',str(out/'TXT-release-motion.mp4'))
crash=adb('logcat','-d','-b','crash')
(out/'crash-log.txt').write_text(crash)
assert 'FATAL EXCEPTION' not in crash
(out/'RESULT.txt').write_text('PASS: signed minified APK install, persisted SAF workspace selection, tree URI routing, UTF-8 auto-save, Undo/Redo disk results, STXT encryption, password re-open, private move, encrypted Trash/Restore, encrypted archive export and Keep Both restore of workspace and vault through system pickers, system light/dark and real interaction recording.\n')
print((out/'RESULT.txt').read_text())

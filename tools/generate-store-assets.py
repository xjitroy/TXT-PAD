#!/usr/bin/env python3
"""Generate store artwork from TXT's original vector mark. Requires cairosvg for PNG."""
from pathlib import Path
from html import escape
import xml.etree.ElementTree as ET
import cairosvg

root=Path(__file__).resolve().parents[1]
output=root/'store/graphics';output.mkdir(parents=True,exist_ok=True)
ns='{http://schemas.android.com/apk/res/android}'
vector=ET.parse(root/'app/src/main/res/drawable/ic_txt_foreground.xml').getroot()
paths=''.join(f'<path fill="{escape(p.get(ns+"fillColor"))}" d="{escape(p.get(ns+"pathData"))}"/>' for p in vector)
icon=f'''<svg xmlns="http://www.w3.org/2000/svg" width="512" height="512" viewBox="0 0 512 512">
<rect width="512" height="512" fill="#102b25"/>
<g transform="translate(256 256) scale(6.1) translate(-54 -54)">{paths}</g></svg>'''
feature=f'''<svg xmlns="http://www.w3.org/2000/svg" width="1024" height="500" viewBox="0 0 1024 500">
<defs><linearGradient id="bg" x2="1" y2="1"><stop stop-color="#102b25"/><stop offset="1" stop-color="#071b19"/></linearGradient>
<linearGradient id="glass" x2="1" y2="1"><stop stop-color="#d0f4e5" stop-opacity=".13"/><stop offset="1" stop-color="#d0f4e5" stop-opacity=".02"/></linearGradient></defs>
<rect width="1024" height="500" fill="url(#bg)"/>
<path d="M-60 160 C310 -50 480 600 1090 130" fill="none" stroke="#8ad7c0" stroke-opacity=".12" stroke-width="80"/>
<path d="M-30 430 C360 600 570 -130 1090 340" fill="none" stroke="#91c8db" stroke-opacity=".08" stroke-width="45"/>
<rect x="52" y="65" width="340" height="370" rx="46" fill="url(#glass)" stroke="#d0f4e5" stroke-opacity=".28"/>
<g transform="translate(222 250) scale(5.1) translate(-54 -54)">{paths}</g>
<g font-family="DejaVu Sans, sans-serif" fill="#e4eae6">
<text x="448" y="181" font-size="94" font-weight="700" letter-spacing="-5">TXT</text>
<text x="448" y="259" font-size="40" font-weight="500">Write clearly.</text>
<text x="448" y="314" font-size="40" font-weight="500">Keep it private.</text>
<text x="450" y="375" font-size="21" fill="#b5d3c4">Offline text editor &amp; document vault</text></g></svg>'''
for name,svg in [('icon-512',icon),('feature-1024x500',feature)]:
 (output/f'{name}.svg').write_text(svg)
 cairosvg.svg2png(bytestring=svg.encode(),write_to=str(output/f'{name}.png'))
 print(output/f'{name}.png')

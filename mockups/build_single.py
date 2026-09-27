# Собирает все экраны в один самодостаточный файл english-file-trainer-mockups.html,
# чтобы его можно было переслать. Запуск: python build_single.py
import re
from pathlib import Path

HERE = Path(__file__).parent
GROUPS = [
    ('Main flow', [('Main', 'Chapter'), ('Section', 'Section'), ('Rules', 'Rules'),
                   ('Session', 'Practice (live)'), ('SessionDone', 'Session done')]),
    ('Other', [('Settings', 'Settings'), ('Chapters', 'All chapters'), ('ChapterDone', 'Chapter done')]),
    ('Later', [('Video', 'Video'), ('Listening', 'Listening'), ('Explain', 'Explain (v2)')]),
]

css = (HERE / 'style.css').read_text(encoding='utf-8')
theme_js = (HERE / 'theme.js').read_text(encoding='utf-8')
styles, templates, scripts = [], [], {}
for _, screens in GROUPS:
    for name, _ in screens:
        html = (HERE / f'{name}.html').read_text(encoding='utf-8')
        styles += re.findall(r'<style>(.*?)</style>', html, re.S)
        body = re.search(r'<body>(.*)</body>', html, re.S).group(1)
        m = re.search(r'<script>(.*?)</script>', body, re.S)
        if m:
            scripts[name] = m.group(1).replace("location.href = 'SessionDone.html'", "go('SessionDone')")
            body = body.replace(m.group(0), '')
        templates.append(f'<template id="t-{name}">{body.strip()}</template>')

nav = ''.join(
    f'<div class="grp">{title}</div>' +
    ''.join(f'<button data-go="{n}">{label}</button>' for n, label in screens)
    for title, screens in GROUPS)

out = f'''<!doctype html>
<html lang="en"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>English File Trainer · mockups</title>
<style>
{css}
{''.join(styles)}
.shell{{display:flex;gap:32px;justify-content:center;align-items:flex-start;padding:24px}}
.nav{{width:200px;display:flex;flex-direction:column;gap:4px;position:sticky;top:24px}}
.nav .grp{{font-size:12px;font-weight:600;text-transform:uppercase;letter-spacing:.06em;color:var(--muted);margin:14px 0 4px}}
.nav .grp:first-child{{margin-top:0}}
.nav button{{text-align:left;border:0;background:none;padding:8px 10px;border-radius:8px;font:500 15px 'Golos Text',sans-serif;color:var(--ink);cursor:pointer}}
.nav button.on{{background:var(--field);color:var(--accent)}}
.hello{{font-size:13px;color:var(--muted);line-height:1.5;margin-bottom:10px}}
.phone{{border-radius:24px;box-shadow:0 4px 24px rgba(0,0,0,.08)}}
@media (max-width:700px){{
  .shell{{flex-direction:column;padding:0;gap:0}}
  .nav{{position:static;width:100%;flex-direction:row;overflow-x:auto;padding:10px 12px;gap:6px;background:var(--page)}}
  .nav .grp,.hello{{display:none}}
  .nav button{{flex-shrink:0;background:var(--field)}}
  .phone{{width:100%;height:calc(100vh - 58px);border-radius:0;box-shadow:none}}
}}
</style></head>
<body>
<div class="shell">
  <div class="nav"><div class="hello">English File Pre-Intermediate trainer · chapter 1A. Everything is clickable; answers are typed.</div>{nav}</div>
  <div id="screen"></div>
</div>
{''.join(templates)}
<script>{theme_js}</script>
<script>
const SCRIPTS = {{{','.join(f'{n!r}: () => {{{s}; window.check = typeof check !== "undefined" ? check : undefined;}}' for n, s in scripts.items())}}};
function go(name) {{
  const box = document.getElementById('screen');
  box.innerHTML = '';
  box.appendChild(document.getElementById('t-' + name).content.cloneNode(true));
  document.querySelectorAll('.nav button').forEach(b => b.classList.toggle('on', b.dataset.go === name));
  if (SCRIPTS[name]) SCRIPTS[name]();
  window.scrollTo(0, 0);
}}
document.addEventListener('click', e => {{
  const b = e.target.closest('[data-go]');
  if (b) {{ go(b.dataset.go); return; }}
  const a = e.target.closest('a[href$=".html"]');
  if (a) {{ e.preventDefault(); go(a.getAttribute('href').replace('.html', '')); }}
}});
go('Main');
</script>
</body></html>
'''
(HERE / 'english-file-trainer-mockups.html').write_text(out, encoding='utf-8')
print('ok', len(out))

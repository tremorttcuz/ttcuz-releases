#!/usr/bin/env python3
"""The panel: badges and plugins, from a browser.

Small on purpose. There is one person using it, from one machine, and what it
does -- hand out a badge, put a plugin in the store -- is what `badgectl` does
from a shell. It exists because typing account ids into a shell over ssh is a
poor way to spend an evening.

The pages are written out here rather than served from files: there are four
of them, they are all form and table, and a template engine would be more
machinery than the thing it renders.

Signing in is a name and a password kept in `admin.txt` beside the database,
and a signed cookie afterwards. Not a session store, because there is one
session: the cookie carries when it was issued and a signature over that, so
the server can check it without remembering anything.
"""

import base64
import hashlib
import hmac
import html
import json
import os
import secrets
import time

HERE = os.path.dirname(os.path.abspath(__file__))

HOURS = 12 * 3600


def who():
    """The name, the password and the key the cookie is signed with."""
    try:
        with open(os.path.join(HERE, "admin.txt"), encoding="utf-8") as handle:
            said = json.load(handle)
        return said.get("user", ""), said.get("password", ""), said.get("secret", "")
    except Exception:
        return "", "", ""


def sign(when, secret):
    return hmac.new(secret.encode(), str(when).encode(), hashlib.sha256).hexdigest()[:32]


def ticket():
    _user, _password, secret = who()
    when = int(time.time())
    return "%d.%s" % (when, sign(when, secret))


def allowed(cookie):
    _user, _password, secret = who()
    if not secret or not cookie:
        return False
    for piece in cookie.split(";"):
        piece = piece.strip()
        if not piece.startswith("ttcuz="):
            continue
        value = piece[len("ttcuz="):]
        try:
            when, mark = value.split(".", 1)
            if int(time.time()) - int(when) > HOURS:
                return False
            return hmac.compare_digest(mark, sign(int(when), secret))
        except Exception:
            return False
    return False


# ------------------------------------------------------------------ the look

STYLE = """
:root { color-scheme: dark; --bg:#121212; --card:#1c1c1e; --line:#2c2c2e;
        --text:#f2f2f2; --dim:#8e8e93; --accent:#8dd1b0; }
* { box-sizing: border-box; }
body { margin:0; padding:32px 16px; background:var(--bg); color:var(--text);
       font:15px/1.5 -apple-system,Segoe UI,Roboto,sans-serif; }
main { max-width: 860px; margin: 0 auto; }
h1 { font-size: 26px; margin: 0 0 4px; }
h2 { font-size: 17px; margin: 28px 0 10px; color: var(--dim); font-weight: 600; }
.card { background:var(--card); border-radius:16px; padding:18px; margin-bottom:14px; }
table { width:100%; border-collapse: collapse; }
td, th { text-align:left; padding:9px 8px; border-bottom:1px solid var(--line); }
th { color: var(--dim); font-weight:600; font-size:13px; }
tr:last-child td { border-bottom: none; }
input, textarea, select { width:100%; padding:10px 12px; border-radius:10px;
       border:1px solid var(--line); background:#151516; color:var(--text);
       font: inherit; }
label { display:block; font-size:13px; color:var(--dim); margin:10px 0 4px; }
button, .button { display:inline-block; padding:10px 16px; border-radius:11px;
       border:none; background:var(--accent); color:#10221f; font:inherit;
       font-weight:600; cursor:pointer; text-decoration:none; }
.quiet { background:transparent; color:var(--text); border:1px solid var(--line); }
.row { display:flex; gap:10px; align-items:flex-end; flex-wrap: wrap; }
.row > * { flex:1; min-width: 140px; }
.row > .narrow { flex:0 0 auto; }
.dim { color: var(--dim); font-size: 13px; }
.dot { width:26px; height:26px; border-radius:7px; vertical-align:middle;
       margin-right:8px; object-fit: contain; }
.said { background:#1f2d27; border:1px solid #2f4a3f; padding:10px 14px;
        border-radius:11px; margin-bottom:14px; }
code, pre { font: 13px ui-monospace, monospace; background: rgba(127,127,127,.16);
            border-radius: 7px; padding: 2px 6px; }
pre { padding: 10px 12px; overflow-x: auto; }
"""


def page(title, body, said=""):
    return ("<!doctype html><meta charset=utf-8>"
            "<meta name=viewport content='width=device-width,initial-scale=1'>"
            "<title>%s</title><style>%s</style><main>%s%s</main>"
            % (html.escape(title), STYLE,
               ("<div class=said>%s</div>" % html.escape(said)) if said else "",
               body)).encode("utf-8")


def sign_in_page(trouble=""):
    user, password, secret = who()
    if not (user and password and secret):
        # no admin.txt yet: say exactly how to make one instead of asking for a key nobody has
        return page("ttcuz", """
        <h1>ttcuz</h1>
        <p class=dim>Панель ещё не настроена</p>
        <div class=card>
          <p>Вход — это <b>имя и пароль</b>, которые вы задаёте сами. Это не ключ телефона и не токен Cloudflare.</p>
          <p>На сервере, в папке <code>server</code>, выполните:</p>
          <pre>python3 panel.py setup admin</pre>
          <p>Команда создаст <code>admin.txt</code> и один раз покажет пароль. Затем обновите эту страницу.</p>
        </div>""", trouble)
    return page("ttcuz", """
        <h1>ttcuz</h1>
        <p class=dim>Панель</p>
        <form class=card method=post action="/admin/in">
          <label>Имя</label><input name=user autocomplete=username>
          <label>Пароль</label>
          <input name=password type=password autocomplete=current-password>
          <p><button>Войти</button></p>
          <p class=dim>Имя и пароль лежат в <code>server/admin.txt</code> на сервере. Забыли пароль —
          выполните там <code>python3 panel.py setup admin</code>: появится новый.</p>
        </form>""", trouble)


def main_page(badges, plugins, said=""):
    rows = []
    for badge in badges:
        rows.append(
            "<tr><td><img class=dot src='/icon/%s'></td><td><b>%s</b></td>"
            "<td class=dim>#%s</td><td class=dim>%s</td><td class=dim>%d</td>"
            "<td><a class='button quiet' href='/admin/badge?id=%s'>Открыть</a></td></tr>"
            % (html.escape(badge["image"] or "note.png"), html.escape(badge["id"]),
               html.escape(badge["colour"] or "-"), html.escape(badge["text"] or ""),
               badge["worn"], html.escape(badge["id"])))

    packs = []
    for one in plugins:
        packs.append(
            "<tr><td><img class=dot src='%s'></td><td><b>%s</b> <span class=dim>%s</span></td>"
            "<td class=dim>%s</td><td class=dim>%s</td>"
            "<td><form method=post action='/admin/plugin/drop' "
            "style='display:inline'><input type=hidden name=id value='%s'>"
            "<button class=quiet>Убрать</button></form></td></tr>"
            % (html.escape(one["icon"] or "/icon/note.png"), html.escape(one["name"]),
               html.escape(one["version"]), html.escape(one["author"]),
               html.escape(one["tiktok"] or "любая"), html.escape(one["id"])))

    return page("ttcuz", """
        <h1>ttcuz</h1>
        <p class=dim>Значки и плагины</p>

        <h2>Значки</h2>
        <div class=card><table>
          <tr><th></th><th>id</th><th>цвет</th><th>текст</th><th>у кого</th><th></th></tr>
          %s
        </table></div>

        <h2>Новый значок</h2>
        <form class=card method=post action="/admin/badge/new"
              enctype="multipart/form-data">
          <div class=row>
            <div><label>id</label><input name=id placeholder="supporter"></div>
            <div><label>цвет</label><input name=colour placeholder="FF6FA5"></div>
          </div>
          <label>картинка — png, прозрачный фон; пусто оставит ноту</label>
          <input type=file name=picture accept="image/png,image/webp">
          <label>текст</label><input name=text placeholder="поддержал разработку ttcuz">
          <div class=row>
            <div><label>текст (ru)</label><input name=text_ru></div>
            <div><label>текст (uk)</label><input name=text_uk></div>
          </div>
          <p><button>Создать</button></p>
        </form>

        <h2>Плагины</h2>
        <div class=card><table>
          <tr><th></th><th>плагин</th><th>автор</th><th>TikTok</th><th></th></tr>
          %s
        </table></div>

        <h2>Загрузить плагин</h2>
        <form class=card method=post action="/admin/plugin/add"
              enctype="multipart/form-data">
          <p class=dim>Файл .ctt — манифест и иконка читаются из него. Ограничения API и обфускации отключены.</p>
          <input type=file name=file accept=".ctt,.zip">
          <p><button>Загрузить</button></p>
        </form>

        <p><a class='button quiet' href="/admin/out">Выйти</a></p>
        """ % ("".join(rows) or "<tr><td class=dim>пока пусто</td></tr>",
               "".join(packs) or "<tr><td class=dim>пока пусто</td></tr>"), said)


def badge_page(badge, wearers, said=""):
    rows = "".join(
        "<tr><td>%s</td><td class=dim>%s</td><td><form method=post "
        "action='/admin/badge/take' style='display:inline'>"
        "<input type=hidden name=id value='%s'><input type=hidden name=uid value='%s'>"
        "<button class=quiet>Забрать</button></form></td></tr>"
        % (html.escape(uid), "показан" if shown else "скрыт",
           html.escape(badge["id"]), html.escape(uid))
        for uid, shown in wearers)

    return page("ttcuz", """
        <h1>%s</h1>
        <p class=dim><img class=dot src='/icon/%s'> #%s</p>

        <h2>Выдать</h2>
        <form class=card method=post action="/admin/badge/give">
          <input type=hidden name=id value='%s'>
          <label>айди аккаунтов, через пробел или с новой строки</label>
          <textarea name=uids rows=3 placeholder="7551880794956989495"></textarea>
          <p><button>Выдать</button></p>
        </form>

        <h2>У кого он есть</h2>
        <div class=card><table>
          <tr><th>аккаунт</th><th></th><th></th></tr>%s
        </table></div>

        <p><a class='button quiet' href="/admin">Назад</a></p>
        """ % (html.escape(badge["text"] or badge["id"]),
               html.escape(badge["image"] or "note.png"), html.escape(badge["id"]),
               html.escape(badge["id"]),
               rows or "<tr><td class=dim>пока ни у кого</td></tr>"), said)


def setup(argv):
    """python3 panel.py setup [name]: writes admin.txt with a fresh password and prints it once."""
    user = argv[0] if argv else "admin"
    password = secrets.token_urlsafe(12)
    path = os.path.join(HERE, "admin.txt")
    with open(os.open(path, os.O_WRONLY | os.O_CREAT | os.O_TRUNC, 0o600), "w", encoding="utf-8") as handle:
        json.dump({"user": user, "password": password, "secret": secrets.token_hex(32)}, handle)
    print("admin.txt written. Sign in at /admin with:")
    print("  name:     " + user)
    print("  password: " + password)
    print("The password is shown only now. Run this again to replace it.")


if __name__ == "__main__":
    import sys
    if len(sys.argv) >= 2 and sys.argv[1] == "setup":
        setup(sys.argv[2:])
    else:
        print("usage: python3 panel.py setup [name]")

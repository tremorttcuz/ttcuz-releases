try:
    from . import script_store
except ImportError:
    import script_store
#!/usr/bin/env python3
"""The badge server.

Badges used to be a file in the repository that every phone re-read every five
minutes. That worked and cost nothing, and it had two limits worth leaving it
for: granting one meant a commit, and nobody could decide anything about their
own -- which of theirs to show, and in what order.

So this holds them instead. It is deliberately small: the standard library, one
sqlite file, and nginx in front for rate limiting. There is no framework here
because there is nothing a framework would do.

    GET  /badges            what every phone reads
    POST /claim             a phone says which account it is, and is given a key
    POST /profile           that account decides what to show and in what order
    POST /old               the badge anyone running the mod before the 24th gets
    GET  /icon/<name>.png   the pictures badges are drawn with

On authentication, plainly: there is none. TikTok will not tell a third party
that somebody is who they say they are, so the first phone to claim an account
id is given the key for it and keeps it. That is enough to stop a passer-by
rearranging somebody else's badges and is not enough to stop somebody who
really wants to. It is a picture beside a name.

What is enforced, and enforced here rather than in the app where it could be
edited out: which badges a claim may grant, how often anything may be written,
and when the free badge stops being free.
"""

import cgi
import hashlib
import io
import json
import os
import secrets
import sqlite3
import time
import urllib.request
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import parse_qs, urlparse

import panel

HERE = os.path.dirname(os.path.abspath(__file__))
DB = os.path.join(HERE, "badges.db")
ICONS = os.path.join(HERE, "icons")
PLUGINS = os.path.join(HERE, "plugins")

#: who may use the panel. Read from a file so it is not in the source, and
#: written by hand on the server rather than by anything here.
WHO = os.path.join(HERE, "admin.txt")

#: the free badge, and the moment it stops being given out. After this the
#: badge stays on everyone who took it and is offered to nobody -- which is
#: what makes it worth having and what makes its wording true.
FREE = "old"
FREE_UNTIL = 1789851600          # 2026-09-20 00:00 UTC+3

#: how often one account, or one address, may change anything
EVERY = 60

MOST_BADGES = 16


# --------------------------------------------------------------- the store

def connect():
    db = sqlite3.connect(DB, timeout=10)
    db.execute("PRAGMA journal_mode=WAL")
    return db


def prepare():
    db = connect()
    db.executescript("""
        CREATE TABLE IF NOT EXISTS badge (
            id      TEXT PRIMARY KEY,
            colour  TEXT NOT NULL DEFAULT '',
            image   TEXT NOT NULL DEFAULT '',
            title   TEXT NOT NULL DEFAULT '',
            text    TEXT NOT NULL DEFAULT '',
            text_ru TEXT NOT NULL DEFAULT '',
            text_uk TEXT NOT NULL DEFAULT '',
            button  TEXT NOT NULL DEFAULT '',
            claimable INTEGER NOT NULL DEFAULT 0
        );
        CREATE TABLE IF NOT EXISTS held (
            uid      TEXT NOT NULL,
            badge    TEXT NOT NULL,
            shown    INTEGER NOT NULL DEFAULT 1,
            place    INTEGER NOT NULL DEFAULT 0,
            given_at INTEGER NOT NULL DEFAULT 0,
            PRIMARY KEY (uid, badge)
        );
        CREATE TABLE IF NOT EXISTS owner (
            uid       TEXT PRIMARY KEY,
            token     TEXT NOT NULL,
            claimed   INTEGER NOT NULL,
            written   INTEGER NOT NULL DEFAULT 0
        );
        CREATE TABLE IF NOT EXISTS face (
            id   TEXT PRIMARY KEY,
            url  TEXT NOT NULL,
            seen INTEGER NOT NULL DEFAULT 0,
            told TEXT NOT NULL DEFAULT ''
        );
        CREATE TABLE IF NOT EXISTS visitor (
            ip      TEXT PRIMARY KEY,
            written INTEGER NOT NULL DEFAULT 0
        );
        CREATE TABLE IF NOT EXISTS plugin (
            id      TEXT PRIMARY KEY,
            name    TEXT NOT NULL DEFAULT '',
            version TEXT NOT NULL DEFAULT '',
            author  TEXT NOT NULL DEFAULT '',
            about   TEXT NOT NULL DEFAULT '',
            tiktok  TEXT NOT NULL DEFAULT '',
            file    TEXT NOT NULL DEFAULT '',
            icon    TEXT NOT NULL DEFAULT '',
            size    INTEGER NOT NULL DEFAULT 0,
            added   INTEGER NOT NULL DEFAULT 0
        );
    """)
    # the name telegram gives a picture came after the table did, and a
    # server that is already running is not going to grow the column by
    # being asked for the table again
    if "told" not in [row[1] for row in db.execute("PRAGMA table_info(face)")]:
        db.execute("ALTER TABLE face ADD COLUMN told TEXT NOT NULL DEFAULT ''")
    db.commit()
    db.close()


# ------------------------------------------------------------ what is served

#: what a badge is drawn with when it says nothing: the mod's own note
PLAIN = "note.png"


def stamped(name):
    """A picture's name with a mark of what is in it.

    Phones keep a picture by the name it came under, and these names do not
    change when the drawing does -- so a new crown would never reach anybody
    who already had the old one. The mark makes a redrawn picture a different
    picture as far as any cache is concerned.
    """
    if not name:
        name = PLAIN
    path = os.path.join(ICONS, os.path.basename(name.split("?")[0]))
    try:
        with open(path, "rb") as handle:
            mark = hashlib.sha256(handle.read()).hexdigest()[:8]
        return "%s?v=%s" % (os.path.basename(path), mark)
    except OSError:
        return name


def public():
    """Every badge and who wears it, in the shape the mod already reads."""
    db = connect()
    badges = {}
    for row in db.execute("SELECT * FROM badge"):
        badges[row[0]] = {
            "id": row[0], "colour": row[1], "image": stamped(row[2]), "title": row[3],
            "text": row[4], "text_ru": row[5], "text_uk": row[6], "button": row[7],
            "users": [],
        }
    # the order a person chose is the order their badges are listed in
    for uid, badge in db.execute(
            "SELECT uid, badge FROM held WHERE shown = 1 ORDER BY place, badge"):
        if badge in badges:
            badges[badge]["users"].append(uid)
    db.close()

    out = [badges[key] for key in sorted(badges)]
    return {"badges": out, "free_until": FREE_UNTIL, "now": int(time.time())}


def mine(uid):
    """What one account holds, shown or not, in the order it chose."""
    db = connect()
    rows = db.execute(
        "SELECT badge, shown, place FROM held WHERE uid = ? ORDER BY place, badge",
        (uid,)).fetchall()
    db.close()
    return [{"id": badge, "shown": bool(shown), "place": place}
            for badge, shown, place in rows]


# ------------------------------------------------------------- what is taken

def claim(uid, ip):
    """Give this account's key out, once, to whoever asks first."""
    if not sane(uid):
        return 400, {"error": "that is not an account id"}
    db = connect()
    row = db.execute("SELECT token FROM owner WHERE uid = ?", (uid,)).fetchone()
    if row:
        db.close()
        # An unauthenticated repeat claim must never disclose the first
        # claimant's key. Recovery requires an independently verified owner.
        return 409, {"error": "account already claimed"}

    token = secrets.token_hex(16)
    db.execute("INSERT INTO owner (uid, token, claimed) VALUES (?, ?, ?)",
               (uid, token, int(time.time())))
    db.commit()
    db.close()
    return 200, {"uid": uid, "token": token, "badges": mine(uid)}


def too_soon(db, uid, ip):
    """Whether this account or this address has written recently."""
    now = int(time.time())
    row = db.execute("SELECT written FROM owner WHERE uid = ?", (uid,)).fetchone()
    if row and now - row[0] < EVERY:
        return EVERY - (now - row[0])
    row = db.execute("SELECT written FROM visitor WHERE ip = ?", (ip,)).fetchone()
    if row and now - row[0] < EVERY:
        return EVERY - (now - row[0])
    return 0


def wrote(db, uid, ip):
    now = int(time.time())
    db.execute("UPDATE owner SET written = ? WHERE uid = ?", (now, uid))
    db.execute("INSERT INTO visitor (ip, written) VALUES (?, ?) "
               "ON CONFLICT(ip) DO UPDATE SET written = ?", (ip, now, now))


def profile(body, ip):
    """Which of an account's badges to show, and in what order."""
    uid, token = body.get("uid", ""), body.get("token", "")
    if not sane(uid) or not token:
        return 400, {"error": "who?"}

    db = connect()
    row = db.execute("SELECT token FROM owner WHERE uid = ?", (uid,)).fetchone()
    if not row or not secrets.compare_digest(row[0], token):
        db.close()
        return 403, {"error": "not your account"}

    wait = too_soon(db, uid, ip)
    if wait:
        db.close()
        return 429, {"error": "too often", "wait": wait}

    order = body.get("order") or []
    hidden = set(body.get("hidden") or [])
    if len(order) > MOST_BADGES:
        db.close()
        return 400, {"error": "that is not a number of badges anybody has"}

    held = {badge for (badge,) in db.execute(
        "SELECT badge FROM held WHERE uid = ?", (uid,))}
    # only what this account actually holds; a list naming anything else is
    # somebody trying to award themselves something
    place = 0
    for badge in order:
        if badge not in held:
            continue
        db.execute("UPDATE held SET place = ?, shown = ? WHERE uid = ? AND badge = ?",
                   (place, 0 if badge in hidden else 1, uid, badge))
        place += 1
    for badge in held:
        if badge not in order:
            db.execute("UPDATE held SET shown = ? WHERE uid = ? AND badge = ?",
                       (0 if badge in hidden else 1, uid, badge))
    wrote(db, uid, ip)
    db.commit()
    out = mine(uid)
    db.close()
    return 200, {"badges": out}


def free(body, ip):
    """The badge anybody running the mod before the day gets, and nothing else.

    The badge is named here and not by the caller. A request cannot ask for
    `owner` because nothing it sends is used to choose which badge it gets.
    """
    uid, token = body.get("uid", ""), body.get("token", "")
    if not sane(uid) or not token:
        return 400, {"error": "who?"}

    now = int(time.time())
    if now >= FREE_UNTIL:
        return 410, {"error": "that one is over"}

    db = connect()
    row = db.execute("SELECT token FROM owner WHERE uid = ?", (uid,)).fetchone()
    if not row or not secrets.compare_digest(row[0], token):
        db.close()
        return 403, {"error": "not your account"}

    wait = too_soon(db, uid, ip)
    if wait:
        db.close()
        return 429, {"error": "too often", "wait": wait}

    db.execute("INSERT OR IGNORE INTO held (uid, badge, given_at) VALUES (?, ?, ?)",
               (uid, FREE, now))
    wrote(db, uid, ip)
    db.commit()
    out = mine(uid)
    db.close()
    return 200, {"badges": out}


def sane(uid):
    return isinstance(uid, str) and uid.isdigit() and 6 <= len(uid) <= 24


# --------------------------------------------------------------- the plugins

try:
    from .plugin_audit import inspect_package, analyze_package
except ImportError:
    from plugin_audit import inspect_package, analyze_package

def plugins():
    """Everything the store offers, newest first."""
    db = connect()
    rows = db.execute(
        "SELECT id, name, version, author, about, tiktok, file, icon, size"
        " FROM plugin ORDER BY added DESC").fetchall()
    db.close()
    offered=[]
    for r in rows:
        try:
            with open(os.path.join(PLUGINS,r[6]),"rb") as handle:raw=handle.read(18*1024*1024+1)
            rep=analyze_package(raw);manifest,digest=rep["manifest"],rep["sha256"]
            if manifest["id"]!=r[0]:continue
        except Exception as why:
            print('[plugins] скрыт %s: %s'%(r[0],why),flush=True);continue
        offered.append({"id":r[0],"name":r[1],"version":r[2],"author":r[3],"about":r[4],"tiktok":r[5],"url":"/plugin/"+r[6],"icon":"/picture/"+r[7] if r[7] else "","size":r[8],"sha256":digest,"can":rep["capabilities"],"domains":rep["domains"],"verdict":rep["verdict"],"notes":rep["notes"]})
    return {"plugins":offered}



def take_plugin(raw, name):
    """Read a packed plugin, keep it, and remember what its manifest says.

    The store shows what the plugin says about itself, so the manifest is read
    here rather than typed into a form: a name and an author that disagree with
    what the phone will load are worse than none.
    """
    said,icon,_=inspect_package(raw)
    which=said["id"]

    os.makedirs(PLUGINS, exist_ok=True)
    file = which + ".ctt"
    with open(os.path.join(PLUGINS, file), "wb") as handle:
        handle.write(raw)

    picture = ""
    if icon:
        picture = which + ".png"
        with open(os.path.join(ICONS, picture), "wb") as handle:
            handle.write(icon)

    db = connect()
    db.execute(
        "INSERT INTO plugin (id, name, version, author, about, tiktok, file, icon,"
        " size, added) VALUES (?,?,?,?,?,?,?,?,?,?)"
        " ON CONFLICT(id) DO UPDATE SET name=excluded.name, version=excluded.version,"
        " author=excluded.author, about=excluded.about, tiktok=excluded.tiktok,"
        " file=excluded.file, icon=excluded.icon, size=excluded.size,"
        " added=excluded.added",
        (which, str(said.get("name") or which), str(said.get("version") or "?"),
         str(said.get("author") or "?"), str(said.get("description") or ""),
         str(said.get("tiktok") or ""), file, picture, len(raw), int(time.time())))
    db.commit()
    db.close()
    return which


def drop_plugin(which):
    db = connect()
    row = db.execute("SELECT file, icon FROM plugin WHERE id = ?", (which,)).fetchone()
    if row:
        for folder, name in ((PLUGINS, row[0]), (ICONS, row[1])):
            if name:
                try:
                    os.remove(os.path.join(folder, name))
                except OSError:
                    pass
    db.execute("DELETE FROM plugin WHERE id = ?", (which,))
    db.commit()
    db.close()


# ------------------------------------------------------------- the plumbing

# --------------------------------------------------------------- avatars

#: avatars fetched lately -- mark -> (when, kind, bytes)
FACES = {}
FACE_KEEPS = 15 * 60

#: where an avatar may come from, and nowhere else
FACE_HOSTS = (".tiktokcdn.com", ".tiktokcdn-eu.com", ".tiktokcdn-us.com",
              ".tiktokcdn-in.com", ".byteimg.com", ".ibyteimg.com")

BROWSER = ("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
           " (KHTML, like Gecko) Chrome/124.0 Safari/537.36")


def face_named(url):
    return hashlib.sha256(url.encode("utf-8")).hexdigest()[:16]


def face_told(url):
    """What telegram calls this avatar, if it has been told about it.

    Telegram will not fetch a picture from this server -- it refuses plain
    http on a bare address, and there is no domain to put a certificate on
    -- so the bot hands it the bytes once and keeps the name it gets back.
    That name can be used in an answer for as long as it lasts, with no
    fetching by anybody.
    """
    db = connect()
    row = db.execute("SELECT told FROM face WHERE id = ?",
                     (face_named(url),)).fetchone()
    db.close()
    return row[0] if row and row[0] else ""


def face_tell(url, told):
    now = int(time.time())
    db = connect()
    db.execute("INSERT INTO face (id, url, seen, told) VALUES (?,?,?,?)"
               " ON CONFLICT(id) DO UPDATE SET seen = excluded.seen,"
               " told = excluded.told", (face_named(url), url, now, told))
    db.execute("DELETE FROM face WHERE seen < ?", (now - 7 * 86400,))
    db.commit()
    db.close()


def face_link(url):
    """Where somebody else can fetch a tiktok avatar.

    Telegram will not fetch one from tiktok -- the CDN does not serve
    whoever telegram is -- so it is fetched here and passed on.

    The address is kept here and the link carries only a short name for it.
    Putting the address in the link does not work for a fetcher that tidies
    up what it is about to fetch: an address with an escaped address inside
    it arrives cut at the first question mark.

    Telegram is not the reader of this -- it will not fetch from plain http
    on a bare address at all, which is what `face_told` is for. This is for
    anything that will, and for when there is a domain here.

    Only the hosts avatars come from, and only an address that was handed
    to this service on purpose: there is no way to make it fetch whatever
    anybody names.
    """
    if not (urlparse(url).hostname or "").lower().endswith(FACE_HOSTS):
        return ""
    which = face_named(url)
    now = int(time.time())
    db = connect()
    db.execute("INSERT INTO face (id, url, seen) VALUES (?,?,?)"
               " ON CONFLICT(id) DO UPDATE SET seen = excluded.seen",
               (which, url, now))
    db.execute("DELETE FROM face WHERE seen < ?", (now - 7 * 86400,))
    db.commit()
    db.close()
    return "/face/%s.jpg" % which


def face(which):
    now = time.time()
    for stale in [key for key, (when, _k, _b) in FACES.items()
                  if now - when > FACE_KEEPS]:
        FACES.pop(stale, None)
    kept = FACES.get(which)
    if kept:
        return kept[1], kept[2]

    db = connect()
    row = db.execute("SELECT url FROM face WHERE id = ?", (which,)).fetchone()
    db.close()
    if not row:
        return None, None
    url = row[0]
    if not (urlparse(url).hostname or "").lower().endswith(FACE_HOSTS):
        return None, None

    try:
        request = urllib.request.Request(url, headers={
            "User-Agent": BROWSER, "Referer": "https://www.tiktok.com/",
            "Accept": "image/avif,image/webp,image/*,*/*;q=0.8"})
        with urllib.request.urlopen(request, timeout=20) as answer:
            kind = (answer.headers.get("Content-Type") or "").split(";")[0]
            if not kind.startswith("image/"):
                return None, None
            blob = answer.read(4 * 1024 * 1024)
    except Exception:
        return None, None
    if not blob:
        return None, None
    FACES[which] = (now, kind, blob)
    return kind, blob


class Handler(BaseHTTPRequestHandler):
    server_version = "margyt"
    sys_version = ""

    #: a HEAD is answered with the headers and nothing else
    headless = False

    def log_message(self, *args):
        pass   # nginx keeps the log; this would only duplicate it

    def push(self, data):
        if not self.headless:
            self.wfile.write(data)

    def do_HEAD(self):
        """The same answer as a GET, minus the body.

        Telegram asks HEAD before it fetches a picture somebody named, and
        an unanswered HEAD is a 501 with an html body -- which it reads as
        the address not being a picture at all, and refuses it.
        """
        self.headless = True
        try:
            self.do_GET()
        finally:
            self.headless = False

    def who(self):
        # nginx is in front, so the address that matters is the one it passes
        return self.headers.get("X-Real-IP") or self.client_address[0]

    def answer(self, code, body, kind="application/json; charset=utf-8", raw=None):
        data = raw if raw is not None else json.dumps(
            body, ensure_ascii=False).encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", kind)
        self.send_header("Content-Length", str(len(data)))
        if code == 200 and raw is None:
            tag = '"%s"' % hashlib.sha256(data).hexdigest()[:32]
            self.send_header("ETag", tag)
            self.send_header("Cache-Control", "public, max-age=30")
            if self.headers.get("If-None-Match") == tag:
                self.send_response(304)
                self.send_header("ETag", tag)
                self.end_headers()
                return
        self.end_headers()
        self.push(data)

    # ------------------------------------------------------------- the panel

    def signed_in(self):
        return panel.allowed(self.headers.get("Cookie", ""))

    def html(self, body, code=200, cookie=None):
        self.send_response(code)
        self.send_header("Content-Type", "text/html; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "no-store")
        if cookie is not None:
            self.send_header("Set-Cookie", cookie)
        self.end_headers()
        self.push(body)

    def go(self, where, cookie=None):
        self.send_response(303)
        self.send_header("Location", where)
        if cookie is not None:
            self.send_header("Set-Cookie", cookie)
        self.send_header("Content-Length", "0")
        self.end_headers()

    def form(self):
        length = int(self.headers.get("Content-Length") or 0)
        if length > 32 * 1024 * 1024:
            return {}
        raw = self.rfile.read(length)
        return {k: v[0] for k, v in parse_qs(raw.decode("utf-8", "replace")).items()}

    def panel_get(self, path, query):
        if not self.signed_in():
            self.html(panel.sign_in_page())
            return

        if path == "/admin":
            db = connect()
            badges = [
                {"id": r[0], "colour": r[1], "image": r[2].split("?")[0],
                 "text": r[4] or r[3], "worn": r[5]}
                for r in db.execute(
                    "SELECT b.id, b.colour, b.image, b.title, b.text, COUNT(h.uid)"
                    " FROM badge b LEFT JOIN held h ON h.badge = b.id"
                    " GROUP BY b.id ORDER BY b.id")]
            db.close()
            self.html(panel.main_page(badges, plugins()["plugins"],
                                      query.get("said", [""])[0]))
            return

        if path == "/admin/badge":
            which = query.get("id", [""])[0]
            db = connect()
            row = db.execute(
                "SELECT id, colour, image, text FROM badge WHERE id = ?",
                (which,)).fetchone()
            if not row:
                db.close()
                self.go("/admin?said=нет такого значка")
                return
            wearers = db.execute(
                "SELECT uid, shown FROM held WHERE badge = ? ORDER BY given_at DESC",
                (which,)).fetchall()
            db.close()
            self.html(panel.badge_page(
                {"id": row[0], "colour": row[1], "image": row[2].split("?")[0],
                 "text": row[3]}, wearers))
            return

        if path == "/admin/out":
            self.go("/admin", "margyt=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax")
            return

        self.answer(404, {"error": "no such page"})

    def panel_post(self, path):
        if path == "/admin/in":
            said = self.form()
            user, password, _secret = panel.who()
            if (user and said.get("user") == user
                    and secrets.compare_digest(said.get("password", ""), password)):
                self.go("/admin", "margyt=%s; Path=/; Max-Age=43200; HttpOnly;"
                                  " SameSite=Lax" % panel.ticket())
            else:
                self.html(panel.sign_in_page("не подошло"))
            return

        if not self.signed_in():
            self.html(panel.sign_in_page())
            return

        if path == "/admin/badge/new":
            # a form with a file in it is read whole rather than as a query
            # string, so the picture arrives the same way a plugin does
            try:
                form = cgi.FieldStorage(
                    fp=self.rfile, headers=self.headers,
                    environ={"REQUEST_METHOD": "POST",
                             "CONTENT_TYPE": self.headers.get("Content-Type", "")})
            except Exception as trouble:
                self.go("/admin?said=не вышло: %s" % trouble)
                return

            def field(name):
                try:
                    return (form.getfirst(name) or "").strip()
                except Exception:
                    return ""

            which = field("id")
            if not which or "/" in which or len(which) > 64:
                self.go("/admin?said=нужен id")
                return

            picture = ""
            try:
                item = form["picture"]
                raw = item.file.read() if item.filename else b""
            except Exception:
                raw = b""
            if raw:
                picture = which + ".png"
                with open(os.path.join(ICONS, picture), "wb") as handle:
                    handle.write(raw)

            said = {"id": which, "colour": field("colour"), "image": picture,
                    "text": field("text"), "text_ru": field("text_ru"),
                    "text_uk": field("text_uk")}
            db = connect()
            db.execute(
                "INSERT INTO badge (id, colour, image, title, text, text_ru, text_uk,"
                " button) VALUES (?,?,?,?,?,?,?,?)"
                " ON CONFLICT(id) DO UPDATE SET colour=excluded.colour,"
                " image=excluded.image, text=excluded.text, text_ru=excluded.text_ru,"
                " text_uk=excluded.text_uk",
                (which, (said.get("colour") or "").strip().lstrip("#"),
                 (said.get("image") or "").strip(), "MargyT",
                 said.get("text", ""), said.get("text_ru", ""),
                 said.get("text_uk", ""), "Got it"))
            db.commit()
            db.close()
            self.go("/admin?said=значок %s готов" % which)
            return

        if path == "/admin/badge/give":
            said = self.form()
            which = said.get("id", "")
            given = 0
            db = connect()
            if db.execute("SELECT 1 FROM badge WHERE id = ?", (which,)).fetchone():
                now = int(time.time())
                for uid in (said.get("uids") or "").replace(",", " ").split():
                    if sane(uid):
                        db.execute("INSERT OR IGNORE INTO held (uid, badge, given_at)"
                                   " VALUES (?, ?, ?)", (uid, which, now))
                        given += 1
                db.commit()
            db.close()
            self.go("/admin/badge?id=%s" % which)
            return

        if path == "/admin/badge/take":
            said = self.form()
            db = connect()
            db.execute("DELETE FROM held WHERE uid = ? AND badge = ?",
                       (said.get("uid", ""), said.get("id", "")))
            db.commit()
            db.close()
            self.go("/admin/badge?id=%s" % said.get("id", ""))
            return

        if path == "/admin/plugin/drop":
            said = self.form()
            drop_plugin(said.get("id", ""))
            self.go("/admin?said=плагин убран")
            return

        if path == "/admin/plugin/add":
            try:
                form = cgi.FieldStorage(
                    fp=self.rfile, headers=self.headers,
                    environ={"REQUEST_METHOD": "POST",
                             "CONTENT_TYPE": self.headers.get("Content-Type", "")})
                item = form["file"]
                which = take_plugin(item.file.read(18*1024*1024+1), item.filename or "plugin.ctt")
                self.go("/admin?said=плагин %s в магазине" % which)
            except Exception as trouble:
                self.go("/admin?said=не вышло: %s" % trouble)
            return

        self.answer(404, {"error": "no such page"})

    def do_GET(self):
        parts = urlparse(self.path)
        path = parts.path
        if path == "/scripts":self.answer(200,script_store.catalog(PLUGINS));return
        if path == "/admin/scripts":
            if not self.signed_in():self.html(panel.sign_in_page());return
            import html
            body='<meta charset="utf-8"><h1>Проверка плагинов</h1><a href="/admin">Назад</a>'
            for digest,source in script_store.pending(PLUGINS):
                body+='<pre>'+html.escape(source)+'</pre>'
                for action,label in [('approve','Одобрить'),('reject','Отклонить')]:
                    token=hashlib.sha256((self.headers.get("Cookie","")+digest+action).encode()).hexdigest()
                    body+=f'<form method="post" action="/admin/scripts/review"><input type="hidden" name="digest" value="{digest}"><input type="hidden" name="token" value="{token}"><input type="hidden" name="action" value="{action}"><button>{label}</button></form>'
            self.html(body.encode());return
        if path.startswith("/admin"):
            self.panel_get(path, parse_qs(parts.query))
            return
        if path.startswith("/plugin/"):
            name = os.path.basename(path[len("/plugin/"):])
            file = os.path.join(PLUGINS, name)
            if not name.endswith((".mtp",".ctt")) or not os.path.isfile(file):
                self.answer(404, {"error": "no such plugin"})
                return
            try:
                with open(file,"rb") as handle:raw=handle.read(18*1024*1024+1)
                inspect_package(raw)
            except Exception:self.answer(404,{"error":"plugin rejected by publication policy"});return
            self.answer(200,None,"application/octet-stream",raw)
            return
        if path == "/plugins":
            self.answer(200, plugins())
            return
        if path.startswith("/picture/"):
            name = os.path.basename(path[len("/picture/"):])
            file = os.path.join(ICONS, name)
            if not os.path.isfile(file):
                self.answer(404, {"error": "no such picture"})
                return
            with open(file, "rb") as handle:
                self.answer(200, None, "image/png", handle.read())
            return
        if path == "/badges":
            data = json.dumps(public(), ensure_ascii=False).encode("utf-8")
            tag = '"%s"' % hashlib.sha256(data).hexdigest()[:32]
            if self.headers.get("If-None-Match") == tag:
                self.send_response(304)
                self.send_header("ETag", tag)
                self.end_headers()
                return
            self.send_response(200)
            self.send_header("Content-Type", "application/json; charset=utf-8")
            self.send_header("Content-Length", str(len(data)))
            self.send_header("ETag", tag)
            self.send_header("Cache-Control", "public, max-age=30")
            self.end_headers()
            self.push(data)
            return

        if path.startswith("/icon/"):
            name = os.path.basename(path[len("/icon/"):])
            if not name.endswith(".png") or "/" in name:
                self.answer(404, {"error": "no"})
                return
            file = os.path.join(ICONS, name)
            if not os.path.isfile(file):
                self.answer(404, {"error": "no such picture"})
                return
            with open(file, "rb") as handle:
                self.answer(200, None, "image/png", handle.read())
            return

        if path.startswith("/face/"):
            which = os.path.basename(path[len("/face/"):])
            if which.endswith(".jpg"):
                which = which[:-len(".jpg")]
            kind, blob = (None, None)
            if len(which) == 16 and all(c in "0123456789abcdef" for c in which):
                kind, blob = face(which)
            if not blob:
                self.answer(404, {"error": "no such face"})
                return
            self.send_response(200)
            self.send_header("Content-Type", kind)
            self.send_header("Content-Length", str(len(blob)))
            self.send_header("Cache-Control", "public, max-age=900")
            self.end_headers()
            self.push(blob)
            return

        if path == "/health":
            self.answer(200, {"ok": True})
            return

        self.answer(404, {"error": "no such thing"})

    def do_POST(self):
        path = urlparse(self.path).path
        if path == "/scripts/submit":
            try:
                length=int(self.headers.get("Content-Length") or 0)
                if not 0 < length <= 8192:raise ValueError("Размер заявки: до 8 КБ")
                self.answer(202, script_store.submit(PLUGINS,json.loads(self.rfile.read(length)),self.who()))
            except (ValueError,TypeError,RecursionError) as error:self.answer(400,{"error":str(error)})
            return
        if path == "/admin/scripts/review":
            if not self.signed_in():self.answer(403,{"error":"admin required"});return
            said=self.form();digest=said.get("digest","");action=said.get("action","")
            token=hashlib.sha256((self.headers.get("Cookie","")+digest+action).encode()).hexdigest()
            if not secrets.compare_digest(token,said.get("token","")):self.answer(403,{"error":"invalid token"});return
            try:
                if action not in {"approve","reject"}:raise ValueError("action")
                script_store.review(PLUGINS,digest,action=="approve");self.go("/admin/scripts")
            except ValueError as error:self.answer(400,{"error":str(error)})
            return
        if path.startswith("/admin"):
            self.panel_post(path)
            return
        try:
            length = int(self.headers.get("Content-Length") or 0)
            if length > 8192:
                self.answer(413, {"error": "that is a lot of json"})
                return
            body = json.loads(self.rfile.read(length) or b"{}")
            if not isinstance(body, dict):
                raise ValueError
        except Exception:
            self.answer(400, {"error": "unreadable"})
            return

        ip = self.who()
        if path == "/claim":
            code, out = claim(body.get("uid", ""), ip)
        elif path == "/profile":
            code, out = profile(body, ip)
        elif path == "/old":
            code, out = free(body, ip)
        else:
            code, out = 404, {"error": "no such thing"}
        self.answer(code, out)


def main():
    prepare()
    server = ThreadingHTTPServer(("127.0.0.1", 8080), Handler)
    server.serve_forever()


if __name__ == "__main__":
    main()

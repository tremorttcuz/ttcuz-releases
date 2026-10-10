"""Readable command plugins: immutable review queue, approval required for discovery."""
import hashlib, json, re, sqlite3, time, unicodedata
from pathlib import Path

def validate(value):
    if not isinstance(value, dict) or len(json.dumps(value,ensure_ascii=False).encode())>8192: raise ValueError('Плагин больше 8 КБ или неверный формат')
    def keys(obj, allowed):
        if not isinstance(obj,dict) or set(obj)-set(allowed): raise ValueError('Неизвестные поля')
    def text(obj,key,limit):
        v=obj.get(key)
        if not isinstance(v,str) or not v or len(v.encode("utf-16-le"))//2>limit or any(unicodedata.category(c) in {'Cc','Cf'} for c in v): raise ValueError('Некорректное поле: '+key)
        return v
    keys(value,['schema','id','name','version','author','rows','names','colours'])
    if type(value.get('schema')) is not int or value['schema']!=1: raise ValueError('Неизвестная схема')
    if not re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9._-]{0,79}',text(value,'id',80)): raise ValueError('Некорректный ID')
    for k,n in [('name',80),('version',32),('author',80)]: text(value,k,n)
    for key in ['rows','names','colours']:
        a=value.get(key,[])
        if not isinstance(a,list) or len(a)>16: raise ValueError('Не больше 16 команд')
        for v in a:
            if key=='rows':
                keys(v,['title','action','value']);text(v,'title',80)
                if text(v,'action',20) not in {'message','copy','window'}:raise ValueError('Команда запрещена')
                text(v,'value',1024)
            elif key=='names':
                keys(v,['uid','value'])
                if not re.fullmatch(r'[0-9]{1,32}',text(v,'uid',32)):raise ValueError('Нужен ID аккаунта')
                text(v,'value',80)
            else:
                keys(v,['from','to'])
                for k in ['from','to']:
                    if not re.fullmatch(r'#[a-fA-F0-9]{6}',text(v,k,7)):raise ValueError('Цвет: #RRGGBB')
    return value

def connect(root):
    Path(root).mkdir(parents=True,exist_ok=True)
    db=sqlite3.connect(str(Path(root)/'script-review.sqlite'),timeout=10)
    db.execute('CREATE TABLE IF NOT EXISTS script (digest TEXT PRIMARY KEY, id TEXT, source TEXT, state TEXT, added REAL, ip TEXT)')
    return db

def submit(root,value,ip,now=None):
    value=validate(value);source=json.dumps(value,ensure_ascii=False,sort_keys=True,separators=(',',':'));digest=hashlib.sha256(source.encode()).hexdigest();now=time.time() if now is None else now
    db=connect(root)
    try:
        db.execute("BEGIN IMMEDIATE")
        with db:
            if db.execute('SELECT 1 FROM script WHERE digest=?',(digest,)).fetchone():return {'status':'pending','digest':digest}
            if db.execute('SELECT count(*) FROM script WHERE added>? AND ip=?',(now-3600,ip)).fetchone()[0]>=10:raise ValueError('Не больше 10 заявок в час')
            if db.execute("SELECT count(*) FROM script WHERE state='pending'").fetchone()[0]>=1000:raise ValueError('Очередь заполнена')
            db.execute('INSERT INTO script VALUES (?,?,?,?,?,?)',(digest,value['id'],source,'pending',now,ip))
        return {'status':'pending','digest':digest}
    finally:db.close()

def review(root,digest,approve):
    if not re.fullmatch('[a-f0-9]{64}',digest):raise ValueError('Некорректный отпечаток')
    db=connect(root)
    try:
        with db:
            row=db.execute("SELECT id,source FROM script WHERE digest=? AND state='pending'",(digest,)).fetchone()
            if not row:raise ValueError('Заявка не найдена')
            validate(json.loads(row[1]))
            if hashlib.sha256(row[1].encode()).hexdigest()!=digest:raise ValueError('Исходник изменён')
            if approve:db.execute("UPDATE script SET state='superseded' WHERE id=? AND state='approved'",(row[0],))
            db.execute('UPDATE script SET state=? WHERE digest=?',('approved' if approve else 'rejected',digest))
    finally:db.close()

def pending(root):
    db=connect(root)
    try:return db.execute("SELECT digest,source FROM script WHERE state='pending' ORDER BY added LIMIT 100").fetchall()
    finally:db.close()

def catalog(root):
    db=connect(root)
    try:
        result=[]
        for digest,source in db.execute("SELECT digest,source FROM script WHERE state='approved' ORDER BY added DESC LIMIT 100"):
            try:
                value=validate(json.loads(source))
                if hashlib.sha256(source.encode()).hexdigest()==digest:result.append(value)
            except (ValueError,TypeError):pass
        return {'scripts':result}
    finally:db.close()

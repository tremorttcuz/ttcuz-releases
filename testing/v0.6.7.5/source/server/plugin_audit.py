"""Publication gate. Static checks reject known concealment; not a malware proof."""
import hashlib, io, json, re, struct, unicodedata, zipfile, zlib
import base64
IMAGE_MAGIC=(b'\x89PNG\r\n\x1a\n',b'\xff\xd8\xff',b'RIFF')
def payload_kind(text):
    """Что спрятано в длинной base64-строке: 'image' для настоящей PNG/JPEG/WebP-картинки, иначе None."""
    try:
        raw=base64.b64decode(text.replace('-','+').replace('_','/')+'='*(-len(text)%4),validate=False)
    except Exception:return None
    if len(raw)>2*1024*1024:return None
    if raw.startswith(b'\xff\xd8\xff') and raw.endswith(b'\xff\xd9'):return 'image'
    if raw.startswith(b'\x89PNG\r\n\x1a\n') and b'IEND' in raw[-16:]:return 'image'
    if raw[:4]==b'RIFF' and raw[8:12]==b'WEBP':return 'image'
    return None
MAX_DEX=16*1024*1024
SHORT={'UI','IO','Db','id','of','is','eq','on','go','by','to','at','up','dx','dy','x','y','z','r','g','b'}

def opaque(name):
    return not name or any(unicodedata.category(c) in {'Cf','Cc'} for c in name) or (len(name)<=2 and name not in SHORT) or bool(re.fullmatch('[a-fA-F0-9]{16,}',name))

class Dex:
    def __init__(self, data):
        self.data=data;self.cache={};self.text_bytes=0;self.uleb_reads=0
        if len(data)<112 or len(data)>MAX_DEX or data[:4]!=b'dex\n' or data[4:8] not in [b'035\0',b'037\0',b'038\0',b'039\0',b'040\0']:raise ValueError('Некорректный DEX')
        if self.u32(32)!=len(data) or self.u32(36)!=112 or self.u32(40)!=0x12345678:raise ValueError('Некорректный заголовок DEX')
        if hashlib.sha1(data[32:]).digest()!=data[12:32] or zlib.adler32(data[12:])&0xffffffff!=self.u32(8):raise ValueError('Повреждён DEX')
        self.sc,self.so=self.u32(56),self.u32(60);self.tc,self.to=self.u32(64),self.u32(68);self.mc,self.mo=self.u32(88),self.u32(92)
        for off,count,size in [(self.so,self.sc,4),(self.to,self.tc,4),(self.mo,self.mc,8),(self.u32(84),self.u32(80),8),(self.u32(100),self.u32(96),32)]:self.table(off,count,size)
        for i in range(self.sc):self.string(i)
    def table(self,off,count,size):
        if count>100000 or bool(off)!=bool(count) or off+count*size>len(self.data):raise ValueError('Некорректная таблица DEX')
    def u32(self,p):
        if p<0 or p+4>len(self.data):raise ValueError('Выход за границы DEX')
        return struct.unpack_from('<I',self.data,p)[0]
    def u16(self,p):
        if p<0 or p+2>len(self.data):raise ValueError('Выход за границы DEX')
        return struct.unpack_from('<H',self.data,p)[0]
    def uleb(self,pos):
        value=0
        for i in range(5):
            self.uleb_reads+=1
            if self.uleb_reads>1000000 or pos>=len(self.data):raise ValueError('Слишком сложные данные DEX')
            byte=self.data[pos];pos+=1;value|=(byte&127)<<(i*7)
            if not byte&128:return value,pos
        raise ValueError('Некорректный ULEB128')
    def check_class_data(self,pos):
        if not pos:return
        counts=[]
        for _ in range(4):value,pos=self.uleb(pos);counts.append(value)
        if sum(counts)>100000:raise ValueError('Слишком сложный класс')
        for _ in range(counts[0]+counts[1]):
            _,pos=self.uleb(pos);_,pos=self.uleb(pos)
        for _ in range(counts[2]+counts[3]):
            _,pos=self.uleb(pos);flags,pos=self.uleb(pos);_,pos=self.uleb(pos)
            if flags&256:raise ValueError('Нативные методы запрещены')
    def string(self,index):
        if index>=self.sc:raise ValueError('Некорректная строка DEX')
        pos=self.u32(self.so+index*4)
        if pos in self.cache:return self.cache[pos]
        start=pos
        for _ in range(5):
            if pos>=len(self.data):raise ValueError('Некорректная строка DEX')
            value=self.data[pos];pos+=1
            if not value&128:break
        else:raise ValueError('Некорректный ULEB128')
        end=self.data.find(b'\0',pos,min(len(self.data),pos+65537))
        if end<0:raise ValueError('Непроверяемая строка DEX')
        self.text_bytes+=end-pos
        if self.text_bytes>MAX_DEX:raise ValueError('Слишком сложный DEX')
        value=self.data[pos:end].decode('utf-8',errors='replace');self.cache[start]=value;return value
    def type(self,index):
        if index>=self.tc:raise ValueError('Некорректный тип DEX')
        return self.string(self.u32(self.to+index*4))

def inspect_dex(data):
    d=Dex(data);own=set();classes=d.u32(96);off=d.u32(100)
    if not classes:raise ValueError('В плагине нет проверяемых классов')
    for i in range(classes):
        at=off+i*32;d.check_class_data(d.u32(at+24));index=d.u32(at);own.add(index);name=d.type(index)
        leaf=name.rsplit('/',1)[-1].rstrip(';').split('$',1)[0]
        if opaque(leaf):raise ValueError('Обфускация запрещена: непрозрачное имя класса')
        source=d.u32(at+16)
        if source==0xffffffff:raise ValueError('Обфускация запрещена: удалены имена исходных файлов')
        filename=d.string(source)
        if not filename.endswith(('.java','.kt')) or opaque(filename.rsplit('.',1)[0]):raise ValueError('Непроверяемое имя исходного файла')
    for i in range(d.mc):
        p=d.mo+i*8;owner=d.type(d.u16(p));name=d.string(d.u32(p+4))
        if d.u16(p) in own and not name.startswith('<') and opaque(name):raise ValueError('Обфускация запрещена: непрозрачное имя метода')
        if (owner=='Ljava/lang/Runtime;' and name=='exec') or (owner=='Ljava/lang/ProcessBuilder;' and name=='start') or (owner in {'Ljava/lang/System;','Ljava/lang/Runtime;'} and name in {'load','loadLibrary'}):raise ValueError('Публикация опасного API запрещена')
    for i in range(d.tc):
        name=d.type(i)
        if name.startswith('Ldalvik/system/') and ('ClassLoader' in name or 'DexFile' in name):raise ValueError('Загрузка дополнительного кода запрещена')
    for value in d.cache.values():
        if len(value)>256 and re.fullmatch('[A-Za-z0-9+/=_-]+',value):
            kind=payload_kind(value)
            if kind is None:raise ValueError('Закодированная строка похожа на скрытый код. Картинки (PNG/JPEG/WebP) разрешены, остальное нет')
    return d

def inspect_package(raw):
    if len(raw)>18*1024*1024:raise ValueError('Слишком большой пакет')
    files={};limits={'manifest.json':65536,'classes.dex':MAX_DEX,'icon.png':1024*1024}
    with zipfile.ZipFile(io.BytesIO(raw)) as archive:
        for info in archive.infolist():
            if info.filename not in limits or info.filename in files or info.is_dir() or info.file_size>limits[info.filename]:raise ValueError('Лишние, повторяющиеся или слишком большие файлы плагина')
            with archive.open(info) as entry:
                content=entry.read(limits[info.filename]+1)
            if len(content)>limits[info.filename]:raise ValueError('Слишком большой распакованный файл')
            files[info.filename]=content
    if not {'manifest.json','classes.dex'}<=files.keys():raise ValueError('Нет manifest.json или classes.dex')
    manifest=json.loads(files['manifest.json'].decode('utf-8'))
    if not isinstance(manifest,dict) or not re.fullmatch('[A-Za-z0-9][A-Za-z0-9._-]{0,79}',str(manifest.get('id',''))):raise ValueError('Некорректный ID плагина')
    dex=inspect_dex(files['classes.dex'])
    entry='L'+str(manifest.get('entry','')).replace('.','/')+';'
    if not any(dex.type(dex.u32(dex.u32(100)+i*32))==entry for i in range(dex.u32(96))):raise ValueError('Entry-класс отсутствует в DEX')
    return manifest,files.get('icon.png',b''),hashlib.sha256(raw).hexdigest()

# ---------------------------------------------------------------- умный отчёт
# BLOCK: у обычного плагина значков/команд таких вызовов быть не может.
# CAPS: возможности, которые просто показываем (не блокируем): человек решает сам.
BLOCK_API={
    ('Landroid/telephony/SmsManager;','sendTextMessage'):'отправка SMS',
    ('Landroid/telephony/SmsManager;','sendMultipartTextMessage'):'отправка SMS',
    ('Landroid/content/pm/PackageInstaller;','createSession'):'установка других приложений',
    ('Landroid/app/admin/DevicePolicyManager;','lockNow'):'управление устройством',
    ('Landroid/app/admin/DevicePolicyManager;','wipeData'):'стирание данных устройства',
}
BLOCK_TYPE={
    'Landroid/accessibilityservice/AccessibilityService;':'сервис доступности (чтение экрана и нажатия)',
    'Landroid/app/admin/DeviceAdminReceiver;':'права администратора устройства',
}
CAPS=[ # (ключ, текст для человека, проверка по владельцу класса)
    ('network','ходит в интернет',lambda o:o.startswith(('Ljava/net/','Ljavax/net/','Landroid/net/','Lokhttp3/')) and 'Uri' not in o),
    ('files','читает и пишет файлы',lambda o:o.startswith(('Ljava/io/File','Ljava/nio/file/'))),
    ('reflection','использует рефлексию (вызов методов по имени)',lambda o:o in ('Ljava/lang/reflect/Method;','Ljava/lang/Class;')),
    ('webview','показывает веб-страницы',lambda o:o=='Landroid/webkit/WebView;'),
    ('clipboard','работает с буфером обмена',lambda o:o=='Landroid/content/ClipboardManager;'),
    ('location','запрашивает геолокацию',lambda o:o.startswith('Landroid/location/')),
    ('contacts','обращается к контактам',lambda o:o.startswith('Landroid/provider/ContactsContract')),
    ('camera_mic','использует камеру или микрофон',lambda o:o in ('Landroid/hardware/Camera;','Landroid/media/AudioRecord;','Landroid/media/MediaRecorder;')),
]
SHORTENERS=('bit.ly','t.co','tinyurl.com','goo.gl','is.gd','cutt.ly','rb.gy')
URL_RE=re.compile(r'https?://[^\s"\'<>\\]+')

def entropy(text):
    from math import log2
    n=len(text);return -sum(c/n*log2(c/n) for c in map(text.count,set(text)))

def analyze_dex(d):
    """Что плагин умеет на самом деле: по вызовам, а не по словам в строках."""
    caps={};why={};notes=[];urls=set();blocks=[]
    for i in range(d.mc):
        p=d.mo+i*8;owner=d.type(d.u16(p));name=d.string(d.u32(p+4))
        if (owner,name) in BLOCK_API:blocks.append(BLOCK_API[(owner,name)])
        for key,text,test in CAPS:
            if test(owner):caps[key]=text
        if owner=='Ljava/lang/Class;' and name=='forName':why['dynamic_class']=1
        if owner=='Ljava/lang/reflect/Method;' and name=='invoke':why['invoke']=1
    for i in range(d.tc):
        t=d.type(i)
        if t in BLOCK_TYPE:blocks.append(BLOCK_TYPE[t])
    domains=set()
    for value in list(d.cache.values()):
        for u in URL_RE.findall(value):
            urls.add(u[:200]);m=re.match(r'https?://([^/:?#]+)',u)
            if m:domains.add(m.group(1).lower())
            if u.startswith('http://'):notes.append('незащищённая ссылка http:// — '+u[:60])
        if len(value)>200 and not re.fullmatch('[A-Za-z0-9+/=_-]+',value) and entropy(value)>5.6:
            notes.append('длинная случайная строка (похоже на шифрование)')
    for dom in sorted(domains):
        if re.fullmatch(r'\d{1,3}(\.\d{1,3}){3}',dom):notes.append('обращение по IP-адресу '+dom)
        if dom in SHORTENERS:notes.append('сокращатель ссылок '+dom)
    # Куда направлена рефлексия. Доступ к классам самого приложения — обычное дело для плагина.
    targets=sorted({v for v in d.cache.values() if re.fullmatch(r'[A-Za-z_][\w]*(\.[A-Za-z_$][\w$]*){2,}',v)})
    HOST=('cat.narezany.margyt.','android.','androidx.','java.','javax.','kotlin.')
    own={d.type(d.u32(d.u32(100)+i*32))[1:-1].replace('/','.').split('$')[0].rsplit('.',1)[0]+'.' for i in range(d.u32(96))}
    if why:
        deadly=[t for t in targets if t in ('java.lang.Runtime','java.lang.ProcessBuilder') or t.startswith(('dalvik.system.DexClassLoader','dalvik.system.PathClassLoader','dalvik.system.InMemoryDexClassLoader','dalvik.system.DexFile'))]
        if deadly:blocks.append('рефлексия к опасному классу '+deadly[0])
        foreign=[t for t in targets if not t.startswith(HOST) and not t.startswith(tuple(own)) and '.' in t and not t.endswith(('.java','.kt'))]
        if 'dynamic_class' in why and not targets:notes.append('Class.forName без читаемых имён классов: нужна ручная проверка')
        if foreign:notes.append('рефлексия к стороннему классу '+foreign[0])
        if targets and not foreign and not deadly:caps['host_api']='использует внутренний API приложения ('+', '.join(t for t in targets if t.startswith('cat.narezany'))[:80]+')'
    return {'blocks':sorted(set(blocks)),'capabilities':sorted(v for k,v in caps.items() if k!='reflection'),'caps':sorted(caps),'domains':sorted(domains),'notes':sorted(set(notes))}

def analyze_package(raw):
    """Полный отчёт. Блокирует только явное зло; остальное — verdict 'review' или 'ok'."""
    manifest,icon,digest=inspect_package(raw)
    with zipfile.ZipFile(io.BytesIO(raw)) as z:report=analyze_dex(inspect_dex(z.read('classes.dex')))
    if report['blocks']:raise ValueError('Запрещено: '+', '.join(report['blocks']))
    if icon and not icon.startswith(b'\x89PNG\r\n\x1a\n'):report['notes'].append('icon.png не является PNG')
    for key in ('name','version','author'):
        if not isinstance(manifest.get(key),str) or not manifest[key].strip():report['notes'].append('в manifest нет поля '+key)
    if not isinstance(manifest.get('min_api',1),int):report['notes'].append('min_api должен быть числом')
    risky=bool(report['notes'])
    report['verdict']='review' if risky else 'ok'
    return {'manifest':manifest,'icon':icon,'sha256':digest,**report}

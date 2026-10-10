"""Mark packaged dot animations for the common native Lottie draw hook.

Composition names are stable across obfuscated resource filenames. The marker
is also read when the caller uses a raw resource, stream or composition cache.
"""
import json
import math

MARKER = "ttcuz_MD3_loader"
DOT_NAMES = frozenset(name.lower() for name in (
    "camera_loading_lottie_final", "光点",
    "Search_Tako_dots_loading_DarkMode_lottie",
    "Search_Tako_dots_loading_LightMode_lottie", "typing1x", "typing@1x",
    "story_loading_unread&upload_yellow new", "creation_ailoading_oral_large_tux_lottie",
    "Infrastructure_minis_open_loading_tux_lottie",
    "Infrastructure_minis_open_loading_dark_tux_lottie",
    "tako_message_loading_tux_lottie", "tako_message_loading_dark_tux_lottie",
    "Preview_Loading_96*96_lottie",
))
PLACEHOLDERS = frozenset(("assets/lottie_double_points_loading.json", "assets/lottie_video_loading.json"))

def replacement(path, source):
    if not path.endswith('.json'): return None
    try: data = json.loads(source)
    except (ValueError, UnicodeError): return None
    if not isinstance(data, dict): return None
    if str(data.get('nm', '')).lower() not in DOT_NAMES and path not in PLACEHOLDERS: return None
    w, h = data.get('w', 48), data.get('h', 48)
    # A static shape remains valid Lottie data. LoadingIndicator supplies all
    # seven shapes, spring motion, rotation and the live Accent.colour().
    radius = min(w, h) * .35
    vertices = [[round(radius*math.cos(i*math.tau/20)*(1 if i%2==0 else .8),4),
                 round(radius*math.sin(i*math.tau/20)*(1 if i%2==0 else .8),4)] for i in range(20)]
    zero = [[0, 0] for _ in vertices]
    data = {'v':'5.7.6','fr':60,'ip':0,'op':273,'w':w,'h':h,'nm':MARKER,'ddd':0,'assets':[],
            'layers':[{'ddd':0,'ind':1,'ty':4,'nm':MARKER,'sr':1,
                'ks':{'o':{'a':0,'k':100},'r':{'a':0,'k':0},'p':{'a':0,'k':[w/2,h/2,0]},
                      'a':{'a':0,'k':[0,0,0]},'s':{'a':0,'k':[100,100,100]}},
                'shapes':[{'ty':'sh','ks':{'a':0,'k':{'i':zero,'o':zero,'v':vertices,'c':True}},'nm':MARKER},
                          {'ty':'fl','c':{'a':0,'k':[.996,.173,.333,1]},'o':{'a':0,'k':100},'r':1,'nm':'Accent'}],
                'ip':0,'op':273,'st':0,'bm':0}]}
    return json.dumps(data,ensure_ascii=False,separators=(',',':')).encode('utf8')

def replace_dots(apk):
    changed=[]
    for path in apk.names():
        if not path.endswith('.json') or not path.startswith(('assets/','res/')): continue
        result=replacement(path,apk.read(path))
        if result is not None:
            apk.replace(path,result)
            changed.append(path)
    return changed

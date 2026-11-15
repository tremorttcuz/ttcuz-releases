import importlib.util,pathlib,tempfile,unittest,copy
ROOT=pathlib.Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('script_store',ROOT/'server/script_store.py');store=importlib.util.module_from_spec(spec);spec.loader.exec_module(store)
def sample():return {'schema':1,'id':'test.commands','name':'Команды','version':'1.0','author':'Автор','rows':[{'title':'Копировать','action':'copy','value':'Текст'}]}
class ScriptPublicationTest(unittest.TestCase):
 def test_approval_required_and_updates_are_separate(self):
  with tempfile.TemporaryDirectory() as root:
   first=store.submit(root,sample(),'ip');self.assertEqual([],store.catalog(root)['scripts'])
   store.review(root,first['digest'],True);self.assertEqual('1.0',store.catalog(root)['scripts'][0]['version'])
   next=sample();next['version']='2.0';second=store.submit(root,next,'ip');self.assertEqual('1.0',store.catalog(root)['scripts'][0]['version'])
   store.review(root,second['digest'],True);self.assertEqual(['2.0'],[v['version'] for v in store.catalog(root)['scripts']])
 def test_rejected_source_is_never_public(self):
  with tempfile.TemporaryDirectory() as root:
   item=store.submit(root,sample(),'ip');store.review(root,item['digest'],False);self.assertEqual([],store.catalog(root)['scripts'])
   with self.assertRaises(ValueError):store.review(root,item['digest'],True)
 def test_invalid_commands_schema_and_unknown_fields(self):
  cases=[]
  for field,value in [('schema',True),('schema','1'),('eval','hello'),('id','../bad'),('name','hidden\u202e')]:
   v=sample();v[field]=value;cases.append(v)
  v=sample();v['rows'][0]['action']='exec';cases.append(v)
  v=sample();v['rows']*=17;cases.append(v)
  for v in cases:
   with self.subTest(value=v),self.assertRaises(ValueError):store.validate(v)
 def test_limits_and_idempotence(self):
  with tempfile.TemporaryDirectory() as root:
   value=sample();a=store.submit(root,value,'ip',1000);self.assertEqual(a,store.submit(root,value,'ip',1000))
   for i in range(9):value=sample();value['version']=str(i);store.submit(root,value,'ip',1000)
   value['version']='next'
   with self.assertRaises(ValueError):store.submit(root,value,'ip',1000)
 def test_modified_approved_source_is_excluded(self):
  with tempfile.TemporaryDirectory() as root:
   item=store.submit(root,sample(),'ip');store.review(root,item['digest'],True)
   db=store.connect(root);db.execute("UPDATE script SET source=?",('invalid',));db.commit();db.close();self.assertEqual([],store.catalog(root)['scripts'])
 def test_names_and_colours_validate(self):
  v=sample();v['names']=[{'uid':'123','value':'Имя'}];v['colours']=[{'from':'#FE2C55','to':'#6750A4'}];self.assertEqual(v,store.validate(v))
  v['colours'][0]['to']='red'
  with self.assertRaises(ValueError):store.validate(v)

 def test_real_http_routes_require_admin_and_review_token(self):
  import ast,io,hashlib,json,secrets
  from urllib.parse import urlparse,parse_qs
  tree=ast.parse((ROOT/'server/badges.py').read_text(encoding='utf-8'))
  native=next(n for n in tree.body if isinstance(n,ast.ClassDef) and n.name=='Handler')
  methods=[n for n in native.body if isinstance(n,ast.FunctionDef) and n.name in ('do_GET','do_POST')]
  module=ast.fix_missing_locations(ast.Module(body=[ast.ClassDef(name='Routes',bases=[],keywords=[],body=methods,decorator_list=[])],type_ignores=[]))
  with tempfile.TemporaryDirectory() as root:
   scope={'script_store':store,'PLUGINS':root,'urlparse':urlparse,'parse_qs':parse_qs,'json':json,'hashlib':hashlib,'secrets':secrets}
   exec(compile(module,'routes','exec'),scope);Routes=scope['Routes']
   class Request(Routes):
    def __init__(self,path,body=b'',admin=False):self.path=path;self.headers={'Content-Length':str(len(body)),'Cookie':'admincookie'};self.rfile=io.BytesIO(body);self.admin=admin;self.result=None;self.said={}
    def signed_in(self):return self.admin
    def who(self):return 'ip'
    def answer(self,code,value):self.result=(code,value)
    def form(self):return self.said
    def go(self,where):self.result=(303,where)
   req=Request('/scripts/submit',json.dumps(sample()).encode());req.do_POST();self.assertEqual(202,req.result[0]);digest=req.result[1]['digest']
   req=Request('/scripts');req.do_GET();self.assertEqual([],req.result[1]['scripts'])
   req=Request('/admin/scripts/review');req.do_POST();self.assertEqual(403,req.result[0])
   req=Request('/admin/scripts/review',admin=True);req.said={'digest':digest,'action':'approve','token':'wrong'};req.do_POST();self.assertEqual(403,req.result[0]);self.assertEqual([],store.catalog(root)['scripts'])
   req.said['token']=hashlib.sha256(('admincookie'+digest+'approve').encode()).hexdigest();req.do_POST();self.assertEqual(303,req.result[0]);self.assertEqual(1,len(store.catalog(root)['scripts']))

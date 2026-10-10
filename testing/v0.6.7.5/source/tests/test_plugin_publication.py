"""Server publication executes the production audit before writing a package."""
import ast,hashlib,importlib.util,io,json,os,pathlib,sqlite3,struct,tempfile,time,unittest,zipfile,zlib
from test_plugin_audit_runtime import dex
ROOT=pathlib.Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('audit',ROOT/'server/plugin_audit.py');audit=importlib.util.module_from_spec(spec);spec.loader.exec_module(audit)
def package(code,entry='sample.DemoPlugin'):
    out=io.BytesIO()
    with zipfile.ZipFile(out,'w') as z:
        z.writestr('manifest.json',json.dumps({'id':'demo','entry':entry}));z.writestr('classes.dex',code)
    return out.getvalue()
class PublicationTest(unittest.TestCase):
    def test_readable_plugin_and_package_digest(self):
        raw=package(dex('Lsample/DemoPlugin;','onStart',source='DemoPlugin.java',define=True))
        manifest,icon,digest=audit.inspect_package(raw)
        self.assertEqual('demo',manifest['id']);self.assertEqual(hashlib.sha256(raw).hexdigest(),digest)
    def test_concealment_rejected(self):
        for code in [dex('Lsample/a;','onStart',source='DemoPlugin.java',define=True),dex('Lsample/DemoPlugin;','a',source='DemoPlugin.java',define=True),dex('Lsample/DemoPlugin;','onStart',define=True),dex('Lsample/DemoPlugin;','onStart',literal='Z'*300,source='DemoPlugin.java',define=True)]:
            with self.subTest(code=hashlib.sha256(code).hexdigest()):
                with self.assertRaises(ValueError):audit.inspect_package(package(code))
    def test_native_method_rejected_before_publication(self):
        code=bytearray(dex('Lsample/DemoPlugin;','onStart',source='DemoPlugin.java',define=True))
        class_offset=struct.unpack_from('<I',code,100)[0];data_offset=len(code)
        code+=bytes([0,0,1,0,0,128,2,0])
        struct.pack_into('<I',code,class_offset+24,data_offset)
        struct.pack_into('<I',code,32,len(code))
        struct.pack_into('<I',code,104,len(code)-struct.unpack_from('<I',code,108)[0])
        code[12:32]=hashlib.sha1(code[32:]).digest();struct.pack_into('<I',code,8,zlib.adler32(code[12:])&0xffffffff)
        with self.assertRaisesRegex(ValueError,'Нативные методы'):audit.inspect_package(package(code))
    def test_rejected_upload_has_no_side_effects(self):
        tree=ast.parse((ROOT/'server/badges.py').read_text(encoding='utf-8'))
        function=next(n for n in tree.body if isinstance(n,ast.FunctionDef) and n.name=='take_plugin')
        with tempfile.TemporaryDirectory() as temp:
            target=pathlib.Path(temp)/'plugins'
            namespace={'inspect_package':audit.inspect_package,'os':os,'PLUGINS':str(target),'ICONS':str(pathlib.Path(temp)/'icons'),'time':time,'connect':lambda: self.fail('Rejected upload opened database')}
            exec(compile(ast.Module(body=[function],type_ignores=[]),'take_plugin','exec'),namespace)
            with self.assertRaises(ValueError):namespace['take_plugin'](package(dex('Lsample/a;','onStart',source='DemoPlugin.java',define=True)),'demo.ctt')
            self.assertFalse(target.exists())
    def test_wrong_entry_and_extra_file_rejected(self):
        code=dex('Lsample/DemoPlugin;','onStart',source='DemoPlugin.java',define=True)
        with self.assertRaises(ValueError):audit.inspect_package(package(code,'sample.Missing'))
        raw=io.BytesIO(package(code))
        with zipfile.ZipFile(raw,'a') as z:z.writestr('../extra','payload')
        with self.assertRaises(ValueError):audit.inspect_package(raw.getvalue())

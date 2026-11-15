import test from 'node:test';
import assert from 'node:assert/strict';
import {inspectSources,inspectPackage,normalise} from '../server/cloudflare/guard.mjs';
import {makeCtt,makeZip,makeDex} from './helpers/plugin-package.mjs';

const clean=[{path:'java/nick/hello/Main.java',text:`package nick.hello;
import cat.ttcuz.framework.plugin.TtcuzPlugin;
// Приветствие в ленте
public class Main extends TtcuzPlugin {
    private int greetings;
    @Override public void onStart() { greetings++; ttcuz().toast("Привет, " + greetings); }
    private String phrase(String name) { String result = "Hi " + name; return result; }
    class Helper { void run() { for (int i = 0; i < 3; i++) phrase("x"); } }
}`}];
const meta={id:'nick.hello',entry:'nick.hello.Main'};

test('readable code passes the source check', () => {
  assert.deepEqual(inspectSources(clean), []);
});

test('one-letter names everywhere are refused as obfuscated', () => {
  const text='package a; public class a { int b; int c; int d; String f; void g(){} void h(int q){} void l(){} void o(){} a u; }';
  assert.ok(inspectSources([{path:'java/a/a.java',text}]).some(r=>r.includes('одной-двух букв')));
});

test('look-alike names, unicode escapes, minified lines and hidden loaders are refused', () => {
  const confusing='package p; class IlIl { int lIlI; void IIll(){} void llII(){} }';
  assert.ok(inspectSources([{path:'java/p/X.java',text:confusing}]).some(r=>r.includes('похожие')));
  const escaped='package p; class X { void run() { \\u0053ystem.\\u006fut.\\u0070rintln(1); } }';
  assert.ok(inspectSources([{path:'java/p/X.java',text:escaped}]).some(r=>r.includes('u-последовательност')));
  const minified='package p; class X { '+'int value = 1; '.repeat(200)+'}';
  assert.ok(inspectSources([{path:'java/p/X.java',text:minified}]).length>0);
  const loader='package p; import dalvik.system.DexClassLoader; class X { void run() { new DexClassLoader("a", "b", null, null); } }';
  assert.ok(inspectSources([{path:'java/p/X.java',text:loader}]).some(r=>r.includes('DexClassLoader')));
  const blob='package p; class X { String payload = "'+'QUJD'.repeat(200)+'"; }';
  assert.ok(inspectSources([{path:'java/p/X.java',text:blob}]).some(r=>r.includes('base64')));
});

test('a package built from the sources matches them, inner and synthetic classes included', async () => {
  const ctt=makeCtt('nick.hello','nick.hello.Main',['Lnick/hello/Main;','Lnick/hello/Main$Helper;','Lnick/hello/Main$$ExternalSyntheticLambda0;']);
  assert.deepEqual(await inspectPackage(ctt,clean,meta), []);
});

test('a class the sources do not declare, an extra file or a hidden loader in the dex is refused', async () => {
  const extra=makeCtt('nick.hello','nick.hello.Main',['Lnick/hello/Main;','Lnick/hello/Stealer;']);
  assert.ok((await inspectPackage(extra,clean,meta)).some(r=>r.includes('Stealer')));
  const lib=makeCtt('nick.hello','nick.hello.Main',['Lnick/hello/Main;'],{'lib/arm64-v8a/libx.so':'ELF'});
  assert.ok((await inspectPackage(lib,clean,meta)).some(r=>r.includes('лишний файл')));
  const loading=makeZip({'manifest.json':JSON.stringify(meta),'classes.dex':makeDex(['Lnick/hello/Main;'],['Ldalvik/system/DexClassLoader;'])});
  assert.ok((await inspectPackage(loading,clean,meta)).some(r=>r.includes('DexClassLoader')));
  const other=makeCtt('other.id','nick.hello.Main',['Lnick/hello/Main;']);
  assert.ok((await inspectPackage(other,clean,meta)).some(r=>r.includes('manifest.json')));
});

test('a Gemini answer is cut down to known fields', () => {
  const r=normalise({verdict:'dangerous',obfuscated:true,summary:'x',issues:['a',5],extra:1},'m');
  assert.equal(r.verdict,'dangerous');assert.equal(r.obfuscated,true);assert.deepEqual(r.issues,['a']);assert.equal(r.extra,undefined);
  assert.equal(normalise({verdict:'evil'}).verdict,'unknown');
});

import test from 'node:test';
import assert from 'node:assert/strict';
import {shield} from '../server/cloudflare/shield.mjs';

const ask=(method,path,headers={})=>new Request('https://mod'+path,{method,headers,body:method==='POST'?'{}':undefined});

test('reads pass, app writes pass, foreign writes and odd methods are refused',async()=>{
  assert.equal(await shield(ask('GET','/badges'),{}),null);
  assert.equal(await shield(ask('POST','/v1/publish',{'user-agent':'ttcuz'}),{}),null);
  assert.equal((await shield(ask('POST','/v1/publish',{'user-agent':'curl/8'}),{})).status,403);
  assert.equal(await shield(ask('POST','/plugins/native/admin/review',{'user-agent':'Mozilla'}),{}),null);
  assert.equal((await shield(ask('DELETE','/badges'),{})).status,405);
  assert.equal((await shield(ask('POST','/v1/publish',{'user-agent':'ttcuz','content-length':'9999999'}),{})).status,413);
});

test('the rate limiter answers 429 and a broken limiter never blocks',async()=>{
  const env={READ_LIMIT:{async limit(){return {success:false};}}};
  assert.equal((await shield(ask('GET','/badges'),env)).status,429);
  assert.equal(await shield(ask('GET','/badges'),{READ_LIMIT:{async limit(){throw new Error('x');}}}),null);
});

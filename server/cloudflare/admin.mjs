// Who may moderate: a PIN typed on /review.html, or a Worker token, checked here.
//
// Neither the PIN nor anything derived from it is in the code: it lives only in the Worker's secrets
// (`wrangler secret put ADMIN_PIN`), so publishing the sources gives nothing to try offline. Without the secret the
// PIN door stays shut. The PIN may be a passphrase (8 to 64 letters, digits, _ or -). Wrong ones are counted in D1:
// five per address per hour, then that address is refused even with the right PIN, and two hundred across everyone
// per hour lock the PIN for that hour. The Bearer tokens (PLUGIN_ADMIN_TOKEN, CRASH_ADMIN_TOKEN) are secrets too.
import {digest} from './profiles.mjs';

const PER_ADDRESS=5,EVERYONE=200;
const PIN_SHAPE=/^[A-Za-z0-9_-]{8,64}$/;

export const ADMIN_HINT='Нужен PIN-код администратора: введите его на странице /review.html. Пять неверных попыток — блокировка на час.';

/** Equal strings, compared in time that does not depend on where they differ. */
function same(a,b){
  const x=new TextEncoder().encode(a),y=new TextEncoder().encode(b);
  let diff=x.length^y.length;
  for(let i=0;i<Math.max(x.length,y.length);i++)diff|=(x[i]||0)^(y[i]||0);
  return diff===0;
}

async function misses(env,key){
  const row=await env.PROFILES.prepare('SELECT hits FROM limits WHERE key=?').bind(key).first();
  return row?.hits||0;
}

async function miss(env,key,until){
  await env.PROFILES.prepare('INSERT INTO limits(key,hits,until) VALUES(?,1,?) ON CONFLICT(key) DO UPDATE SET hits=hits+1,until=excluded.until').bind(key,until).run();
}

/** The PIN the Worker holds as a secret, if it was set and has a usable shape; null otherwise. */
function configuredPin(env){
  const pin=String(env.ADMIN_PIN||'').trim();
  return PIN_SHAPE.test(pin)?pin:null;
}

/** 'ok', 'wrong', 'locked' or 'none' (nothing was presented, or the PIN door is not set up). */
export async function admin(request,env){
  const bearer=request.headers.get('authorization')||'';
  for(const secret of [env.PLUGIN_ADMIN_TOKEN,env.CRASH_ADMIN_TOKEN])
    if(typeof secret==='string'&&secret.length>=16&&same(bearer,'Bearer '+secret))return 'ok';
  const pin=(request.headers.get('x-admin-pin')||'').trim();
  if(!pin)return 'none';
  const expected=configuredPin(env);
  if(!expected)return 'none';
  const hour=Math.floor(Date.now()/3600000),until=(hour+1)*3600000;
  const ip=request.headers.get('cf-connecting-ip')||'unknown';
  const mine='admin-pin:'+await digest(ip+':'+hour),all='admin-pin-all:'+hour;
  if(env.PROFILES&&(await misses(env,mine)>=PER_ADDRESS||await misses(env,all)>=EVERYONE))return 'locked';
  if(PIN_SHAPE.test(pin)&&same(pin,expected))return 'ok';
  if(env.PROFILES){await miss(env,mine,until);await miss(env,all,until);}
  return 'wrong';
}

export function refusal(result,reply){
  if(result==='locked')return reply({error:'admin_locked',hint:'Слишком много неверных PIN-кодов. Попробуйте через час.'},429);
  return reply({error:result==='wrong'?'wrong_pin':'admin_required',hint:ADMIN_HINT},403);
}

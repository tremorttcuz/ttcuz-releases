import { digest } from './profiles.mjs';
import { reviewSource, analyzeSource } from './plugin-analysis.mjs';
import { notifyDirect } from './discord.mjs';

const reply = (value, status = 200) =>
  Response.json(value, {
    status,
    headers: {
      'cache-control': 'no-store',
      'access-control-allow-origin': '*',
    },
  });
const idPattern = /^[A-Za-z0-9][A-Za-z0-9._-]{0,79}$/;
export function submission(value) {
  const metadata = value?.metadata;
  if (!metadata || !idPattern.test(metadata.id || '')) throw new Error('Некорректный ID');
  for (const key of ['name', 'author', 'version'])
    if (
      typeof metadata[key] !== 'string' ||
      !metadata[key].trim() ||
      metadata[key].length > 80 ||
      /[\p{Cc}\p{Cf}]/u.test(metadata[key])
    )
      throw new Error('Проверьте поле ' + key);
  if (!/^[a-zA-Z_$][\w$]*(?:\.[a-zA-Z_$][\w$]*)+$/.test(metadata.entry || ''))
    throw new Error('Некорректный entry-класс');
  const files = reviewSource(value).sort((a, b) => a.path.localeCompare(b.path));
  return {
    metadata: {
      id: metadata.id,
      name: metadata.name,
      author: metadata.author,
      version: metadata.version,
      entry: metadata.entry,
      api: 1,
      description: String(metadata.description || '').slice(0, 240),
    },
    files,
  };
}

export async function nativePluginRoute(request, env, readJson, admin, context) {
  const path = new URL(request.url).pathname;
  if (!path.startsWith('/plugins/native/')) return null;
  if (!env.PROFILES) return reply({ error: 'storage_unavailable' }, 503);
  if (request.method !== 'POST') return reply({ error: 'method' }, 405);
  let body;
  try {
    body = await readJson(request, 1600000);
  } catch {
    return reply({ error: 'Некорректный или слишком большой запрос' }, 400);
  }
  if (path === '/plugins/native/submit') {
    let source;
    try {
      source = submission(body);
    } catch (error) {
      return reply({ error: error.message }, 400);
    }
    const hash = await digest(JSON.stringify(source));
    const existing = await env.PROFILES.prepare(
      'SELECT state FROM native_plugin_submissions WHERE digest=?',
    )
      .bind(hash)
      .first();
    if (existing) return reply({ digest: hash, status: existing.state });
    const now = Date.now();
    const hour = Math.floor(now / 3600000);
    const ip = await digest((request.headers.get('cf-connecting-ip') || 'unknown') + ':' + hour);
    const quota = await env.PROFILES.prepare(
      'INSERT INTO limits(key,hits,until) VALUES(?,1,?) ON CONFLICT(key) DO UPDATE SET hits=hits+1 RETURNING hits',
    )
      .bind('native-plugin:' + ip, (hour + 1) * 3600000)
      .first();
    if (quota?.hits > 60)
      return reply(
        {
          error: 'Не больше 60 новых заявок в час с одной сети',
          retry_after_seconds: Math.ceil(((hour + 1) * 3600000 - now) / 1000),
        },
        429,
      );
    const count = await env.PROFILES.prepare(
      "SELECT COUNT(*) AS count FROM native_plugin_submissions WHERE state='pending'",
    ).first();
    if (count?.count >= 1000) return reply({ error: 'Очередь заполнена' }, 429);
    // Persist before Google/Discord: an external outage cannot lose a submission.
    await env.PROFILES.prepare(
      "INSERT OR IGNORE INTO native_plugin_submissions(digest,plugin_id,metadata,source,state,added,ip_hash) VALUES(?,?,?,?,'pending',?,?)",
    )
      .bind(
        hash,
        source.metadata.id,
        JSON.stringify(source.metadata),
        JSON.stringify(source.files),
        Date.now(),
        ip,
      )
      .run();
    const notice = await notifyDirect(
      env,
      `**Новый плагин ttcuz**\n${source.metadata.name} · ${source.metadata.author}\nЗаявка: \`${hash}\`\nПроверить: ${new URL('/plugin-admin.html', request.url)}\nИсходники ожидают решения владельца.`,
    );
    if (notice.sent)
      await env.PROFILES.prepare('UPDATE native_plugin_submissions SET notified=1 WHERE digest=?')
        .bind(hash)
        .run();
    if (context?.waitUntil && env.GEMINI_API_KEY)
      context.waitUntil(
        (async () => {
          const analysis = await analyzeSource(env, source.files);
          await env.PROFILES.prepare(
            'UPDATE native_plugin_submissions SET analysis=? WHERE digest=?',
          )
            .bind(JSON.stringify(analysis), hash)
            .run();
          await notifyDirect(
            env,
            `**ИИ · ${source.metadata.name}**\n${analysis.status === 'complete' ? 'Риск: ' + analysis.risk + '\n' + analysis.summary : 'Разбор недоступен: ' + analysis.reason}\nРешение остаётся за владельцем.\n${new URL('/plugin-admin.html', request.url)}`,
          );
        })().catch(() => {}),
      );
    return reply({ status: 'pending', digest: hash }, 202);
  }
  if (!(await admin(body))) return reply({ error: 'owner_required' }, 403);
  if (path === '/plugins/native/list') {
    const rows = await env.PROFILES.prepare(
      'SELECT digest,metadata,state,analysis,added,notified FROM native_plugin_submissions ORDER BY added DESC LIMIT 100',
    ).all();
    const marks = await env.PROFILES.prepare(
      'SELECT author,mark FROM plugin_author_marks ORDER BY author',
    ).all();
    return reply({ submissions: rows.results || [], marks: marks.results || [] });
  }
  if (path === '/plugins/native/mark') {
    if (
      typeof body.author !== 'string' ||
      !body.author.trim() ||
      body.author.length > 80 ||
      typeof body.mark !== 'string' ||
      body.mark.length > 40 ||
      /[\p{Cc}\p{Cf}]/u.test(body.author + body.mark)
    )
      return reply({ error: 'invalid_mark' }, 400);
    if (!body.mark)
      await env.PROFILES.prepare('DELETE FROM plugin_author_marks WHERE author=?')
        .bind(body.author)
        .run();
    else
      await env.PROFILES.prepare(
        'INSERT INTO plugin_author_marks(author,mark,updated) VALUES(?,?,?) ON CONFLICT(author) DO UPDATE SET mark=excluded.mark,updated=excluded.updated',
      )
        .bind(body.author, body.mark, Date.now())
        .run();
    return reply({ ok: true });
  }
  if (!/^[a-f0-9]{64}$/.test(body.digest || '')) return reply({ error: 'invalid_digest' }, 400);
  const row = await env.PROFILES.prepare('SELECT * FROM native_plugin_submissions WHERE digest=?')
    .bind(body.digest)
    .first();
  if (!row) return reply({ error: 'not_found' }, 404);
  if (path === '/plugins/native/source')
    return reply({ metadata: JSON.parse(row.metadata), files: JSON.parse(row.source) });
  if (path === '/plugins/native/analyze') {
    const analysis = await analyzeSource(env, reviewSource({ files: JSON.parse(row.source) }));
    await env.PROFILES.prepare('UPDATE native_plugin_submissions SET analysis=? WHERE digest=?')
      .bind(JSON.stringify(analysis), body.digest)
      .run();
    const notice = await notifyDirect(
      env,
      `**Разбор плагина ttcuz**\n${JSON.parse(row.metadata).name}\n${analysis.status === 'complete' ? 'Риск: ' + analysis.risk + '\n' + analysis.summary : 'ИИ недоступен: ' + analysis.reason}\nРешение принимает владелец.\n${new URL('/plugin-admin.html', request.url)}`,
    );
    return reply({ analysis, notified: notice.sent });
  }
  if (path === '/plugins/native/review' && ['approve', 'reject'].includes(body.action)) {
    const state = body.action === 'approve' ? 'approved' : 'rejected';
    await env.PROFILES.prepare('UPDATE native_plugin_submissions SET state=? WHERE digest=?')
      .bind(state, body.digest)
      .run();
    return reply({ status: state });
  }
  if (path === '/plugins/native/notify') {
    const notice = await notifyDirect(
      env,
      `**Заявка ttcuz**\n${JSON.parse(row.metadata).name}\n${new URL('/plugin-admin.html', request.url)}\n\`${body.digest}\``,
    );
    if (notice.sent)
      await env.PROFILES.prepare('UPDATE native_plugin_submissions SET notified=1 WHERE digest=?')
        .bind(body.digest)
        .run();
    return reply({ sent: notice.sent });
  }
  return reply({ error: 'not_found' }, 404);
}

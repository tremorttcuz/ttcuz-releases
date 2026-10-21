import { projectZip, download } from './plugin-project.mjs';
const get = (id) => document.getElementById(id),
  status = get('status');
let token = '',
  session = new URLSearchParams(location.hash.slice(1)).get('session') || '',
  selected = null,
  project = null;
async function api(path, body = {}) {
  const response = await fetch(path, {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ ...body, session, uid: '7491898648855512119', token }),
  });
  const value = await response.json();
  if (!response.ok) throw Error(value.error || 'Ошибка запроса');
  return value;
}
async function run(action) {
  for (const b of document.querySelectorAll('button')) b.disabled = true;
  status.textContent = 'Обработка…';
  try {
    await action();
    status.textContent = 'Готово';
  } catch (error) {
    status.textContent = error.message;
  } finally {
    for (const b of document.querySelectorAll('button')) b.disabled = false;
  }
}
function button(label, action) {
  const b = document.createElement('button');
  b.textContent = label;
  b.className = 'secondary';
  b.onclick = () => run(action);
  return b;
}
async function load() {
  const value = await api('/plugins/native/list');
  get('owner').hidden = false;
  get('queue').replaceChildren();
  for (const row of value.submissions) {
    const m = JSON.parse(row.metadata),
      item = document.createElement('div');
    item.className = 'entry';
    const title = document.createElement('p');
    title.textContent = m.name + ' · ' + m.author + ' · ' + row.state;
    item.append(
      title,
      button('Открыть', () => open(row)),
    );
    get('queue').append(item);
  }
  if (!value.submissions.length) get('queue').textContent = 'Заявок пока нет';
  get('marks').replaceChildren();
  for (const mark of value.marks) {
    const p = document.createElement('p');
    p.textContent = mark.author + ' · ' + mark.mark;
    get('marks').append(p);
  }
  const published = await api('/plugins/admin', { action: 'list' });
  get('published').replaceChildren();
  for (const m of published.packages) {
    const item = document.createElement('div');
    item.className = 'entry';
    const p = document.createElement('p');
    p.textContent = m.name + ' · ' + m.version + (m.verified ? ' · проверен' : '');
    item.append(
      p,
      button('Удалить', async () => {
        if (!confirm('Убрать плагин из магазина?')) return;
        await api('/plugins/admin', { action: 'remove', kind: 'package', id: m.id });
        await load();
      }),
    );
    get('published').append(item);
  }
}
async function open(row) {
  selected = row;
  project = await api('/plugins/native/source', { digest: row.digest });
  get('review').hidden = false;
  get('title').textContent = project.metadata.name;
  get('meta').textContent = row.state + ' · ' + row.digest;
  get('files').replaceChildren(...project.files.map((f, i) => new Option(f.path, String(i))));
  get('source').textContent = project.files[0].text;
  get('analysis').textContent = row.analysis
    ? JSON.stringify(JSON.parse(row.analysis), null, 2)
    : 'Разбор ещё не выполнен';
  for (const [field, key] of [
    ['plugin-id', 'id'],
    ['name', 'name'],
    ['version', 'version'],
    ['publisher', 'author'],
    ['about', 'description'],
  ])
    get(field).value = project.metadata[key] || '';
  get('verified').checked = false;
  get('review').scrollIntoView({ block: 'start' });
}
get('files').onchange = () =>
  (get('source').textContent = project.files[Number(get('files').value)].text);
get('login').onsubmit = (event) => {
  event.preventDefault();
  session = '';
  token = get('token').value;
  get('token').value = '';
  run(load);
};
get('logout').onclick = () => {
  token = '';
  session = '';
  project = selected = null;
  get('owner').hidden = true;
  get('queue').replaceChildren();
  get('source').textContent = '';
  get('analysis').textContent = '';
  status.textContent = 'Вы вышли';
};
for (const action of ['analyze', 'approve', 'reject', 'notify'])
  get(action).onclick = () =>
    run(async () => {
      if (!selected) return;
      const path = ['approve', 'reject'].includes(action) ? 'review' : action;
      const value = await api('/plugins/native/' + path, { digest: selected.digest, action });
      if (value.analysis) get('analysis').textContent = JSON.stringify(value.analysis, null, 2);
      if (value.status) {
        selected.state = value.status;
        get('meta').textContent = value.status + ' · ' + selected.digest;
      }
      await load();
    });
get('download').onclick = () => {
  if (project) download(projectZip(project), project.metadata.id + '-source.zip');
};
get('mark-form').onsubmit = (event) => {
  event.preventDefault();
  run(async () => {
    await api('/plugins/native/mark', { author: get('author').value, mark: get('mark').value });
    await load();
  });
};
get('publish').onsubmit = (event) => {
  event.preventDefault();
  run(async () => {
    const file = get('package').files[0];
    if (!file || file.size > 1048576) throw Error('Выбери пакет до 1 МБ');
    const bytes = new Uint8Array(await file.arrayBuffer());
    let text = '';
    for (let i = 0; i < bytes.length; i += 8192)
      text += String.fromCharCode(...bytes.subarray(i, i + 8192));
    const verified = get('verified').checked;
    if (verified && (!selected || selected.state !== 'approved'))
      throw Error('Сначала выбери и прими исходники');
    await api('/plugins/admin', {
      action: 'publish-package',
      archive: btoa(text),
      verified,
      builtFromReviewedSource: verified,
      sourceDigest: verified ? selected.digest : undefined,
      metadata: {
        id: get('plugin-id').value,
        name: get('name').value,
        version: get('version').value,
        author: get('publisher').value,
        about: get('about').value,
        tiktok: '47.2.41',
        format: 'ctt',
      },
    });
    await load();
  });
};

if (session) {
  history.replaceState(null, '', location.pathname);
  run(load);
}

import { projectZip, download } from './plugin-project.mjs';
const get = (id) => document.getElementById(id),
  code = get('code'),
  current = get('current'),
  status = get('status');
let files = [
    {
      path: 'java/ttcuz/plugins/MyPlugin.java',
      text: `package ttcuz.plugins;

import cat.ttcuz.framework.plugin.TtcuzPlugin;
import cat.ttcuz.framework.plugin.PluginSettings;

public class MyPlugin extends TtcuzPlugin {
    @Override public void onSettings(PluginSettings settings) {
        settings.action("Привет", () -> ttcuz().toast("Привет из плагина!"));
    }
}
`,
    },
  ],
  selected = 0;
function save() {
  if (files[selected]) files[selected].text = code.value;
}
function show() {
  current.replaceChildren(...files.map((f, i) => new Option(f.path, String(i))));
  current.value = String(selected);
  code.value = files[selected]?.text || '';
}
current.onchange = () => {
  save();
  selected = Number(current.value);
  code.value = files[selected].text;
};
get('files').onchange = async (event) => {
  save();
  const added = [];
  for (const file of event.target.files) {
    if (file.size > 240000 || !file.name.endsWith('.java')) {
      status.textContent = 'Нужны Java-файлы до 240 КБ';
      return;
    }
    added.push({ path: 'java/' + file.name, text: await file.text() });
  }
  if (added.length) {
    files = added;
    selected = 0;
    show();
  }
  event.target.value = '';
};
function project() {
  save();
  return {
    metadata: {
      id: get('plugin-id').value,
      name: get('name').value,
      author: get('author').value,
      version: get('version').value,
      description: get('description').value,
      entry: get('entry').value,
      api: 1,
    },
    files,
  };
}
get('editor').onsubmit = (event) => {
  event.preventDefault();
  const value = project();
  download(projectZip(value), value.metadata.id + '-source.zip');
  status.textContent = 'Проект скачан. Команда сборки — в разделе «API и сборка».';
};
get('submit').onclick = async () => {
  if (!get('editor').reportValidity()) return;
  const button = get('submit');
  button.disabled = true;
  status.textContent = 'Отправка…';
  try {
    const response = await fetch('/plugins/native/submit', {
      method: 'POST',
      headers: { 'content-type': 'application/json' },
      body: JSON.stringify(project()),
    });
    const body = await response.json();
    if (!response.ok) throw Error(body.error || 'Не удалось отправить');
    status.textContent =
      'Заявка ' +
      body.digest +
      '\nСтатус: ' +
      (body.status === 'pending' ? 'ожидает проверки' : body.status);
  } catch (error) {
    status.textContent = error.message;
  } finally {
    button.disabled = false;
  }
};
show();

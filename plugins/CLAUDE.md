# Плагины ttcuz — для ИИ-ассистентов

Актуальный Plugin API — **4**. Единственный источник правды: [docs/plugins.md](../docs/plugins.md)
(то же самое одним файлом с сигнатурами SDK: `server/cloudflare/public/plugin-api.md`, на сайте — `/plugin-api.md`).

- Сигнатуры, по которым компилируется плагин: `plugins/sdk/cat/ttcuz/framework/plugin/`.
- Настоящая реализация в моде: `inject/java/cat/ttcuz/framework/plugin/` и `PluginEngine`, `PluginKit`, `PluginPermissions`.
- Пример API 4: `examples/api4-tour`.
- После изменения docs/plugins.md или SDK: `python3 tools/sync_plugin_docs.py` (обновляет plugin-api.md и llms.txt),
  а SDK пересобирается в `assets/plugin-build/sdk.jar` (javac --release 8 по plugins/sdk, затем jar).

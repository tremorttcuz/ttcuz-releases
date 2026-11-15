# Плагины

Полный справочник Plugin API 4: **[docs/plugins.md](../docs/plugins.md)** (он же для ИИ-ассистентов: `https://ttcuz.ttcuz1337.workers.dev/plugin-api.md`). Сборка на ПК и в Termux: [docs/plugins-guide.md](../docs/plugins-guide.md).

Коротко:

```sh
python3 -m ttcuz.plugin examples/hello -o hello.ctt
```

- `sdk/` — сигнатуры API `cat.ttcuz.framework.plugin` для компиляции плагинов (в `.ctt` не попадает).
- `legacy/` — старый загрузчик MargyT (API `cat.narezany.margyt`), только для истории.
- Загрузчик, который работает в приложении: `inject/java/cat/ttcuz/framework/PluginEngine.java`.
- `../examples/hello` — простейший пример; `../examples/api4-tour` — возможности API 4.

Установка: настройки мода → «Плагины» → «Установить .ctt».

# Плагины ttcuz: как написать и собрать

Руководство для ПК (Windows, Linux, macOS) и для телефона (Termux). Опыт с Android не нужен: достаточно уметь открыть терминал и скопировать команду.

> Здесь только сборка и первый плагин. Полный справочник Plugin API **4** — все хуки, разрешения, события TikTok, плеер, селекторы, ошибки — в [plugins.md](plugins.md). Его же в одном файле читают ИИ-ассистенты: `https://ttcuz.ttcuz1337.workers.dev/plugin-api.md`.

- [Что такое плагин](#что-такое-плагин)
- [Сборка на ПК](#сборка-на-пк)
- [Сборка на телефоне (Termux)](#сборка-на-телефоне-termux)
- [Первый плагин с нуля](#первый-плагин-с-нуля)
- [manifest.json](#manifestjson)
- [Хуки (что плагин может перехватить)](#хуки)
- [Возможности мода (PluginContext)](#возможности-мода-plugincontext)
- [Правила и ограничения](#правила-и-ограничения)
- [Ошибки и их решение](#ошибки-и-их-решение)

---

## Что такое плагин

Плагин — это файл `.ctt`, то есть обычный zip ровно из трёх файлов:

```
my-plugin.ctt
├── manifest.json   описание: id, имя, версия, автор, главный класс
├── classes.dex     код плагина
└── icon.png        иконка (необязательно)
```

Код пишется на Java. Главный класс наследует `cat.ttcuz.framework.plugin.TtcuzPlugin` и переопределяет нужные хуки, например `onStart`. Сборщик `python -m ttcuz.plugin` сам компилирует Java, собирает dex и упаковывает `.ctt`.

> **Установка.** Настройки мода → «Плагины» → «Установить .ctt». Там же плагин включается, выключается и удаляется; «Магазин плагинов» и «Редактор плагинов» тоже в этом разделе.

> **Безопасность.** У плагина нет песочницы: он работает внутри TikTok со всеми его правами. Ставьте только то, что прочитали сами.

---

## Сборка на ПК

### 1. Что установить

| Что | Зачем | Как проверить |
|---|---|---|
| **JDK 17 или новее** | компиляция Java и d8 | `javac -version` |
| **Python 3.8+** | сам сборщик | `python3 --version` (в Windows: `python --version`) |
| Интернет при первом запуске | скачать `android.jar` и `r8.jar` (~80 МБ) | — |

Установка:

- **Windows:** JDK — [Adoptium Temurin 17](https://adoptium.net) (при установке отметьте «Set JAVA_HOME» и «Add to PATH»); Python — [python.org](https://www.python.org/downloads/) (отметьте «Add python.exe to PATH»).
- **Linux:** Debian/Ubuntu: `sudo apt install openjdk-17-jdk python3`. Arch: `sudo pacman -S jdk17-openjdk python`.
- **macOS:** `brew install openjdk@17 python`

### 2. Получить исходники

```bash
git clone https://github.com/<ваш-аккаунт>/ttcuz.git
cd ttcuz
```

Или распакуйте zip с исходниками и откройте терминал в получившейся папке.

### 3. Собрать пример

```bash
python3 -m ttcuz.plugin examples/hello
```

В Windows вместо `python3` пишите `python`.

Готовый файл: `examples/hello/ttcuz.hello.ctt`. Чтобы положить его в другое место, добавьте `-o путь/к/файлу.ctt`.

При первом запуске сборщик скачает инструменты в папку `tools/`, дальше они берутся оттуда. Другая папка для них: `--tools ПУТЬ`.

Нормальный вывод выглядит так:

```
==> Compiling the plugin API
==> Compiling Привет 1.1
==> One dex, without the API
==> Packing ttcuz.hello.ctt
    .../ttcuz.hello.ctt, 1781 bytes
```

---

## Сборка на телефоне (Termux)

### 1. Подготовка (один раз)

Установите **Termux из F-Droid**: версия из Google Play устарела и пакеты в ней не ставятся.

```sh
pkg update -y && pkg upgrade -y
pkg install -y python openjdk-17 git unzip
termux-setup-storage
```

`termux-setup-storage` попросит доступ к файлам, разрешите его. После этого «Загрузки» телефона доступны как `~/storage/downloads`.

### 2. Исходники (один раз)

С GitHub:

```sh
cd ~ && git clone https://github.com/<ваш-аккаунт>/ttcuz.git && cd ttcuz
```

Или из zip, скачанного в «Загрузки»:

```sh
cd ~ && unzip -q ~/storage/downloads/ttcuz-sticker-sheet-liquid-glass.zip && cd ttcuz-sticker-sheet
```

### 3. Сборка

```sh
python -m ttcuz.plugin examples/hello -o ~/storage/downloads/hello.ctt
```

Готовый `hello.ctt` появится в «Загрузках». Чтобы собрать свой плагин, удобно держать его папку там же:

```sh
python -m ttcuz.plugin ~/storage/downloads/my-plugin -o ~/storage/downloads/my-plugin.ctt
```

Советы для телефона:
- Первая сборка скачивает ~80 МБ, лучше делать её по Wi-Fi.
- Код удобно править в Acode или Markor прямо в папке «Загрузки».
- Если Termux закрывается в фоне посреди сборки, включите `termux-wake-lock` или держите его на экране.

---

## Первый плагин с нуля

### Шаг 1. Папки

```
my-plugin/
├── manifest.json
└── java/
    └── com/me/counter/
        └── Main.java
```

Путь в `java/` должен совпадать с пакетом класса: пакет `com.me.counter` лежит в папке `java/com/me/counter/`.

### Шаг 2. manifest.json

```json
{
  "id": "com.me.counter",
  "name": "Счётчик запусков",
  "version": "1.0",
  "author": "me",
  "description": "Считает запуски приложения",
  "entry": "com.me.counter.Main",
  "api": 4,
  "tiktok": "47.2.41"
}
```

### Шаг 3. Main.java

```java
package com.me.counter;

import android.app.Activity;

import cat.ttcuz.framework.plugin.PluginSettings;
import cat.ttcuz.framework.plugin.TtcuzPlugin;

public final class Main extends TtcuzPlugin {

    @Override
    public void onStart() {
        int starts = ttcuz().prefs().getInt("starts", 0) + 1;
        ttcuz().prefs().edit().putInt("starts", starts).apply();
        ttcuz().log("запуск №" + starts);
    }

    // Своя страница настроек плагина; значения попадают в prefs() под этими ключами.
    @Override
    public void onSettings(PluginSettings settings) {
        settings.toggle("greet", "Показывать приветствие", true);
    }

    @Override
    public void onActivityResumed(Activity activity) {
        // Вызывается на каждом экране: должно работать быстро.
        if (ttcuz().prefs().getBoolean("greet", true)
                && activity.getClass().getName().endsWith("MainActivity")) {
            ttcuz().toast("Это запуск №" + ttcuz().prefs().getInt("starts", 0));
            ttcuz().prefs().edit().putBoolean("greet", false).apply();
        }
    }
}
```

### Шаг 4. Сборка

```bash
python3 -m ttcuz.plugin my-plugin -o my-plugin.ctt
```

Иконку добавить просто: положите `icon.png` (квадрат, 128×128 достаточно) в папку плагина.

---

## manifest.json

| Поле | Обязательно | Что это |
|---|---|---|
| `id` | да | Имя плагина на диске и в настройках: латиница, цифры, `.`, `-`, `_`, начинается с буквы или цифры, до 128 символов. По нему плагин обновляется, поэтому не меняйте его между версиями. |
| `entry` | да | Полное имя главного класса, например `com.me.counter.Main`. |
| `name` | нет | Имя в списке (по умолчанию `id`). |
| `version` | нет | Строка рядом с именем (по умолчанию `1.0`). |
| `author`, `description` | нет | Автор и описание в списке. |
| `api` | нет | Версия Plugin API: `4` — текущая; `1`–`3` тоже ставятся. С API 2 нужен список `permissions` ([plugins.md, раздел 6](plugins.md#6-разрешения)). |
| `permissions` | нет | Что использует плагин: `["interface", "network", "watch"]` и т. д. |
| `tiktok` | нет | Версия TikTok, для которой плагин написан. Если указана, должна быть `47.2.41`. |

---

## Хуки (TtcuzPlugin)

Переопределяйте только нужные. Все остальные по умолчанию ничего не делают.

| Хук | Когда вызывается |
|---|---|
| `onLoad()` | Плагин загружен, до `onStart`. Может бросить исключение: тогда плагин не включится, а ошибка будет видна в списке. |
| `onStart()` / `onStop()` | Плагин включён / выключен (или приложение закрывается). |
| `onActivityCreated`, `onActivityResumed`, `onActivityPaused`, `onActivityDestroyed(Activity)` | Жизненный цикл экранов. `onActivityResumed` вызывается часто, должен работать быстро. |
| `onAccountChanged(String uid)` | Сменился аккаунт. |
| `onPreferenceChanged(String key, String value)` | Изменилась настройка плагина. |
| `onSettings(PluginSettings)` | Строится страница настроек плагина: `text`, `action`, `toggle`, `choice`, `slider`, `view`. |
| `boolean onOpenSettings(Activity)` | Пользователь открыл настройки плагина. Верните `true`, если показали своё окно вместо страницы. |
| `int onColour(int)` | Каждый перекрашиваемый цвет. Это горячий путь: здесь ничего не создавайте и ничего не логируйте. |
| `onView(View)` | Каждый view, который мод перекрашивает. |
| `String onRegion(String key, String value)` | Ответ о стране или операторе (`sim_country`, `network_operator` и т. д.). |
| `List onFeed(List posts)` | Страница ленты до отрисовки. Можно вернуть список без лишних постов. |
| `String onNickname(String uid, String name)` | Ник перед выводом на экран. |
| `CharSequence onText(TextView, CharSequence)` | Текст перед выводом во view. |
| `onEvent(String name, Object value)` | Событие от другого плагина (`ttcuz().emit`). |
| `onScreen(Activity, ViewGroup)` | Экран вышел на передний план и разложен (API 2, `interface`). |
| `onModScreen`, `onModPanel`, `onModSettings` | Экраны и настройки самого мода (API 3, `mod`). |
| `onTikTok(String event, Map data)` | Событие TikTok: видео, комментарии, сообщения, поиск, лента (API 4, `watch`). |

Какие разрешения нужны каждому хуку — в [plugins.md, раздел 8](plugins.md#8-хуки--полный-список).

Если хук выбросит исключение, ошибка запишется в журнал мода и появится в списке плагинов. TikTok при этом не упадёт.

---

## Возможности мода (PluginContext)

Доступ к моду — через `ttcuz()` внутри плагина.

- `app()` — контекст приложения; `screen()` — текущий экран или `null`.
- `prefs()`, `accountPrefs()` — свои настройки, общие или для текущего аккаунта.
- `bundle()` — распакованный `.ctt` (только чтение); `data()` — своя папка для файлов.
- `accountId()`, `accent()` — текущий аккаунт и цвет акцента.
- `hostLoader()`, `type(name)`, `method(type, name, params…)`, `field(type, name)` — доступ к классам TikTok через рефлексию.
- `log(line)` — строка в журнал мода; `toast(text)` — короткое сообщение.
- `ui(runnable)`, `later(runnable, ms)`, `away(runnable)` — главный поток, отложенный запуск, фоновый поток. Отложенное отменяется, когда плагин выключают.
- `emit(name, value)` — событие всем плагинам.
- API 2: `overlay`, `find`, `views`, `dialog`, `menu`, `http`, `notify`, `copy`/`paste`, `saveDownload`, `request`.
- API 3: `blur`, `motionBlur`, `shader`, `modSetting`/`setModSetting`.
- API 4: `query`/`tree`/`hide`/`inject` (селекторы), `click`/`tap`/`swipe`/`type`, `video`/`pause`/`play`/`speed`/`saveVideo`, `expose`/`call`, `every`.

Подробно с примерами — [plugins.md, раздел 9](plugins.md#9-plugincontext--что-даёт-мод). Полные сигнатуры: `plugins/sdk/cat/ttcuz/framework/plugin/`.

---

## Правила и ограничения

- `classes.dex` до 16 МиБ, `manifest.json` до 64 КиБ, `icon.png` до 1 МиБ. Сборщик проверяет это заранее.
- Только один dex: если код не помещается в один, сборщик откажется собирать.
- Классы `cat.ttcuz.framework.plugin.*` в ваш dex не попадают, их предоставляет приложение. Не кладите в плагин свои копии этих классов.
- Java 17 в исходниках разрешена; d8 переводит код под Android 7+ (min-api 24).
- Сторонние библиотеки (`.jar`) пока не поддерживаются: переносите нужный код в `java/` исходниками.

---

## Ошибки и их решение

| Сообщение | Причина и решение |
|---|---|
| `javac not found: install JDK 17 or newer` | Не установлен JDK или он не в PATH. Termux: `pkg install openjdk-17`. |
| `no manifest.json in …` | Неверная папка: укажите ту, где лежит `manifest.json`. |
| `manifest.json has no id, entry …` | В манифесте не хватает обязательных полей. |
| `api must be 1..4` / `tiktok '…'` | Плагин написан под другую версию API или TikTok. |
| `manifest.json is not valid JSON` | Ошибка синтаксиса: лишняя или пропущенная запятая, кавычки «ёлочки» вместо `"`. |
| `id 'a b': Latin letters, digits…` | В `id` недопустимые символы. |
| `entry 'Main' is not a fully qualified class name` | Нужно полное имя: `com.me.counter.Main`. |
| `the entry class … is not among the compiled classes` | Пакет в `.java` не совпадает с `entry` или с путём папки. |
| `no .java under …/java` | Исходники должны лежать в подпапке `java/`. |
| `icon.png is not a PNG` | Файл переименован из jpg или webp. Сохраните его настоящим PNG. |
| `javac failed` с текстом ошибки | Ошибка в коде: строка и причина показаны над сообщением. |
| Ошибка загрузки при первом запуске | Нет интернета или сайт недоступен. Повторите команду; если что-то повреждено, удалите папку `tools/`. |
| `OutOfMemoryError` (Termux) | Закройте другие приложения: d8 нужно несколько сотен МБ. |

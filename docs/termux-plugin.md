# Сборка плагина ttcuz в Termux

Короткая версия. Полное руководство: [plugins-guide.md](plugins-guide.md).

Плагин можно собрать прямо на телефоне. Нужны Termux, Python, JDK 17 и интернет при первом запуске.

## 1. Подготовка Termux (один раз)

Установите Termux из F-Droid: версия из Google Play устарела и пакеты в ней не ставятся.

```sh
pkg update -y && pkg upgrade -y
pkg install -y python openjdk-17 unzip
termux-setup-storage
```

`termux-setup-storage` спросит доступ к файлам, разрешите его. После этого папка «Загрузки» телефона доступна как `~/storage/downloads`.

Проверка:

```sh
python --version
javac -version
```

## 2. Исходники ttcuz (один раз)

Скопируйте архив проекта в «Загрузки» телефона и распакуйте его в домашнюю папку Termux:

```sh
cd ~
unzip -q ~/storage/downloads/ttcuz-sticker-sheet-liquid-glass.zip
cd ~/ttcuz-sticker-sheet
```

Сборщик плагинов использует только `ttcuz/`, `plugins/sdk/` и ваш плагин. Собирать APK на телефоне не нужно.

## 3. Сборка плагина

Пример из проекта:

```sh
cd ~/ttcuz-sticker-sheet
python -m ttcuz.plugin examples/hello -o ~/storage/downloads/hello.ctt
```

При первом запуске сборщик скачает в `tools/` два файла, дальше они берутся оттуда:

- `android.jar` (Android API 35, архив около 60 МБ);
- `r8.jar` (d8, около 17 МБ).

Если всё прошло успешно, в конце будет строка вида `…/hello.ctt, NNNN bytes`, а файл появится в «Загрузках».

## 4. Свой плагин

Структура папки:

```
my-plugin/
├── manifest.json      обязательно
├── java/…             исходники, например java/com/me/myplugin/Main.java
└── icon.png           необязательно, PNG до 1 МиБ
```

Минимальный `manifest.json`:

```json
{
  "id": "com.me.myplugin",
  "name": "Мой плагин",
  "version": "1.0",
  "author": "me",
  "entry": "com.me.myplugin.Main",
  "api": 1
}
```

- `id` — только латинские буквы, цифры, точки, `-` и `_`. Не меняйте его между версиями: по нему плагин обновляется.
- `entry` — полное имя класса, который наследует `cat.ttcuz.framework.plugin.TtcuzPlugin` (см. `examples/hello`).

Сборка:

```sh
python -m ttcuz.plugin ~/storage/downloads/my-plugin -o ~/storage/downloads/my-plugin.ctt
```

Сборщик сначала проверяет манифест, иконку и лимиты загрузчика (dex до 16 МиБ, манифест до 64 КиБ), и только потом компилирует.

## Если что-то не так

| Сообщение | Что делать |
|---|---|
| `javac not found` | `pkg install openjdk-17` |
| `manifest.json has no …` | В манифесте не хватает обязательного поля |
| `id …: Latin letters, digits…` | В `id` только латиница, цифры, `.`, `-`, `_` |
| `api must be 1` | Уберите `api` из манифеста или поставьте `1` |
| `the entry class … is not among the compiled classes` | Значение `entry` не совпадает с пакетом и именем класса в `java/` |
| Ошибка загрузки при первом запуске | Проверьте интернет и запустите команду ещё раз: загрузка продолжится в `tools/` |
| `java.lang.OutOfMemoryError` | Закройте другие приложения. Для небольшого плагина d8 хватает нескольких сотен МБ |

## Установка

Скопируйте `.ctt` на телефон и откройте настройки мода → «Плагины» → «Установить .ctt». Там же плагин включается, выключается и удаляется.

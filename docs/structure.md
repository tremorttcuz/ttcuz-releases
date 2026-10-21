# Карта исходников

## Android-мод

Java-классы: `inject/java/cat/ttcuz/framework/`. Имена пакета и публичных
методов сохранены: на них ссылаются патчи DEX и нативные хуки.
`inject/stubs/` нужен только при компиляции и не добавляется в APK.
Крупные экраны держат контроллер отдельно от своих элементов: фабрика значков,
отрисовка переключателей и каталог SVG-путей лежат в отдельных Java-классах.

| Подсистема | Основные классы |
| --- | --- |
| Запуск и настройки | `Ttcuz`, `TtcuzProvider`, `SettingsActivity`, `SettingsIconFactory`, `SettingsGlyph`, `SettingsGlyphPaths`, `SettingsSchema` |
| Сеть и обновления | `Net`, `Updater`, `UpdatePackage`, `Version` |
| Целостность | `AppIntegrity`, `IntegrityVerifier`, `IntegrityActivity` |
| Темы и цвета | `Themes`, `Accent`, `Palette`, `AppearanceColors`, `UiContrast` |
| Профиль и синхронизация | `AccountAppearance`, `CloudAppearance`, `ProfileStyle`, `TtcuzProfileSync` |
| Значки и аватары | `Badges`, `BadgeImage`, `Avatars`, `AvatarShape`, `CreatorCrown` |
| Навигация и анимации | `NavigationIsland`, `IslandTouch`, `VideoOpenTransition`, `Motion`, `SheetDialog` |
| Лента и видео | `Feed`, `FeedRail`, `FeedCaptions`, `PlayerTools`, `Hdr` |
| Скачивание | `Download`, `DownloadTasks`, `DownloadSheet`, `DownloadIsland` |
| Комментарии | `Comments`, `CommentSelection`, `OfflineComments`, `OfflineCommentArchive` |
| Шрифты и эмодзи | `Fonts`, `NicknameFont`, `EmojiText`, `EmojiPackManager` |
| Приватность и уведомления | `Privacy`, `NotificationStrings` |
| Фон комментариев | `CommentBackground`, `CommentMediaView` |
| Значки сторонних модов | `Badges` (MargyT), `TtyBadges` (ТТЮ) |
| Лайки историй | `StoryLikes`, `DateInfo`, `DateOverlay` |
| Диагностика | `Diary`, `Diagnostics`, `CrashConsent`, `CrashReports` |

`Baked`, `Version`, `BuildInfo`, `IntegrityKey` содержат данные конкретной
сборки. Их подготавливает сборщик; изменения ключей и идентификаторов
приложения требуют отдельного решения.

## Сборщик Python

Вход: `python -m ttcuz` → `ttcuz/__main__.py` → `build.py`.
`toolchain.py` получает внешние инструменты, `apkzip.py` работает с APK.

| Задача | Модули |
| --- | --- |
| Манифест и ресурсы | `manifest.py`, `axml.py`, `arsc.py`, `resadd.py` |
| Патчи и кеш DEX | `dexpatch.py`, `dexcache.py` |
| Проверенные хуки TikTok 47.2.41 | `privacy.py`, `comment_background.py`, `story_likes.py`, `profile_banner.py` |
| Цвета и графика | `accent.py`, `palette.py`, `prerecolour.py`, `artwork.py`, `png.py`, `vector.py` |
| Иконки и индикатор | `icon.py`, `icon_collection.py`, `loading.py` |
| Целостность | `integrity.py`, `SealSigner.java` |

## Сервер

Основная конфигурация — `server/cloudflare/wrangler.toml`.
Точка входа — `worker.mjs`; отдельные модули отвечают за профили,
оформление, устройства, релизы, отчёты и каталог плагинов.
Миграции D1 — `migrations/`, статические файлы — `public/`.

Старый `dashboard-worker.mjs` содержал дублирующий сервер, не являлся
точкой входа Wrangler и исключён из чистой копии.
`server/badges.py` — альтернативный Python-сервер;
его инструкция сохранена в `server/README.md`.

## Изменения

1. Найдите подсистему в таблице и прочитайте её вызывающий код.
2. Сохраняйте Java-методы, используемые `dexpatch.py`.
3. Исправляйте общую реализацию, если проблема затрагивает несколько экранов.
4. Запускайте проверки подсистемы и форматирование.
5. При выпуске обновления увеличивайте `VERSION`, сохраняя пакет и ключ подписи.

Строки Java и Smali внутри Python-тестов — входные данные проверок.
Они могут намеренно содержать компактный код. Форматировать содержимое
фикстур вместе с тестируемым поведением не следует.

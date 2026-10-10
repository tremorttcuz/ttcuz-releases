package cat.narezany.margyt;

import java.util.Locale;

/**
 * The words the mod puts on screen, in the phone's language where it has them.
 *
 * Three languages and a handful of lines: enough that the mod does not look
 * bolted onto a Russian phone in English, and small enough to keep in one file
 * rather than in string resources -- which this build cannot add.
 */
final class Text {

    private Text() {}

    // These two must stay above every pick(): static initialisers run top to bottom, and a pick()
    // that ran before them saw both as false and answered in English on a Russian phone.
    private static final boolean RU = "ru".equals(language()) || "be".equals(language());
    private static final boolean UK = "uk".equals(language());

    static final String INPUT_LIMITS=pick("Лимиты ввода","Ліміти введення","Input limits");
    static final String UNLIMITED_COMMENTS=pick("Комментарии без ограничения длины","Коментарі без обмеження довжини","Comments of any length");
    static final String UNLIMITED_REPOSTS=pick("Репосты без ограничения длины","Репости без обмеження довжини","Reposts of any length");
    static final String UNLIMITED_HASHTAGS=pick("Любое число хештегов в видео","Будь-яка кількість хештегів у відео","Any number of video hashtags");
    static final String INPUT_LIMITS_NOTE=pick("Изменение действует после повторного открытия поля ввода. Снимается только ограничение в самом приложении: сервер TikTok всё равно может не принять слишком длинный текст.","Зміна діє після повторного відкриття поля введення. Знімається лише обмеження в самому застосунку: сервер TikTok однаково може не прийняти надто довгий текст.","Takes effect after you reopen the input field. Only the app's own limit is lifted: TikTok's server may still refuse text that is too long.");

    static final String PINNED_FRIENDS=pick("Закреплённые друзья","Закріплені друзі","Pinned friends");
    static final String FRIEND_CANDIDATES=pick("Друзья","Друзі","Friends");
    static final String MOVE_UP=pick("Выше","Вище","Move up");
    static final String MOVE_DOWN=pick("Ниже","Нижче","Move down");
    static final String UNPIN=pick("Открепить","Відкріпити","Unpin");
    static final String PIN_LIMIT=pick("Можно закрепить до 20 друзей","Можна закріпити до 20 друзів","Pin up to 20 friends");
    static final String FRIENDS_LOAD_HINT=pick("Сначала откройте список друзей TikTok. Из него появятся доступные профили.","Спочатку відкрийте список друзів TikTok.","Open the TikTok friends list first to load available profiles.");
    static final String NOTHING_FOUND=pick("Ничего не найдено","Нічого не знайдено","No matches");
    static final String PROFILE_LAYOUT=pick("Вид профиля","Вигляд профілю","Profile layout");
    static final String LAYOUT_NATIVE=pick("Обычный","Звичайний","Original");
    static final String LAYOUT_COMPACT=pick("Компактный","Компактний","Compact");
    static final String LAYOUT_EXPANDED=pick("Расширенный","Розширений","Expanded");
    static final String MESSAGE_TIMES=pick("Время сообщений","Час повідомлень","Message timestamps");
    static final String NOTIFICATIONS=pick("Уведомления","Сповіщення","Notifications");
    static final String NOTIFY_MESSAGES=pick("Сообщения","Повідомлення","Messages");
    static final String NOTIFY_PROMOS=pick("Промоуведомления","Промосповіщення","Promotional notifications");
    static final String NOTIFY_OTHER=pick("Другие уведомления","Інші сповіщення","Other notifications");
    static final String NOTIFY_NO_PROMPTS=pick("Без напоминаний о разрешении","Без нагадувань про дозвіл","No permission reminders");
    static final String NOTIFY_SETTINGS=pick("Разрешение Android","Дозвіл Android","Android permission");
    static final String REPOST=pick("Репост","Репост","Repost");
    static final String REPOST_POSITION=pick("Расположение репоста","Розташування репосту","Repost placement");
    static final String REPOST_FAVORITE=pick("Вместо избранного","Замість обраного","Replace favorite");
    static final String REPOST_AVATAR=pick("Над аватаром","Над аватаром","Above avatar");
    static final String REPOST_FREE=pick("Рядом с избранным","Поруч з обраним","Beside favorite");
    static final String REPOST_NOT_READY=pick("Штатный репост недоступен","Штатний репост недоступний","Native repost is unavailable");
    static final String BLOCK_SUGGESTION=pick("Скрыть такие предложения","Приховати такі пропозиції","Hide this suggestion type");
    static final String VIDEO_ACTIONS=pick("Действия видео","Дії відео","Video actions");
    static final String VIDEO_NOT_READY=pick("Откройте видео в ленте","Відкрийте відео у стрічці","Open a feed video first");
    static final String SAVE_VIDEO=pick("Скачать видео","Завантажити відео","Download video");
    static final String SAVE_AUDIO=pick("Скачать аудио","Завантажити аудіо","Download audio");
    static final String SAVE_IMAGE=pick("Скачать фото","Завантажити фото","Download photo");
    static final String SAVE_FRAME=pick("Сохранить кадр","Зберегти кадр","Save current frame");
    static final String FRAME_NOT_AVAILABLE=pick("Не удалось получить кадр: поверхность недоступна","Не вдалося отримати кадр: поверхня недоступна","Cannot capture this player surface");
    static final String MEDIA_NOT_ALLOWED=pick("Автор ограничил скачивание","Автор обмежив завантаження","The author restricted downloads");
    static final String AUDIO_NOT_AVAILABLE=pick("В этом видео нет отдельной аудиодорожки","У цьому відео немає окремої аудіодоріжки","No separate audio track is available");
    static final String PHOTO_NOT_AVAILABLE=pick("Фото недоступно для скачивания","Фото недоступне для завантаження","Photo is unavailable");
    static final String MEDIA_FAILED=pick("Не удалось сохранить файл. Подробности в диагностике","Не вдалося зберегти файл. Подробиці в діагностиці","Could not save the file. See diagnostics");
    static final String DOWNLOADING=pick("Загрузка","Завантаження","Downloading");
    static final String PLAYER_SPEED=pick("Скорость удержания","Швидкість утримання","Hold speed");
    static final String CUSTOM_SPEED=pick("Своё значение","Власне значення","Custom value");
    static final String SPEED_RANGE=pick("Допустимая скорость: 0.5–3×","Допустима швидкість: 0.5–3×","Speed must be between 0.5 and 3×");
    static final String PLAYER_NOT_READY=pick("Плеер ещё не готов","Плеєр ще не готовий","The player is not ready yet");
    static final String SEEK_BACK=pick("Назад на выбранный шаг","Назад на вибраний крок","Seek back one step");
    static final String SEEK_FORWARD=pick("Вперёд на выбранный шаг","Уперед на вибраний крок","Seek forward one step");
    static final String SEEK_SMOOTH=pick("Плавно назад","Плавно назад","Smooth rewind");
    static final String SEEK_STEP=pick("Шаг перемотки, секунды","Крок перемотування, секунди","Seek step, seconds");
    static final String REVERSE_SPEED=pick("Скорость перемотки назад","Швидкість перемотування назад","Rewind rate");
    static final String BLOCK_AUTHOR=pick("Не предлагать этого автора","Не пропонувати цього автора","Hide this author");
    static final String BLOCK_LIMIT=pick("В списке уже 50 авторов","У списку вже 50 авторів","The list already contains 50 authors");
    static final String HIDE_STORIES=pick("Скрывать истории","Приховувати історії","Hide stories");
    static final String HIDE_SUGGESTIONS=pick("Скрывать карточки рекомендаций","Приховувати картки рекомендацій","Hide recommendation cards");
    static final String DL_QUALITY=pick("Качество загрузки","Якість завантаження","Download quality");
    static final String DL_NATIVE=pick("Штатное","Стандартна","Original");
    static final String DL_MAX=pick("Максимальное доступное","Максимальна доступна","Highest available");
    static final String DL_LOW=pick("Минимальное доступное","Мінімальна доступна","Lowest available");
    static final String DL_AUDIO=pick("Только звук","Лише звук","Audio only");
    static final String FRAME_QUALITY=pick("Размер кадра","Розмір кадру","Frame size");
    static final String VIDEO_ACTIONS_HINT=pick("Удерживайте «Отправить» на видео, чтобы открыть действия ttcuz.","Утримуйте «Надіслати» на відео, щоб відкрити дії ttcuz.","Hold Share on a video to open ttcuz actions.");
    static final String APPLY=pick("Применить","Застосувати","Apply");
    static final String RAIL=pick("Кнопки видео","Кнопки відео","Video buttons");
    static final String RAIL_LIKE=pick("Лайк","Вподобайка","Like");
    static final String RAIL_COMMENT=pick("Комментарии","Коментарі","Comments");
    static final String RAIL_FAV=pick("Избранное","Обране","Favourites");
    static final String RAIL_SHARE=pick("Отправить","Надіслати","Share");
    static final String COMMENT_COPY_SELECTED=pick("Копировать выделенное","Копіювати виділене","Copy selection");
    static final String COMMENT_SELECT_HINT=pick("Выделите нужный фрагмент текста","Виділіть потрібний фрагмент тексту","Select a text fragment first");
    static final String HIDDEN_ITEMS=pick("Что скрыто","Що приховано","Hidden items");
    static final String HIDDEN_ITEMS_NOTE=pick("Вернуть элементы в ленту","Повернути елементи до стрічки","Restore feed items");
    static final String HIDDEN_EMPTY=pick("Ничего не скрыто","Нічого не приховано","Nothing is hidden");
    static final String HIDDEN_RESTORE=pick("Вернуть","Повернути","Restore");
    static final String HIDDEN_RESTORE_ALL=pick("Вернуть всё","Повернути все","Restore all");
    static final String HIDDEN_NEXT_PAGE=pick("Изменения применятся при следующей загрузке ленты.","Зміни застосуються під час наступного завантаження стрічки.","Changes apply when the feed next loads.");

    static final String ROW = pick("Настройки ttcuz", "Налаштування ttcuz", "ttcuz settings");
    static final String UPDATE_UNAVAILABLE = pick(
            "Источник обновлений ttcuz ещё не настроен",
            "Джерело оновлень ttcuz ще не налаштовано",
            "The ttcuz update source is not configured yet");
    static final String DIAGNOSTICS = pick("Диагностика", "Діагностика", "Diagnostics");
    static final String COPY_DIAGNOSTICS = pick("Копировать диагностику",
            "Копіювати діагностику", "Copy diagnostics");
    static final String PROFILE_STYLE = pick("Оформление профиля",
            "Оформлення профілю", "Profile style");
    static final String APPEARANCE = pick("Внешний вид", "Зовнішній вигляд", "Appearance");
    static final String PROFILE_PREVIEW = pick("Так значок будет выглядеть у ника",
            "Так значок виглядатиме біля імені", "Badge preview beside nickname");
    static final String PROFILE_PREVIEW_NICK = pick("@ваш_ник", "@ваше_ім’я", "@your_name");
    static final String PROFILE_PREVIEW_EMPTY = pick("Реальный профиль TikTok",
            "Справжній профіль TikTok", "Your real TikTok profile");
    static final String PROFILE_PREVIEW_LOADING = pick(
            "Откройте свой профиль TikTok — имя и фото появятся здесь",
            "Відкрийте свій профіль TikTok — ім’я та фото з’являться тут",
            "Open your TikTok profile to load its real name and photo");
    static final String PROFILE_PREVIEW_REAL = pick("Предпросмотр вашего профиля TikTok",
            "Попередній перегляд вашого профілю TikTok",
            "Preview of your TikTok profile");
    static final String PROFILE_EYEDROPPER = pick(
            "Пипетка: коснитесь изображения значка, чтобы взять цвет",
            "Піпетка: торкніться зображення значка, щоб взяти колір",
            "Eyedropper: tap the badge image to sample a colour");
    static final String PROFILE_PICK_COLOUR = pick("Выберите цвет", "Виберіть колір", "Choose colour");
    static final String PROFILE_BADGE = pick("Значок у ника", "Значок біля імені",
            "Badge beside nickname");
    static final String PROFILE_BADGE_REMOVE = pick("Удалить значок", "Видалити значок",
            "Remove badge");
    static final String PROFILE_CROP_X = pick("Положение по горизонтали",
            "Положення по горизонталі", "Horizontal position");
    static final String PROFILE_CROP_Y = pick("Положение по вертикали",
            "Положення по вертикалі", "Vertical position");
    static final String PROFILE_CROP_ZOOM = pick("Масштаб значка",
            "Масштаб значка", "Badge scale");
    static final String PROFILE_GRADIENT = pick("Градиент ника", "Градієнт імені",
            "Nickname gradient");
    static final String PROFILE_START = pick("Первый цвет", "Перший колір", "First colour");
    static final String PROFILE_END = pick("Второй цвет", "Другий колір", "Second colour");
    static final String PROFILE_MIDDLE = pick("Центральный цвет", "Центральний колір", "Middle colour");
    static final String PROFILE_THREE = pick("Градиент из трёх цветов", "Градієнт із трьох кольорів", "Three-colour gradient");
    static final String PROFILE_DIRECTION = pick("Направление градиента", "Напрямок градієнта", "Gradient direction");
    static final String PROFILE_DIRECTION_RIGHT = pick("Слева направо", "Зліва направо", "Left to right");
    static final String PROFILE_DIRECTION_DOWN = pick("Сверху вниз", "Згори вниз", "Top to bottom");
    static final String PROFILE_DIRECTION_DIAGONAL = pick("По диагонали", "По діагоналі", "Diagonal");
    static final String PROFILE_DIRECTION_REVERSE = pick("Диагональ в обратную сторону", "Діагональ у зворотний бік", "Reverse diagonal");
    static final String PROFILE_BOLD = pick("Полужирный ник", "Напівжирне ім’я", "Bold nickname");
    static final String PROFILE_GLOW = pick("Мягкое свечение", "М’яке сяйво", "Subtle glow");
    static final String PROFILE_ANIMATED = pick("Анимировать градиент", "Анімувати градієнт", "Animate gradient");
    static final String PROFILE_SPEED = pick("Скорость градиента", "Швидкість градієнта", "Gradient speed");
    static final String PROFILE_SHARE = pick("Показывать стиль другим ttcuz",
            "Показувати стиль іншим ttcuz", "Share style with other ttcuz users");
    static final String PROFILE_SHARE_WAIT = pick(
            "Общий стиль станет доступен после подключения HTTPS-сервера ttcuz",
            "Спільний стиль стане доступним після підключення HTTPS-сервера ttcuz",
            "Shared style needs a ttcuz HTTPS service");
    static final String STORE_OFFLINE = pick("Магазин плагинов недоступен без HTTPS-сервера",
            "Магазин плагінів недоступний без HTTPS-сервера",
            "Plugin store needs an HTTPS service");
    static final String PROFILE_RESET = pick("Сбросить оформление", "Скинути оформлення",
            "Reset profile style");
    static final String PROFILE_BADGE_FAILED = pick("Не удалось прочитать изображение",
            "Не вдалося прочитати зображення", "Could not read the image");
    static final String THEME_PRESET_DARK = pick("Классический пресет",
            "Класичний пресет", "Classic preset");
    static final String THEME_PRESET_BLUE = pick("Синий пресет",
            "Синій пресет", "Blue preset");
    static final String THEME_RESET = pick("Сбросить к теме TikTok",
            "Скинути до теми TikTok", "Reset to TikTok theme");
    static final String THEME_PRESETS = pick("Готовые темы", "Готові теми", "Theme presets");
    static final String THEME_PRESETS_NOTE = pick("Классическая, синяя или исходная тема TikTok",
            "Класична, синя або початкова тема TikTok",
            "Classic, blue or original TikTok theme");
    static final String OPEN_TTCUZ_SITE = pick("Инструкции на сайте ttcuz",
            "Інструкції на сайті ttcuz", "Instructions on the ttcuz site");
    static final String THEME_COLOURS = pick("Цвета интерфейса",
            "Кольори інтерфейсу", "Interface colours");
    static final String THEME_COLOURS_NOTE = pick(
            "Цвета сохраняются отдельно для светлой и тёмной темы. Применение зависит от имён элементов в этой версии TikTok.",
            "Кольори зберігаються окремо для світлої й темної теми. Застосування залежить від назв елементів у цій версії TikTok.",
            "Colours are saved separately for light and dark mode. Applying them depends on view names in this TikTok version.");

    static final String REGION = pick("Регион", "Регіон", "Region");

    static final String CHANGE_REGION = pick(
            "Менять регион", "Змінювати регіон", "Change the region");

    static final String COUNTRY = pick("Страна", "Країна", "Country");

    static final String ACCENT = pick("Акцент", "Акцент", "Accent");

    static final String ACCENT_COLOUR = pick("Акцент", "Акцент", "Accent");
    static final String LIKE_COLOUR = pick("Цвет лайков", "Колір вподобань", "Like colour");
    static final String LIKE_RESET = pick("Цвет TikTok по умолчанию", "Типовий колір TikTok",
            "TikTok default colour");
    static final String CUSTOM_COLOUR = pick("Свой цвет #RRGGBB",
            "Свій колір #RRGGBB", "Custom colour #RRGGBB");
    static final String ACCENT_PICKER = pick("Выберите цвет акцента",
            "Виберіть колір акценту", "Choose accent colour");

    static final String ACCENT_NOTE = pick(
            "Меняет розовый, которым TikTok рисует лайки, кнопки и вкладки. "
                    + "Часть значков нарисована картинками — их цвет задаётся при сборке "
                    + "и здесь не меняется.",
            "Змінює рожевий, яким TikTok малює лайки, кнопки та вкладки. "
                    + "Частина значків намальована картинками — їхній колір задається "
                    + "під час збірки і тут не змінюється.",
            "Changes the pink TikTok draws likes, buttons and tabs with. Some icons "
                    + "are pictures rather than code; their colour is settled at build "
                    + "time and does not follow.");

    /** The bar at the foot of the screen: one line, not a paragraph. */
    static final String RESTART_PENDING = pick(
            "Нужен перезапуск",
            "Потрібен перезапуск",
            "Restart needed");

    static final String RESTART = pick("Перезапустить", "Перезапустити", "Restart");

    // ------------------------------------------------ what TikTok ships off

    static final String HIDDEN = pick("Анти A/B", "Анти A/B", "Anti A/B");

    static final String HIDDEN_NOTE = pick(
            "Функции у TikTok уже написаны, но выдаются случайной части людей. "
                    + "Здесь они включаются всем.",
            "Функції в TikTok уже написані, але видаються випадковій частині людей. "
                    + "Тут вони вмикаються всім.",
            "TikTok has written these already and hands them to a random share of "
                    + "people. Here they are switched on for everyone.");

    static final String CLOSE = pick("Понятно", "Зрозуміло", "Got it");

    static final String SAVE_STICKER = pick(
            "Скачать стикер", "Завантажити стікер", "Download the sticker");

    static final String STICKER_FAILED = pick(
            "Не получилось сохранить стикер", "Не вдалося зберегти стікер",
            "Could not save the sticker");

    static final String SAVE_AVATARS_ON = pick(
            "Кнопка на аватарках", "Кнопка на аватарках", "The button on avatars");

    static final String SAVE_STICKERS_ON = pick(
            "Кнопка на стикерах", "Кнопка на стікерах", "The button on stickers");

    static final String COMMENTS = pick("Комментарии", "Коментарі", "Comments");
    static final String COMMENT_COPY_ON = pick("Копирование комментариев", "Копіювання коментарів", "Copy comments");
    static final String COMMENT_COPY = pick("Скопировать комментарий", "Скопіювати коментар", "Copy comment");
    static final String COMMENT_SELECT = pick("Выбрать фрагмент", "Вибрати фрагмент", "Select part");
    static final String COMMENT_SELECT_COPY = pick("Выделить и копировать", "Виділити й копіювати", "Select and copy");
    static final String COMMENT_TIKTOK = pick("Действия TikTok", "Дії TikTok", "TikTok actions");
    static final String COMMENT_COPIED = pick("Комментарий скопирован", "Коментар скопійовано", "Comment copied");

    static final String BADGES_OF_ACCOUNT = pick("Все значки аккаунта", "Усі значки акаунта",
            "All badges of this account");
    static final String BADGES_ON = pick("Значки", "Значки", "Badges");

    static final String SAVE_AVATAR = pick(
            "Сохранить аватарку", "Зберегти аватарку", "Save the avatar");

    static final String SAVED = pick("Сохранено", "Збережено", "Saved");

    static final String AVATAR_NOTHING = pick(
            "Нечего сохранять — откройте аватарку сначала",
            "Нема чого зберігати — відкрийте аватарку спершу",
            "Nothing to save yet -- open an avatar first");

    static final String AVATAR_FAILED = pick(
            "Не получилось сохранить", "Не вдалося зберегти", "Could not save it");

    static final String BADGE_OWNER = pick(
            "Автор исходного проекта", "Автор вихідного проєкту",
            "Author of the original project");

    static final String VIDEO = pick("Видео", "Відео", "Video");

    static final String THEME = pick("Тема", "Тема", "Theme");

    static final String THEME_ON = pick(
            "Своя тема", "Своя тема", "A theme of your own");

    static final String THEME_MATERIAL = pick(
            "Цвета с обоев", "Кольори зі шпалер", "Colours from the wallpaper");

    static final String THEME_TEXT = pick("Текст", "Текст", "Text");

    static final String THEME_BACKGROUND = pick("Фон", "Тло", "Background");

    static final String THEME_NOTE = pick(
            "Перекрашивается только то, что TikTok и сам перекрашивает при "
                    + "смене светлой темы на тёмную. Акцент живёт отдельно.",
            "Перефарбовується лише те, що TikTok і сам перефарбовує при зміні "
                    + "світлої теми на темну. Акцент живе окремо.",
            "Only what TikTok itself repaints when you switch between light and "
                    + "dark. The accent is its own thing.");

    static final String BACKGROUND = pick(
            "Играть в фоне", "Грати у фоні", "Play in the background");

    static final String AUTOSCROLL = pick(
            "Автопрокрутка ленты", "Автопрокрутка стрічки", "Scroll the feed by itself");

    static final String SOUND = pick(
            "Звук, снятый по копирайту", "Звук, знятий за копірайтом",
            "Sound pulled for copyright");

    static final String SEEKBAR = pick(
            "Перемотка на всех видео", "Перемотка на всіх відео",
            "The scrubbing bar everywhere");

    static final String VOICE = pick(
            "Голосовые комментарии", "Голосові коментарі", "Voice comments");

    static final String THEME_STRENGTH = pick(
            "Насыщенность фона", "Насиченість тла", "How much of that background");

    static final String THEME_STRENGTH_NOTE = pick(
            "Слева — почти чёрный с оттенком выбранного цвета, справа — сам цвет.",
            "Ліворуч — майже чорний з відтінком обраного кольору, праворуч — сам колір.",
            "To the left, near black with a hint of the colour; to the right, the colour.");

    static final String ACCENT_WALLPAPER = pick(
            "Взять цвет с обоев", "Взяти колір зі шпалер", "Take the colour from the wallpaper");

    // --------------------------------------------------------------- fonts

    static final String FONT = pick("Шрифт", "Шрифт", "Typeface");

    static final String FONT_SYSTEM = pick("Системный", "Системний", "The system one");
    static final String FONT_SANS = pick("Обычный", "Звичайний", "Sans");
    static final String FONT_SANS_LIGHT = pick("Тонкий", "Тонкий", "Light");
    static final String FONT_SANS_CONDENSED = pick("Узкий", "Вузький", "Condensed");
    static final String FONT_SERIF = pick("С засечками", "Із засічками", "Serif");
    static final String FONT_MONOSPACE = pick("Моноширинный", "Моноширинний", "Monospace");
    static final String FONT_CURSIVE = pick("Рукописный", "Рукописний", "Cursive");
    static final String FONT_FILE = pick("Свой файл", "Свій файл", "A file of your own");

    static final String FONT_PICK = pick(
            "Выбрать .ttf или .otf", "Обрати .ttf або .otf", "Pick a .ttf or .otf");

    static final String FONT_FAILED = pick(
            "Не получилось прочитать шрифт", "Не вдалося прочитати шрифт",
            "That file is not a font this phone can read");

    static final String EMOJI = pick("Шрифт эмодзи", "Шрифт емодзі", "Emoji");

    static final String EMOJI_SYSTEM = pick("Системные", "Системні", "The system ones");
    static final String EMOJI_TWEMOJI = pick("Twemoji", "Twemoji", "Twemoji");
    static final String EMOJI_NOTO = pick("Noto", "Noto", "Noto");
    static final String EMOJI_BLOB = pick("Blobmoji", "Blobmoji", "Blobmoji");
    static final String EMOJI_FILE = pick("Свой файл", "Свій файл", "A file of your own");
    static final String EMOJI_PREVIEW = "😀  🐱  🫶  👩🏽‍💻  🇷🇺  1️⃣";

    static final String EMOJI_NOTE = pick(
            "Все паки уже внутри мода, скачивать ничего не нужно. Нужен Android 10 "
                    + "и выше: ниже него меняются только буквы.",
            "Усі паки вже всередині мода, завантажувати нічого не треба. Потрібен "
                    + "Android 10 і вище: нижче змінюються лише літери.",
            "Every pack is already inside the mod; nothing is fetched. Android 10 "
                    + "and up: below that only the letters change.");

    static final String EMOJI_READY = pick(
            "Готово — перезапусти приложение", "Готово — перезапусти застосунок",
            "Done -- restart the app");

    // ---------------------------------------------------------- the icon

    static final String ICON = pick("Иконка", "Іконка", "The icon");


    static final String ICON_NOTE = pick("Иконка на экране приложений сменится, когда вы свернёте приложение.", "Іконка на екрані застосунків зміниться, коли ви згорнете застосунок.", "The launcher icon changes once you leave the app.");
    static final String ICON_DEFAULT = "ttcuz";

    static final String STREAK_TEST = pick(
            "Отправить тестовое сообщение", "Надіслати тестове повідомлення",
            "Send a test message");

    static final String STREAK_TEST_NOTE = pick(
            "Всем, у кого есть серия — даже если огонёк горит. Результат в дневнике.",
            "Усім, у кого є серія — навіть якщо вогник горить. Результат у щоденнику.",
            "To everyone with a streak, lit or not. The diary says what happened.");

    static final String STREAK_TEST_GOING = pick(
            "Отправляю, смотри дневник", "Надсилаю, дивись щоденник",
            "Sending; the diary will say");

    static final String STREAK_ALL = pick(
            "Отправить огонёк всем", "Надіслати вогник усім", "Send the flame to everyone");

    static final String STREAK_ALL_NOTE = pick(
            "Выбранный стикер уйдёт всем, с кем у тебя есть серия, прямо сейчас",
            "Вибраний стікер піде всім, з ким у тебе є серія, просто зараз",
            "The chosen sticker goes to everyone you have a streak with, right now");

    static final String STREAK_ALL_ASK = pick(
            "Выбранный стикер будет отправлен всем, с кем у тебя есть серия, — и тем, у кого огонёк уже горит. Это настоящие сообщения.",
            "Вибраний стікер буде надіслано всім, з ким у тебе є серія, — і тим, у кого вогник уже горить. Це справжні повідомлення.",
            "The chosen sticker will be sent to everyone you have a streak with, including lit ones. These are real messages.");

    static final String STREAK_ALL_SEND = pick("Отправить", "Надіслати", "Send");

    static final String STREAK_ALL_NO_STICKER = pick(
            "Сначала выбери стикер для серий (текст пока недоступен)",
            "Спочатку обери стікер для серій (текст поки недоступний)",
            "Choose a streak sticker first (text is not available yet)");

    static final String STREAK_ALL_NO_CHATS = pick(
            "Сначала открой список чатов TikTok, чтобы мод увидел серии",
            "Спочатку відкрий список чатів TikTok, щоб мод побачив серії",
            "Open the TikTok chat list first so the mod can see your streaks");

    static final String STREAK_ALL_NONE = pick(
            "Серий не найдено", "Серій не знайдено", "No streaks found");

    static final String STREAK_ALL_BUSY = pick(
            "Уже отправляю…", "Уже надсилаю…", "Already sending…");

    static final String STREAK_ALL_DONE = pick(
            "Отправлено: %1$d из %2$d", "Надіслано: %1$d з %2$d", "Sent: %1$d of %2$d");

    static final String CAT_FROM = pick("Принёс", "Приніс", "Brought by");

    static final String CAT_YOURS = pick(
            "Хочешь своего кота здесь? Напиши @narezany в Telegram",
            "Хочеш свого кота тут? Напиши @narezany в Telegram",
            "Want your cat here? Write to @narezany on Telegram");

    static final String CAT_WAIT = pick(
            "Коты ещё едут, попробуй ещё раз", "Коти ще їдуть, спробуй ще раз",
            "The cats are still on their way; try again");

    static final String HIDE_LIVE = pick(
            "Скрыть трансляции", "Сховати трансляції", "Hide live rooms");

    static final String HIDE_PHOTOS = pick(
            "Скрыть фото-посты", "Сховати фото-пости", "Hide slideshows");

    static final String DIM = pick(
            "Анти-выгорание", "Анти-вигоряння", "Stop the screen burning in");

    static final String DIM_HOW = pick(
            "Насколько приглушить", "Наскільки приглушити", "How far down");

    static final String DIM_EYE_ON = pick(
            "Прозрачность включена. Нажмите, чтобы выключить",
            "Прозорість увімкнена. Натисніть, щоб вимкнути",
            "Dimming on. Tap to turn off");

    static final String DIM_EYE_OFF = pick(
            "Прозрачность выключена. Нажмите, чтобы включить",
            "Прозорість вимкнена. Натисніть, щоб увімкнути",
            "Dimming off. Tap to turn on");

    static final String DIM_SHIFT = pick(
            "Плавно сдвигать кнопки", "Плавно зсувати кнопки", "Slowly drift controls");

    static final String DIM_SHIFT_SIZE = pick(
            "Насколько сдвигать, dp", "Наскільки зсувати, dp", "How far to drift, dp");

    static final String DIM_SECTION = pick(
            "Защита экрана", "Захист екрана", "Screen protection");

    static final String DIM_NOTE = pick(
            "Приглушение делает панели, кнопки, подпись и индикаторы видео и слайд-шоу тусклее, чтобы экран OLED не выгорал. "
                    + "Фото и видео не меняются, кнопки нажимаются так же. Глаз в ленте включает и выключает приглушение на лету.",
            "Приглушення робить панелі, кнопки, підпис та індикатори відео й слайд-шоу тьмянішими, щоб екран OLED не вигорав. "
                    + "Фото та відео не змінюються, кнопки натискаються так само. Око в стрічці вмикає й вимикає приглушення на льоту.",
            "Dimming makes video and slideshow panels, controls, captions and indicators fainter so an OLED screen does not burn in. "
                    + "Photos and videos stay unchanged and the controls work exactly as before. The eye in the feed turns dimming on and off instantly.");

    // ------------------------------------------------------ the texture packs

    static final String TEXTURES = pick("Текстурпаки", "Текстурпаки", "Texture packs");

    static final String TEXTURES_ON = pick(
            "Включить текстурпак", "Увімкнути текстурпак", "Use a texture pack");

    static final String TEXTURES_EXPORT = pick(
            "Выгрузить картинки", "Вивантажити картинки", "Write the pictures out");

    static final String TEXTURES_EXPORT_XML = pick(
            "Выгрузить вместе с xml", "Вивантажити разом з xml",
            "Write them out with the xml");

    static final String TEXTURES_EXPORT_XML_NOTE = pick(
            "Тяжелее и почти всё там не правится руками",
            "Важче, і майже все там не редагується руками",
            "Heavier, and most of it cannot be edited by hand");

    static final String TEXTURES_EXPORT_NOTE = pick(
            "Файл .cuztx со всеми картинками сборки, в Downloads/ttcuz",
            "Файл .cuztx з усіма картинками збірки, у Downloads/ttcuz",
            "A .cuztx of every picture in this build, into Downloads/ttcuz");

    static final String TEXTURES_EXPORTING = pick(
            "Собираю текстуры", "Збираю текстури", "Writing the textures out");

    static final String TEXTURES_EXPORTED = pick(
            "Готово, файл в Downloads/ttcuz", "Готово, файл у Downloads/ttcuz",
            "Done -- the file is in Downloads/ttcuz");

    static final String TEXTURES_INSTALL = pick(
            "Загрузить текстурпак", "Завантажити текстурпак", "Add a texture pack");

    static final String TEXTURES_INSTALL_NOTE = pick(
            "файл .cuztx с папкой res внутри", "файл .cuztx з текою res усередині",
            "a .cuztx with a res folder in it");

    static final String TEXTURES_FAILED = pick(
            "В этом файле нет картинок из res", "У цьому файлі немає картинок з res",
            "That file has nothing under res in it");

    static final String TEXTURES_DOCS = pick(
            "Как сделать текстурпак", "Як зробити текстурпак",
            "How to make a texture pack");

    static final String TEXTURES_DOCS_NOTE = pick(
            "Инструкции на сайте ttcuz", "Інструкції на сайті ttcuz",
            "Instructions on the ttcuz site");

    static final String TEXTURES_WRONG_VERSION = pick(
            "Для другой версии TikTok", "Для іншої версії TikTok",
            "For a different TikTok");

    static final String TEXTURES_NONE = pick(
            "Пока ни одного", "Поки жодного", "None yet");

    static final String TEXTURES_NOTE = pick(
            "Пак сделан под конкретную версию TikTok. На другой версии часть "
                    + "картинок просто не подставится — ничего не сломается.",
            "Пак зроблено під конкретну версію TikTok. На іншій версії частина "
                    + "картинок просто не підставиться — нічого не зламається.",
            "A pack is drawn against one version of TikTok. On another, some "
                    + "pictures simply are not swapped -- nothing breaks.");

    // ------------------------------------------------------------ own badges

    static final String MINE = pick("Мои значки", "Мої значки", "My badges");

    static final String MINE_NOTE = pick(
            "Порядок и что показывать. Сохраняется на сервере, видно всем.",
            "Порядок і що показувати. Зберігається на сервері, видно всім.",
            "The order, and which to show. Kept on the server, seen by everyone.");

    static final String MINE_NONE = pick(
            "У этого аккаунта пока нет значков", "У цього акаунта поки немає значків",
            "This account has no badges yet");

    static final String MINE_WAIT = pick("Спрашиваю сервер…", "Питаю сервер…",
            "Asking the server...");

    static final String MINE_SAVE = pick(
            "Сохранить значки", "Зберегти значки", "Save my badges");

    static final String MINE_SAVED = pick("Сохранено", "Збережено", "Saved");

    static final String MINE_TOO_OFTEN = pick(
            "Не чаще раза в минуту", "Не частіше разу на хвилину",
            "Once a minute at most");

    static final String FREE_BADGE = pick(
            "Бесплатный значок олда!", "Безкоштовний значок олда!",
            "A free badge for the old lot");

    static final String FREE_BADGE_TEXT = pick(
            "Бирюзовый значок раннего участника исходного проекта. "
                    + "После 20-го его больше не выдадут — ни за что.",
            "Бірюзовий значок раннього учасника вихідного проєкту. "
                    + "Після 20-го його більше не видадуть — ні за що.",
            "A turquoise badge that says you were here before the 20th of "
                    + "September. After that nobody gets one, at any price.");

    static final String FREE_BADGE_TAKE = pick("Забрать", "Забрати", "Take it");

    static final String FREE_BADGE_GOT = pick(
            "Значок твой", "Значок твій", "It is yours");

    static final String ALWAYS_DATE = pick(
            "Дата под каждым видео", "Дата під кожним відео",
            "The date on every video");

    static final String ALWAYS_DATE_NOTE = pick(
            "Тикток показывает её только если открыть видео с профиля автора.",
            "Тікток показує її лише якщо відкрити відео з профілю автора.",
            "TikTok shows it only when the video was opened from a profile.");

    // ------------------------------------------------------ the plugin store

    static final String STORE = pick("Магазин плагинов", "Магазин плагінів",
            "The plugin store");

    static final String STORE_OPEN = pick("Открыть магазин", "Відкрити магазин",
            "Open the store");

    static final String STORE_WAIT = pick("Смотрю, что есть…", "Дивлюсь, що є…",
            "Looking...");

    static final String STORE_GET = pick("Поставить", "Поставити", "Get it");
    static final String STORE_ANYWAY = pick("Всё равно", "Все одно", "Anyway");
    static final String STORE_GETTING = pick("Качаю плагин", "Завантажую плагін",
            "Fetching the plugin");
    static final String STORE_GOT = pick("Готово, перезапусти приложение",
            "Готово, перезапусти застосунок", "Done -- restart the app");
    static final String STORE_FAILED = pick("Не вышло", "Не вийшло", "That did not work");

    static final String STORE_WRONG_VERSION = pick(
            "Для другой версии TikTok", "Для іншої версії TikTok",
            "For a different TikTok");

    static final String STORE_NOTE = pick(
            "Плагины загружает владелец мода. Ставь только то, чему доверяешь: "
                    + "плагин работает внутри тиктока с твоим аккаунтом.",
            "Плагіни завантажує власник мода. Став лише те, чому довіряєш: "
                    + "плагін працює всередині тіктока з твоїм акаунтом.",
            "Plugins are put there by whoever runs the mod. Install what you trust: "
                    + "a plugin runs inside TikTok with your account.");

    static final String SAVE = pick("Сохранить", "Зберегти", "Save");

    // -------------------------------------------------------- the downloads

    static final String DOWNLOADS = pick("Скачивание", "Завантаження", "Downloads");

    static final String SEARCH = pick("Найти", "Знайти", "Search");
    static final String SEARCH_CLEAR = pick("Очистить", "Очистити", "Clear");
    static final String SEARCH_HINT = pick("Поиск по настройкам", "Пошук у налаштуваннях", "Search settings");
    static final String SEARCH_RESULTS = pick("Результаты поиска", "Результати пошуку", "Search results");
    static final String SEARCH_NONE = pick("Ничего не найдено", "Нічого не знайдено", "Nothing found");
    static final String SEARCH_REGION = pick("Страна и регион", "Країна та регіон", "Country and region");
    static final String SEARCH_ACCENT = pick("Цвет, палитра, сердечко", "Колір, палітра, сердечко", "Colour, palette, heart");
    static final String SEARCH_THEME = pick("Светлая, тёмная и Material-тема", "Світла, темна та Material-тема", "Light, dark and Material theme");
    static final String SEARCH_FEED = pick("Реклама, трансляции и фото-посты", "Реклама, трансляції та фото-пости", "Ads, live rooms and slideshows");
    static final String SEARCH_VIDEO = pick("Звук, перемотка, дата и anti-burn", "Звук, перемотування, дата та anti-burn", "Sound, seek, date and anti-burn");
    static final String SEARCH_DOWNLOADS = pick("Видео, аватар и стикеры", "Відео, аватар і стікери", "Video, avatar and stickers");
    static final String SEARCH_COMMENTS = pick("Копирование целиком и выбор фрагмента", "Копіювання цілком і вибір фрагмента", "Copy all or select a part");
    static final String SEARCH_BACKUP = pick("Импорт, экспорт и сброс", "Імпорт, експорт і скидання", "Import, export and reset");
    static final String SEARCH_TEXTURES = pick("Установить и экспортировать текстурпак", "Встановити й експортувати текстурпак", "Install and export texture pack");
    static final String SEARCH_FONT = pick("Шрифт и эмодзи", "Шрифт та емодзі", "Font and emoji");

    static final String BACKUP = pick("Резервная копия", "Резервна копія", "Backup");
    static final String BACKUP_EXPORT = pick("Экспортировать настройки", "Експортувати налаштування", "Export settings");
    static final String BACKUP_EXPORT_NOTE = pick("JSON без аккаунта, токенов и кэша", "JSON без акаунта, токенів і кешу", "JSON without account, tokens or cache");
    static final String BACKUP_IMPORT = pick("Импортировать настройки", "Імпортувати налаштування", "Import settings");
    static final String BACKUP_IMPORT_NOTE = pick("Сначала покажем, сколько параметров будет изменено", "Спочатку покажемо, скільки параметрів буде змінено", "Shows how many settings will change first");
    static final String BACKUP_APPLY = pick("Применить", "Застосувати", "Apply");
    static final String BACKUP_READING = pick("Читаю резервную копию…", "Читаю резервну копію…", "Reading backup…");
    static final String BACKUP_WRITING = pick("Сохраняю резервную копию…", "Зберігаю резервну копію…", "Writing backup…");
    static final String BACKUP_DONE = pick("Готово", "Готово", "Done");
    static final String BACKUP_FAILED = pick("Не удалось обработать резервную копию", "Не вдалося обробити резервну копію", "Could not process backup");
    static final String BACKUP_RESET = pick("Сбросить категорию", "Скинути категорію", "Reset a category");
    static final String BACKUP_RESET_NOTE = pick("Внешний вид, плеер, лента или все настройки мода", "Вигляд, плеєр, стрічка або всі налаштування мода", "Appearance, player, feed or every mod setting");
    static final String BACKUP_RESET_ASK = pick("Аккаунт, токены, история и кэш не затрагиваются.", "Акаунт, токени, історія та кеш не зачіпаються.", "Account, tokens, history and cache are untouched.");
    static final String RESET_APPEARANCE = pick("Внешний вид", "Вигляд", "Appearance");
    static final String RESET_PLAYER = pick("Плеер и скачивание", "Плеєр і завантаження", "Player and downloads");
    static final String RESET_FEED = pick("Лента и регион", "Стрічка та регіон", "Feed and region");
    static final String RESET_ALL = pick("Все настройки мода", "Усі налаштування мода", "Every mod setting");

    static String backupPreview(int count) {
        return pick("Будут изменены " + count + " настроек. Аккаунт, токены, история и кэш не затрагиваются.",
                "Буде змінено " + count + " налаштувань. Акаунт, токени, історія та кеш не зачіпаються.",
                count + " settings will change. Account, tokens, history and cache are untouched.");
    }

    static final String NO_WATERMARK = pick(
            "Без водяного знака", "Без водяного знака", "Without the watermark");

    static final String DOWNLOAD_ALWAYS = pick(
            "Сохранять можно всё", "Зберігати можна все", "Save anything");

    static final String FEED = pick("Лента", "Стрічка", "Feed");

    static final String HIDE_ADS = pick(
            "Убирать рекламу", "Прибирати рекламу", "Drop the advertisements");

    // --------------------------------------------------------- the plugins

    static final String PLUGINS = pick("Плагины", "Плагіни", "Plugins");

    static final String PLUGIN_INSTALL = pick(
            "Установить плагин", "Встановити плагін", "Install a plugin");

    static final String PLUGIN_INSTALL_NOTE = pick(
            "Файл .ctt", "Файл .ctt", "An .ctt file");

    static final String PLUGIN_NONE = pick(
            "Пока ничего не установлено.",
            "Поки нічого не встановлено.",
            "Nothing installed yet.");

    static final String PLUGIN_WARNING = pick(
            "Песочницы нет. Ставьте только то, чему доверяете.",
            "Пісочниці немає. Встановлюйте лише те, чому довіряєте.",
            "No sandbox. Install only what you trust.");

    static final String PLUGIN_DOCS = pick(
            "Как писать плагины", "Як писати плагіни", "Writing plugins");

    static final String PLUGIN_DOCS_NOTE = pick(
            "Инструкции на сайте ttcuz", "Інструкції на сайті ttcuz",
            "Instructions on the ttcuz site");

    static final String PLUGIN_INSTALLED = pick(
            "Плагин установлен", "Плагін встановлено", "Plugin installed");

    static final String PLUGIN_REMOVE = pick("Удалить", "Видалити", "Remove");

    static final String PLUGIN_REMOVE_ASK = pick(
            "Удалить плагин?", "Видалити плагін?", "Remove the plugin?");

    static final String CANCEL = pick("Отмена", "Скасувати", "Cancel");

    static final String PLUGIN_HOLD = pick(
            "Долгое нажатие — удалить",
            "Довге натискання — видалити",
            "Hold to remove");

    // ----------------------------------------------------------- the links

    static final String LINKS = pick("Ссылки", "Посилання", "Links");

    static final String CHANNEL = pick("Канал", "Канал", "Channel");

    static final String FORUM = pick("Форум", "Форум", "Forum");

    // ----------------------------------------------------------- the streaks

    static final String STREAKS = pick("Серии", "Серії", "Streaks");

    static final String STREAK_AUTO = pick(
            "Продлевать серии сами", "Продовжувати серії самі", "Keep streaks alive");

    static final String STREAK_HEALTH = pick(
            "Готовность автопродления", "Готовність автопродовження", "Autoprodlenie readiness");

    static final String STREAK_HEALTH_FIX = pick(
            "Нажмите, чтобы разрешить точный будильник и снять экономию батареи",
            "Натисніть, щоб дозволити точний будильник і зняти економію батареї",
            "Tap to allow exact alarms and lift the battery saver");

    static final String BETA = pick("бета", "бета", "beta");

    static final String STREAK_NOTE = pick(
            "Отправляет выбранный стикер тем, с кем серия вот-вот погаснет. "
                    + "Не чаще раза в сутки на человека.",
            "Надсилає вибраний стікер тим, з ким серія ось-ось згасне. "
                    + "Не частіше разу на добу на людину.",
            "Sends the sticker you picked to whoever the streak is about to lapse "
                    + "with, at most once a day each.");

    static final String STREAK_STICKER = pick(
            "Чем продлевать", "Чим продовжувати", "What to send");

    static final String STREAK_NOTHING = pick(
            "Избранные стикеры загружаются из TikTok",
            "Обрані стікери завантажуються з TikTok",
            "Favourite stickers load from TikTok");

    // ------------------------------------------------------- the updates

    static final String UPDATE = pick("Обновление", "Оновлення", "An update");

    static final String UPDATE_THERE_IS = pick(
            "Вышла версия", "Вийшла версія", "There is a version");

    static final String UPDATE_GET = pick("Скачать", "Завантажити", "Get it");

    static final String UPDATE_LATER = pick("Назад", "Назад", "Back");

    static final String UPDATE_NEVER = pick(
            "Больше не напоминать", "Більше не нагадувати", "Stop reminding me");

    static final String UPDATE_GETTING = pick("Скачиваю", "Завантажую", "Getting it");

    static final String UPDATE_FAILED = pick(
            "Не получилось скачать", "Не вдалося завантажити", "Could not get it");

    static final String UPDATE_NONE = pick(
            "Уже последняя версия", "Вже остання версія", "This is the latest");

    static final String UPDATE_NO_ANSWER = pick(
            "GitHub не ответил", "GitHub не відповів", "GitHub did not answer");

    static final String UPDATE_ALLOW = pick(
            "Разрешите установку из этого источника, и я поставлю",
            "Дозвольте встановлення з цього джерела, і я поставлю",
            "Allow installing from this source and it will go on");

    static final String UPDATE_CHECK = pick(
            "Проверить обновления", "Перевірити оновлення", "Check for updates");

    static final String UPDATE_REMIND = pick(
            "Напоминать об обновлениях", "Нагадувати про оновлення",
            "Remind me about updates");

    static final String UPDATE_INSTALL = pick(
            "Установить скачанное", "Встановити завантажене", "Install what was downloaded");

    static final String VERSIONS = pick("Версии", "Версії", "Versions");

    static final String SOURCE = pick("Исходники", "Вихідники", "The source");


    static final String CHANNEL_TITLE = pick("Канал", "Канал", "Channel");

    static final String CHANNEL_NEWS_A = pick("Новости/", "Новини/", "News/");

    static final String CHANNEL_NEWS_B = pick("обновления", "оновлення", "updates");

    static final String CHANNEL_SUBSCRIBE = pick("Подписаться", "Підписатися", "Subscribe");

    static final String THANKS = pick("Благодарности", "Подяки", "Thanks");

    static final String THANKS_NOTE = pick(
            "Люди, без которых мода бы не было.",
            "Люди, без яких мода б не було.",
            "The people the mod would not exist without.");

    static final String THANKS_OWNER = pick(
            "Автор margyT — взяты исходники мода",
            "Автор margyT — взято вихідники мода",
            "Author of margyT — the mod's source was taken from it");

    static final String THANKS_ICONS = pick(
            "Рисовал иконки", "Малював іконки", "Drew the icons");

    static final String ATTR_DEFAULT = "*сделано на основе margyT";
    static final String ATTR_MENU = pick("Надпись под названием", "Напис під назвою", "Title note");
    static final String ATTR_EDIT = pick("Изменить надпись", "Змінити напис", "Edit the note");
    static final String ATTR_HIDE = pick("Скрыть надпись", "Приховати напис", "Hide the note");
    static final String ATTR_SHOW = pick("Показать надпись", "Показати напис", "Show the note");
    static final String ATTR_RESET = pick("Вернуть стандартную", "Повернути стандартний", "Restore the default");
    static final String ATTR_HINT = pick("Ваша надпись (пусто — скрыть)", "Ваш напис (порожньо — приховати)", "Your note (empty hides it)");

    static final String THANKS_AI = pick(
            "Помог с разработкой",
            "Допоміг із розробкою",
            "Helped with development");

    static final String THANKS_HELPER = pick(
            "Создал форк MargyT и развивает ttcuz",
            "Створив форк MargyT і розвиває ttcuz",
            "Created the MargyT fork and develops ttcuz");

    static final String NO_BROWSER = pick(
            "Нечем открыть ссылку", "Нема чим відкрити посилання", "Nothing here opens links");

    static final String DIARY_TITLE = pick("Журнал мода", "Журнал мода", "The mod's diary");

    static final String COPY = pick("Скопировать", "Скопіювати", "Copy");

    static final String COPIED = pick("Скопировано", "Скопійовано", "Copied");

    static final String CLEAR = pick("Очистить", "Очистити", "Clear");

    static final String DIARY = pick(
            "Что мод видел", "Що мод бачив", "What the mod saw");

    static final String CLEARED = pick("Журнал очищен", "Журнал очищено", "The diary is empty");

    static final String ACCOUNT = pick("Аккаунт", "Акаунт", "Account");

    static final String ACCOUNT_ID = pick("ID", "ID", "ID");

    static final String ACCOUNT_SEC_ID = pick(
            "ID для ссылки", "ID для посилання", "The id a link is built from");

    static final String ACCOUNT_UNKNOWN = pick(
            "Пока неизвестен", "Поки невідомий", "Not seen yet");

    private static String language() {
        try {
            return Locale.getDefault().getLanguage();
        } catch (Throwable ignored) {
            return "en";
        }
    }

    static final String DEV_ON = pick("Режим разработчика включён",
            "Режим розробника увімкнено", "Developer mode is on");
    static final String DEV_ALREADY = pick("Режим разработчика уже включён",
            "Режим розробника вже увімкнено", "Developer mode is already on");
    static final String DEV_SWITCH = pick("Режим разработчика",
            "Режим розробника", "Developer mode");
    static final String DEV_FULL_REPORT = pick("Скопировать полный отчёт",
            "Скопіювати повний звіт", "Copy the full report");
    static final String DEV_FULL_NOTE = pick(
            "Устройство, сборка, хуки, плагины и весь журнал. Без id аккаунта и токенов.",
            "Пристрій, збірка, хуки, плагіни та весь журнал. Без id акаунта й токенів.",
            "Device, build, hooks, plugins and the whole diary. No account id or tokens.");

    // Profile appearance: shorter cards.
    static final String PROFILE_LOOK = pick("Вид ника", "Вигляд імені", "Nickname look");
    static final String PROFILE_SYNC = pick("Синхронизация", "Синхронізація", "Sync");
    static final String PROFILE_FINE = pick("Тонкая настройка", "Тонке налаштування", "Fine tuning");

    // Redesigned sheets: second lines under the menu rows.
    static final String SHEET_DL_HINT = pick("Чем выше качество, тем больше файл", "Чим вища якість, тим більший файл", "Higher quality means a bigger file");
    static final String ACT_DOWNLOAD_HINT = pick("Видео, фото или только звук", "Відео, фото або лише звук", "Video, photo or audio only");
    static final String ACT_PLAYBACK_HINT = pick("Скорость и перемотка", "Швидкість і перемотування", "Speed and seeking");
    static final String ACT_FRAME_HINT = pick("Текущий момент как фото", "Поточний момент як фото", "The current moment as a photo");
    static final String ACT_HIDE = pick("Скрыть из ленты", "Приховати зі стрічки", "Hide from feed");
    static final String ACT_HIDE_HINT = pick("Автор или подборка", "Автор або добірка", "Author or suggestion");
    static final String ACT_PLAYBACK = pick("Воспроизведение", "Відтворення", "Playback");
    static final String ACT_DOWNLOAD = pick("Скачать", "Завантажити", "Download");
    static final String SHEET_PICK_QUALITY = pick("Выберите качество", "Оберіть якість", "Choose quality");
    static final String SHEET_BEST = pick("Лучшее качество", "Найкраща якість", "Best quality");
    static final String SHEET_WORST = pick("Худшее качество", "Найгірша якість", "Lowest quality");
    static final String SHEET_PHOTOS = pick("Фото", "Фото", "Photos");
    static final String SHEET_POST_TITLE = pick("Скачать публикацию", "Завантажити публікацію", "Download post");
    static String allPhotos(int count) {
        return pick("Скачать все фото (" + count + ")", "Завантажити всі фото (" + count + ")", "Download all photos (" + count + ")");
    }
    static String photosSaved(int saved, int total) {
        return pick("Сохранено " + saved + " из " + total + " фото", "Збережено " + saved + " із " + total + " фото", "Saved " + saved + " of " + total + " photos");
    }
    static final String SHEET_AUDIO = pick("Скачать аудио", "Завантажити аудіо", "Download audio");
    static final String SHEET_ONE_SIZE = pick("Для этого видео TikTok отдаёт только одно качество", "Для цього відео TikTok віддає лише одну якість", "TikTok offers only one quality for this video");

    // First-run question about crash reports.
    static final String CONSENT_TITLE = pick("Помочь исправлять сбои?", "Допомогти виправляти збої?", "Help fix crashes?");
    static final String CONSENT_BODY = pick(
            "Если ttcuz вылетит, он отправит разработчику технический след сбоя: версию мода и Android, список вызовов. Без сообщений, аккаунта и личных данных.",
            "Якщо ttcuz вилетить, він надішле розробнику технічний слід збою: версію мода й Android, список викликів. Без повідомлень, акаунта й особистих даних.",
            "If ttcuz crashes, it sends the developer a technical trace: the mod and Android versions and the call list. No messages, account or personal data.");
    static final String CONSENT_NOTE = pick(
            "Выбор можно изменить в настройках мода: «Диагностика» → «Отчёты о сбоях».",
            "Вибір можна змінити в налаштуваннях мода: «Діагностика» → «Звіти про збої».",
            "You can change this later in the mod settings: Diagnostics → Crash reports.");
    static final String CONSENT_WAIT = pick("Ответить можно через %d с", "Відповісти можна за %d с", "You can answer in %d s");
    static final String CONSENT_YES = pick("Да, отправлять", "Так, надсилати", "Yes, send them");
    static final String CONSENT_NO = pick("Нет", "Ні", "No");

    // The walkthrough.
    static final String TUTORIAL_NEXT = pick("Далее", "Далі", "Next");
    static final String TUTORIAL_BACK = pick("Назад", "Назад", "Back");
    static final String TUTORIAL_DONE = pick("Готово", "Готово", "Done");
    static final String TUTORIAL_SKIP = pick("Пропустить", "Пропустити", "Skip");
    static final String[] TUTORIAL_TITLES = {
            pick("Добро пожаловать в ttcuz", "Ласкаво просимо в ttcuz", "Welcome to ttcuz"),
            pick("Своя палитра", "Своя палітра", "Your own palette"),
            pick("Подтвердите профиль", "Підтвердьте профіль", "Verify your profile"),
            pick("Всё под рукой", "Все під рукою", "Everything at hand")};
    static final String[] TUTORIAL_WORDS = {
            pick("Все функции мода собраны в одном месте: профиль → меню → настройки TikTok → ttcuz.",
                    "Усі функції мода зібрано в одному місці: профіль → меню → налаштування TikTok → ttcuz.",
                    "Every feature of the mod lives in one place: profile → menu → TikTok settings → ttcuz."),
            pick("Откройте «Внешний вид» → «Акцент». Круг — доступный цвет, квадрат — выбранный.",
                    "Відкрийте «Зовнішній вигляд» → «Акцент». Коло — доступний колір, квадрат — вибраний.",
                    "Open Appearance → Accent. A circle is an available colour, a square is the chosen one."),
            pick("Откройте «Профиль» в ttcuz → «Подтвердить». Мод сам добавит код и вернёт описание. Не получилось — вставьте код вручную и вернитесь: оформление появится в профиле и чатах.",
                    "Відкрийте «Профіль» у ttcuz → «Підтвердити». Мод сам додасть код і поверне опис. Не вийшло — вставте код вручну й поверніться: оформлення з’явиться в профілі та чатах.",
                    "Open Profile in ttcuz → Verify. The mod adds the code and restores your bio. If that fails, paste the code by hand and come back: the look then appears in your profile and chats."),
            pick("Прозрачность меняется сразу. Если другой настройке нужен перезапуск, снизу появится кнопка. Отчёты о сбоях включаются в «Диагностике». Это обучение можно открыть снова в ttcuz.",
                    "Прозорість змінюється одразу. Якщо іншому налаштуванню потрібен перезапуск, знизу з’явиться кнопка. Звіти про збої вмикаються в «Діагностиці». Це навчання можна відкрити знову в ttcuz.",
                    "Transparency changes at once. When another setting needs a restart, a button appears at the bottom. Crash reports are switched in Diagnostics. You can reopen this tour in ttcuz.")};

    private static String pick(String russian, String ukrainian, String english) {
        if (RU) return russian;
        if (UK) return ukrainian;
        return english;
    }
}

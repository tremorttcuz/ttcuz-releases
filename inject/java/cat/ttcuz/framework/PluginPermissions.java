package cat.ttcuz.framework;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * What a plugin may do, in words a person can judge.
 *
 * A plugin lists what it needs in manifest.json ("permissions"): the ttcuz ones below and Android's own
 * (android.permission.CAMERA and so on). Plugin API 2 holds a plugin to that list -- a hook or a call it did not ask
 * for fails with the name of the permission it lacks. API 1 plugins predate the list and keep everything they had;
 * for them the hooks they override are shown instead, so the key button still tells what they do.
 */
public final class PluginPermissions {
    private PluginPermissions() {}

    public static final String INTERFACE = "interface", TEXT = "text", FEED = "feed", NETWORK = "network",
            NOTIFICATIONS = "notifications", CLIPBOARD = "clipboard", TIKTOK = "tiktok", ACCOUNT = "account",
            STORAGE = "storage", COLOURS = "colours", REGION = "region", EVENTS = "events",
            EFFECTS = "effects", MOD = "mod", WATCH = "watch", AUTOMATION = "automation", PLAYER = "player";

    /** icon, title, what it allows */
    private static String[] about(String name) {
        switch (name) {
            case INTERFACE: return new String[]{"view_compact", Text.t("Интерфейс"), Text.t("Добавляет свои кнопки и окна поверх экранов TikTok, меняет вид и расположение элементов")};
            case TEXT: return new String[]{"text_fields", Text.t("Тексты и ники"), Text.t("Меняет подписи, ники и другие надписи в приложении")};
            case FEED: return new String[]{"dynamic_feed", Text.t("Лента"), Text.t("Убирает или переставляет видео в ленте")};
            case NETWORK: return new String[]{"public", Text.t("Интернет"), Text.t("Отправляет и получает данные через интернет")};
            case NOTIFICATIONS: return new String[]{"notifications", Text.t("Уведомления"), Text.t("Показывает свои уведомления")};
            case CLIPBOARD: return new String[]{"content_paste", Text.t("Буфер обмена"), Text.t("Копирует текст и читает скопированное")};
            case TIKTOK: return new String[]{"code", Text.t("Внутренности TikTok"), Text.t("Вызывает скрытые методы и поля TikTok напрямую")};
            case ACCOUNT: return new String[]{"person", Text.t("Аккаунт"), Text.t("Видит id аккаунта, в который вы вошли")};
            case STORAGE: return new String[]{"folder", Text.t("Файлы"), Text.t("Сохраняет файлы в «Загрузки»")};
            case COLOURS: return new String[]{"palette", Text.t("Цвета"), Text.t("Перекрашивает цвета приложения")};
            case REGION: return new String[]{"language", Text.t("Регион"), Text.t("Меняет регион, который видит TikTok")};
            case EFFECTS: return new String[]{"blur_on", Text.t("Эффекты"), Text.t("Размывает, перекрашивает и рисует шейдеры поверх экранов, добавляет motion blur")};
            case MOD: return new String[]{"tune", Text.t("Интерфейс и настройки мода"), Text.t("Меняет вид экранов мода, добавляет свои настройки и меняет настройки мода")};
            case EVENTS: return new String[]{"bolt", Text.t("События"), Text.t("Получает события мода и других плагинов")};
            case WATCH: return new String[]{"visibility", Text.t("Действия в TikTok"), Text.t("Узнаёт, какое видео открыто, открыты ли комментарии, что вы искали и какие сообщения в чате видны")};
            case AUTOMATION: return new String[]{"vibration", Text.t("Нажатия за вас"), Text.t("Нажимает, листает и печатает в TikTok вместо вас")};
            case PLAYER: return new String[]{"play_circle", Text.t("Плеер"), Text.t("Ставит видео на паузу, включает его, меняет скорость и сохраняет видео")};
            default: break;
        }
        if (name.startsWith("android.permission.")) {
            String bare = name.substring("android.permission.".length());
            switch (bare) {
                case "CAMERA": return new String[]{"photo_camera", Text.t("Камера"), Text.t("Разрешение Android: снимать фото и видео")};
                case "RECORD_AUDIO": return new String[]{"mic", Text.t("Микрофон"), Text.t("Разрешение Android: записывать звук")};
                case "ACCESS_FINE_LOCATION": return new String[]{"location_on", Text.t("Точное местоположение"), Text.t("Разрешение Android: знать, где вы находитесь")};
                case "ACCESS_COARSE_LOCATION": return new String[]{"location_on", Text.t("Примерное местоположение"), Text.t("Разрешение Android: знать город или район")};
                case "READ_CONTACTS": return new String[]{"contacts", Text.t("Контакты"), Text.t("Разрешение Android: читать контакты телефона")};
                case "READ_CALENDAR": return new String[]{"calendar_month", Text.t("Календарь"), Text.t("Разрешение Android: читать события календаря")};
                case "READ_MEDIA_IMAGES": return new String[]{"image", Text.t("Фото"), Text.t("Разрешение Android: видеть фотографии телефона")};
                case "READ_MEDIA_VIDEO": return new String[]{"movie", Text.t("Видео"), Text.t("Разрешение Android: видеть видео телефона")};
                case "READ_MEDIA_AUDIO": return new String[]{"music_note", Text.t("Музыка"), Text.t("Разрешение Android: видеть аудиофайлы")};
                case "BLUETOOTH_CONNECT": return new String[]{"bluetooth", Text.t("Bluetooth"), Text.t("Разрешение Android: подключаться к устройствам")};
                case "VIBRATE": return new String[]{"vibration", Text.t("Вибрация"), Text.t("Разрешение Android: включать вибрацию")};
                case "POST_NOTIFICATIONS": return new String[]{"notifications", Text.t("Уведомления"), Text.t("Разрешение Android: показывать уведомления")};
                default: return new String[]{"shield", bare.toLowerCase(Locale.ROOT).replace('_', ' '), Text.t("Разрешение Android") + ": " + bare};
            }
        }
        return new String[]{"warning", name, Text.t("Неизвестное разрешение")};
    }

    public static String icon(String name) { return about(name)[0]; }
    public static String title(String name) { return about(name)[1]; }
    public static String detail(String name) { return about(name)[2]; }

    /** Sensitive enough to stand out in red on the key screen. */
    public static boolean sensitive(String name) {
        return TIKTOK.equals(name) || MOD.equals(name) || WATCH.equals(name) || AUTOMATION.equals(name) || NETWORK.equals(name) || ACCOUNT.equals(name) || CLIPBOARD.equals(name)
                || (name.startsWith("android.permission.") && !name.endsWith("VIBRATE") && !name.endsWith("POST_NOTIFICATIONS"));
    }

    /**
     * How much a permission lets a plugin do, for the key screen (API 4): 0 "safe" -- its own look and its own
     * things; 1 "elevated" -- the network, the clipboard, the account, the mod's settings; 2 "full access" -- TikTok's
     * insides, what you do in TikTok, acting for you, Android's permissions.
     */
    public static int tier(String name) {
        if (TIKTOK.equals(name) || WATCH.equals(name) || AUTOMATION.equals(name)) return 2;
        if (name.startsWith("android.permission.")) return name.endsWith("VIBRATE") || name.endsWith("POST_NOTIFICATIONS") ? 0 : 2;
        if (NETWORK.equals(name) || CLIPBOARD.equals(name) || ACCOUNT.equals(name) || MOD.equals(name) || PLAYER.equals(name)) return 1;
        return 0;
    }

    /** The highest tier among a plugin's permissions. */
    public static int tier(java.util.Collection<String> names) {
        int most = 0;
        for (String name : names) most = Math.max(most, tier(name));
        return most;
    }

    public static String tierName(int tier) {
        return tier >= 2 ? Text.t("Полный доступ") : tier == 1 ? Text.t("Расширенный доступ") : Text.t("Безопасный");
    }

    /** The permission an overridden hook needs, or null when any plugin may override it. */
    public static String forHook(String hook) {
        switch (hook) {
            case "onView": case "onScreen": return INTERFACE;
            case "onText": case "onNickname": return TEXT;
            case "onFeed": return FEED;
            case "onColour": return COLOURS;
            case "onRegion": return REGION;
            case "onEvent": return EVENTS;
            case "onAccountChanged": return ACCOUNT;
            case "onModScreen": case "onModPanel": case "onModSettings": return MOD;
            case "onTikTok": return WATCH;
            default: return null;
        }
    }

    /** What the plugin can do: its list, plus (API 1) what its hooks imply. */
    public static List<String> effective(int api, List<String> declared, Set<String> hooks) {
        Set<String> out = new LinkedHashSet<>(declared);
        if (api < 2) {
            for (String hook : hooks) { String needed = forHook(hook); if (needed != null) out.add(needed); }
            out.add(TIKTOK); // API 1 always could reach TikTok's classes
        }
        return new ArrayList<>(out);
    }

    /** API 2: hooks overridden without the permission they need. Empty when everything was asked for. */
    public static List<String> missing(int api, List<String> declared, Set<String> hooks) {
        List<String> out = new ArrayList<>();
        if (api < 2) return out;
        for (String hook : hooks) {
            String needed = forHook(hook);
            if (needed != null && !declared.contains(needed) && !out.contains(needed)) out.add(needed);
        }
        return out;
    }
}

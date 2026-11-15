package cat.ttcuz.framework;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import cat.ttcuz.framework.Expressive;
import cat.ttcuz.framework.Panel;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

final class DateInfo {
    static final int CELL_TAG = 2114782005;
    private static final String[][] EN_UNITS;
    private static final String LANGUAGE;
    private static final boolean RU;
    private static final String[][] RU_UNITS;
    private static final boolean UK;
    private static final String[][] UK_UNITS;

    private DateInfo() {
    }

    static final class Data {
        final String aid;
        final String author;
        final String handle;
        long likes = -1;
        final long seconds;

        Data(long j, String str, String str2, String str3) {
            this.seconds = j;
            this.aid = str == null ? "" : str;
            this.author = str2 == null ? "" : str2;
            this.handle = str3 == null ? "" : str3;
        }

        static Data of(Object obj) {
            if (obj == null) {
                return null;
            }
            long number = NativeRead.number(NativeRead.get(obj, "getCreateTime"));
            if (number > 100000000000L) {
                number /= 1000;
            }
            long j = number;
            if (j < 1000000000) {
                return null;
            }
            Object obj2 = NativeRead.get(obj, "getAuthor");
            Data data = new Data(j, NativeRead.aid(obj), NativeRead.string(NativeRead.get(obj2, "getNickname")), NativeRead.string(NativeRead.get(obj2, "getUniqueId")));
            data.likes = StoryLikes.count(obj);
            return data;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void open(View view, View view2) {
        Object tag;
        if (view2 == null) {
            tag = null;
        } else {
            try {
                tag = view2.getTag(CELL_TAG);
            } catch (Throwable th) {
                Diary.note("date info: " + String.valueOf(th));
                return;
            }
        }
        Data data = tag instanceof Data ? (Data) tag : Dates.selectedInfo;
        if (data != null && view != null) {
            show(view.getContext(), data);
        }
    }

    static {
        String language = Locale.getDefault().getLanguage();
        LANGUAGE = language;
        RU = "ru".equals(language) || "be".equals(language);
        UK = "uk".equals(language);
        RU_UNITS = new String[][]{new String[]{"год", "года", "лет"}, new String[]{"месяц", "месяца", "месяцев"}, new String[]{"день", "дня", "дней"}, new String[]{"час", "часа", "часов"}, new String[]{"минуту", "минуты", "минут"}, new String[]{"секунду", "секунды", "секунд"}};
        UK_UNITS = new String[][]{new String[]{"рік", "роки", "років"}, new String[]{"місяць", "місяці", "місяців"}, new String[]{"день", "дні", "днів"}, new String[]{"годину", "години", "годин"}, new String[]{"хвилину", "хвилини", "хвилин"}, new String[]{"секунду", "секунди", "секунд"}};
        EN_UNITS = new String[][]{new String[]{"year", "years", "years"}, new String[]{"month", "months", "months"}, new String[]{"day", "days", "days"}, new String[]{"hour", "hours", "hours"}, new String[]{"minute", "minutes", "minutes"}, new String[]{"second", "seconds", "seconds"}};
    }

    private static String pick(String str, String str2, String str3) {
        return RU ? str : UK ? str2 : str3;
    }

    static void show(final Context context, Data data) {
        String str;
        String str2;
        String str3;
        String str4;
        Locale locale = Locale.getDefault();
        TimeZone timeZone = TimeZone.getDefault();
        long j = data.seconds * 1000;
        Date date = new Date(j);
        boolean is24HourFormat = DateFormat.is24HourFormat(context);
        String format = format(best(locale, "dMMMMyyyy", "d MMMM yyyy"), date, timeZone, locale);
        String capital = capital(format("EEEE", date, timeZone, locale));
        String format2 = format(best(locale, is24HourFormat ? "Hms" : "hms", "HH:mm:ss"), date, timeZone, locale);
        String format3 = format("dd.MM.yyyy HH:mm:ss", date, timeZone, locale);
        String str5 = format("dd.MM.yyyy HH:mm:ss", date, TimeZone.getTimeZone("UTC"), locale) + " UTC";
        String offset = offset(timeZone.getOffset(j));
        String ago = ago(data.seconds, System.currentTimeMillis() / 1000);
        Skin remembered = Skin.remembered(context);
        Panel with = Panel.with(context, remembered, null);
        with.view(header(context, remembered, format, capital + ", " + format2, ago));
        with.section(pick("Точное время", "Точний час", "Exact time")).info("article", format3, pick("Дата и время публикации", "Дата й час публікації", "Published"), copy(context, format3)).info("schedule", format2, pick("Время до секунды", "Час до секунди", "Time to the second"), copy(context, format2)).info("timeline", capital, pick("День недели", "День тижня", "Day of the week"), copy(context, capital)).info("language", offset + " · " + timeZone.getID(), pick("Часовой пояс", "Часовий пояс", "Time zone"), copy(context, offset + " " + timeZone.getID())).info("public", str5, pick("По Гринвичу", "За Грінвічем", "In UTC"), copy(context, str5));
        with.section(pick("Технические данные", "Технічні дані", "Technical")).info("update", String.valueOf(data.seconds), "Unix timestamp", copy(context, String.valueOf(data.seconds)));
        if (data.aid.length() > 0) {
            with.info("fingerprint", data.aid, pick("ID видео", "ID відео", "Video ID"), copy(context, data.aid));
        }
        if (data.author.length() > 0 || data.handle.length() > 0) {
            if (data.author.length() > 0) {
                str = data.author;
            } else {
                str = "@" + data.handle;
            }
            String pick = pick("Автор", "Автор", "Author");
            if (data.handle.length() <= 0 || data.author.length() <= 0) {
                str2 = "";
                str3 = str2;
            } else {
                str2 = "";
                str3 = " · @" + data.handle;
            }
            with.info("account_circle", str, pick + str3, copy(context, str));
        } else {
            str2 = "";
        }
        String id = timeZone.getID();
        long j2 = data.seconds;
        if (data.aid.length() > 0) {
            str4 = "\nID: " + data.aid;
        } else {
            str4 = str2;
        }
        final String str6 = format + ", " + capital + ", " + format2 + "\n" + format3 + "\n" + offset + " " + id + "\n" + str5 + "\nUnix: " + j2 + str4;
        with.primary(pick("Скопировать всё", "Скопіювати все", "Copy everything"), new Panel.Click() { // from class: cat.ttcuz.framework.DateInfo$$ExternalSyntheticLambda0
            @Override // cat.ttcuz.framework.Panel.Click
            public final void on() {
                DateInfo.copy(context, str6).on();
            }
        }).quiet(pick("Закрыть", "Закрити", "Close"), null).show();
    }

    private static Panel.Click copy(final Context context, final String str) {
        return new Panel.Click() { // from class: cat.ttcuz.framework.DateInfo$$ExternalSyntheticLambda1
            @Override // cat.ttcuz.framework.Panel.Click
            public final void on() {
                DateInfo.lambda$copy$1(context, str);
            }
        };
    }

    static /* synthetic */ void lambda$copy$1(Context context, String str) {
        try {
            ClipboardManager clipboardManager = (ClipboardManager) context.getSystemService("clipboard");
            if (clipboardManager != null) {
                clipboardManager.setPrimaryClip(ClipData.newPlainText("ttcuz", str));
            }
            Screen.say(Text.COPIED);
        } catch (Throwable th) {
            Diary.note("date copy: " + String.valueOf(th));
        }
    }

    private static View header(Context context, Skin skin, String str, String str2, String str3) {
        int colour = Accent.colour();
        float f = context.getResources().getDisplayMetrics().density;
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(1);
        float f2 = 4.0f * f;
        linearLayout.setPadding(Math.round(f2), Math.round(f2), Math.round(f2), Math.round(8.0f * f));
        LinearLayout linearLayout2 = new LinearLayout(context);
        linearLayout2.setOrientation(0);
        linearLayout2.setGravity(16);
        int i = colour & 16777215;
        Expressive.Badge badge = new Expressive.Badge(context, 0, SettingsGlyph.make("schedule", colour), 72, i | 855638016, colour, colour, onAccent(colour));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-2, -2);
        layoutParams.rightMargin = Math.round(f * 16.0f);
        linearLayout2.addView(badge, layoutParams);
        badge.set(true, true);
        LinearLayout linearLayout3 = new LinearLayout(context);
        linearLayout3.setOrientation(1);
        TextView textView = new TextView(context);
        textView.setText(str);
        textView.setTextColor(skin.text);
        textView.setTextSize(2, 26.0f);
        textView.setTypeface(Typeface.create("sans-serif-medium", 0));
        textView.setLineSpacing(0.0f, 0.96f);
        linearLayout3.addView(textView, new LinearLayout.LayoutParams(-1, -2));
        TextView textView2 = new TextView(context);
        textView2.setText(str2);
        textView2.setTextColor(colour);
        textView2.setTextSize(2, 16.0f);
        textView2.setTypeface(Typeface.create("sans-serif-medium", 0));
        LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(-1, -2);
        layoutParams2.topMargin = Math.round(2.0f * f);
        linearLayout3.addView(textView2, layoutParams2);
        linearLayout2.addView(linearLayout3, new LinearLayout.LayoutParams(0, -2, 1.0f));
        linearLayout.addView(linearLayout2, new LinearLayout.LayoutParams(-1, -2));
        TextView textView3 = new TextView(context);
        textView3.setText(str3);
        textView3.setTextColor(colour);
        textView3.setTextSize(2, 13.0f);
        textView3.setTypeface(Typeface.create("sans-serif-medium", 0));
        float f3 = 14.0f * f;
        float f4 = 7.0f * f;
        textView3.setPadding(Math.round(f3), Math.round(f4), Math.round(f3), Math.round(f4));
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(i | 637534208);
        gradientDrawable.setCornerRadius(Math.round(f * 20.0f));
        textView3.setBackground(gradientDrawable);
        LinearLayout.LayoutParams layoutParams3 = new LinearLayout.LayoutParams(-2, -2);
        layoutParams3.topMargin = Math.round(f3);
        linearLayout.addView(textView3, layoutParams3);
        return linearLayout;
    }

    private static int onAccent(int i) {
        return (((((i >> 16) & 255) * 299) + (((i >> 8) & 255) * 587)) + ((i & 255) * 114)) / 1000 > 150 ? -15658735 : -1;
    }

    private static String best(Locale locale, String str, String str2) {
        try {
            String bestDateTimePattern = DateFormat.getBestDateTimePattern(locale, str);
            return bestDateTimePattern != null ? bestDateTimePattern.length() == 0 ? str2 : bestDateTimePattern : str2;
        } catch (Throwable unused) {
            return str2;
        }
    }

    private static String format(String str, Date date, TimeZone timeZone, Locale locale) {
        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(str, locale);
            simpleDateFormat.setTimeZone(timeZone);
            return simpleDateFormat.format(date);
        } catch (Throwable unused) {
            return String.valueOf(date.getTime() / 1000);
        }
    }

    private static String capital(String str) {
        if (str == null || str.length() == 0) {
            return "";
        }
        return str.substring(0, 1).toUpperCase(Locale.getDefault()) + str.substring(1);
    }

    private static String offset(int i) {
        int abs = Math.abs(i) / 60000;
        return "UTC" + (i < 0 ? "−" : "+") + two(abs / 60) + ":" + two(abs % 60);
    }

    private static String two(int i) {
        if (i >= 10) {
            return String.valueOf(i);
        }
        return "0" + i;
    }

    /* JADX WARN: Code restructure failed: missing block: B:9:0x0025, code lost:
    
        if (r11 == 1) goto L16;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private static String unit(int i, long j) {
        boolean z = RU;
        String[][] strArr = z ? RU_UNITS : UK ? UK_UNITS : EN_UNITS;
        long j2 = j % 100;
        long j3 = j % 10;
        // Russian and Ukrainian: one, few (2-4, not 12-14), many; English: one or many
        int c;
        if (z || UK) {
            if (j3 == 1 && j2 != 11) c = 0;
            else if (j3 >= 2 && j3 <= 4 && (j2 < 12 || j2 > 14)) c = 1;
            else c = 2;
        } else {
            c = j == 1 ? 0 : 1;
        }
        return j + " " + strArr[i][c];
    }

    static String ago(long j, long j2) {
        StringBuilder sb;
        String str;
        if (j2 - j < 60) {
            return pick("только что", "щойно", "just now");
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(j * 1000);
        Calendar calendar2 = Calendar.getInstance();
        calendar2.setTimeInMillis(j2 * 1000);
        long[] jArr = new long[6];
        int[] iArr = {1, 2};
        for (int i = 0; i < 2; i++) {
            int i2 = 0;
            while (true) {
                Calendar calendar3 = (Calendar) calendar.clone();
                calendar3.add(iArr[i], 1);
                if (calendar3.after(calendar2)) {
                    break;
                }
                i2++;
                calendar = calendar3;
            }
            jArr[i] = i2;
        }
        long max = Math.max(0L, (calendar2.getTimeInMillis() - calendar.getTimeInMillis()) / 1000);
        jArr[2] = max / 86400;
        jArr[3] = (max % 86400) / 3600;
        jArr[4] = (max % 3600) / 60;
        jArr[5] = max % 60;
        StringBuilder sb2 = new StringBuilder();
        int i3 = 0;
        for (int i4 = 0; i4 < 6 && i3 < 2; i4++) {
            if (jArr[i4] != 0) {
                if (sb2.length() > 0) {
                    sb2.append(' ');
                }
                sb2.append(unit(i4, jArr[i4]));
                i3++;
            }
        }
        if (RU) {
            String valueOf = String.valueOf(sb2);
            sb = new StringBuilder();
            sb.append(valueOf);
            str = " назад";
        } else if (UK) {
            String valueOf2 = String.valueOf(sb2);
            sb = new StringBuilder();
            sb.append(valueOf2);
            str = " тому";
        } else {
            String valueOf3 = String.valueOf(sb2);
            sb = new StringBuilder();
            sb.append(valueOf3);
            str = " ago";
        }
        sb.append(str);
        return sb.toString();
    }
}

package cat.ttcuz.framework;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.zip.*;
import org.json.*;

/** Native store and persistent source editor. No browser or JavaScript bridge. */
final class PluginWorkbench {
    static final int EXPORT = 3072;
    private static final String ROOT = CloudProfileProvider.ROOT;

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences("ttcuz_plugin_editor", Context.MODE_PRIVATE);
    }

    private static int dp(Context c, int value) {
        return Math.round(value * c.getResources().getDisplayMetrics().density);
    }

    private static LinearLayout box(Activity a) {
        LinearLayout view = new LinearLayout(a);
        view.setOrientation(LinearLayout.VERTICAL);
        view.setPadding(dp(a, 12), dp(a, 8), dp(a, 12), dp(a, 8));
        return view;
    }

    private static TextView text(Activity a, Skin skin, String value) {
        TextView view = new TextView(a);
        view.setText(value);
        view.setTextColor(skin.text);
        view.setTextSize(15);
        view.setPadding(dp(a, 8), dp(a, 10), dp(a, 8), dp(a, 6));
        return view;
    }

    private static Button button(Activity a, Skin skin, LinearLayout parent, String title, Runnable action) {
        Button b = new Button(a);
        b.setText(title);
        b.setAllCaps(false);
        b.setTextColor(skin.text);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(skin.card));
        LinearLayout.LayoutParams size = new LinearLayout.LayoutParams(-1, -2);
        size.topMargin = dp(a, 6);
        parent.addView(b, size);
        b.setOnClickListener(v -> action.run());
        return b;
    }

    static void store(Activity a, Skin skin) {
        LinearLayout list = box(a);
        TextView status = text(a, skin, "Загрузка…");
        list.addView(status);
        new ModDialog.Builder(a, skin).setTitle("Магазин плагинов").setView(list)
            .setNegativeButton("Закрыть", null).show();
        Runnable load = () -> {
            status.setText("Загрузка…");
            Net.away("plugin catalog", () -> {
                try {
                    byte[] data = Net.bytes(ROOT + "/plugins/catalog.json");
                    if (data == null) throw new IOException("Магазин недоступен. Повторите позже.");
                    JSONArray plugins = new JSONObject(new String(data, StandardCharsets.UTF_8)).optJSONArray("plugins");
                    a.runOnUiThread(() -> {
                        if (a.isFinishing() || a.isDestroyed()) return;
                        list.removeAllViews();
                        list.addView(status);
                        status.setText("Нет опубликованных плагинов");
                        int shown = 0;
                        if (plugins != null) for (int i = 0; i < plugins.length(); i++) {
                            JSONObject item = plugins.optJSONObject(i);
                            if (item == null || !("ctt".equals(item.optString("format")) ||
                                    "ttplugin".equals(item.optString("format")))) continue;
                            String target = item.optString("tiktok");
                            if (!target.isEmpty() && !Version.TIKTOK.equals(target)) continue;
                            shown++;
                            list.addView(text(a, skin, item.optString("name") + " · " + item.optString("version") +
                                (item.optBoolean("verified") ? " · Проверен" : "")));
                            String about = item.optString("about");
                            if (!about.isEmpty()) list.addView(text(a, skin, about));
                            final Button[] install = new Button[1];
                            install[0] = button(a, skin, list, "Установить", () -> install(a, item, install[0]));
                        }
                        if (shown > 0) status.setText("Плагинов: " + shown);
                    });
                } catch (Exception error) {
                    a.runOnUiThread(() -> status.setText(error.getMessage()));
                }
            });
        };
        button(a, skin, list, "Повторить", load);
        load.run();
    }

    private static void install(Activity a, JSONObject item, Button button) {
        button.setEnabled(false);
        button.setText("Установка…");
        Net.away("plugin download", () -> {
            File file = null;
            try {
                String url = item.getString("url");
                if (url.startsWith("/")) url = ROOT + url;
                Uri uri = Uri.parse(url);
                if (!"https".equals(uri.getScheme()) || !Uri.parse(ROOT).getHost().equals(uri.getHost()))
                    throw new IOException("Некорректный адрес пакета");
                byte[] data = Net.bytes(url);
                if (data == null) throw new IOException("Не удалось скачать плагин");
                String expected = item.getString("sha256");
                StringBuilder hash = new StringBuilder();
                for (byte b : MessageDigest.getInstance("SHA-256").digest(data))
                    hash.append(String.format(java.util.Locale.ROOT, "%02x", b & 255));
                if (!expected.equalsIgnoreCase(hash.toString())) throw new IOException("Пакет изменился. Обновите магазин.");
                file = File.createTempFile("plugin-", ".ctt", a.getCacheDir());
                try (OutputStream out = new FileOutputStream(file)) { out.write(data); }
                File downloaded = file;
                PluginEngine.install(Uri.fromFile(file), error -> {
                    downloaded.delete();
                    button.setEnabled(true);
                    button.setText(error == null ? "Установлен" : "Повторить установку");
                    if (error != null) Screen.say(error);
                });
            } catch (Exception error) {
                if (file != null) file.delete();
                a.runOnUiThread(() -> {
                    button.setEnabled(true);
                    button.setText("Повторить установку");
                    Screen.say(error.getMessage());
                });
            }
        });
    }

    static void editor(Activity a, Skin skin) {
        try {
            final String scope = "draft_" + Account.id();
            JSONObject project = new JSONObject(prefs(a).getString(scope, "{}"));
            JSONObject metadata = project.optJSONObject("metadata");
            if (metadata == null) {
                metadata = new JSONObject().put("id", "ttcuz.my-plugin").put("name", "Мой плагин")
                    .put("version", "1.0.0").put("author", a.getSharedPreferences(Ttcuz.PREFS, 0)
                        .getString("profile_preview_handle", "ttcuz"))
                    .put("entry", "ttcuz.plugins.MyPlugin").put("api", 1);
                project.put("metadata", metadata);
            }
            JSONArray files = project.optJSONArray("files");
            if (files == null || files.length() == 0) {
                files = new JSONArray().put(new JSONObject().put("path", "java/ttcuz/plugins/MyPlugin.java")
                    .put("text", "package ttcuz.plugins;\n\nimport cat.ttcuz.framework.plugin.TtcuzPlugin;\nimport cat.ttcuz.framework.plugin.PluginSettings;\n\npublic class MyPlugin extends TtcuzPlugin {\n    @Override public void onSettings(PluginSettings s) {\n        s.action(\"Привет\", () -> ttcuz().toast(\"Работает!\"));\n    }\n}\n"));
                project.put("files", files);
            }
            final JSONObject meta = metadata;
            final JSONArray sources = files;
            LinearLayout form = box(a);
            form.addView(text(a, skin, "Исходники Java. Черновик сохраняется автоматически."));
            String[][] labels = {{"name", "Название"}, {"id", "ID"}, {"version", "Версия"},
                {"author", "Автор"}, {"entry", "Главный класс"}, {"description", "Описание"}};
            android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
            Runnable persist = () -> prefs(a).edit().putString(scope, project.toString()).apply();
            Runnable save = () -> { handler.removeCallbacks(persist); handler.postDelayed(persist, 500L); };
            for (String[] pair : labels) {
                form.addView(text(a, skin, pair[1]));
                EditText input = new EditText(a);
                input.setTextColor(skin.text);
                input.setSingleLine(true);
                input.setText(meta.optString(pair[0]));
                input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(240)});
                form.addView(input);
                input.addTextChangedListener(watch(() -> {
                    try { meta.put(pair[0], input.getText().toString()); save.run(); }
                    catch (JSONException ignored) { }
                }));
            }
            Spinner fileList = new Spinner(a);
            ArrayAdapter<String> names = new ArrayAdapter<>(a, android.R.layout.simple_spinner_dropdown_item);
            for (int i = 0; i < sources.length(); i++) names.add(sources.getJSONObject(i).getString("path"));
            fileList.setAdapter(names);
            form.addView(fileList);
            EditText code = new EditText(a);
            code.setTextColor(skin.text);
            code.setTypeface(Typeface.MONOSPACE);
            code.setTextSize(13);
            code.setGravity(android.view.Gravity.TOP | android.view.Gravity.START);
            code.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE |
                android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
            code.setMinLines(10);
            code.setHorizontallyScrolling(true);
            code.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(240000)});
            final int[] current = {0};
            code.setText(sources.getJSONObject(0).getString("text"));
            code.addTextChangedListener(watch(() -> {
                try { sources.getJSONObject(current[0]).put("text", code.getText().toString()); save.run(); }
                catch (JSONException ignored) { }
            }));
            fileList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                public void onNothingSelected(AdapterView<?> parent) { }
                public void onItemSelected(AdapterView<?> parent, View view, int index, long id) {
                    current[0] = index;
                    code.setText(sources.optJSONObject(index).optString("text"));
                }
            });
            form.addView(code);
            button(a, skin, form, "Добавить Java-файл", () -> {
                EditText path = new EditText(a);
                path.setTextColor(skin.text);
                path.setText("java/ttcuz/plugins/Helper.java");
                new ModDialog.Builder(a, skin).setTitle("Новый файл").setView(path)
                    .setPositiveButton("Добавить", (d, which) -> {
                        String name = path.getText().toString().trim();
                        if (!name.matches("java/(?:[A-Za-z_$][\\w$]*/)*[A-Za-z_$][\\w$]*\\.java") || names.getPosition(name) >= 0 || sources.length() >= 32) {
                            Screen.say("Нужен уникальный путь java/…/Имя.java, до 32 файлов"); return;
                        }
                        try {
                            sources.put(new JSONObject().put("path", name).put("text", ""));
                            names.add(name);
                            fileList.setSelection(sources.length() - 1);
                            save.run();
                        } catch (JSONException ignored) { }
                    }).setNegativeButton("Отмена", null).show();
            });
            TextView status = text(a, skin, "Сборка .ctt выполняется на компьютере через ttcuz.plugin.");
            form.addView(status);
            button(a, skin, form, "Экспорт исходников ZIP", () -> {
                try {
                    validate(project);
                    if (!prefs(a).edit().putString("export_pending", project.toString()).commit())
                        throw new IOException("Не удалось сохранить проект");
                    a.startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                        .setType("application/zip").putExtra(Intent.EXTRA_TITLE, meta.optString("id") + "-source.zip"), EXPORT);
                } catch (Exception error) { status.setText(error.getMessage()); }
            });
            final Button[] submit = new Button[1];
            submit[0] = button(a, skin, form, "Отправить на проверку", () -> {
                try { validate(project); } catch (Exception error) { status.setText(error.getMessage()); return; }
                String body = project.toString();
                submit[0].setEnabled(false);
                status.setText("Отправка…");
                Net.away("plugin submission", () -> {
                    String message;
                    try {
                        JSONObject reply = new JSONObject(Net.postResult(ROOT + "/plugins/native/submit", body));
                        if (reply.has("error")) throw new IOException(reply.optString("error"));
                        message = "Заявка: " + reply.getString("digest") + "\nСтатус: " + reply.optString("status");
                    } catch (Exception error) { message = "Не удалось отправить: " + error.getMessage(); }
                    final String result = message;
                    a.runOnUiThread(() -> { status.setText(result); submit[0].setEnabled(true); });
                });
            });
            save.run();
            new ModDialog.Builder(a, skin).setTitle("Редактор плагинов").setView(form)
                .setNegativeButton("Закрыть", (d, which) -> { handler.removeCallbacks(persist); persist.run(); }).show();
        } catch (Exception error) { Screen.say("Не удалось открыть черновик: " + error.getMessage()); }
    }

    private static TextWatcher watch(Runnable action) {
        return new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
            public void afterTextChanged(Editable value) { action.run(); }
        };
    }

    private static void validate(JSONObject project) throws Exception {
        JSONObject meta = project.getJSONObject("metadata");
        if (!meta.optString("id").matches("[A-Za-z0-9][A-Za-z0-9._-]{0,79}")) throw new IOException("Проверьте ID плагина");
        if (!meta.optString("entry").matches("[A-Za-z_$][\\w$]*(?:\\.[A-Za-z_$][\\w$]*)+")) throw new IOException("Проверьте главный класс");
        for (String key : new String[]{"name", "author", "version"})
            if (meta.optString(key).trim().isEmpty() || meta.optString(key).length() > 80)
                throw new IOException("Заполните название, автора и версию (до 80 символов)");
        JSONArray files = project.getJSONArray("files");
        if (files.length() == 0 || files.length() > 32) throw new IOException("Нужно от 1 до 32 Java-файлов");
        java.util.HashSet<String> names = new java.util.HashSet<>();
        for (int i = 0; i < files.length(); i++) {
            JSONObject file = files.getJSONObject(i);
            String path = file.getString("path");
            if (!path.matches("java/(?:[A-Za-z_$][\\w$]*/)*[A-Za-z_$][\\w$]*\\.java") || !names.add(path))
                throw new IOException("Некорректный или повторяющийся путь Java-файла");
            if (file.getString("text").length() > 240000) throw new IOException("Java-файл больше 240000 символов");
        }
        if (project.toString().getBytes(StandardCharsets.UTF_8).length > 1000000)
            throw new IOException("Проект больше 1 МБ");
    }

    static void export(Activity a, Uri uri) {
        String pending = prefs(a).getString("export_pending", null);
        if (pending == null) { Screen.say("Нет проекта для экспорта"); return; }
        Net.away("plugin export", () -> {
            try {
                JSONObject project = new JSONObject(pending);
                validate(project);
                try (OutputStream stream = a.getContentResolver().openOutputStream(uri, "wt")) {
                    if (stream == null) throw new IOException("Не удалось открыть файл");
                    try (ZipOutputStream zip = new ZipOutputStream(stream)) {
                        write(zip, "manifest.json", project.getJSONObject("metadata").toString(2));
                        JSONArray files = project.getJSONArray("files");
                        for (int i = 0; i < files.length(); i++) {
                            JSONObject file = files.getJSONObject(i);
                            write(zip, file.getString("path"), file.getString("text"));
                        }
                    }
                }
                a.runOnUiThread(() -> Screen.say("Исходники сохранены"));
            } catch (Exception error) { a.runOnUiThread(() -> Screen.say("Ошибка экспорта: " + error.getMessage())); }
        });
    }

    private static void write(ZipOutputStream zip, String path, String value) throws IOException {
        zip.putNextEntry(new ZipEntry(path));
        zip.write(value.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }
}

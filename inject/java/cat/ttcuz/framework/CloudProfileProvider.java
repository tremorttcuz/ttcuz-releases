package cat.ttcuz.framework;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import java.io.File;
import org.json.JSONObject;

/**
 * Colours and effects publish without any proof: the first device to publish owns
 * the id. The bio code is needed only for a custom badge image, or to take an id
 * back from another device.
 */
final class CloudProfileProvider implements TtcuzProfileSync.Provider {

    static final String ROOT = "https://ttcuz.daniilsolovatulin.workers.dev";
    private final SharedPreferences prefs;
    private final Context context;
    private static final Object TOKEN_LOCK = new Object();

    CloudProfileProvider(Context context) {
        this.context = context.getApplicationContext();
        prefs = context.getSharedPreferences("ttcuz_profile_identity", Context.MODE_PRIVATE);
    }

    String pluginModerationLink() throws Exception {
        String uid = Account.activeId();
        if (!AdminAccess.allowed(uid)) throw new Exception("Нужен аккаунт владельца");
        recover(uid); // Restore the already trusted device before opening owner tools.
        String response = Net.postResult(ROOT + "/plugins/session", identity(uid).toString());
        requireSuccess(uid, response);
        JSONObject body = new JSONObject(response);
        String session = body.optString("session", "");
        if (!session.matches("[a-f0-9]{64}")) throw new Exception("Не удалось открыть модерацию");
        return ROOT + "/plugin-admin.html#session=" + session;
    }

    private String token(String uid) {
        synchronized (TOKEN_LOCK) {
            String token = prefs.getString("token_" + uid, null);
            if (token == null) {
                byte[] bytes = new byte[32];
                new java.security.SecureRandom().nextBytes(bytes);
                StringBuilder out = new StringBuilder();
                for (byte b : bytes)
                    out.append(String.format(java.util.Locale.ROOT, "%02x", b & 255));
                token = out.toString();
                if (
                    !prefs
                        .edit()
                        .putString("token_" + uid, token)
                        .commit()
                ) throw new IllegalStateException(
                    "Не удалось сохранить ключ подтверждения профиля"
                );
            }
            return token;
        }
    }

    String code(String uid) throws Exception {
        byte[] hash = java.security.MessageDigest.getInstance("SHA-256").digest(
            ("verify:" + uid + ":" + token(uid)).getBytes("UTF-8")
        );
        StringBuilder code = new StringBuilder("ttcuz-");
        for (int i = 0; i < 8; i++) code.append(
            String.format(java.util.Locale.ROOT, "%02x", hash[i] & 255)
        );
        return code.toString();
    }

    JSONObject identity(String uid) throws Exception {
        JSONObject identity = new JSONObject().put("uid", uid).put("token", token(uid));
        String androidId = android.provider.Settings.Secure.getString(
            context.getContentResolver(),
            android.provider.Settings.Secure.ANDROID_ID
        );
        if (androidId != null && !androidId.isEmpty() && !"9774d56d682e549c".equals(androidId)) {
            byte[] bytes = java.security.MessageDigest.getInstance("SHA-256").digest(
                (
                    "ttcuz-device-v1:" +
                    uid +
                    ":" +
                    androidId +
                    ":" +
                    IntegrityKey.PUBLIC_KEY
                ).getBytes("UTF-8")
            );
            StringBuilder secret = new StringBuilder();
            for (byte b : bytes)
                secret.append(String.format(java.util.Locale.ROOT, "%02x", b & 255));
            identity.put("device", secret.toString());
        }
        return identity;
    }

    JSONObject appearance(String uid, JSONObject body) throws Exception {
        JSONObject credentials = identity(uid);
        body.put("uid", uid).put("token", credentials.getString("token"));
        if (credentials.has("device")) body.put("device", credentials.getString("device"));
        String response = Net.postResult(ROOT + "/v1/appearance", body.toString());
        requireSuccess(uid, response);
        JSONObject result = new JSONObject(response);
        if (result.has("verified")) prefs
            .edit()
            .putBoolean("verified_" + uid, result.optBoolean("verified"))
            .remove("proof_" + uid)
            .apply();
        return result;
    }

    boolean recover(String uid) throws Exception {
        String response = Net.postResult(ROOT + "/v1/device", identity(uid).toString());
        if (response == null) return false;
        JSONObject result = new JSONObject(response);
        if (!result.optBoolean("ok")) return false;
        prefs
            .edit()
            .putBoolean("verified_" + uid, result.optBoolean("verified"))
            .remove("proof_" + uid)
            .apply();
        return true;
    }

    org.json.JSONArray awards() throws Exception {
        String uid = Account.activeId();
        if (!AdminAccess.allowed(uid)) throw new Exception("Нужен аккаунт администратора");
        String response = Net.postResult(ROOT + "/v1/badges/awards", identity(uid).toString());
        requireSuccess(uid, response);
        return new JSONObject(response).getJSONArray("awards");
    }

    boolean verified(String uid) {
        return prefs.getBoolean("verified_" + uid, false);
    }

    /** The id is held by another device: only the bio code can take it over. */
    boolean needsProof(String uid) {
        return prefs.getBoolean("proof_" + uid, false);
    }

    /** The server kept the colours but held the custom badge until verification. */
    boolean badgeHeld(String uid) {
        return prefs.getBoolean("held_" + uid, false);
    }

    boolean verify(String uid, String handle) throws Exception {
        String response = Net.postResult(
            ROOT + "/v1/verify",
            identity(uid).put("handle", handle).toString()
        );
        if (response == null) throw new java.io.IOException("Сервер проверки недоступен");
        JSONObject result = new JSONObject(response);
        boolean okay = result.optBoolean("ok", false);
        if (
            !okay && !"verification_unavailable_or_code_missing".equals(result.optString("error"))
        ) throw new java.io.IOException(
            "Ошибка сервера проверки: " + result.optString("error", "unknown")
        );
        // A failed re-check (TikTok throttling, the code already removed from the
        // bio) must never undo a proof that was accepted before. Only the server
        // saying "verify_required" on a publish takes it back.
        if (okay) prefs
            .edit()
            .putBoolean("verified_" + uid, true)
            .remove("proof_" + uid)
            .apply();
        return okay;
    }

    public String badgeHost() {
        return "ttcuz.daniilsolovatulin.workers.dev";
    }

    public TtcuzProfileSync.Metadata fetch(String uid) throws Exception {
        byte[] body = Net.profile(ROOT + "/v1/profiles/" + uid);
        if (body == null || body.length > 16384) return null;
        JSONObject j = new JSONObject(new String(body, "UTF-8"));
        TtcuzProfileSync.Metadata data = new TtcuzProfileSync.Metadata(
            j.getString("profileId"),
            compatibleBadge(j.optString("badgeUrl", null)),
            j.getInt("firstColour"),
            j.getInt("lastColour"),
            j.getLong("updatedAt"),
            j.optInt("middleColour"),
            j.optBoolean("threeColours"),
            j.optInt("direction"),
            j.optBoolean("bold"),
            j.optBoolean("glow"),
            j.optBoolean("animated"),
            j.optInt("speed", 50)
        );
        org.json.JSONArray colours = j.optJSONArray("gradientColours");
        if (
            colours != null && colours.length() >= 2 && colours.length() <= ProfileStyle.MAX_COLOURS
        ) {
            data.gradientColours = new int[colours.length()];
            for (int i = 0; i < colours.length(); i++) data.gradientColours[i] =
                0xFF000000 | colours.getInt(i);
        }
        String name = j.optString("displayName", "");
        data.displayName =
            name.length() <= 80 && !name.matches("(?s).*[\\x00-\\x1f\\x7f].*") ? name : "";
        data.gradientWidth = Math.max(50, Math.min(200, j.optInt("gradientWidth", 100)));
        data.gradientStop = Math.max(5, Math.min(95, j.optInt("gradientStop", 50)));
        data.glowStrength = Math.max(0, Math.min(100, j.optInt("glowStrength", 50)));
        data.offsetX = Math.max(-80, Math.min(80, j.optInt("offsetX")));
        data.offsetY = Math.max(-50, Math.min(50, j.optInt("offsetY")));
        data.badgeSize = Math.max(50, Math.min(200, j.optInt("badgeSize", 100)));
        data.crownOffsetX = Math.max(-100, Math.min(100, j.optInt("crownOffsetX")));
        data.crownOffsetY = Math.max(-100, Math.min(100, j.optInt("crownOffsetY")));
        data.profileLayout = Math.max(0, Math.min(2, j.optInt("profileLayout", 0)));
        String family = j.optString("nicknameFont", "");
        String fontHash = j.optString("nicknameFontHash", "");
        if (
            NicknameFont.validChoice(family) &&
            (!Fonts.FILE.equals(family) || fontHash.matches("[a-f0-9]{64}"))
        ) {
            data.nicknameFont = family;
            data.nicknameFontHash = fontHash.matches("[a-f0-9]{64}") ? fontHash : "";
        }
        return data;
    }

    /** An unsupported badge transport must not discard an otherwise valid gradient. */
    private String compatibleBadge(String value) {
        if (
            value == null ||
            value.length() == 0 ||
            value.length() > 512 ||
            !value.startsWith("https://")
        ) return null;
        try {
            java.net.URI uri = new java.net.URI(value);
            return badgeHost().equalsIgnoreCase(uri.getHost()) && uri.getUserInfo() == null
                ? value
                : null;
        } catch (Exception e) {
            return null;
        }
    }

    public boolean publish(TtcuzProfileSync.Metadata m, File badge) throws Exception {
        if (Fonts.FILE.equals(m.nicknameFont)) {
            File font = NicknameFont.file(context);
            if (!m.profileId.equals(Account.liveId())) return false;
            byte[] raw = Net.read(font);
            if (
                !NicknameFont.validFile(raw) || !m.nicknameFontHash.equals(NicknameFont.digest(raw))
            ) return false;
            if (!prefs.getString("font_uploaded_" + m.profileId, "").equals(m.nicknameFontHash)) {
                String sent = Net.postFont(
                    ROOT + "/v1/nickname-font",
                    identity(m.profileId)
                        .put("font", Base64.encodeToString(raw, Base64.NO_WRAP))
                        .toString()
                );
                forgetIfUnverified(m.profileId, sent);
                if (
                    !accepted(sent) ||
                    !m.nicknameFontHash.equals(new JSONObject(sent).optString("hash", ""))
                ) return false;
                prefs
                    .edit()
                    .putString("font_uploaded_" + m.profileId, m.nicknameFontHash)
                    .apply();
            }
        }
        JSONObject style = new JSONObject()
            .put("nicknameFont", m.nicknameFont)
            .put("nicknameFontHash", m.nicknameFontHash)
            .put("firstColour", m.firstColour)
            .put("lastColour", m.lastColour)
            .put("middleColour", m.middleColour)
            .put("threeColours", m.threeColours)
            .put("direction", m.direction)
            .put("bold", m.bold)
            .put("glow", m.glow)
            .put("glowStrength", m.glowStrength)
            .put("animated", m.animated)
            .put("speed", m.speed)
            .put("offsetX", m.offsetX)
            .put("offsetY", m.offsetY)
            .put("badgeSize", m.badgeSize)
            .put("crownOffsetX", m.crownOffsetX)
            .put("crownOffsetY", m.crownOffsetY)
            .put("displayName", m.displayName)
            .put("gradientWidth", m.gradientWidth)
            .put("gradientStop", m.gradientStop);
        style.put(
            "gradientColours",
            m.gradientColours == null ? JSONObject.NULL : new org.json.JSONArray(m.gradientColours)
        );
        style.put("profileLayout", m.profileLayout).put("musicOn", false);
        byte[] image = badge == null ? null : Net.read(badge);
        if (image != null && image.length > 32768) {
            android.graphics.Bitmap original = android.graphics.BitmapFactory.decodeByteArray(
                image,
                0,
                image.length
            );
            if (original == null) return false;
            try {
                for (int size = 96; size >= 32 && image.length > 32768; size -= 16) {
                    android.graphics.Bitmap small = android.graphics.Bitmap.createScaledBitmap(
                        original,
                        size,
                        size,
                        true
                    );
                    java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
                    small.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out);
                    if (small != original) small.recycle();
                    image = out.toByteArray();
                }
            } finally {
                original.recycle();
            }
            if (image.length > 32768) return false;
        }
        JSONObject body = identity(m.profileId)
            .put("style", style)
            .put(
                "badge",
                image == null ? JSONObject.NULL : Base64.encodeToString(image, Base64.NO_WRAP)
            );
        String response = Net.postResult(ROOT + "/v1/publish", body.toString());
        forgetIfUnverified(m.profileId, response);
        if (response == null) return false;
        if ("nickname_font_missing".equals(new JSONObject(response).optString("error", ""))) prefs
            .edit()
            .remove("font_uploaded_" + m.profileId)
            .apply();
        boolean okay = accepted(response);
        if (okay) prefs
            .edit()
            .remove("proof_" + m.profileId)
            .putBoolean(
                "held_" + m.profileId,
                new JSONObject(response).optBoolean("badgeHeld", false)
            )
            .apply();
        return okay;
    }

    static String fontAddress(String uid, String hash) {
        if (
            !uid.matches("[0-9]{1,24}") || !hash.matches("[a-f0-9]{64}")
        ) throw new IllegalArgumentException("Invalid nickname font identity");
        return ROOT + "/v1/nickname-font/" + uid + "/" + hash;
    }

    public boolean unpublish(String uid) throws Exception {
        return accepted(Net.post(ROOT + "/v1/remove", identity(uid).toString()));
    }

    boolean setCommunityBadge(String targetHandle, boolean grant) throws Exception {
        return setBadge("community", targetHandle, grant);
    }

    /** Hand out or take back one badge from the server's catalog. Creator only. */
    boolean setBadge(String badgeId, String targetHandle, boolean grant) throws Exception {
        String creator = Account.activeId();
        if (!AdminAccess.allowed(creator)) throw new Exception(
            "Выдавать значки может только администратор"
        );
        String target = BadgeTarget.parse(targetHandle);
        if (target == null) throw new Exception("Укажите @ник, ссылку на профиль или UID");
        boolean explicitHandle = targetHandle.trim().startsWith("@") || targetHandle.contains("/@");
        String uid =
            !explicitHandle && target.matches("[0-9]{1,24}") ? target : RecentProfiles.uid(target);
        JSONObject body = identity(creator).put("badgeId", badgeId);
        if (uid != null) body.put("targetUid", uid);
        else body.put("targetHandle", target);
        String route = grant ? "/v1/badges/grant" : "/v1/badges/revoke";
        requireSuccess(creator, Net.postResult(ROOT + route, body.toString()));
        return true;
    }

    String createBadge(String title, byte[] png) throws Exception {
        String creator = Account.activeId();
        if (!AdminAccess.allowed(creator)) throw new Exception("Нужен аккаунт администратора");
        JSONObject body = identity(creator)
            .put("title", title)
            .put("badge", Base64.encodeToString(png, Base64.NO_WRAP));
        String response = Net.postResult(ROOT + "/v1/badges/create", body.toString());
        requireSuccess(creator, response);
        catalogCache = null;
        return new JSONObject(response).getString("badgeId");
    }

    private void requireSuccess(String uid, String response) throws Exception {
        if (response == null) throw new Exception(
            "Сервер недоступен. Проверьте соединение и повторите"
        );
        JSONObject body;
        try {
            body = new JSONObject(response);
        } catch (Exception error) {
            throw new Exception("Сервер вернул неожиданный ответ");
        }
        if (body.optBoolean("ok", false)) return;
        String error = body.optString("error", "unknown");
        if ("creator_verification_required".equals(error) || "verify_required".equals(error)) {
            prefs
                .edit()
                .remove("verified_" + uid)
                .putBoolean("proof_" + uid, true)
                .apply();
            throw new Exception(
                "Подтвердите аккаунт администратора на этом устройстве в разделе профиля"
            );
        }
        if ("invalid_target".equals(error)) throw new Exception(
            "Сервер отклонил получателя. Выберите профиль из недавних или проверьте UID; для выдачи себе обновите сервер значков"
        );
        if ("target_lookup_unavailable".equals(error)) throw new Exception(
            "Публичный поиск TikTok недоступен. Нажмите «Открыть профиль», вернитесь и выберите получателя в «Недавних профилях»; либо укажите UID"
        );
        if ("rate_limited".equals(error)) throw new Exception(
            "Слишком много запросов. Повторите через минуту"
        );
        if ("not_found".equals(error)) throw new Exception(
            "Нужно обновить сервер значков до версии 0.1.8"
        );
        if ("badge_storage_unavailable".equals(error)) throw new Exception(
            "На сервере нужно применить обновление каталога значков"
        );
        if ("catalog_full".equals(error)) throw new Exception("Каталог заполнен");
        if ("unknown_badge".equals(error)) {
            catalogCache = null;
            throw new Exception("Значок отсутствует в каталоге. Откройте каталог заново");
        }
        if ("invalid_badge".equals(error)) throw new Exception(
            "Не удалось принять картинку значка"
        );
        throw new Exception("Не удалось выполнить действие: " + error);
    }

    /** One badge the creator can give. `picture` is null for the mod's own note. */
    static final class CatalogItem {

        final String id, title;
        final int colour;
        final String image;
        android.graphics.Bitmap picture;

        CatalogItem(String id, String title, int colour, String image) {
            this.id = id;
            this.title = title;
            this.colour = colour;
            this.image = image;
        }
    }

    private static volatile java.util.List<CatalogItem> catalogCache;
    private static volatile long catalogAt;

    /** The badges on offer, with their pictures. Blocks: call off the main thread. */
    java.util.List<CatalogItem> catalog() throws Exception {
        java.util.List<CatalogItem> known = catalogCache;
        if (
            known != null && android.os.SystemClock.uptimeMillis() - catalogAt < 60000L
        ) return known;
        byte[] body = Net.bytes(ROOT + "/v1/badge-catalog");
        if (body == null || body.length > 65536) return null;
        org.json.JSONArray list = new JSONObject(new String(body, "UTF-8")).getJSONArray("badges");
        java.util.List<CatalogItem> items = new java.util.ArrayList<CatalogItem>();
        for (int i = 0; i < list.length() && i < 200; i++) {
            JSONObject one = list.getJSONObject(i);
            String id = one.getString("id");
            String image = one.optString("image", "");
            if (!id.matches("[A-Za-z0-9_.-]{1,64}")) continue;
            if (image.length() > 0 && !image.matches("badges/[a-z0-9_-]{1,32}\\.png")) continue;
            String tint = one.optString("colour", "");
            int colour = tint.length() == 0 ? 0 : android.graphics.Color.parseColor(tint);
            CatalogItem item = new CatalogItem(id, one.optString("title", id), colour, image);
            if (image.length() > 0) {
                byte[] png = Net.bytes(ROOT + "/" + image);
                if (png != null && png.length < 65536) item.picture =
                    android.graphics.BitmapFactory.decodeByteArray(png, 0, png.length);
            } else item.picture = Badges.note();
            items.add(item);
        }
        if (items.isEmpty()) return null;
        catalogCache = items;
        catalogAt = android.os.SystemClock.uptimeMillis();
        return items;
    }

    /** The server no longer knows this device as the owner: ask for the proof again. */
    private void forgetIfUnverified(String uid, String response) {
        try {
            if (
                response != null &&
                "verify_required".equals(new JSONObject(response).optString("error"))
            ) {
                prefs
                    .edit()
                    .remove("verified_" + uid)
                    .putBoolean("proof_" + uid, true)
                    .apply();
                Diary.note("profile sync: server asked for verification again");
            }
        } catch (Throwable ignored) {}
    }

    private static boolean accepted(String response) throws Exception {
        return response != null && new JSONObject(response).optBoolean("ok", false);
    }
}

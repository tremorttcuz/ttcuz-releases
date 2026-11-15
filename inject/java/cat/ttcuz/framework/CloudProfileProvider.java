package cat.ttcuz.framework;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import org.json.JSONObject;
import java.io.File;

/**
 * Colours and effects publish without any proof: the first device to publish owns
 * the id. The bio code is needed only for a custom badge image, or to take an id
 * back from another device.
 */
final class CloudProfileProvider implements TtcuzProfileSync.Provider {
    static final String ROOT = Server.ROOT;
    private final SharedPreferences prefs;
    private final Context context;
    private static final Object TOKEN_LOCK = new Object();
    CloudProfileProvider(Context context) {
        this.context=context.getApplicationContext();
        prefs = context.getSharedPreferences("ttcuz_profile_identity", Context.MODE_PRIVATE);
    }
    private String token(String uid) {
        synchronized (TOKEN_LOCK) {
        String token = prefs.getString("token_" + uid, null);
        if (token == null) {
            byte[] bytes = new byte[32]; new java.security.SecureRandom().nextBytes(bytes);
            StringBuilder out = new StringBuilder();
            for (byte b : bytes) out.append(String.format(java.util.Locale.ROOT, "%02x", b & 255));
            token = out.toString();
            if (!prefs.edit().putString("token_" + uid, token).commit())
                throw new IllegalStateException(Text.t("Не удалось сохранить ключ подтверждения профиля"));
        }
        return token;
        }
    }
    String code(String uid) throws Exception {
        byte[] hash = java.security.MessageDigest.getInstance("SHA-256")
                .digest(("verify:" + uid + ":" + token(uid)).getBytes("UTF-8"));
        StringBuilder code = new StringBuilder("ttcuz-");
        for (int i = 0; i < 8; i++) code.append(String.format(java.util.Locale.ROOT, "%02x", hash[i] & 255));
        return code.toString();
    }
    /** Salt of the device id the cloud profile already knows this phone by; changing it would orphan every profile. */
    private static final String DEVICE_SALT = "MIIBojANBgkqhkiG9w0BAQEFAAOCAY8AMIIBigKCAYEAv41Hoa4qvw4uitQkeDK5llUXO8m0Db0bIngd/Txu+9Vf3KEbhozG62mEjBoxBob9FOUnrK38ZxnylpOS72tcp5/JSVJOeMcxP6WFeqsN+ISGqN++pT88bASBNzQQEAc+nTJThYb6MqKpsw73LDgH5VeyAZ6L0VKmvsi25UvGKz9ZuNLtyR87HlXIf591n2cK9sRfU8mkakQgnmJOPC/J7oRwQ9C2Ig2+wGVjVdG5Uv+vNa9KFR0zHYsywXv4/p++P2MlR9JIzjFELIaEWExvx3esLyaW5BrT0Ikupe6gd2/8LXUDOIVAGa+Yu7w7oVDOBbbHoEjh+QiVoGz3v0n24FGMAeGdy0y1YlDX7lHsxGf2w+EpYWrSJMvSMkzafxJ5w1R/RN7tcZSudHSz1hgXnuOXt4xfFbPgCBAqf1grybq7ejeLwlzIQZqcThIgJj0OPTXcrxq7Ewor2lcix6kiBEqEuDXIxbvkNKnC8MFGiPW81VgXzIcMEP44kKbS8wmTAgMBAAE=";
    JSONObject identity(String uid) throws Exception {
        JSONObject identity=new JSONObject().put("uid", uid).put("token", token(uid));
        String androidId=android.provider.Settings.Secure.getString(context.getContentResolver(),android.provider.Settings.Secure.ANDROID_ID);
        if(androidId!=null && !androidId.isEmpty() && !"9774d56d682e549c".equals(androidId)) {
            byte[] bytes=java.security.MessageDigest.getInstance("SHA-256").digest(("ttcuz-device-v1:"+uid+":"+androidId+":"+DEVICE_SALT).getBytes("UTF-8"));
            StringBuilder secret=new StringBuilder();for(byte b:bytes)secret.append(String.format(java.util.Locale.ROOT,"%02x",b&255));
            identity.put("device",secret.toString());
        }
        return identity;
    }
    JSONObject appearance(String uid,JSONObject body) throws Exception {
        JSONObject credentials=identity(uid);body.put("uid",uid).put("token",credentials.getString("token"));
        if(credentials.has("device"))body.put("device",credentials.getString("device"));
        String response=Net.postResult(ROOT+"/v1/appearance",body.toString());requireSuccess(uid,response);
        JSONObject result=new JSONObject(response);
        if(result.has("verified"))prefs.edit().putBoolean("verified_"+uid,result.optBoolean("verified")).remove("proof_"+uid).apply();
        return result;
    }
    boolean recover(String uid) throws Exception {
        String response=Net.postResult(ROOT+"/v1/device",identity(uid).toString());
        if(response==null)return false;JSONObject result=new JSONObject(response);
        if(!result.optBoolean("ok"))return false;
        prefs.edit().putBoolean("verified_"+uid,result.optBoolean("verified")).remove("proof_"+uid).apply();return true;
    }
    org.json.JSONArray awards() throws Exception {
        String uid=Account.activeId();if(!AdminAccess.allowed(uid))throw new Exception(Text.t("Нужен аккаунт администратора"));
        String response=Net.postResult(ROOT+"/v1/badges/awards",identity(uid).toString());requireSuccess(uid,response);
        return new JSONObject(response).getJSONArray("awards");
    }
    boolean verified(String uid) { return prefs.getBoolean("verified_" + uid, false); }

    /**
     * Asks the server what it knows of this device and this account, and keeps exactly that: verified, or "verify
     * again" (another device holds the account, or it was never proven). A failed request changes nothing. Before
     * this the phone only learned it from the side of other answers, and its "verified" could stick either way --
     * "not verified" shown for a proven account, or "verified" for one the server no longer took.
     */
    void refreshVerified(String uid) throws Exception {
        String response = Net.postResult(ROOT + "/v1/device", identity(uid).toString());
        if (response == null) return;
        JSONObject result;
        try { result = new JSONObject(response); } catch (Exception error) { return; }
        if (result.optBoolean("ok", false)) {
            boolean proven = result.optBoolean("verified", false);
            SharedPreferences.Editor edit = prefs.edit().putBoolean("verified_" + uid, proven);
            if (proven) edit.remove("proof_" + uid);
            edit.apply();
        } else if ("verify_required".equals(result.optString("error"))) {
            prefs.edit().remove("verified_" + uid).putBoolean("proof_" + uid, true).apply();
        }
        prefs.edit().putLong("checked_" + uid, System.currentTimeMillis()).apply();
    }

    /** The last time the server was asked, to ask at most every few hours. */
    long lastChecked(String uid) { return prefs.getLong("checked_" + uid, 0L); }
    /** The id is held by another device: only the bio code can take it over. */
    boolean needsProof(String uid) { return prefs.getBoolean("proof_" + uid, false); }
    /** The server kept the colours but held the custom badge until verification. */
    boolean badgeHeld(String uid) { return prefs.getBoolean("held_" + uid, false); }
    boolean verify(String uid, String handle) throws Exception {
        String response=Net.postResult(ROOT + "/v1/verify",identity(uid).put("handle",handle).toString());
        if(response==null)throw new java.io.IOException(Text.t("Сервер проверки недоступен"));
        JSONObject result=new JSONObject(response);
        boolean okay=result.optBoolean("ok",false);
        if(!okay && !"verification_unavailable_or_code_missing".equals(result.optString("error")))
            throw new java.io.IOException(Text.t("Ошибка сервера проверки: ")+result.optString("error","unknown"));
        // A failed re-check (TikTok throttling, the code already removed from the
        // bio) must never undo a proof that was accepted before. Only the server
        // saying "verify_required" on a publish takes it back.
        if (okay) prefs.edit().putBoolean("verified_" + uid, true).remove("proof_" + uid).apply();
        return okay;
    }
    public String badgeHost() { return "ttcuz.ttcuz1337.workers.dev"; }
    @Override public java.util.Map<String, Long> directory() throws Exception {
        byte[] body = Net.bytes(ROOT + "/v1/directory");
        if (body == null) return null;
        JSONObject profiles = new JSONObject(new String(body, "UTF-8")).optJSONObject("profiles");
        if (profiles == null) return null;
        java.util.Map<String, Long> known = new java.util.HashMap<>();
        for (java.util.Iterator<String> it = profiles.keys(); it.hasNext(); ) {
            String uid = it.next();
            if (Ids.uid(uid)) known.put(uid, profiles.optLong(uid));
        }
        return known;
    }
    public TtcuzProfileSync.Metadata fetch(String uid) throws Exception {
        byte[] body = Net.bytes(ROOT + "/v1/profiles/" + uid);
        if (body == null || body.length > 16384) return null;
        JSONObject j = new JSONObject(new String(body, "UTF-8"));
        TtcuzProfileSync.Metadata data = new TtcuzProfileSync.Metadata(j.getString("profileId"),
                compatibleBadge(j.optString("badgeUrl", null)),
                j.getInt("firstColour"), j.getInt("lastColour"), j.getLong("updatedAt"),
                j.optInt("middleColour"), j.optBoolean("threeColours"), j.optInt("direction"),
                j.optBoolean("bold"), j.optBoolean("glow"), j.optBoolean("animated"), j.optInt("speed", 50));
        org.json.JSONArray colours=j.optJSONArray("gradientColours");
        if(colours!=null && colours.length()>=2 && colours.length()<=ProfileStyle.MAX_COLOURS) {
            data.gradientColours=new int[colours.length()];
            for(int i=0;i<colours.length();i++)data.gradientColours[i]=0xFF000000|colours.getInt(i);
        }
        String name=j.optString("displayName","");data.displayName=name.length()<=80&&!name.matches("(?s).*[\\x00-\\x1f\\x7f].*")?name:"";
        data.gradientWidth=Math.max(50,Math.min(200,j.optInt("gradientWidth",100)));data.gradientStop=Math.max(5,Math.min(95,j.optInt("gradientStop",50)));data.gradientMotion=Math.max(0,Math.min(4,j.optInt("gradientMotion",0)));
        data.appearEffect=Math.max(0,Math.min(4,j.optInt("appearEffect",0)));data.particles=Math.max(0,Math.min(5,j.optInt("particles",0)));{ParticleRecipe recipe=ParticleRecipe.parse(j.optString("particleRecipe",""));data.particleRecipe=recipe==null?"":recipe.json();if(data.particles==ProfileStyle.CUSTOM&&recipe==null)data.particles=ProfileStyle.SPARKLES;}data.commentColour=j.optInt("commentColour",0);
        String tag=j.optString("tag","");data.tag=tag.length()<=16&&!tag.matches("(?s).*[\\x00-\\x1f\\x7f].*")?tag:"";
        // the server keeps this account's tag: a phone that lost it (reinstalled, cleared) gets it back from here
        if(uid.equals(Account.id())&&!data.tag.isEmpty())ProfileStyle.restoreTag(data.tag);
        data.offsetX = Math.max(-80, Math.min(80, j.optInt("offsetX")));
        data.offsetY = Math.max(-50, Math.min(50, j.optInt("offsetY")));
        data.badgeSize = Math.max(50, Math.min(200, j.optInt("badgeSize", 100)));
        data.crownOffsetX = Math.max(-100, Math.min(100, j.optInt("crownOffsetX")));
        data.crownOffsetY = Math.max(-100, Math.min(100, j.optInt("crownOffsetY")));
        data.profileLayout = Math.max(0, Math.min(2, j.optInt("profileLayout", 0)));
        data.glowStrength = Math.max(0, Math.min(100, j.optInt("glowStrength", 50)));
        String font = j.optString("nicknameFont", ""), fontHash = j.optString("nicknameFontHash", "");
        if (NicknameFont.validChoice(font) && (!Fonts.FILE.equals(font) || Ids.sha256(fontHash))) {
            data.nicknameFont = font;
            data.nicknameFontHash = Ids.sha256(fontHash) ? fontHash : "";
        }
        return data;
    }
    /** Where others fetch an uploaded nickname font: by owner and SHA-256 only. */
    static String fontAddress(String uid, String hash) {
        if (!Ids.uid(uid) || !Ids.sha256(hash)) throw new IllegalArgumentException("Invalid nickname font identity");
        return ROOT + "/v1/nickname-font/" + uid + "/" + hash;
    }
    /** An unsupported badge transport must not discard an otherwise valid gradient. */
    private String compatibleBadge(String value){
        if(value==null||value.length()==0||value.length()>512||!value.startsWith("https://"))return null;
        try{java.net.URI uri=new java.net.URI(value);String host=uri.getHost();boolean ours="ttcuz.ttcuz1337.workers.dev".equalsIgnoreCase(host);return ours&&uri.getUserInfo()==null?value:null;}
        catch(Exception e){return null;}
    }
    public boolean publish(TtcuzProfileSync.Metadata m, File badge) throws Exception {
        // An uploaded nickname font goes to the server once per file (by its SHA-256), before the style that names it.
        if (Fonts.FILE.equals(m.nicknameFont) && m.profileId.equals(Account.liveId())) {
            byte[] font = Net.read(NicknameFont.file(context));
            if (NicknameFont.validFile(font) && m.nicknameFontHash.equals(NicknameFont.digest(font))
                    && !prefs.getString("font_uploaded_" + m.profileId, "").equals(m.nicknameFontHash)) {
                String answer = Net.postFont(ROOT + "/v1/nickname-font", identity(m.profileId)
                        .put(Fonts.KEY, android.util.Base64.encodeToString(font, android.util.Base64.NO_WRAP)).toString());
                if (accepted(answer) && m.nicknameFontHash.equals(new JSONObject(answer).optString("hash", "")))
                    prefs.edit().putString("font_uploaded_" + m.profileId, m.nicknameFontHash).apply();
            }
        }
        JSONObject style = new JSONObject().put("nicknameFont", m.nicknameFont).put("nicknameFontHash", m.nicknameFontHash)
                .put("glowStrength", m.glowStrength).put("firstColour", m.firstColour).put("lastColour", m.lastColour)
                .put("middleColour", m.middleColour).put("threeColours", m.threeColours)
                .put("direction", m.direction).put("bold", m.bold).put("glow", m.glow)
                .put("animated", m.animated).put("speed", m.speed).put("offsetX", m.offsetX)
                .put("offsetY", m.offsetY).put("badgeSize", m.badgeSize)
                .put("crownOffsetX", m.crownOffsetX).put("crownOffsetY", m.crownOffsetY).put("displayName",m.displayName).put("gradientWidth",m.gradientWidth).put("gradientStop",m.gradientStop).put("gradientMotion",m.gradientMotion)
                .put("appearEffect",m.appearEffect).put("particles",m.particles).put("particleRecipe",m.particleRecipe==null?"":m.particleRecipe).put("commentColour",m.commentColour).put("tag",m.tag);
        style.put("gradientColours",m.gradientColours==null?JSONObject.NULL:new org.json.JSONArray(m.gradientColours));
        style.put("profileLayout", m.profileLayout).put("musicOn", false);
        byte[] image = badge == null ? null : Net.read(badge);
        if (image != null && Gif.isGif(image) && image.length > ProfileStyle.GIF_MAX) return false;
        if (image != null && !Gif.isGif(image) && image.length > 32768) {
            android.graphics.Bitmap original = android.graphics.BitmapFactory.decodeByteArray(image, 0, image.length);
            if (original == null) return false;
            try {
                for (int size = 96; size >= 32 && image.length > 32768; size -= 16) {
                    android.graphics.Bitmap small = android.graphics.Bitmap.createScaledBitmap(original, size, size, true);
                    java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
                    small.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out);
                    if (small != original) small.recycle();
                    image = out.toByteArray();
                }
            } finally { original.recycle(); }
            if (image.length > 32768) return false;
        }
        // The picture goes up only when it changed since the server last took it; otherwise the server keeps its copy.
        String picture = image == null ? "" : Base64.encodeToString(image, Base64.NO_WRAP);
        String pictureHash = image == null ? "none" : Integer.toHexString(java.util.Arrays.hashCode(image)) + ":" + image.length;
        boolean same = image != null && pictureHash.equals(prefs.getString("badge_sent_" + m.profileId, ""));
        JSONObject body = identity(m.profileId).put("style", style).put("badge",
                image == null || same ? JSONObject.NULL : picture);
        if (same) body.put("keepBadge", true);
        String response = Net.postProfile(ROOT + "/v1/publish", body.toString());
        forgetIfUnverified(m.profileId, response);
        boolean okay = accepted(response);
        boolean held = okay && new JSONObject(response).optBoolean("badgeHeld", false);
        if (okay) prefs.edit().remove("proof_" + m.profileId)
                .putBoolean("held_" + m.profileId, held)
                // a held picture was not stored: it has to go up again once the account is verified
                .putString("badge_sent_" + m.profileId, held ? "" : pictureHash).apply();
        return okay;
    }
    /**
     * Takes a tag for the account, or lets it go (""). The server holds who has which tag; a taken one (in any case,
     * or with look-alike letters) comes back as an error that says so.
     */
    String claimTag(String uid, String tag) throws Exception {
        String response = Net.postResult(ROOT + "/v1/tag", identity(uid).put("tag", tag).toString());
        if (response != null && response.contains("\"tag_taken\"")) throw new Exception(Text.t("Этот тег уже занят"));
        if (response != null && response.contains("\"invalid_tag\"")) throw new Exception(Text.t("Тег: 2–16 букв, цифр, «_», «.» или «-»"));
        if (response != null && response.contains("\"verify_required\"")) throw new Exception(Text.t("Сначала подтвердите аккаунт в разделе профиля"));
        requireSuccess(uid, response);
        return new JSONObject(response).optString("tag", tag);
    }

    /** The creator promotes a video for some days (0 takes it back); returns until when, for the notice. */
    String promote(String uid, String aid, int days, int weight) throws Exception {
        String response = Net.postResult(ROOT + "/v1/promote", identity(uid).put("aid", aid).put("days", days).put("weight", weight).toString());
        if (response != null && response.contains("creator_verification_required")) throw new Exception(Text.t("Продвигать может только подтверждённый аккаунт создателя"));
        requireSuccess(uid, response);
        return days == 0 ? "" : days + " " + Text.t(days == 1 ? "день" : days < 5 ? "дня" : "дней");
    }

    public boolean unpublish(String uid) throws Exception {
        prefs.edit().remove("badge_sent_" + uid).apply();
        return accepted(Net.post(ROOT + "/v1/remove", identity(uid).toString()));
    }
    boolean setCommunityBadge(String targetHandle, boolean grant) throws Exception {
        return setBadge("community", targetHandle, grant);
    }

    /** Hand out or take back one badge from the server's catalog. Creator only. */
    boolean setBadge(String badgeId, String targetHandle, boolean grant) throws Exception {
        String creator = Account.activeId();
        if (!AdminAccess.allowed(creator)) throw new Exception(Text.t("Выдавать значки может только администратор"));
        String target = BadgeTarget.parse(targetHandle);
        if (target == null) throw new Exception(Text.t("Укажите @ник, ссылку на профиль или UID"));
        boolean explicitHandle=targetHandle.trim().startsWith("@")||targetHandle.contains("/@");
        String uid = !explicitHandle&&Ids.uid(target) ? target : RecentProfiles.uid(target);
        JSONObject body = identity(creator).put("badgeId", badgeId);
        if (uid != null) body.put("targetUid", uid);
        else body.put("targetHandle", target);
        String route = grant ? "/v1/badges/grant" : "/v1/badges/revoke";
        requireSuccess(creator, Net.postResult(ROOT + route, body.toString()));
        return true;
    }

    String createBadge(String title, byte[] png) throws Exception {
        String creator = Account.activeId();
        if (!AdminAccess.allowed(creator)) throw new Exception(Text.t("Нужен аккаунт администратора"));
        JSONObject body = identity(creator).put("title", title)
                .put("badge", Base64.encodeToString(png, Base64.NO_WRAP));
        String response = Net.postResult(ROOT + "/v1/badges/create", body.toString());
        requireSuccess(creator, response);
        catalogCache = null;
        return new JSONObject(response).getString("badgeId");
    }

    private void requireSuccess(String uid, String response) throws Exception {
        if (response == null) throw new Exception(Text.t("Сервер недоступен. Проверьте соединение и повторите"));
        JSONObject body;
        try { body = new JSONObject(response); }
        catch (Exception error) { throw new Exception(Text.t("Сервер вернул неожиданный ответ")); }
        if (body.optBoolean("ok", false)) return;
        String error = body.optString("error", "unknown");
        if ("creator_verification_required".equals(error) || "verify_required".equals(error)) {
            prefs.edit().remove("verified_" + uid).putBoolean("proof_" + uid, true).apply();
            throw new Exception(Text.t("Подтвердите аккаунт администратора на этом устройстве в разделе профиля"));
        }
        if ("invalid_target".equals(error))
            throw new Exception(Text.t("Сервер отклонил получателя. Выберите профиль из недавних или проверьте UID; для выдачи себе обновите сервер значков"));
        if ("target_lookup_unavailable".equals(error))
            throw new Exception(Text.t("Публичный поиск TikTok недоступен. Нажмите «Открыть профиль», вернитесь и выберите получателя в «Недавних профилях»; либо укажите UID"));
        if ("rate_limited".equals(error)) throw new Exception(Text.t("Слишком много запросов. Повторите через минуту"));
        if ("not_found".equals(error)) throw new Exception(Text.t("Нужно обновить сервер значков до версии 0.1.8"));
        if ("badge_storage_unavailable".equals(error)) throw new Exception(Text.t("На сервере нужно применить обновление каталога значков"));
        if ("catalog_full".equals(error)) throw new Exception(Text.t("Каталог заполнен"));
        if ("unknown_badge".equals(error)) { catalogCache = null; throw new Exception(Text.t("Значок отсутствует в каталоге. Откройте каталог заново")); }
        if ("invalid_badge".equals(error)) throw new Exception(Text.t("Не удалось принять картинку значка"));
        throw new Exception(Text.t("Не удалось выполнить действие: ") + error);
    }

    /** One badge the creator can give. `picture` is null for the mod's own note. */
    static final class CatalogItem {
        final String id, title;
        final int colour;
        final String image;
        android.graphics.Bitmap picture;
        CatalogItem(String id, String title, int colour, String image) {
            this.id = id; this.title = title; this.colour = colour; this.image = image;
        }
    }

    private static volatile java.util.List<CatalogItem> catalogCache;
    private static volatile long catalogAt;

    /** The badges on offer, with their pictures. Blocks: call off the main thread. */
    java.util.List<CatalogItem> catalog() throws Exception {
        java.util.List<CatalogItem> known = catalogCache;
        if (known != null && android.os.SystemClock.uptimeMillis() - catalogAt < 60000L) return known;
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
                if (png != null && png.length < 65536)
                    item.picture = android.graphics.BitmapFactory.decodeByteArray(png, 0, png.length);
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
            if (response != null && "verify_required".equals(
                    new JSONObject(response).optString("error"))) {
                prefs.edit().remove("verified_" + uid).putBoolean("proof_" + uid, true).apply();
                Diary.note("profile sync: server asked for verification again");
            }
        } catch (Throwable ignored) { }
    }
    private static boolean accepted(String response) throws Exception {
        return response != null && new JSONObject(response).optBoolean("ok", false);
    }
}

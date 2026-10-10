package cat.narezany.margyt;

/** Short-lived identities supplied by TikTok itself, including opened profiles. */
final class RecentProfiles {
    private static final java.util.LinkedHashMap<String, Entry> seen = new java.util.LinkedHashMap<>();
    private static final class Entry {
        final String uid; final long at;
        Entry(String uid) { this.uid = uid; at = android.os.SystemClock.uptimeMillis(); }
    }
    static synchronized void remember(String uid, String handle) {
        if (uid == null || !uid.matches("[0-9]{1,24}") || handle == null
                || !handle.matches("[A-Za-z0-9_.]{2,24}")) return;
        String key = handle.toLowerCase(java.util.Locale.ROOT);
        Entry known=seen.get(key);
        // Native getters can be called hundreds of times for the same visible row.
        // Refresh at most once a minute, without reallocating/scanning the cache.
        if(known!=null && known.uid.equals(uid)
                && android.os.SystemClock.uptimeMillis()-known.at<60000L)return;
        java.util.Iterator<java.util.Map.Entry<String,Entry>> aliases = seen.entrySet().iterator();
        while (aliases.hasNext()) {
            java.util.Map.Entry<String,Entry> entry = aliases.next();
            if (entry.getValue().uid.equals(uid) && !entry.getKey().equals(key)) aliases.remove();
        }
        seen.remove(key); seen.put(key, new Entry(uid));
        while (seen.size() > 256) seen.remove(seen.keySet().iterator().next());
    }
    static synchronized java.util.List<String[]> choices(){
        java.util.ArrayList<String[]> result=new java.util.ArrayList<>();long now=android.os.SystemClock.uptimeMillis();
        for(java.util.Map.Entry<String,Entry> item:seen.entrySet())if(now-item.getValue().at<=600000L)result.add(new String[]{item.getKey(),item.getValue().uid});
        java.util.Collections.reverse(result);return result;
    }
    static synchronized String uid(String handle) {
        if (handle == null) return null;
        Entry entry = seen.get(handle.toLowerCase(java.util.Locale.ROOT));
        return entry == null || android.os.SystemClock.uptimeMillis() - entry.at > 600000L
                ? null : entry.uid;
    }
}

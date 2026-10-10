package cat.narezany.margyt;

/** Strict target parsing: never silently truncate a nickname or trust another host. */
final class BadgeTarget {
    private BadgeTarget() {}
    static String parse(String raw) {
        if (raw == null) return null;
        String text = raw.trim();
        if (text.matches("[0-9]{1,24}")) return text;
        if (text.startsWith("www.tiktok.com/")) text = "https://" + text;
        if (text.startsWith("https://") || text.startsWith("http://")) {
            try {
                java.net.URI uri = new java.net.URI(text);
                String host = uri.getHost();
                if (host == null || !(host.equalsIgnoreCase("www.tiktok.com")
                        || host.equalsIgnoreCase("tiktok.com") || host.equalsIgnoreCase("m.tiktok.com"))) return null;
                String path = uri.getPath();
                if (path == null || !path.matches("/@[A-Za-z0-9_.]{2,24}/?")) return null;
                text = path.substring(2).replaceFirst("/$", "");
            } catch (Exception error) { return null; }
        } else if (text.startsWith("@")) text = text.substring(1);
        return text.matches("[A-Za-z0-9_.]{2,24}") ? text : null;
    }
}

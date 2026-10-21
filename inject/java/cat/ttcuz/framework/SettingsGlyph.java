package cat.ttcuz.framework;

/** Stroke vectors shared with assets/ui/settings/*.svg. */
final class SettingsGlyph extends android.graphics.drawable.Drawable {

    private final android.graphics.Path path;
    private final android.graphics.Paint ink = new android.graphics.Paint(3);

    static android.graphics.drawable.Drawable make(String name, int colour) {
        String d = SettingsGlyphPaths.find(name);
        return d == null ? null : new SettingsGlyph(d, colour);
    }

    private static final java.util.Map<String, android.graphics.Path> parsed =
        new java.util.HashMap<>();

    private SettingsGlyph(String d, int colour) {
        ink.setStyle(android.graphics.Paint.Style.FILL);
        ink.setStrokeCap(android.graphics.Paint.Cap.ROUND);
        ink.setStrokeJoin(android.graphics.Paint.Join.ROUND);
        ink.setColor(colour);
        android.graphics.Path cached = parsed.get(d);
        if (cached != null) {
            path = cached;
            return;
        }
        path = new android.graphics.Path();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(
            "[MLCZ]|-?[0-9]+(?:\\.[0-9]+)?"
        ).matcher(d);
        java.util.ArrayList<String> tokens = new java.util.ArrayList<>();
        while (m.find()) tokens.add(m.group());
        for (int i = 0; i < tokens.size(); ) {
            String command = tokens.get(i++);
            if (command.equals("Z")) {
                path.close();
                continue;
            }
            int count = command.equals("C") ? 6 : 2;
            float[] values = new float[count];
            for (int n = 0; n < count; n++) values[n] = Float.parseFloat(tokens.get(i++));
            if (command.equals("M")) path.moveTo(values[0], values[1]);
            else if (command.equals("L")) path.lineTo(values[0], values[1]);
            else path.cubicTo(values[0], values[1], values[2], values[3], values[4], values[5]);
        }
        parsed.put(d, path);
    }

    @Override
    public void draw(android.graphics.Canvas canvas) {
        android.graphics.Rect b = getBounds();
        int save = canvas.save();
        canvas.translate(b.left, b.top);
        canvas.scale(b.width() / 24f, b.height() / 24f);
        canvas.drawPath(path, ink);
        canvas.restoreToCount(save);
    }

    @Override
    public void setAlpha(int alpha) {
        ink.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(android.graphics.ColorFilter filter) {
        ink.setColorFilter(filter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return android.graphics.PixelFormat.TRANSLUCENT;
    }
}

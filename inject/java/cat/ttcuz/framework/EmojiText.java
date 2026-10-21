package cat.ttcuz.framework;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.MetricAffectingSpan;
import android.text.style.ReplacementSpan;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.WeakHashMap;

/** Apply the selected font to complete Unicode emoji clusters, not letters. */
public final class EmojiText {

    private EmojiText() {}

    private static final Map<TextView, Binding> bindings = new WeakHashMap<>();

    private static final class Binding implements android.text.TextWatcher, android.text.SpanWatcher {

        boolean busy;
        WeakReference<CharSequence> text;
        Typeface face;
        String content;
        int count;
        WeakReference<TextView> view;

        private void repair() {
            TextView target = view == null ? null : view.get();
            if (!busy && face != null && target != null) apply(target, face);
        }
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        @Override public void afterTextChanged(android.text.Editable text) { repair(); }
        @Override public void onSpanAdded(Spannable text, Object span, int start, int end) {
            if (span instanceof ReplacementSpan || span instanceof EmojiSpan) repair();
        }
        @Override public void onSpanRemoved(Spannable text, Object span, int start, int end) {
            if (span instanceof EmojiSpan) repair();
        }
        @Override public void onSpanChanged(Spannable text, Object span, int oldStart, int oldEnd,
                int start, int end) { onSpanAdded(text, span, start, end); }
    }

    private static final class Rule {

        final HashSet<String> values = new HashSet<>();
        int longest;
    }

    private static Map<Integer, Rule> rules;

    static boolean active() {
        return (
            !bindings.isEmpty() ||
            (android.os.Build.VERSION.SDK_INT >= 29 && !Fonts.SYSTEM.equals(Fonts.emoji()))
        );
    }

    private static synchronized Map<Integer, Rule> rules(Context context) throws Exception {
        if (rules != null) return rules;
        Map<Integer, Rule> loaded = new HashMap<>();
        try (
            BufferedReader input = new BufferedReader(
                new InputStreamReader(
                    context.getAssets().open("ttcuz/emoji/sequences-17.txt"),
                    "UTF-8"
                )
            )
        ) {
            String line;
            int count = 0;
            while ((line = input.readLine()) != null) {
                if (
                    line.isEmpty() || line.length() > 64 || ++count > 10000
                ) throw new IllegalArgumentException("invalid emoji sequences");
                int first = line.codePointAt(0);
                Rule rule = loaded.get(first);
                if (rule == null) {
                    rule = new Rule();
                    loaded.put(first, rule);
                }
                rule.values.add(line);
                rule.longest = Math.max(rule.longest, line.length());
            }
        }
        rules = loaded;
        return loaded;
    }

    static void warm(Context context) {
        try { rules(context); } catch (Exception error) {
            Diary.note("emoji sequences: " + error.getClass().getSimpleName());
        }
    }

    /** Longest match keeps flags, modifiers, keycaps and ZWJ sequences together. */
    private static int end(String text, int start, Map<Integer, Rule> table) {
        Rule rule = table.get(text.codePointAt(start));
        if (rule == null) return start;
        for (int end = Math.min(text.length(), start + rule.longest); end > start; end--) {
            if (end < text.length() && Character.isLowSurrogate(text.charAt(end))) continue;
            if (rule.values.contains(text.substring(start, end))) {
                // VS15 explicitly asks for a text symbol, not a coloured emoji.
                return end < text.length() && text.charAt(end) == '\uFE0E' ? start : end;
            }
        }
        return start;
    }

    // An application span is not parcelled as a framework TypefaceSpan: after
    // screen restoration the active preference is applied again, not an old font.
    private static final class EmojiSpan extends MetricAffectingSpan {

        final Typeface face;
        final String original;
        final ReplacementSpan[] images;
        final int[] flags;

        EmojiSpan(Typeface face, String original, ReplacementSpan[] images, int[] flags) {
            this.face = face;
            this.original = original;
            this.images = images;
            this.flags = flags;
        }

        @Override
        public void updateMeasureState(TextPaint paint) {
            paint.setTypeface(face);
        }

        @Override
        public void updateDrawState(TextPaint paint) {
            paint.setTypeface(face);
        }
    }

    public static void apply(TextView view, Typeface face) {
        if (view == null) return;
        Binding binding = bindings.get(view);
        if (binding == null && face == null) return;
        if (binding == null) {
            binding = new Binding();
            binding.view = new WeakReference<>(view);
            bindings.put(view, binding);
            view.addTextChangedListener(binding);
        }
        if (binding.busy) return;
        CharSequence current = view.getText();
        if (current == null) return;
        String content = current.toString();
        // Mutable text may have acquired a native EmojiCompat span since last time.
        if (
            binding.text != null &&
            binding.text.get() == current &&
            binding.face == face &&
            content.equals(binding.content)
        ) {
            boolean nativeOverride = false;
            if (current instanceof Spannable) {
                Spanned spans = (Spanned) current;
                EmojiSpan[] ours = spans.getSpans(0, current.length(), EmojiSpan.class);
                nativeOverride = ours.length != binding.count;
                for (ReplacementSpan image : spans.getSpans(
                    0,
                    current.length(),
                    ReplacementSpan.class
                ))
                    for (EmojiSpan own : ours)
                        if (
                            spans.getSpanStart(image) == spans.getSpanStart(own) &&
                            spans.getSpanEnd(image) == spans.getSpanEnd(own)
                        ) nativeOverride = true;
            }
            if (!nativeOverride) return;
        }
        binding.busy = true;
        boolean copied = false;
        try {
            Map<Integer, Rule> table = face == null ? null : rules(view.getContext());
            Spannable text =
                current instanceof Spannable ? (Spannable) current : new SpannableString(current);
            boolean changed = false;
            for (EmojiSpan old : text.getSpans(0, text.length(), EmojiSpan.class)) {
                int start = text.getSpanStart(old),
                    end = text.getSpanEnd(old);
                text.removeSpan(old);
                changed = true;
                if (
                    start >= 0 &&
                    end >= start &&
                    old.original.equals(text.subSequence(start, end).toString())
                ) for (int i = 0; i < old.images.length; i++) text.setSpan(
                    old.images[i],
                    start,
                    end,
                    old.flags[i]
                );
            }
            if (face != null) for (int start = 0; start < content.length(); ) {
                int end = end(content, start, table);
                if (end == start) {
                    start += Character.charCount(content.codePointAt(start));
                    continue;
                }
                java.util.ArrayList<ReplacementSpan> images = new java.util.ArrayList<>();
                java.util.ArrayList<Integer> flags = new java.util.ArrayList<>();
                for (ReplacementSpan image : text.getSpans(start, end, ReplacementSpan.class)) {
                    // Only an image for this exact Unicode cluster is a font override.
                    if (
                        text.getSpanStart(image) != start || text.getSpanEnd(image) != end
                    ) continue;
                    images.add(image);
                    flags.add(text.getSpanFlags(image));
                    text.removeSpan(image);
                }
                int[] saved = new int[flags.size()];
                for (int i = 0; i < saved.length; i++) saved[i] = flags.get(i);
                text.setSpan(
                    new EmojiSpan(
                        face,
                        content.substring(start, end),
                        images.toArray(new ReplacementSpan[0]),
                        saved
                    ),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
                changed = true;
                start = end;
            }
            if (changed) {
                if (text != current) {
                    view.setText(text, TextView.BufferType.SPANNABLE);
                    copied = true;
                } else view.invalidate();
            }
            binding.text = new WeakReference<>(view.getText());
            binding.content = content;
            binding.face = face;
            binding.count = text.getSpans(0, text.length(), EmojiSpan.class).length;
            if (face == null) {
                if (view.getText() instanceof Spannable) ((Spannable) view.getText()).removeSpan(binding);
                view.removeTextChangedListener(binding);
                bindings.remove(view);
            } else if (view.getText() instanceof Spannable) {
                Spannable committed = (Spannable) view.getText();
                committed.setSpan(binding, 0, committed.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            }
        } catch (Throwable error) {
            Diary.note("emoji text: " + error.getClass().getSimpleName());
        } finally {
            binding.busy = false;
        }
        // Input filters may insert EmojiCompat images during setText. The second
        // pass edits the committed Spannable in place and cannot reset the text.
        if (copied && face != null && view.getText() instanceof Spannable) apply(view, face);
    }
}

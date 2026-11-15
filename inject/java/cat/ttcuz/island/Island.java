package cat.ttcuz.island;

import android.graphics.Canvas;
import android.view.MotionEvent;

/**
 * A whole bottom bar written in Java: the island draws everything, puts its buttons where it wants them and gets the
 * touches on the bar first. Extend it (or {@link Styled}, which already draws a complete MD3 island you can change
 * piece by piece) and write the island in Settings → Appearance → Own islands; the mod compiles it on the phone.
 *
 * It runs on the main thread inside TikTok's frame: draw() should allocate nothing and do no I/O. Whatever the island
 * leaves alone stays the mod's: TikTok still opens the tab that was tapped, unread counts still follow the icons.
 * When an island throws, it is switched off and the mod's own island comes back.
 *
 * The island's name and author come from two comment lines at the top of its file:
 * <pre>
 * // @name My island
 * // @author @me
 * </pre>
 */
public abstract class Island {
    /**
     * Where things go: set bar.body, bar.radius, bar.x / bar.y (each button's centre) and bar.hit (each button's tap
     * area). Called before every frame; the fields already hold the mod's own placement.
     */
    public void layout(Bar bar) {}

    /** Draws the whole bar: glass, selection, icons, labels, the create button. Nothing of the mod's is drawn. */
    public abstract void draw(Canvas canvas, Bar bar);

    /** Any touch on the bar (x, y in the bar's coordinates). true: used, the mod does nothing with this event. */
    public boolean onTouch(Bar bar, MotionEvent event) { return false; }

    /** A tap on button `tab`. true: handled here and TikTok is not told; false: TikTok opens that tab. */
    public boolean onTap(Bar bar, int tab) { return false; }

    /** A long press on button `tab`. true: handled. */
    public boolean onHold(Bar bar, int tab) { return false; }

    /** The island goes away (another chosen, or none): drop what draw() keeps. */
    public void onRelease() {}
}

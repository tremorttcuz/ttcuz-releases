// @name Мой островок
// @author ttcuz
package cat.ttcuz.island.examples;

import cat.ttcuz.island.Styled;

/**
 * A starting point: the MD3 island with every setting written out. Change the numbers, or override layout() and
 * draw() for anything the settings do not cover. Sizes are dp, places in the bar are fractions 0..1.
 */
public class Starter extends Styled {
    public Starter() {
        // the bar
        barHeight = 60;          // 0 = TikTok's own height
        side = 12;               // from the screen's left and right edges
        lift = 6;                // above the bottom
        radius = 30;             // -1 = a capsule
        backdrop = true;         // the blurred video under the bar
        fill = Ink.CARD.opacity(.72f);
        stroke = Ink.TEXT.opacity(.08f);
        strokeWidth = 1;
        shadow = Ink.of(0x40000000);
        shadowRadius = 14;
        shadowY = 4;

        // the buttons
        order = new String[]{"home", "friends", "create", "inbox", "profile"};
        positions = null;        // e.g. new float[]{.1f, .3f, .5f, .7f, .9f}
        iconSize = 24;
        iconColor = Ink.MUTED;
        iconSelected = Ink.ON_ACCENT;
        labels = BESIDE;         // NONE, BELOW, SELECTED, BESIDE
        labelSize = 13;
        labelSelected = Ink.ON_ACCENT;

        // the selection
        indicator = PILL;        // PILL, CIRCLE, LINE, DOT, NONE
        indWidth = 56;
        indHeight = 40;
        indColor = Ink.ACCENT;
        stretch = .35f;

        // the create button
        createShape = SUNNY;     // SUNNY, CIRCLE, SQUARE, SQUIRCLE, PILL, NONE
        createSize = 44;
        createColor = Ink.ACCENT;
        plusColor = Ink.ON_ACCENT;
        spin = 45;

        // motion
        duration = 360;
        easing = SPRING;         // SPRING, SMOOTH, LINEAR, BOUNCE
    }
}

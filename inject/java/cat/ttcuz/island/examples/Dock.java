// @name Док
// @author ttcuz
package cat.ttcuz.island.examples;

import cat.ttcuz.island.Styled;

/** Clear glass in the iOS manner: labels under the icons, a large light selection, a rounded square "+". */
public class Dock extends Styled {
    public Dock() {
        barHeight = 64;
        side = 10;
        lift = 4;
        radius = 32;
        fill = Ink.of(0x33FFFFFF);
        stroke = Ink.of(0x59FFFFFF);
        shadow = Ink.of(0x38000000);
        shadowRadius = 16;

        buttonY = .44f;
        iconSize = 23;
        iconColor = Ink.TEXT;
        iconSelected = Ink.ACCENT;
        labels = BELOW;
        labelSize = 10.5f;
        labelColor = Ink.TEXT;
        labelSelected = Ink.ACCENT;

        indicator = PILL;
        indWidth = 64;
        indHeight = 56;
        indColor = Ink.of(0x26FFFFFF);
        stretch = .25f;

        createShape = SQUIRCLE;
        createSize = 40;
        createColor = Ink.of(0x33FFFFFF);
        plusColor = Ink.TEXT;
        spin = 0;

        duration = 300;
        easing = SMOOTH;
    }
}

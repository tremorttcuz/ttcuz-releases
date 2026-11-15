// @name Аврора
// @author ttcuz
package cat.ttcuz.island.examples;

import cat.ttcuz.island.Styled;

/** A whole-bar shader (effect): an aurora runs over the glass and the buttons together. */
public class Aurora extends Styled {
    public Aurora() {
        barHeight = 60;
        fill = Ink.of(0x99101820);
        effect = ""
                + "uniform shader content;\n"
                + "uniform float2 size;\n"
                + "uniform float time;\n"
                + "uniform half4 accent;\n"
                + "half4 main(float2 p) {\n"
                + "  half4 c = content.eval(p);\n"
                + "  float band = 0.5 + 0.5 * sin(p.x / size.x * 9.42 + time * 0.8);\n"
                + "  float fade = 1.0 - smoothstep(0.0, size.y, p.y);\n"
                + "  half3 glow = mix(accent.rgb, half3(0.3, 1.0, 0.7), half(band));\n"
                + "  c.rgb += glow * half(0.18 * fade) * c.a;\n"
                + "  return c;\n"
                + "}\n";
        iconColor = Ink.of(0xB3FFFFFF);
        iconSelected = Ink.of(0xFFFFFFFF);
        labels = SELECTED;
        labelSize = 10;
        buttonY = .42f;
        indicator = NONE;
        createSize = 42;
        spin = 60;
        duration = 300;
        easing = SMOOTH;
    }
}

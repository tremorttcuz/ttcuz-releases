// @name Неон
// @author ttcuz
package cat.ttcuz.island.examples;

import cat.ttcuz.island.Styled;

/** A running neon edge (a shader with time), a glowing line under the chosen tab and a turning rainbow "+". */
public class Neon extends Styled {
    public Neon() {
        barHeight = 58;
        radius = 18;
        fill = Ink.of(0xB00B0B14);
        shadow = Ink.ACCENT.opacity(.45f);
        shadowRadius = 22;
        shadowY = 0;
        barShader = ""
                + "uniform float2 size;\n"
                + "uniform float time;\n"
                + "uniform half4 accent;\n"
                + "half4 main(float2 p) {\n"
                + "  float2 uv = p / size;\n"
                + "  float edge = min(min(p.x, size.x - p.x), min(p.y, size.y - p.y));\n"
                + "  float glow = smoothstep(3.0, 0.0, edge);\n"
                + "  float wave = 0.5 + 0.5 * sin(uv.x * 9.0 - time * 2.4);\n"
                + "  half3 neon = mix(accent.rgb, half3(0.2, 0.9, 1.0), half(wave));\n"
                + "  half3 body = half3(0.04, 0.04, 0.08);\n"
                + "  return half4(mix(body, neon, half(glow)), mix(0.72, 1.0, glow));\n"
                + "}\n";

        iconSize = 25;
        iconColor = Ink.of(0x80FFFFFF);
        iconSelected = Ink.of(0xFFFFFFFF);
        selectedScale = 1.15f;

        indicator = LINE;
        indWidth = 28;
        stretch = .8f;
        indShader = ""
                + "uniform float time;\n"
                + "uniform half4 accent;\n"
                + "half4 main(float2 p) {\n"
                + "  float t = 0.5 + 0.5 * sin(time * 3.0 + p.x * 0.2);\n"
                + "  return half4(mix(accent.rgb, half3(1.0), half(t * 0.5)), 1.0);\n"
                + "}\n";

        createShape = CIRCLE;
        createSize = 42;
        plusColor = Ink.of(0xFFFFFFFF);
        spin = 90;
        createShader = ""
                + "uniform float2 size;\n"
                + "uniform float time;\n"
                + "uniform half4 accent;\n"
                + "half4 main(float2 p) {\n"
                + "  float2 c = p / size - 0.5;\n"
                + "  float k = 0.5 + 0.5 * sin(atan(c.y, c.x) * 3.0 + time * 1.5);\n"
                + "  return half4(mix(accent.rgb, half3(0.2, 0.9, 1.0), half(k)), 1.0);\n"
                + "}\n";

        duration = 420;
    }
}

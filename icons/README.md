# Icons

Most icons are Google's Material Design Icons, taken from
[google/material-design-icons](https://github.com/google/material-design-icons)
and used under the **Apache License 2.0**.

The compact fallback images are `delete.png`, `refresh.png`, `remove.png`,
`search.png`, `video_library.png`, `aspect_ratio.png`, and `blur_on.png`.
They use a 48px transparent canvas, sized for the settings menu and to keep
the embedded app assets compact.

The active settings icons are the matching 24px rounded vectors in
`assets/ui/settings/`. They are flattened to simple filled paths so the Android
renderer can draw them without adding resources to TikTok's APK. `SOURCES.json`
records the pinned Lucide source revision and the original TTCuz letterforms.

They are here as files rather than as resources because this build adds no
resources to somebody else's apk: `ttcuz/build.py` writes them into
`inject/java/cat/ttcuz/framework/Icons.java` as base64, and the mod decodes
and tints them at runtime.

`ttcuz/build.py` embeds every PNG in `Icons.java`. The renderer prefers the
matching vector and uses a PNG only as a fallback.

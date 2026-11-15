# TTCuz icon set

The complete SVG collection contains 94 icons: 72 settings icons, 15 music
controls, 5 expressive music shapes, and 2 shared interface symbols. The
settings and music controls use a consistent rounded filled-path style. The
five expressive silhouettes keep their distinct TTCuz shapes and are included
in the package and preview as well.

`assets/ui/settings/SOURCES.json` records the pinned Lucide revision and the
original custom symbols. `licenses/LUCIDE-ISC-LICENSE.txt` contains the source
license. `assets/ui/redraw_icon_set.py` refreshes the vectors and syncs the full
collection into the current source snapshot.

For runtime use, `ttcuz/build.py` embeds compact PNG fallbacks into
`inject/java/cat/ttcuz/framework/Icons.java`; the renderer prefers the matching
vector and falls back to the PNG where necessary.

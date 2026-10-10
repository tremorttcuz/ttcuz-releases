# ttcuz project map

Use this file as the compact entry point before exploring the repository.

## Start here

1. `README.md` — project overview and workflow.
2. `docs/brief.md` — product requirements and expected behavior.
3. `docs/status.md` — current progress and known gaps.
4. `tests/test_margyt.py` — regression coverage; run with `python -m unittest discover -s tests`.

## Project tree

```text
.
├── README.md, build.sh, VERSION, version.json   project entry points/version
├── docs/                                        brief, status, design and service notes
├── assets/                                      fonts, sounds, icons, emoji configuration
├── icons/                                       settings interface icon assets
├── inject/
│   ├── java/cat/narezany/margyt/                Android mod and settings UI
│   │   ├── SettingsActivity.java, SettingsRow.java, SettingsSchema.java
│   │   ├── Launcher.java, Shots.java             launcher icon selection/previews
│   │   ├── Badge*.java, ProfileStyle.java        profile badge and nickname appearance
│   │   ├── Accent.java, Themes.java, Fonts.java, Textures.java
│   │   ├── Margy.java, Anchors.java               lifecycle and TikTok integration
│   │   └── plugin/                                plugin API and manager
│   └── stubs/                                    TikTok/Android compile-time API shapes
├── margyt/                                      Python APK patcher/build pipeline
│   ├── __main__.py, build.py                     command line and patch orchestration
│   ├── apkzip.py, axml.py, arsc.py                APK/resource readers
│   ├── dexpatch.py, manifest.py, resadd.py         DEX/manifest/resource changes
│   └── artwork.py, icon_collection.py              icon asset generation/selection
├── tests/                                       unit tests and fixtures
├── examples/                                    sample plugin
└── server/                                      optional badge service, bot and panel
```

## Common change paths

- Settings wording/layout → `inject/java/cat/narezany/margyt/SettingsActivity.java`, `SettingsRow.java`, `Text.java`, `Skin.java`.
- App/choice icons → `assets/icons/manifest.json`, `assets/icons/<id>/`, `margyt/icon_collection.py`, `margyt/build.py`, `Launcher.java`.
- APK parsing/patching → `margyt/build.py` plus the relevant parser or patcher module.
- Tests → `tests/test_margyt.py`; fixtures live under `tests/`.
- Build outputs and local toolchains → `build/`, `work/`, `tools/` (generated/local; normally skip when reading source).

Follow the existing implementation and brief for details; this map is only an index.

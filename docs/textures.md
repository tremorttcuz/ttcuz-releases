# ttcuz texture packs

A texture pack replaces the pictures inside TikTok: the icons, the buttons,
the backgrounds, and the little animations like the heart that fills in when
something is liked. It is a file with a `.cuztx` on the end, which is a zip
underneath and nothing but a zip.

## Making one

In ttcuz settings, under **Текстурпаки**, press **Выгрузить картинки**. The
mod reads the apk it is running from and writes every picture in it to
`Downloads/ttcuz/ttcuz-<tiktok version>.cuztx`, along with a
`manifest.json` to fill in.

It is read out of the running apk on purpose: what comes out is exactly what
this build draws, at the sizes it draws it, so there is nothing to keep in
step with anything.

Unzip it, change whatever you like, and zip it back up with the same layout.

```
ttcuz-47.2.41.cuztx
├── manifest.json
├── res/
│   ├── ap/a.png
│   ├── aq/b.webp
│   └── raw/like.json
└── assets/
    └── lottie/heart.json
```

**Delete everything you have not changed.** A pack is not a replacement for
the apk's pictures -- it is a list of the ones to swap, and anything it does
not carry is left exactly as TikTok drew it. A pack of eleven files is a
perfectly good pack.

## The manifest

```json
{
  "name": "Пример",
  "author": "@you",
  "about": "What this pack changes",
  "tiktok": "47.2.41"
}
```

`tiktok` is the version this pack was drawn against. **A pack that names one
is only used on that version.** The paths inside a pack are that build's
paths: TikTok renames its own resource files between releases, so on another
version some would still match and some would not, and half a screenful of
swapped pictures looks like a broken app rather than a theme. Leave the field
out to let a pack run anywhere, and expect gaps.

## Why the paths look like that

TikTok's resource names -- `ic_heart`, `bg_button` -- are stripped out of the
apk before it ships. There is no table anywhere that would turn a name back
into a picture, so a pack cannot say "replace the heart".

What every file does still have is its path inside the apk, and Android will
hand that path over for any resource id while the app runs. So the path is
what a pack is keyed by, and `res/ap/a.png` is the same `res/ap/a.png` in
everybody's copy of the same TikTok.

## What can be replaced

**Pictures** -- `.png`, `.webp`, `.jpg`. Keep the size the original had:
TikTok lays its screens out in dp and a picture twice the size is a picture
twice the size on screen.

**Animations** -- `.json`. These are Lottie files, shapes and the paths they
move along, written as text. The heart that fills in on a like is one of
these. There are five hundred and sixty two of them in this build.

**Vector drawables** -- `.xml`. These are compiled binary XML and nobody edits
one by hand. To replace one, put a `.png` at the vector's path with the
extension changed:

```
res/a/aq0.xml   ← the vector in the apk
res/a/aq0.png   ← what your pack carries instead
```

The xml files are left out of the export by default -- there are eighteen
thousand of them and almost all are layouts and selectors that cannot be
edited at all. **Выгрузить вместе с xml** includes them if you want to see
which vectors exist.

## Installing one

**Загрузить текстурпак** takes a `.cuztx` and keeps it. Tapping a pack in
the list turns it on, tapping it again turns it off, and a long press removes
it. Only one pack is on at a time: two packs would both claim the same path
and the answer would depend on which was read first.

## When something does not change

Not every picture goes through a path the mod stands on. A drawable built in
code, one drawn by the video player, or anything inside a module TikTok
downloads after installing is not a resource in the apk and has no path for a
pack to key on.

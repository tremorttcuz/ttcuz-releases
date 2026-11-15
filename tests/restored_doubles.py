"""Inert doubles of the classes restored from 0.6.8.13, for runtime tests that compile only part of the mod.

A test that copies a few real classes into its own sandbox needs every class they name; these say nothing and do
nothing, so the behaviour under test stays the real one. `add` never replaces a class the test brought itself."""

P = 'package cat.ttcuz.framework;'
DOUBLES = {
    'PluginEngine': P + 'final class PluginEngine{static void accountChanged(String s){}static java.util.List feed(java.util.List l){return l;}'
        'static int colour(int c){return c;}static void view(Object v){}static boolean hasViewHooks(){return false;}'
        'static String nickname(String id,String n){return n;}static CharSequence text(Object v,CharSequence c){return c;}'
        'static String region(String k,String v){return v;}static String owner(String c){return null;}static void start(Object c){}'
        'static void created(Object a){}static void resumed(Object a){}static void paused(Object a){}static void destroyed(Object a){}}',
    'AccentGradient': P + 'final class AccentGradient{static boolean enabled(){return false;}static void reload(){}'
        'static void text(Object v){}static void image(Object v,int c){}static int surface(){return 0xFFFE2C55;}}',
    'FeedUnderBar': P + 'final class FeedUnderBar{static boolean isEnabled(){return false;}static void apply(Object g){}static void restore(){}}',
    'SheetPost': P + 'final class SheetPost{static Object of(Object dialog){return null;}}',
    'StoryLikes': P + 'final class StoryLikes{static void openDate(Object v,Object s){}static void bind(Object a){}static void bind(Object a,Object b){}}',
    'ProfileHeader': P + 'final class ProfileHeader{static boolean oldGroup(){return false;}}',
    'NicknameFont': P + 'final class NicknameFont{static String chosen(){return "";}static String hash(){return "";}static boolean validChoice(String s){return true;}static void apply(Object v){}}',
    'Fonts': P + 'final class Fonts{static final String FILE="file";}',
    'RailPresence': P + 'final class RailPresence{static boolean onScreen(Object v){return true;}}',
}


def add(sources, *names):
    for name in names or DOUBLES:
        path = 'cat/ttcuz/framework/%s.java' % name
        if path not in sources and any(name + '.' in str(text) for text in sources.values()):
            sources[path] = DOUBLES[name]
    return sources


def write(work, sources):
    """Writes the missing doubles into the sandbox `work` and names them in `sources`, just before javac runs."""
    import pathlib
    for name in DOUBLES:
        path = 'cat/ttcuz/framework/%s.java' % name
        if path in sources or not any(name + '.' in str(text) for text in sources.values()):
            continue
        target = pathlib.Path(work) / path
        if target.exists():
            continue
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(DOUBLES[name], encoding='utf-8')
        sources[path] = DOUBLES[name]

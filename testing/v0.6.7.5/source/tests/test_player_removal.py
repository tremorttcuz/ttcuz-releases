"""Ensure removing the custom music player preserves native downloads and profile layout."""
import pathlib, unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]
JAVA=ROOT/'inject/java/cat/narezany/margyt'

class PlayerRemovalTest(unittest.TestCase):
    def test_no_custom_player_hooks_remain(self):
        for path in JAVA.glob('*.java'):
            source=path.read_text(encoding='utf-8')
            for obsolete in ('MusicIsland', 'ProfileMusic', 'MusicEngine', 'MusicLibrary', 'MusicPrefs', 'Text.MUSIC'):
                self.assertNotIn(obsolete,source,path.name)
        self.assertFalse(list(JAVA.glob('Music*.java')))
        settings=(JAVA/'SettingsActivity.java').read_text(encoding='utf-8')
        self.assertNotIn('shelfChips',settings)
        self.assertIn('column.addView(title(',settings)

    def test_download_and_profile_contract_survive(self):
        self.assertTrue((JAVA/'DownloadIsland.java').exists())
        self.assertIn('Text.DL_AUDIO',(JAVA/'DownloadSheet.java').read_text(encoding='utf-8'))
        self.assertIn('DownloadQuality.audio',(JAVA/'VideoActions.java').read_text(encoding='utf-8'))
        provider=(JAVA/'CloudProfileProvider.java').read_text(encoding='utf-8')
        self.assertIn('j.optInt("profileLayout", 0)',provider)
        self.assertIn('style.put("profileLayout", m.profileLayout).put("musicOn", false)',provider)


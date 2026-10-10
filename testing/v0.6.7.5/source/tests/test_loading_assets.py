import json,unittest
from margyt import loading

class LoadingAssetsTest(unittest.TestCase):
    def test_dot_compositions_are_replaced_independent_of_resource_filename(self):
        for name in loading.DOT_NAMES:
            source=json.dumps({'nm':name,'w':96,'h':96,'layers':[{'nm':'dot'}]}).encode()
            data=json.loads(loading.replacement('res/raw_obfuscated.json',source))
            self.assertEqual(loading.MARKER,data['layers'][0]['nm'])
            self.assertEqual((96,96),(data['w'],data['h']))
            self.assertEqual([],data['assets'])
            self.assertEqual(273,data['op'])
            self.assertEqual({'sh','fl'},{s['ty'] for s in data['layers'][0]['shapes']})
            self.assertNotIn('dot',json.dumps(data))
    def test_unrelated_animations_are_kept(self):
        for name in ('heart','camera_waveform','creation_ailoading_hex_large_tux_lottie'):
            self.assertIsNone(loading.replacement('assets/anything.json',json.dumps({'nm':name}).encode()))
        self.assertIsNone(loading.replacement('assets/broken.json',b'{broken'))
        self.assertIsNotNone(loading.replacement('assets/lottie_double_points_loading.json',b'{}'))

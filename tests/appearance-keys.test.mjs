import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {cosmeticKey} from '../server/cloudflare/appearance.mjs';

// The phone sends every key its own filter lets through; one the server does not know refuses the whole backup
// (the phone then used to retry every 30 seconds). Both filters must agree.
test('the server accepts every look setting the phone backs up', () => {
  const java = readFileSync(new URL('../inject/java/cat/ttcuz/framework/CloudAppearance.java', import.meta.url), 'utf8');
  const source = java.match(/key\.matches\("((?:[^"\\]|\\.)*)"\)/)[1].replace(/\\\\/g, '\\');
  const phone = new RegExp('^(?:' + source + ')$');
  const samples = ['accent', 'accent_custom', 'accent_gradient_on', 'accent_gradient_colour', 'accent_gradient_angle',
    'like_color', 'font', 'font_emoji', 'dim_how', 'island_style', 'nav_labels', 'theme_mode', 'appearance_color_text',
    'profile_badge_x', 'profile_display_name', 'rail_show_like', 'seekbar', 'dates', 'profile_layout'];
  for (const key of samples) assert.equal(cosmeticKey(key), phone.test(key), key);
  for (const key of samples) if (phone.test(key)) assert.ok(cosmeticKey(key), 'server refuses ' + key);
});

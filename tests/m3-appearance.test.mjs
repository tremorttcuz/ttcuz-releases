import test from 'node:test';
import assert from 'node:assert/strict';
import { cosmeticKey, validateSnapshot } from '../server/cloudflare/appearance.mjs';
import { normalizeStyle } from '../server/cloudflare/profiles.mjs';

const base = {
  firstColour: 0, lastColour: 0, middleColour: 0, direction: 0, speed: 50,
  offsetX: 0, offsetY: 0, badgeSize: 100,
  threeColours: false, bold: false, glow: false, animated: false,
};

test('glow strength survives publication and a later older-client update', () => {
  const previous = normalizeStyle({ ...base, glow: true, glowStrength: 73 }, '123', 1);
  assert.equal(previous.glowStrength, 73);
  assert.equal(normalizeStyle(base, '123', 2, previous).glowStrength, 73);
  assert.equal(normalizeStyle({ ...base, glowStrength: 0 }, '123', 3, previous).glowStrength, 0);
  for (const glowStrength of [-1, 101, 2.5, '50']) {
    assert.throws(() => normalizeStyle({ ...base, glowStrength }, '123', 1));
  }
  assert.equal(normalizeStyle(base, '123', 1).glowStrength, 50);
});

test('the two-colour accent is part of the private appearance backup', () => {
  for (const key of ['accent_gradient_on', 'accent_gradient_colour', 'accent_gradient_mix', 'accent_gradient_angle', 'accent_gradient_width'])
    assert.equal(cosmeticKey(key), true);
  for (const key of ['account_token', 'streak_auto', 'accent_password'])
    assert.equal(cosmeticKey(key), false);
});

test('angle and transition width survive a private snapshot', () => {
  const account = {
    accent_gradient_angle: { type: 'int', value: 137 },
    accent_gradient_width: { type: 'int', value: 73 },
  };
  const result = validateSnapshot({ schema: 1, global: {}, account, files: [] });
  assert.deepEqual(result.account, account);
});

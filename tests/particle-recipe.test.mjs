import test from 'node:test';
import assert from 'node:assert/strict';
import { normalizeParticleRecipe } from '../server/cloudflare/profiles.mjs';

const base = { v: 1, shape: 1, count: 8, size: 6, vary: 40, speed: 100, motion: 0, wiggle: 30, spin: 0, fade: true,
  colourMode: 0, colour: -10496, opacity: 90, spread: 30 };

test('a recipe is kept field by field, and nothing else', () => {
  const out = JSON.parse(normalizeParticleRecipe(JSON.stringify({ ...base, extra: 'x' })));
  assert.equal(out.extra, undefined);
  assert.equal(out.count, 8);
});

test('numbers out of range, a picture that is not a PNG, a picture shape without a picture: refused', () => {
  assert.throws(() => normalizeParticleRecipe(JSON.stringify({ ...base, count: 99 })));
  assert.throws(() => normalizeParticleRecipe(JSON.stringify({ ...base, image: 'R0lGODlh' })));
  assert.throws(() => normalizeParticleRecipe(JSON.stringify({ ...base, shape: 5 })));
  assert.throws(() => normalizeParticleRecipe('not json'));
});

test('a small PNG picture is kept', () => {
  const out = JSON.parse(normalizeParticleRecipe(JSON.stringify({ ...base, shape: 5, image: 'iVBORw0KGgoAAAA' })));
  assert.equal(out.image, 'iVBORw0KGgoAAAA');
});

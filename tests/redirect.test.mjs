import test from 'node:test';
import assert from 'node:assert/strict';
import {moved, TARGET} from '../server/redirect/worker.mjs';

test('any old link lands on the same page of the new address', () => {
  for (const path of ['/store.html', '/review.html', '/plugins/catalog.json', '/guide.html?x=1', '/']) {
    const answer = moved(new Request('https://ttcuz.old.workers.dev' + path));
    assert.equal(answer.status, 301);
    assert.equal(answer.headers.get('location'), TARGET + path);
  }
});

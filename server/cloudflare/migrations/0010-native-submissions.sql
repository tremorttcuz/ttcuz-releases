-- Plugins sent for review from the in-app editor: the sources, the .ctt the phone built from them, the picture.
-- The Worker also creates this table on first use, so applying the migration is optional.
CREATE TABLE IF NOT EXISTS native_submissions (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  plugin_id TEXT NOT NULL,
  metadata TEXT NOT NULL,
  sources TEXT NOT NULL,
  archive TEXT NOT NULL,
  icon TEXT,
  handle TEXT,
  uid TEXT,
  state TEXT NOT NULL DEFAULT 'pending',
  added INTEGER NOT NULL,
  ip_hash TEXT
);
CREATE INDEX IF NOT EXISTS native_submissions_state ON native_submissions(state, added);

CREATE TABLE IF NOT EXISTS profiles (
  uid TEXT PRIMARY KEY,
  owner_hash TEXT NOT NULL,
  metadata TEXT,
  badge TEXT,
  music TEXT,
  verified INTEGER NOT NULL DEFAULT 0,
  updated_at INTEGER NOT NULL
);
CREATE TABLE IF NOT EXISTS limits (key TEXT PRIMARY KEY, hits INTEGER NOT NULL, until INTEGER NOT NULL);
CREATE TABLE IF NOT EXISTS badge_awards (
  uid TEXT NOT NULL,
  badge_id TEXT NOT NULL,
  awarded_at INTEGER NOT NULL,
  PRIMARY KEY (uid, badge_id)
);
CREATE TABLE IF NOT EXISTS custom_badges (
  id TEXT PRIMARY KEY,
  title TEXT NOT NULL,
  png TEXT NOT NULL,
  created_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS crash_reports (id TEXT PRIMARY KEY,version TEXT NOT NULL,sdk INTEGER NOT NULL,stack TEXT NOT NULL,occurred_at INTEGER NOT NULL,received_at INTEGER NOT NULL);
CREATE INDEX IF NOT EXISTS crash_reports_received ON crash_reports(received_at);
CREATE TABLE IF NOT EXISTS script_submissions (
  digest TEXT PRIMARY KEY,
  plugin_id TEXT NOT NULL,
  source TEXT NOT NULL,
  state TEXT NOT NULL CHECK(state IN ('pending','approved','rejected','superseded')),
  added INTEGER NOT NULL,
  ip_hash TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS script_submissions_state_added ON script_submissions(state,added);

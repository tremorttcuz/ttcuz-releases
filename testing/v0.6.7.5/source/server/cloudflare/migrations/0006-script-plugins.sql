CREATE TABLE IF NOT EXISTS script_submissions (
  digest TEXT PRIMARY KEY,
  plugin_id TEXT NOT NULL,
  source TEXT NOT NULL,
  state TEXT NOT NULL CHECK(state IN ('pending','approved','rejected','superseded')),
  added INTEGER NOT NULL,
  ip_hash TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS script_submissions_state_added ON script_submissions(state,added);

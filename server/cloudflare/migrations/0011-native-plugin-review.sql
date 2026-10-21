CREATE TABLE IF NOT EXISTS native_plugin_submissions (
 digest TEXT PRIMARY KEY,
 plugin_id TEXT NOT NULL,
 metadata TEXT NOT NULL,
 source TEXT NOT NULL,
 state TEXT NOT NULL DEFAULT 'pending',
 analysis TEXT,
 added INTEGER NOT NULL,
 ip_hash TEXT NOT NULL,
 notified INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS native_plugin_review_state ON native_plugin_submissions(state,added);
CREATE TABLE IF NOT EXISTS plugin_author_marks (
 author TEXT PRIMARY KEY,
 mark TEXT NOT NULL,
 updated INTEGER NOT NULL
);

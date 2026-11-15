CREATE TABLE IF NOT EXISTS crash_reports (id TEXT PRIMARY KEY,version TEXT NOT NULL,sdk INTEGER NOT NULL,stack TEXT NOT NULL,occurred_at INTEGER NOT NULL,received_at INTEGER NOT NULL);
CREATE INDEX IF NOT EXISTS crash_reports_received ON crash_reports(received_at);

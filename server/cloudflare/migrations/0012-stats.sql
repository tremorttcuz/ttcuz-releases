-- who uses ttcuz now (anonymous, hashed) and how often each store plugin was downloaded; stats.mjs also creates these
CREATE TABLE IF NOT EXISTS presence (id TEXT PRIMARY KEY, seen INTEGER NOT NULL);
CREATE INDEX IF NOT EXISTS presence_seen ON presence(seen);
CREATE TABLE IF NOT EXISTS plugin_downloads (id TEXT PRIMARY KEY, count INTEGER NOT NULL DEFAULT 0);

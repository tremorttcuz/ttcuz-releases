CREATE TABLE IF NOT EXISTS plugin_packages (
 id TEXT PRIMARY KEY,
 metadata TEXT NOT NULL,
 archive TEXT NOT NULL,
 icon TEXT,
 updated INTEGER NOT NULL
);

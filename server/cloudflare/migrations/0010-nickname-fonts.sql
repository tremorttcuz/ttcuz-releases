CREATE TABLE IF NOT EXISTS nickname_fonts (
  uid TEXT PRIMARY KEY,
  hash TEXT NOT NULL,
  font TEXT NOT NULL,
  updated_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS appearance_backups (
  uid TEXT PRIMARY KEY,
  snapshot TEXT NOT NULL,
  updated_at INTEGER NOT NULL
);
CREATE TABLE IF NOT EXISTS appearance_devices (
  uid TEXT NOT NULL,
  device_hash TEXT NOT NULL,
  PRIMARY KEY(uid,device_hash)
);
CREATE TABLE IF NOT EXISTS appearance_files (
  uid TEXT NOT NULL,
  hash TEXT NOT NULL,
  part INTEGER NOT NULL,
  data BLOB NOT NULL,
  PRIMARY KEY(uid,hash,part)
);
CREATE TABLE IF NOT EXISTS appearance_staging (
  uid TEXT PRIMARY KEY,
  snapshot TEXT NOT NULL,
  updated_at INTEGER NOT NULL
);

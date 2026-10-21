-- Existing profiles were all proven with a bio code, so they stay verified.
ALTER TABLE profiles ADD COLUMN verified INTEGER NOT NULL DEFAULT 0;
UPDATE profiles SET verified = 1;

-- A person's music list, as plain text (no audio). Private restore for the owner; public references only with explicit musicOn.
ALTER TABLE profiles ADD COLUMN music TEXT;

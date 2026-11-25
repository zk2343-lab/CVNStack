-- AI-assisted.
-- Stores the client instance that created each waitlist and entry.
ALTER TABLE waitlists
    ADD COLUMN client_instance_id TEXT NOT NULL DEFAULT 'test-instance';
ALTER TABLE waitlists
    ALTER COLUMN client_instance_id DROP DEFAULT;

ALTER TABLE entries
    ADD COLUMN client_instance_id TEXT NOT NULL DEFAULT 'test-instance';
ALTER TABLE entries
    ALTER COLUMN client_instance_id DROP DEFAULT;

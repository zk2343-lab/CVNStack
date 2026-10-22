-- AI-assisted.
-- Creates the initial PostgreSQL source of truth for waitlists and entries.
CREATE TABLE waitlists (
    id UUID PRIMARY KEY NOT NULL,
    name TEXT NOT NULL,
    max_length INTEGER NOT NULL CHECK (max_length > 0),
    processing_time_per_entry_minutes INTEGER NOT NULL
        CHECK (processing_time_per_entry_minutes > 0),
    CONSTRAINT uq_waitlists_name UNIQUE (name)
);

CREATE TABLE entries (
    id UUID PRIMARY KEY NOT NULL,
    waitlist_id UUID NOT NULL,
    name TEXT NOT NULL,
    priority INTEGER NOT NULL CHECK (priority BETWEEN 1 AND 5),
    status TEXT NOT NULL CHECK (status IN ('WAITING', 'CALLED', 'SERVED', 'CANCELLED')),
    inserted_at TIMESTAMPTZ NOT NULL,
    called_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_entries_waitlist FOREIGN KEY (waitlist_id) REFERENCES waitlists(id)
);

CREATE INDEX idx_entries_waitlist ON entries (waitlist_id);
CREATE INDEX idx_entries_waitlist_status ON entries (waitlist_id, status);

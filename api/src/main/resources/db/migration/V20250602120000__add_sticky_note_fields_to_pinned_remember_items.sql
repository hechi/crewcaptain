-- Add color, tag, and sensitive flag to transform pinned remember items into sticky notes
ALTER TABLE pinned_remember_items
    ADD COLUMN color VARCHAR(20) NOT NULL DEFAULT 'cyan',
    ADD COLUMN tag VARCHAR(30),
    ADD COLUMN sensitive BOOLEAN NOT NULL DEFAULT FALSE;

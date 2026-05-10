-- Add soft-delete support to persons table
ALTER TABLE persons ADD COLUMN deleted_at TIMESTAMP WITH TIME ZONE DEFAULT NULL;

-- Index for efficient filtering of non-deleted records
CREATE INDEX idx_persons_deleted_at ON persons(user_id, deleted_at) WHERE deleted_at IS NULL;

-- Index for listing deleted records (trash view)
CREATE INDEX idx_persons_deleted ON persons(user_id, deleted_at) WHERE deleted_at IS NOT NULL;

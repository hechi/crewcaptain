-- Add GIN index for strategy_goals full-text search
-- This follows the same pattern as other entity search indexes

CREATE OR REPLACE FUNCTION strategy_goals_search_vector(
    p_title text,
    p_description text
) RETURNS tsvector AS $$
BEGIN
    RETURN to_tsvector('english',
        COALESCE(p_title, '') || ' ' ||
        COALESCE(p_description, '')
    );
END;
$$ LANGUAGE plpgsql IMMUTABLE PARALLEL SAFE;

CREATE INDEX idx_strategy_goals_fts ON strategy_goals USING GIN (
    strategy_goals_search_vector(title, description)
);

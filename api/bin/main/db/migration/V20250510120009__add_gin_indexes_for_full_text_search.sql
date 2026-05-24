-- Add GIN indexes for full-text search performance optimization.
-- PostgreSQL requires all functions in an index expression to be IMMUTABLE.
-- to_tsvector(regconfig, text) is STABLE, and array_to_string is STABLE.
-- We create per-table IMMUTABLE wrapper functions that encapsulate the full
-- tsvector computation. This is safe because:
-- 1. We control the 'english' text search configuration and won't change it
-- 2. array_to_string behavior is deterministic for our use case
-- These functions are used both in the GIN index definition and in queries,
-- allowing the query planner to use the indexes.

-- 1. Persons search vector function
CREATE OR REPLACE FUNCTION persons_search_vector(
    p_name text,
    p_preferred_name text,
    p_role_title text,
    p_email text,
    p_tags text[]
) RETURNS tsvector AS $$
BEGIN
    RETURN to_tsvector('english',
        COALESCE(p_name, '') || ' ' ||
        COALESCE(p_preferred_name, '') || ' ' ||
        COALESCE(p_role_title, '') || ' ' ||
        COALESCE(p_email, '') || ' ' ||
        COALESCE(array_to_string(p_tags, ' '), '')
    );
END;
$$ LANGUAGE plpgsql IMMUTABLE PARALLEL SAFE;

CREATE INDEX idx_persons_fts ON persons USING GIN (
    persons_search_vector(name, preferred_name, role_title, email, tags)
);

-- 2. One-on-one entries search vector function
CREATE OR REPLACE FUNCTION one_on_one_entries_search_vector(
    p_notes_markdown text,
    p_outcomes_markdown text
) RETURNS tsvector AS $$
BEGIN
    RETURN to_tsvector('english',
        COALESCE(p_notes_markdown, '') || ' ' ||
        COALESCE(p_outcomes_markdown, '')
    );
END;
$$ LANGUAGE plpgsql IMMUTABLE PARALLEL SAFE;

CREATE INDEX idx_one_on_one_entries_fts ON one_on_one_entries USING GIN (
    one_on_one_entries_search_vector(notes_markdown, outcomes_markdown)
);

-- 3. Quick notes search vector function
CREATE OR REPLACE FUNCTION quick_notes_search_vector(
    p_text text
) RETURNS tsvector AS $$
BEGIN
    RETURN to_tsvector('english', COALESCE(p_text, ''));
END;
$$ LANGUAGE plpgsql IMMUTABLE PARALLEL SAFE;

CREATE INDEX idx_quick_notes_fts ON quick_notes USING GIN (
    quick_notes_search_vector(text)
);

-- 4. Action items search vector function
CREATE OR REPLACE FUNCTION action_items_search_vector(
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

CREATE INDEX idx_action_items_fts ON action_items USING GIN (
    action_items_search_vector(title, description)
);

-- 5. PDP goals search vector function
CREATE OR REPLACE FUNCTION pdp_goals_search_vector(
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

CREATE INDEX idx_pdp_goals_fts ON pdp_goals USING GIN (
    pdp_goals_search_vector(title, description)
);

-- 6. PDP updates search vector function
CREATE OR REPLACE FUNCTION pdp_updates_search_vector(
    p_text_markdown text
) RETURNS tsvector AS $$
BEGIN
    RETURN to_tsvector('english', COALESCE(p_text_markdown, ''));
END;
$$ LANGUAGE plpgsql IMMUTABLE PARALLEL SAFE;

CREATE INDEX idx_pdp_updates_fts ON pdp_updates USING GIN (
    pdp_updates_search_vector(text_markdown)
);

-- 7. Kudos search vector function
CREATE OR REPLACE FUNCTION kudos_search_vector(
    p_text text,
    p_tags text[]
) RETURNS tsvector AS $$
BEGIN
    RETURN to_tsvector('english',
        COALESCE(p_text, '') || ' ' ||
        COALESCE(array_to_string(p_tags, ' '), '')
    );
END;
$$ LANGUAGE plpgsql IMMUTABLE PARALLEL SAFE;

CREATE INDEX idx_kudos_fts ON kudos USING GIN (
    kudos_search_vector(text, tags)
);

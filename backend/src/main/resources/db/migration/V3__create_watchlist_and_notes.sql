CREATE TABLE watchlist_entries (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    platform VARCHAR(10) NOT NULL,
    puuid VARCHAR(100) NOT NULL,
    game_name VARCHAR(40) NOT NULL,
    tag_line VARCHAR(15) NOT NULL,
    alias_label VARCHAR(50),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_watchlist_user_summoner UNIQUE (user_id, platform, puuid)
);

CREATE INDEX idx_watchlist_user_id ON watchlist_entries(user_id);

CREATE TABLE match_notes (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    match_id VARCHAR(40) NOT NULL,
    puuid VARCHAR(100) NOT NULL,
    mood_tag VARCHAR(20) NOT NULL DEFAULT 'FOCUSED',
    note_text VARCHAR(500) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_match_notes_user_match ON match_notes(user_id, match_id);

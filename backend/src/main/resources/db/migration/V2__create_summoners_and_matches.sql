CREATE TABLE summoner_snapshots (
    id UUID PRIMARY KEY,
    puuid VARCHAR(100) NOT NULL,
    platform VARCHAR(10) NOT NULL,
    game_name VARCHAR(40) NOT NULL,
    tag_line VARCHAR(15) NOT NULL,
    summoner_level INT NOT NULL,
    profile_icon_id INT NOT NULL,
    solo_tier VARCHAR(20),
    solo_rank VARCHAR(10),
    solo_lp INT NOT NULL DEFAULT 0,
    solo_wins INT NOT NULL DEFAULT 0,
    solo_losses INT NOT NULL DEFAULT 0,
    tilt_score INT NOT NULL DEFAULT 0,
    fetched_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_summoner_platform_puuid UNIQUE (platform, puuid)
);

CREATE INDEX idx_summoner_riot_id ON summoner_snapshots (platform, LOWER(game_name), LOWER(tag_line));

CREATE TABLE match_records (
    id UUID PRIMARY KEY,
    match_id VARCHAR(40) NOT NULL,
    puuid VARCHAR(100) NOT NULL,
    platform VARCHAR(10) NOT NULL,
    queue_id INT NOT NULL,
    champion_name VARCHAR(40) NOT NULL,
    lane VARCHAR(20) NOT NULL,
    win BOOLEAN NOT NULL,
    kills INT NOT NULL,
    deaths INT NOT NULL,
    assists INT NOT NULL,
    cs_total INT NOT NULL,
    duration_seconds INT NOT NULL,
    vision_score INT NOT NULL,
    gold_earned INT NOT NULL,
    played_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_match_puuid UNIQUE (match_id, puuid)
);

CREATE INDEX idx_match_records_puuid_played_at ON match_records (puuid, played_at DESC);

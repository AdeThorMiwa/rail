CREATE TABLE user_connie_logs (
    id                 BIGSERIAL    PRIMARY KEY,
    pid                UUID         NOT NULL DEFAULT gen_random_uuid(),
    user_id            BIGINT       NOT NULL REFERENCES users(id),
    type               VARCHAR(20)  NOT NULL DEFAULT 'ANALYSIS',
    period_start       DATE         NOT NULL,
    period_end         DATE         NOT NULL,
    observed_patterns  TEXT,
    stated_preferences TEXT,
    merged_count       INTEGER,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ,
    version            BIGINT       NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX ux_user_connie_logs_pid ON user_connie_logs(pid);
CREATE INDEX idx_user_connie_logs_user_created ON user_connie_logs(user_id, created_at);

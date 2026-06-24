CREATE TABLE next_goal_proposals (
    id             BIGSERIAL PRIMARY KEY,
    pid            UUID        NOT NULL UNIQUE,
    owner_id       BIGINT      NOT NULL REFERENCES users(id),
    intention_id   BIGINT      NOT NULL REFERENCES intentions(id),
    chat_id        BIGINT      NOT NULL REFERENCES chats(id),
    goal_blueprint JSONB,
    context        TEXT,
    status         VARCHAR(32) NOT NULL DEFAULT 'REFINING',
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version        BIGINT      NOT NULL DEFAULT 0
);

CREATE INDEX idx_next_goal_proposals_chat_status ON next_goal_proposals(chat_id, status);

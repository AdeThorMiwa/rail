CREATE TABLE IF NOT EXISTS tool_call_logs (
    id          BIGSERIAL PRIMARY KEY,
    chat_id     BIGINT      NOT NULL REFERENCES chats(id),
    call_id     TEXT        NOT NULL,
    tool_name   TEXT        NOT NULL,
    arguments   TEXT,
    result      TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    version     BIGINT      NOT NULL DEFAULT 0
);

CREATE INDEX idx_tool_call_logs_chat_created ON tool_call_logs(chat_id, created_at);

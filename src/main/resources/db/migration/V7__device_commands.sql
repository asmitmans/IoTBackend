-- V7__device_commands.sql
CREATE TABLE device_command (
    id           BIGSERIAL    PRIMARY KEY,
    device_id    BIGINT       NOT NULL REFERENCES device(id),
    company_id   BIGINT       NOT NULL,
    type         VARCHAR(50)  NOT NULL,
    payload      JSONB,
    status       VARCHAR(20)  NOT NULL DEFAULT 'QUEUED'
                     CHECK (status IN ('QUEUED', 'DELIVERED', 'CANCELED')),
    queued_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    delivered_at TIMESTAMPTZ,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_device_command_device_status
    ON device_command(device_id, status);
-- V4__iot_devices.sql
CREATE TABLE device_model (
    id              BIGSERIAL    PRIMARY KEY,
    name            VARCHAR(100) NOT NULL UNIQUE,
    config_schema   JSONB,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE device (
    id                  BIGSERIAL    PRIMARY KEY,
    company_id          BIGINT       REFERENCES company(id),
    device_model_id     BIGINT       NOT NULL REFERENCES device_model(id),
    serial_number       VARCHAR(100) NOT NULL UNIQUE,
    name                VARCHAR(100) NOT NULL,
    api_key_hash        VARCHAR(255),
    api_key_prefix      VARCHAR(8),
    current_config_hash VARCHAR(8),
    claimed_at          TIMESTAMPTZ,
    claim_expires_at    TIMESTAMPTZ,
    status              VARCHAR(20)  NOT NULL DEFAULT 'UNCLAIMED'
        CHECK (status IN ('UNCLAIMED', 'PENDING', 'ACTIVE', 'INACTIVE')),
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_device_company_id      ON device(company_id);
CREATE INDEX idx_device_device_model_id ON device(device_model_id);
CREATE INDEX idx_device_api_key_prefix  ON device(api_key_prefix);
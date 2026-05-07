-- V5__iot_core.sql

CREATE TABLE location (
    id          BIGSERIAL    PRIMARY KEY,
    company_id  BIGINT       NOT NULL REFERENCES company(id),
    parent_id   BIGINT       REFERENCES location(id),
    name        VARCHAR(100) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (company_id, parent_id, name)
);

ALTER TABLE device
    ADD COLUMN location_id BIGINT REFERENCES location(id);

CREATE INDEX idx_location_company_id ON location(company_id);
CREATE INDEX idx_location_parent_id  ON location(parent_id);
CREATE INDEX idx_device_location_id  ON device(location_id);

-- -----------------------------------------------------

CREATE TABLE measurement (
    id               BIGSERIAL    PRIMARY KEY,
    device_id        BIGINT       NOT NULL REFERENCES device(id),
    company_id       BIGINT       NOT NULL,
    recorded_at      TIMESTAMPTZ  NOT NULL,
    received_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    config_version   BIGINT,
    sequence_number  BIGINT,
    config_checksum  VARCHAR(8),
    payload          JSONB        NOT NULL
);

CREATE INDEX idx_measurement_device_recorded
    ON measurement(device_id, recorded_at DESC);
CREATE INDEX idx_measurement_company_recorded
    ON measurement(company_id, recorded_at DESC);
CREATE INDEX idx_measurement_payload
    ON measurement USING GIN(payload);

-- -----------------------------------------------------

CREATE TABLE device_config (
    id              BIGSERIAL    PRIMARY KEY,
    device_id       BIGINT       NOT NULL REFERENCES device(id),
    config_version  BIGINT       NOT NULL,
    key             VARCHAR(100) NOT NULL,
    desired_value   VARCHAR(255),
    reported_value  VARCHAR(255),
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
                        CHECK (status IN ('PENDING', 'SYNCED', 'CONFLICT')),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (device_id, key)
);

CREATE INDEX idx_device_config_device_id ON device_config(device_id);
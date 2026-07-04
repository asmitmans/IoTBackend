-- V8__device_model_properties.sql
CREATE TABLE measurement_type (
    id         BIGSERIAL    PRIMARY KEY,
    name       VARCHAR(100) NOT NULL UNIQUE,
    si_unit    VARCHAR(20),
    category   VARCHAR(20)  NOT NULL
                   CHECK (category IN ('SENSOR', 'ACTUATOR', 'STATUS')),
    data_type  VARCHAR(20)  NOT NULL
                   CHECK (data_type IN ('DOUBLE', 'INTEGER', 'BOOLEAN', 'STRING')),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE command_type (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE device_model_measurement (
    device_model_id     BIGINT NOT NULL REFERENCES device_model(id),
    measurement_type_id BIGINT NOT NULL REFERENCES measurement_type(id),
    PRIMARY KEY (device_model_id, measurement_type_id)
);

CREATE TABLE device_model_command (
    device_model_id BIGINT NOT NULL REFERENCES device_model(id),
    command_type_id BIGINT NOT NULL REFERENCES command_type(id),
    PRIMARY KEY (device_model_id, command_type_id)
);
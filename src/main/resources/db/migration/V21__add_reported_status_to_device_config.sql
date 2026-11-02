-- V21__add_reported_status_to_device_config.sql
ALTER TABLE device_config DROP CONSTRAINT device_config_status_check;

ALTER TABLE device_config
    ADD CONSTRAINT device_config_status_check
        CHECK (status IN ('PENDING', 'SYNCED', 'CONFLICT', 'REPORTED'));
-- V20__add_superseded_status_to_device_command.sql
ALTER TABLE device_command DROP CONSTRAINT device_command_status_check;

ALTER TABLE device_command
    ADD CONSTRAINT device_command_status_check
        CHECK (status IN ('QUEUED', 'DELIVERED', 'CANCELED', 'SUPERSEDED'));
-- V15__make_device_name_nullable.sql
ALTER TABLE device ALTER COLUMN name DROP NOT NULL;
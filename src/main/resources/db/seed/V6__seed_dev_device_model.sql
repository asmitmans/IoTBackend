-- src/main/resources/db/seed/V6__seed_dev_device_model.sql
INSERT INTO device_model (name, config_schema)
VALUES (
    'Generic Sensor v1',
    '{"temperature": {"unit": "C", "factor": 1.0}, "humidity": {"unit": "%", "factor": 1.0}}'
) ON CONFLICT DO NOTHING;
-- Development seed only. Do not run in production.

INSERT INTO roles (name) VALUES ('ROLE_ADMIN') ON CONFLICT DO NOTHING;

INSERT INTO company (company_name, api_key_hash, api_key_prefix)
VALUES (
    'Default Company',
    'seed-hash-placeholder',
    'seed1234'
) ON CONFLICT DO NOTHING;

INSERT INTO users (username, password, enabled, company_id)
VALUES (
    'admin',
    '$2a$10$cRY.TuhEoHe8YEd2XMWEu.yZF8MxcZlydtdfy1DnM6W9DpXVHazuy',
    true,
    (SELECT id FROM company WHERE company_name = 'Default Company')
) ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
VALUES (
    (SELECT id FROM users WHERE username = 'admin'),
    (SELECT id FROM roles WHERE name = 'ROLE_ADMIN')
) ON CONFLICT DO NOTHING;
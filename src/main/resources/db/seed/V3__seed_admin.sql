INSERT INTO roles (name) VALUES ('ROLE_ADMIN') ON CONFLICT DO NOTHING;
INSERT INTO roles (name) VALUES ('ROLE_USER') ON CONFLICT DO NOTHING;

INSERT INTO company (company_name, api_key_hash, api_key_prefix)
VALUES ('Default Company', 'dev-placeholder', 'dev00000')
ON CONFLICT DO NOTHING;
CREATE
EXTENSION IF NOT EXISTS "uuid-ossp";
ALTER TABLE client_group
    ALTER COLUMN id SET DEFAULT uuid_generate_v4();
ALTER TABLE client
    ALTER COLUMN client_id SET DEFAULT uuid_generate_v4();
ALTER TABLE client DROP COLUMN IF EXISTS contact_addresses;
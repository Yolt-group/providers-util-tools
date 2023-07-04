ALTER TABLE request_token_public_key
    ADD COLUMN IF NOT EXISTS updated timestamp;

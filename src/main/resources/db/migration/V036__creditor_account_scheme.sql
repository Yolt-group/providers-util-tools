ALTER TABLE creditor_account ADD COLUMN account_identifier_scheme TEXT NOT NULL default 'IBAN';
ALTER TABLE creditor_account ADD COLUMN secondary_identification TEXT;

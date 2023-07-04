ALTER TABLE client_mtls_certificates
    ALTER COLUMN first_seen DROP NOT NULL,
    ALTER COLUMN last_seen DROP NOT NULL,
    ADD COLUMN if not exists sort_date timestamp;

update client_mtls_certificates
set sort_date = case
                    when last_seen is null then valid_start
                    else last_seen
    end
where sort_date is null;

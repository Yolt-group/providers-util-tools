CREATE TABLE IF NOT EXISTS client_group_certificates
(
    certificate_type            varchar(256) not null,
    key_id                      varchar(256) not null,
    client_group_id             uuid         not null,
    name                        varchar(256) not null,
    certificate_usage_type      varchar(256),
    key_algorithm               varchar(256),
    signature_algorithm         varchar(256),
    certificate_signing_request text,
    signed_certificate_chain    text,
    PRIMARY KEY (certificate_type, key_id),
    UNIQUE (client_group_id, name),
    FOREIGN KEY (client_group_id) REFERENCES client_group (id)
);

CREATE TABLE IF NOT EXISTS client_group_certificates_service_types
(
    certificate_type varchar(256) not null,
    key_id           varchar(256) not null,
    service_type     varchar(256) not null,
    FOREIGN KEY (certificate_type, key_id) REFERENCES client_group_certificates (certificate_type, key_id)
);

CREATE TABLE IF NOT EXISTS client_group_certificates_subject_alternative_names
(
    certificate_type         varchar(256) not null,
    key_id                   varchar(256) not null,
    subject_alternative_name varchar(256) not null,
    FOREIGN KEY (certificate_type, key_id) REFERENCES client_group_certificates (certificate_type, key_id)
);
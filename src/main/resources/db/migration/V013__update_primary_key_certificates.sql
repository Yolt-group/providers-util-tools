/* create new tables */
CREATE TABLE IF NOT EXISTS client_group_certificates_service_types_v2
(
    certificate_type varchar(256) not null,
    key_id           varchar(256) not null,
    client_group_id  uuid         not null,
    service_type     varchar(256) not null
);

CREATE TABLE IF NOT EXISTS client_group_certificates_subject_alternative_names_v2
(
    certificate_type         varchar(256) not null,
    key_id                   varchar(256) not null,
    client_group_id          uuid         not null,
    subject_alternative_name varchar(256) not null
);

/* insert data in tables from old tables */
insert into client_group_certificates_service_types_v2 (certificate_type, key_id, client_group_id, service_type)
select T2.certificate_type, T2.key_id, T2.client_group_id, T1.service_type
from client_group_certificates_service_types T1
         join client_group_certificates T2 on T2.certificate_type = T1.certificate_type and T2.key_id = T1.key_id;

insert into client_group_certificates_subject_alternative_names_v2 (certificate_type, key_id, client_group_id, subject_alternative_name)
select T2.certificate_type, T2.key_id, T2.client_group_id, T1.subject_alternative_name
from client_group_certificates_subject_alternative_names T1
         join client_group_certificates T2 on T2.certificate_type = T1.certificate_type and T2.key_id = T1.key_id;

/* drop old tables */
DROP TABLE client_group_certificates_service_types;
DROP TABLE client_group_certificates_subject_alternative_names;

/* update primary key */
ALTER TABLE client_group_certificates
    DROP CONSTRAINT IF EXISTS client_group_certificates_pkey;
ALTER TABLE client_group_certificates
    ADD CONSTRAINT client_group_certificates_pkey primary key (certificate_type, key_id, client_group_id);

/* rename tables */
ALTER TABLE client_group_certificates_service_types_v2
    RENAME TO client_group_certificates_service_types;
ALTER TABLE client_group_certificates_subject_alternative_names_v2
    RENAME TO client_group_certificates_subject_alternative_names;

/* set foreign keys */
ALTER TABLE client_group_certificates_service_types
    ADD CONSTRAINT client_group_certificates_service_types_certificate_type_fkey FOREIGN KEY (certificate_type, key_id, client_group_id) REFERENCES client_group_certificates (certificate_type, key_id, client_group_id);

ALTER TABLE client_group_certificates_subject_alternative_names
    ADD CONSTRAINT client_group_certificates_subject_alterna_certificate_type_fkey FOREIGN KEY (certificate_type, key_id, client_group_id) REFERENCES client_group_certificates (certificate_type, key_id, client_group_id);


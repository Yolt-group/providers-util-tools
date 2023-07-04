create table if not exists client_mtls_certificates_dn
(
    id                uuid          not null
        constraint pk_mtls_dns primary key,
    client_id         uuid          not null,
    subject_dn        varchar(1024) not null,
    issuer_dn         varchar(1024) not null,
    certificate_chain text          not null,
    status            varchar(256)  not null,
    created_at        timestamp     not null,
    updated_at        timestamp     not null,
    jira_ticket       varchar(256)
);

alter table client_mtls_certificates_dn
    add constraint fk_client_mtls_certificates_dn_client_id foreign key (client_id) references client (client_id);
create unique index unq_client_mtls_certificates_dn_client_id_subject_dn_issuer_dn on client_mtls_certificates_dn (client_id, subject_dn, issuer_dn);
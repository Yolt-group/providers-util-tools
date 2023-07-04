drop table if exists client_mtls_certificates;

create table if not exists client_mtls_certificates
(
    client_id   uuid          not null,
    fingerprint varchar(40)   not null,
    serial      numeric       not null,
    subject_dn  varchar(1024) not null,
    issuer_dn   varchar(1024) not null,
    valid_start timestamp     not null,
    valid_end   timestamp     not null,
    first_seen  timestamp     not null,
    last_seen   timestamp     not null,
    certificate text          not null,
    primary key (client_id, fingerprint),
    foreign key (client_id) references client (client_id)
);
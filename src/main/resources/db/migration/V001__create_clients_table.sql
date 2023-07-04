create table if not exists client_group(
    id uuid not null constraint pk_client_group primary key,
    name varchar(256) not null
);
create table if not exists client(
    client_id uuid not null constraint pk_clients primary key,
    name varchar(256) not null,
    kyc_country_code varchar(256) not null,
    client_group_id uuid not null,
    kyc_private_individuals boolean not null,
    kyc_entities boolean not null,
    psd2_licensed boolean not null,
    data_enrichment_merchant_recognition boolean not null,
    data_enrichment_categorization boolean not null,
    data_enrichment_cycle_detection boolean not null,
    data_enrichment_labels boolean not null,
    grace_period_in_days integer,
    FOREIGN KEY (client_group_id) REFERENCES client_group (id)
);
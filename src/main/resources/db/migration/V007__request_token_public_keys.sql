create table if not exists request_token_public_key(
    client_id uuid not null,
    key_id varchar(512),
    request_token_public_key text,
    created timestamp,
    primary key (key_id, client_id)
);

alter table request_token_public_key add constraint fk_request_token_public_key_client_id foreign key (client_id) references client(client_id);
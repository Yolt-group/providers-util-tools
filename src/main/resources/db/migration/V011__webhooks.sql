create table if not exists webhooks
(
    client_id   uuid not null,
    webhook_url text not null,
    enabled     boolean,
    primary key (client_id, webhook_url)
);

alter table webhooks
    add constraint fk_webhooks_client_id foreign key (client_id) references client (client_id);

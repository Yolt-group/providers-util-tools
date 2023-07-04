create table if not exists redirect_url
(
    client_id       uuid not null,
    redirect_url_id uuid not null,
    redirect_url    text,
    primary key (redirect_url_id, client_id)
);

alter table redirect_url
    add constraint fk_redirect_url_client_id foreign key (client_id) references client (client_id);

create table if not exists allowed_outbound_hosts
(
    id           uuid          not null,
    client_id    uuid          not null,
    host         varchar(1024) not null,
    status       varchar(256)  not null,
    last_updated timestamp     not null,
    jira_ticket  varchar(256),
    primary key (id),
    unique (client_id, host),
    foreign key (client_id) references client (client_id)
);

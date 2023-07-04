create table if not exists ip_allow_list
(
    id           uuid         not null,
    client_id    uuid         not null,
    cidr         varchar(256) not null,
    status       varchar(256) not null,
    last_updated timestamp    not null,
    jira_ticket  varchar(256),
    primary key (id),
    unique (client_id, cidr),
    foreign key (client_id) references client (client_id)
);

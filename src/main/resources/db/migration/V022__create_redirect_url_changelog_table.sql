drop table if exists redirect_url_changelog;

create table if not exists redirect_url_changelog
(
    id                  uuid            not null,
    client_id           uuid            not null,
    request_date        timestamp       not null,
    action              varchar(256)    not null,
    redirect_url_id     uuid            not null,
    redirect_url        text,
    new_redirect_url    text,
    comment             varchar(500),
    status              varchar(256)    not null,
    jira_ticket         varchar(256),
    primary key (id),
    foreign key (client_id) references client (client_id)
);
create table client_onboarded_scraping_provider (
    client_id uuid not null references client (client_id),
    provider text not null,
    service_type text not null,
    created_at timestamptz not null default now(),
    primary key (client_id, provider, service_type),
    constraint service_type_valid_values check (service_type in ('AIS'))
);
comment on table client_onboarded_scraping_provider is 'Onboarded scraping providers.';
comment on column client_onboarded_scraping_provider.client_id is 'The client for which the scraping provider is onboarded.';
comment on column client_onboarded_scraping_provider.provider is 'Identifier of the scraping provider.';
comment on column client_onboarded_scraping_provider.service_type is 'The service type for which the provider is onboarded.';
comment on constraint service_type_valid_values on client_onboarded_scraping_provider is 'Scraping providers offer only AIS services.';

create table client_onboarded_provider (
    client_id uuid not null,
    redirect_url_id uuid not null,
    service_type text not null,
    provider text not null,
    created_at timestamptz not null default now(),
    primary key (client_id, redirect_url_id, service_type, provider),
    foreign key (client_id, redirect_url_id) references redirect_url (client_id, redirect_url_id),
    constraint service_type_valid_values check (service_type in ('AIS', 'PIS'))
);
comment on table client_onboarded_provider is 'Onboarded providers on client level (non-scraping).';
comment on column client_onboarded_provider.client_id is 'The client for which the provider is onboarded.';
comment on column client_onboarded_provider.redirect_url_id is 'Identifier of the redirectUrl with which the provider is onboarded.';
comment on column client_onboarded_provider.service_type is 'The service type for which the provider is onboarded.';
comment on column client_onboarded_provider.provider is 'Identifier of the onboarded provider.';
comment on constraint service_type_valid_values on client_onboarded_provider is 'Either AIS or PIS are permitted.  And yes this should be an enum but the app layer cant deal with it.';

--
-- The table below is missing a reference to a "client_group_redirect_url", we don't store these in the database
-- or permit administration thereof in the system.  The reasons are historical, in practice there is only a single
-- ClientGroup and that is the Yolt client group, used by our unlicensed clients.  All onboarded providers
-- for the Yolt client group utilize the same redirectUrl (hard-coded to '7a900fdd-2048-4359-975a-d2646f2718a8').
--
create table client_group_onboarded_provider (
    client_group_id uuid not null references client_group (id),
    provider text not null,
    service_type text not null,
    created_at timestamptz not null default now(),
    primary key (client_group_id, provider, service_type),
    constraint service_type_valid_values check (service_type in ('AIS', 'PIS'))
);
comment on table client_group_onboarded_provider is 'Onboarded providers on client group level (non-scraping).';
comment on column client_group_onboarded_provider.client_group_id is 'Identifier of the client group.';
comment on column client_group_onboarded_provider.provider is 'Identifier of the onboarded provider.';
comment on column client_group_onboarded_provider.service_type is 'The service type for which the provider is onboarded.';
comment on constraint service_type_valid_values on client_group_onboarded_provider is 'Either AIS or PIS are permitted.  And yes this should be an enum but the app layer cant deal with it.';


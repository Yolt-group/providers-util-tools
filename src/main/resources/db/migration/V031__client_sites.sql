--
-- Create a view that contains a row for every onboarded bank.
--
-- Note: this view offers a "clients' perspective" on the onboarded banks.  For example, a non-PSD2-licensed
--       has 0 onboarded banks in practice.  But this view will nevertheless contain a row for each combination
--       of a redirect_url & an onboarded bank at the client_group level, because that is what the client
--       can use in practice.
--
-- Note: the column redirect_url_id is only null for scraping providers
--
create view view_onboarded_client_sites (
        client_id,
        provider,
        service_type,
        redirect_url_id
) as select cosp.client_id
          , cosp.provider
          , cosp.service_type
          , null
       from client_onboarded_scraping_provider cosp
     union all
     select cop.client_id
          , cop.provider
          , cop.service_type
          , cop.redirect_url_id
       from client_onboarded_provider cop
     union all
     select ru.client_id
          , cgop.provider
          , cgop.service_type
          , ru.redirect_url_id
       from client_group_onboarded_provider cgop
       join client c on cgop.client_group_id = c.client_group_id
       join redirect_url ru on c.client_id = ru.client_id;

--
-- This table purposefully doesn't have any foreign keys to these tables:
-- * client_onboarded_scraping_provider
-- * client_onboarded_provider
-- * client_group_onboarded_provider
--
-- It might be "correct" to have these keys, we didn't add them because:
-- * this service does not own the authentication means data / bank onboardings, we merely have a copy, and thus we
--   are not in charge of when an onboarding might be deleted.  Hence we don't want to add FKs to those tables
-- * it might be desirable to delete a bank onboarding / authentication means and to then add it back in later
--   without removing / altering the meta data for the sites/banks affected by the onboarding
--
-- A consequence of the above is that this table may contain "stale" data: there might be a row in this table for
-- a bank that was once onboarded but that is no longer onboarded.  Do not query this table to provide clients
-- with information about what banks they can communicate with.  Use the view_client_site for that instead.
--
create table client_site_metadata (
    client_id uuid not null references client (client_id),
    provider text not null,
    site_id uuid not null,
    available boolean not null default false,
    enabled boolean not null default false,
    use_experimental_version boolean not null default false,
    created_at timestamptz not null default now(),
    primary key (client_id, site_id)
);
comment on table client_site_metadata is 'Contains meta data about an onboarded bank for a given client.';
comment on column client_site_metadata.client_id is 'Identifier of the client.';
comment on column client_site_metadata.site_id is 'Identifier of the site.  No FK because providers owns this data.';
comment on column client_site_metadata.provider is 'Identifier of the provider for the site.';
comment on column client_site_metadata.available is 'Is the site available for the client to enable?';
comment on column client_site_metadata.enabled is 'Is the site enabled for the client?  E.g.: can the client interact with this bank?';

create table client_site_metadata_tags (
    client_id uuid not null,
    site_id uuid not null,
    tag text not null,
    created_at timestamptz not null default now(),
    primary key (client_id, site_id, tag),
    foreign key (client_id, site_id) references client_site_metadata (client_id, site_id)
);
comment on table client_site_metadata_tags is 'A set of tags linked to a particular site for a client.';
comment on column client_site_metadata_tags.client_id is 'Identifier of the client.';
comment on column client_site_metadata_tags.site_id is 'Identifier of the site / bank.  Data is owned by providers, hence no FK.';
comment on column client_site_metadata_tags.tag is 'A tag.';

create view view_flattened_client_site_metadata_tags
as select client_id
        , site_id
        , array_agg(tag order by tag) as tags
     from client_site_metadata_tags
 group by client_id, site_id;

----
---- The table that can be queried to retrieve client site information.
----
--create view view_client_site (
--        client_id,
--        provider,
--        service_type,
--        redirect_url_id,
--        client_site_metadata_exists,
--        available,
--        enabled,
--        tags
--) as select vocs.client_id
--          , vocs.provider
--          , vocs.service_type
--          , vocs.redirect_url_id
--          , csm.available is not null
--          , coalesce(csm.available, false)
--          , coalesce(csm.enabled, false)
--          , vfcsmt.tags
--       from view_onboarded_client_sites vocs
--  left join client_site_metadata csm
--         on vocs.client_id = csm.client_id
--        and vocs.provider = csm.provider
--  left join view_flattened_client_site_metadata_tags vfcsmt
--         on csm.client_id = vfcsmt.client_id
--        and csm.site_id = vfcsmt.site_id;

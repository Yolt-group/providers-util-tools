----
---- rename of view_onboarded_client_sites, which is to be dropped later.
----
create view onboarded_provider_view (
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
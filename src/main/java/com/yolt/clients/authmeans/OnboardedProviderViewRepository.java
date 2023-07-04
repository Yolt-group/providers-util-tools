package com.yolt.clients.authmeans;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The onboarded provider view. This is a view on {@link ClientGroupOnboardedProvider}, {@link ClientOnboardedProvider}
 * and {@link ClientOnboardedScrapingProvider}
 *
 * This repository was written in a rather non-Java-ish way because we were tired of fighting with JPA/Hibernate and
 * just wanted to do a "select * from onboarded_provider_view" without having to appease JPA by adding primary keys
 * to an entity when it doesn't make sense (subsequently causing all sorts of strange behaviour when one of the PK
 * columns was null in practice, etc.).
 */
@Repository
@RequiredArgsConstructor
public class OnboardedProviderViewRepository {

    private final EntityManager entityManager;

    public List<OnboardedProviderView> selectAllForClientAndProvider(UUID clientId, String provider) {
        return executeNativeQuery(entityManager.createNativeQuery("""
                        select cast(client_id as text)
                             , provider
                             , service_type
                             , cast(redirect_url_id as text)
                          from onboarded_provider_view
                         where client_id = ?1
                           and provider = ?2
                        """)
                .setParameter(1, clientId)
                .setParameter(2, provider)
        );
    }

    public List<OnboardedProviderView> selectAllForClient(UUID clientId) {
        return executeNativeQuery(entityManager.createNativeQuery("""
                        select cast(client_id as text)
                             , provider
                             , service_type
                             , cast(redirect_url_id as text)
                          from onboarded_provider_view
                         where client_id = ?1
                        """)
                .setParameter(1, clientId)
        );
    }

    public List<OnboardedProviderView> selectAll() {
        return executeNativeQuery(entityManager.createNativeQuery("""
                select cast(client_id as text)
                     , provider
                     , service_type
                     , cast(redirect_url_id as text)
                  from onboarded_provider_view
                """)
        );
    }

    private List<OnboardedProviderView> executeNativeQuery(Query q) {
        //noinspection unchecked
        return ((List<Object[]>) q.getResultList())
                .stream()
                .map(OnboardedProviderViewRepository::map)
                .collect(Collectors.toList());
    }

    private static OnboardedProviderView map(Object[] row) {
        return new OnboardedProviderView(
                UUID.fromString((String) row[0]),
                (String) row[1],
                ServiceType.valueOf((String) row[2]),
                // redirectUrlId is nullable
                row[3] != null ? UUID.fromString((String) row[3]) : null
        );
    }

}

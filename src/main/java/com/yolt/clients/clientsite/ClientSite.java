package com.yolt.clients.clientsite;

import com.yolt.clients.sites.Site;
import lombok.Value;
import nl.ing.lovebird.providerdomain.ServiceType;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The client site. The site enriched with information that belongs to a particular client.
 * You need to have an onboarded provider for the client-provider combination in order to even have a client site.
 */
@Value
public class ClientSite {
    Site site;
    boolean available;
    boolean enabled;
    boolean useExperimentalVersion;
    Set<String> tags;
    List<AuthenticationMeansScope> registeredAuthenticationMeans;

    /**
     * @return a {@see List} of redirect-urls for the given <code>serviceType</code>
     */
    public Set<UUID> getRedirectUrlIds(@NonNull final ServiceType serviceType) {
        return registeredAuthenticationMeans.stream()
                .filter(AuthenticationMeansScope.hasServiceType(serviceType)
                        .and(AuthenticationMeansScope.hasType(AuthenticationMeansScope.Type.REDIRECT_URL)))
                .flatMap(scope -> scope.getRedirectUrlId().stream())
                .collect(Collectors.toSet());
    }
    public Set<UUID> getRedirectUrlIds() {
        return registeredAuthenticationMeans.stream()
                .flatMap(scope -> scope.getRedirectUrlId().stream())
                .collect(Collectors.toSet());
    }
}

package com.yolt.clients.clientgroup.certificatemanagement.providers.dto;

import lombok.AllArgsConstructor;
import lombok.Value;
import nl.ing.lovebird.providerdomain.ServiceType;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Set;
import java.util.UUID;

@Value
@AllArgsConstructor
public class ProviderAuthenticationMeansBatch {

    @NotNull
    String provider;

    @NotEmpty
    Set<UUID> redirectUrlIds;

    @NotEmpty
    Set<ServiceType> serviceTypes;

    @NotNull
    Set<AuthenticationMeans> authenticationMeans;

    boolean ignoreAutoOnboarding;

    @Value
    public static class AuthenticationMeans {
        @NotNull
        String name;
        @NotNull
        String value;
    }

}

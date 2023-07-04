package com.yolt.clients.authmeans;

import lombok.Value;
import org.springframework.lang.Nullable;

import java.util.UUID;

/**
 * See {@link OnboardedProviderViewRepository}
 */
@Value
public class OnboardedProviderView {
    UUID clientId;
    String provider;
    ServiceType serviceType;
    /**
     * This field is only nullable for scrapers.
     * If the provider is onboarded on clientGroup level, there will be an entry for each redirect-url-id of the client.
     */
    @Nullable
    UUID redirectUrlId;
}

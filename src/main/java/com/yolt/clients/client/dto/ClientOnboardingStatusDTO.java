package com.yolt.clients.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ClientOnboardingStatusDTO {
    Status distinguishedNameConfigured;
    // Apparently, Jackson serializer is confused with properties named with 'm' in front, like 'mTls...'
    // Renaming the property into a more verbose 'mutualTls...' resolves Jackson's confusion but requires the annotation to indicate the name per API.
    @JsonProperty("mTlsCertificatesConfigured")
    Status mutualTlsCertificatesConfigured;
    Status redirectUrlConfigured;
    Status webhookUrlConfigured;
    Status ipAllowListConfigured;
    Status requestTokenConfigured;
    Status webhookDomainAllowListConfigured;

    public enum Status {
        CONFIGURED, PENDING, NOT_CONFIGURED
    }
}

package com.yolt.clients.authmeans;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Onboarded scraping providers.
 */
@Data
@Builder
@Entity
@Table(name = "client_onboarded_scraping_provider")
@NoArgsConstructor
@AllArgsConstructor
class ClientOnboardedScrapingProvider {

    @EmbeddedId
    ClientOnboardedScrapingProviderId clientOnboardedScrapingProviderId;

    @Column(insertable = false, updatable = false)
    Instant createdAt;

    @Data
    @Builder
    @Embeddable
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClientOnboardedScrapingProviderId implements Serializable {

        UUID clientId;

        String provider;

        @Enumerated(EnumType.STRING)
        ServiceType serviceType;

    }
}

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
 * Onboarded providers on client level (non-scraping).
 */
@Data
@Entity
@Builder
@Table(name = "client_onboarded_provider")
@NoArgsConstructor
@AllArgsConstructor
class ClientOnboardedProvider {

    @EmbeddedId
    ClientOnboardedProviderId clientOnboardedProviderId;

    @Column(insertable = false, updatable = false)
    Instant createdAt;

    @Data
    @Embeddable
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClientOnboardedProviderId implements Serializable {

        UUID clientId;

        UUID redirectUrlId;

        @Enumerated(EnumType.STRING)
        ServiceType serviceType;

        String provider;

    }
}

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
 * Onboarded providers on client group level (non-scraping).
 */
@Data
@Entity
@Builder
@Table(name = "client_group_onboarded_provider")
@NoArgsConstructor
@AllArgsConstructor
class ClientGroupOnboardedProvider {

    @EmbeddedId
    ClientGroupOnboardedProviderId clientGroupOnboardedProviderId;

    @Column(insertable = false, updatable = false)
    Instant createdAt;

    @Data
    @Builder
    @Embeddable
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClientGroupOnboardedProviderId implements Serializable {

        UUID clientGroupId;

        String provider;

        @Enumerated(EnumType.STRING)
        ServiceType serviceType;

    }
}

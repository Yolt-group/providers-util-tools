package com.yolt.clients.clientsitemetadata;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Fetch;

import javax.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.hibernate.annotations.FetchMode.JOIN;

/**
 * Metadata for a ClientSite.
 */
@Data
@Entity
@Builder
@Table(name = "client_site_metadata")
@NoArgsConstructor
@AllArgsConstructor
public class ClientSiteMetadata {

    @EmbeddedId
    ClientSiteMetadataId id;

    String provider;

    boolean available;

    boolean enabled;

    boolean useExperimentalVersion;

    @Column(insertable = false, updatable = false)
    Instant createdAt;

    @Fetch(JOIN)
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumns({
            @JoinColumn(name = "client_id", referencedColumnName = "client_id", insertable = false, updatable = false),
            @JoinColumn(name = "site_id", referencedColumnName = "site_id", insertable = false, updatable = false),
    })
    Set<ClientSiteMetadataTags> tags;

    @Data
    @Embeddable
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClientSiteMetadataId implements Serializable {
        @Column(name = "client_id")
        UUID clientId;
        @Column(name = "site_id")
        UUID siteId;
    }

    public ClientSiteMetadata(UUID clientId, UUID siteId, String provider) {
        this(new ClientSiteMetadataId(clientId, siteId), provider, false, false, false, null, new HashSet<>());
    }
}

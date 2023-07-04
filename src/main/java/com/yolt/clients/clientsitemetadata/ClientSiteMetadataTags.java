package com.yolt.clients.clientsitemetadata;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Tags for a ClientSite.
 */
@Data
@Entity
@Builder
@Table(name = "client_site_metadata_tags")
@NoArgsConstructor
@AllArgsConstructor
public class ClientSiteMetadataTags {

    @EmbeddedId
    ClientSiteMetadataTagsId clientSiteMetadataTagsId;

    @Column(insertable = false, updatable = false)
    Instant createdAt;

    public ClientSiteMetadataTags(UUID clientId, UUID siteId, String tag) {
        this.clientSiteMetadataTagsId = new ClientSiteMetadataTagsId(clientId, siteId, tag);
    }

    @Data
    @Embeddable
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClientSiteMetadataTagsId implements Serializable {
        @Column(name = "client_id")
        UUID clientId;
        @Column(name = "site_id")
        UUID siteId;
        String tag;
    }
}

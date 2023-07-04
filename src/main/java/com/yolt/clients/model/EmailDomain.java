package com.yolt.clients.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "client_group_allowed_email_domain")
@Builder
@AllArgsConstructor
@Data
@NoArgsConstructor
@IdClass(EmailDomain.EmailDomainId.class)
public class EmailDomain {

    @Id
    @Column(name = "client_group_id")
    private UUID clientGroupId;

    @Id
    @Column(name = "domain")
    private String domain;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class EmailDomainId implements Serializable {
        private String domain;
        private UUID clientGroupId;
    }
}

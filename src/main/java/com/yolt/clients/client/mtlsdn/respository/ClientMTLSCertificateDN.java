package com.yolt.clients.client.mtlsdn.respository;

import com.yolt.clients.jira.Status;
import lombok.*;
import org.hibernate.Hibernate;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "client_mtls_certificates_dn")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ClientMTLSCertificateDN {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "client_id")
    private UUID clientId;

    @Column(name = "subject_dn")
    private String subjectDN;

    @Column(name = "issuer_dn")
    private String issuerDN;

    @Column(name = "certificate_chain")
    private String certificateChain;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "jira_ticket")
    private String jiraTicket;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        ClientMTLSCertificateDN that = (ClientMTLSCertificateDN) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}

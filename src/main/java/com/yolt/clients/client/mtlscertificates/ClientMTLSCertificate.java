package com.yolt.clients.client.mtlscertificates;

import lombok.*;
import org.hibernate.Hibernate;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "client_mtls_certificates")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@IdClass(ClientMTLSCertificate.CompositeKey.class)
public class ClientMTLSCertificate {
    @Id
    @Column(name = "client_id")
    private UUID clientId;
    @Id
    @Column(name = "fingerprint")
    private String fingerprint;

    @Column(name = "serial")
    private BigInteger serial;
    @Column(name = "subject_dn")
    private String subjectDN;
    @Column(name = "issuer_dn")
    private String issuerDN;
    @Column(name = "valid_start")
    private LocalDateTime validStart;
    @Column(name = "valid_end")
    private LocalDateTime validEnd;
    @Column(name = "first_seen")
    private LocalDateTime firstSeen;
    @Column(name = "last_seen")
    private LocalDateTime lastSeen;
    @Column(name = "certificate")
    private String certificate;
    @Column(name = "sort_date")
    private LocalDateTime sortDate;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CompositeKey implements Serializable {
        private UUID clientId;
        private String fingerprint;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        ClientMTLSCertificate that = (ClientMTLSCertificate) o;

        if (!Objects.equals(clientId, that.clientId)) return false;
        return Objects.equals(fingerprint, that.fingerprint);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(clientId);
        result = 31 * result + (Objects.hashCode(fingerprint));
        return result;
    }
}

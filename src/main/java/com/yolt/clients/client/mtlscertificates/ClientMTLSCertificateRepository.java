package com.yolt.clients.client.mtlscertificates;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface ClientMTLSCertificateRepository extends CrudRepository<ClientMTLSCertificate, ClientMTLSCertificate.CompositeKey> {
    boolean existsByClientId(UUID clientId);

    boolean existsByClientIdAndFingerprint(UUID clientId, String fingerprint);

    Optional<ClientMTLSCertificate> findByClientIdAndFingerprint(UUID clientId, String fingerprint);

    @Modifying
    @Query("update ClientMTLSCertificate cert set cert.lastSeen = :lastSeen, cert.sortDate = :lastSeen where cert.clientId = :clientId and cert.fingerprint = :fingerprint and (cert.lastSeen < :lastSeen or cert.lastSeen is null)")
    void updateLastSeen(@Param("clientId") UUID clientId, @Param("fingerprint") String fingerprint, @Param("lastSeen") LocalDateTime lastSeen);

    @Modifying
    @Query("update ClientMTLSCertificate cert set cert.firstSeen = :firstSeen where cert.clientId = :clientId and cert.fingerprint = :fingerprint and (cert.firstSeen > :firstSeen or cert.firstSeen is null)")
    void updateFirstSeen(@Param("clientId") UUID clientId, @Param("fingerprint") String fingerprint, @Param("firstSeen") LocalDateTime firstSeen);

    Page<ClientMTLSCertificate> findAllByClientId(UUID clientId, Pageable pageable);
}

package com.yolt.clients.client.admins;


import com.yolt.clients.client.admins.models.ClientAdminInvitation;
import com.yolt.clients.client.admins.models.ClientAdminInvitationWithLastCode;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface ClientAdminInvitationRepository extends CrudRepository<ClientAdminInvitation, UUID> {
    Optional<ClientAdminInvitation> findByCodes_code(String code);

    @Query(nativeQuery = true, value = """
            SELECT CAST(A.id AS VARCHAR)                                             AS idText,
                   CAST(A.client_id AS VARCHAR)                                      AS clientIdText,
                   A.email,
                   A.name,
                   A.generated_at                                                    AS generatedAt,
                   A.expires_at                                                      AS expiresAt,
                   A.used_at                                                         AS usedAt,
                   CAST(A.used_by AS VARCHAR)                                        AS usedByText,
                   CASE WHEN B.numberOfCodes IS NULL THEN 0 ELSE B.numberOfCodes END AS numberOfCodes
            FROM (SELECT DISTINCT ON (T1.email) T1.id, T1.client_id, T1.email, T1.name, T2.generated_at, T2.expires_at, T2.used_at, T2.used_by
                  FROM client_admin_invitation_v2 T1 LEFT JOIN client_admin_invitation_codes T2 ON T1.id = T2.id
                  WHERE client_id = :clientId
                  ORDER BY T1.email, T2.generated_at DESC NULLS LAST) A
            LEFT JOIN (SELECT COUNT(code) AS numberOfCodes, id FROM client_admin_invitation_codes GROUP BY id) B ON A.id = B.id;
            """)
    Set<ClientAdminInvitationWithLastCode> findAllByClientIdWithLastInvite(UUID clientId);

    boolean existsByClientIdAndEmail(UUID clientId, String email);

    Optional<ClientAdminInvitation> findByClientIdAndId(UUID clientId, UUID invitationId);

    List<ClientAdminInvitation> findAll();

    int countClientAdminInvitationByClientId(UUID clientId);
}

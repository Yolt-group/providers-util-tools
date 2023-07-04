package com.yolt.clients.client.admins.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "client_admin_invitation_v2")
@Builder
@AllArgsConstructor
@Data
@NoArgsConstructor
public class ClientAdminInvitation {
    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "client_id")
    private UUID clientId;

    @Column(name = "email")
    private String email;

    @Column(name = "name")
    private String name;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @JoinColumn(name = "id", nullable = false)
    Set<ClientAdminInvitationCode> codes;

    public ClientAdminInvitation(UUID clientId, String email, String name, String activationCode, LocalDateTime generationTimeStamp, LocalDateTime expirationTimeStamp) {
        this(UUID.randomUUID(), clientId, email, name, Set.of(new ClientAdminInvitationCode(activationCode, generationTimeStamp, expirationTimeStamp)));
    }
}

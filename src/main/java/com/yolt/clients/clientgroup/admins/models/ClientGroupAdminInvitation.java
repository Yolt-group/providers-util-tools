package com.yolt.clients.clientgroup.admins.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "client_group_admin_invitation_v2")
@Builder
@AllArgsConstructor
@Data
@NoArgsConstructor
public class ClientGroupAdminInvitation {
    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "client_group_id")
    private UUID clientGroupId;

    @Column(name = "email")
    private String email;

    @Column(name = "name")
    private String name;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @JoinColumn(name = "id", nullable = false)
    Set<ClientGroupAdminInvitationCode> codes;

    public ClientGroupAdminInvitation(UUID clientGroupId, String email, String name, String activationCode, LocalDateTime generationTimeStamp, LocalDateTime expirationTimeStamp) {
        this(UUID.randomUUID(), clientGroupId, email, name, Set.of(new ClientGroupAdminInvitationCode(activationCode, generationTimeStamp, expirationTimeStamp)));
    }
}

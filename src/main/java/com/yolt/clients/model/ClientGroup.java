package com.yolt.clients.model;

import com.yolt.clients.clientgroup.admins.models.ClientGroupAdminInvitation;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "client_group")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClientGroup {
    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "name")
    private String name;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @JoinColumn(name = "client_group_id", nullable = false, insertable = false, updatable = false)
    private Set<Client> clients;

    public ClientGroup(UUID clientGroupId, String clientGroupName) {
        this(clientGroupId, clientGroupName, new HashSet<>(), new HashSet<>(), new HashSet<>());
    }

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @JoinColumn(name = "client_group_id", nullable = false, insertable = false, updatable = false)
    private Set<EmailDomain> emailDomains;

    /**
     * @deprecated see YCL-2517
     */
    @Deprecated(forRemoval = true)
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @JoinColumn(name = "client_group_id", nullable = false, insertable = false, updatable = false)
    private Set<ClientGroupAdminInvitation> clientGroupAdminInvitations;

}

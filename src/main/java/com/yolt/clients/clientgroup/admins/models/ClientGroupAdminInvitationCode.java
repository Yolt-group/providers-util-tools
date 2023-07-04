package com.yolt.clients.clientgroup.admins.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "client_group_admin_invitation_codes")
@Builder
@AllArgsConstructor
@Data
@NoArgsConstructor
public class ClientGroupAdminInvitationCode {
    @Id
    @Column(name = "code")
    private String code;

    @Column(name = "generated_at")
    private LocalDateTime generatedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "used_by")
    private UUID usedBy;

    public ClientGroupAdminInvitationCode(String code, LocalDateTime generatedAt, LocalDateTime expiresAt) {
        this(code, generatedAt, expiresAt, null, null);
    }
}
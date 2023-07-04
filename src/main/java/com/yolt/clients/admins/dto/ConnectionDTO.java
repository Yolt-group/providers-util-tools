package com.yolt.clients.admins.dto;

import com.yolt.clients.client.admins.models.ClientAdminInvitation;
import com.yolt.clients.clientgroup.admins.models.ClientGroupAdminInvitation;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.springframework.lang.Nullable;

import javax.validation.constraints.NotNull;
import java.util.UUID;

@Value
@AllArgsConstructor
public class ConnectionDTO {

    @NotNull
    UUID inviteId;

    @Nullable
    UUID clientGroupId;

    @Nullable
    UUID clientId;

    public ConnectionDTO(ClientGroupAdminInvitation invitation) {
        this.inviteId = invitation.getId();
        this.clientGroupId = invitation.getClientGroupId();
        this.clientId = null;
    }

    public ConnectionDTO(ClientAdminInvitation invitation) {
        this.inviteId = invitation.getId();
        this.clientGroupId = null;
        this.clientId = invitation.getClientId();
    }
}

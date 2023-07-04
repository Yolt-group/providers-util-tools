package com.yolt.clients.clientgroup.dto;

import com.yolt.clients.client.dto.ClientDTO;
import lombok.Value;

import java.util.List;
import java.util.UUID;

@Value
public class ClientGroupDetailsDTO {
    UUID clientGroupId;
    String name;

    List<ClientDTO> clients;
    List<DomainDTO> domains;

    /**
     * @deprecated see YCL-2517
     */
    @Deprecated(forRemoval = true)
    List<AdminInviteDTO> clientGroupAdminInvites;
}

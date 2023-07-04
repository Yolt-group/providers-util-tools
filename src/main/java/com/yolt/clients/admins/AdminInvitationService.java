package com.yolt.clients.admins;

import com.yolt.clients.admins.dto.ConnectionDTO;
import com.yolt.clients.admins.dto.InvitationCodeDTO;
import com.yolt.clients.client.admins.ClientAdminService;
import com.yolt.clients.clientgroup.admins.ClientGroupAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AdminInvitationService {
    private final ClientGroupAdminService clientGroupAdminService;
    private final ClientAdminService clientAdminService;

    public Optional<ConnectionDTO> connect(InvitationCodeDTO code) {
        return clientGroupAdminService.connect(code.getCode(), code.getPortalUserId())
                .or(() -> clientAdminService.connect(code.getCode(), code.getPortalUserId()));
    }
}

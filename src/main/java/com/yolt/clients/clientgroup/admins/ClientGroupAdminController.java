package com.yolt.clients.clientgroup.admins;

import com.yolt.clients.admins.dto.NewInviteDTO;
import com.yolt.clients.admins.dto.PortalUserIdDTO;
import com.yolt.clients.clientgroup.admins.models.ClientGroupAdminDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.clienttokens.ClientGroupToken;
import nl.ing.lovebird.clienttokens.annotations.VerifiedClientToken;
import nl.ing.lovebird.clienttokens.verification.ClientGroupIdVerificationService;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/internal/client-groups/{clientGroupId}/admins")
public class ClientGroupAdminController {
    private final ClientGroupAdminService clientGroupAdminService;
    private final ClientGroupIdVerificationService clientGroupIdVerificationService;

    @GetMapping(produces = APPLICATION_JSON_VALUE)
    public List<ClientGroupAdminDTO> getClientGroupInvites(@PathVariable UUID clientGroupId,
                                                           @VerifiedClientToken ClientGroupToken clientGroupToken) {
        clientGroupIdVerificationService.verify(clientGroupToken, clientGroupId);
        return clientGroupAdminService.getClientGroupAdmins(clientGroupToken);
    }

    @PostMapping(consumes = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void createClientGroupInvite(@PathVariable UUID clientGroupId,
                                        @VerifiedClientToken ClientGroupToken clientGroupToken,
                                        @RequestBody NewInviteDTO newInviteDTO) {
        clientGroupIdVerificationService.verify(clientGroupToken, clientGroupId);
        clientGroupAdminService.inviteClientGroupAdmin(clientGroupToken, newInviteDTO);
    }

    @PostMapping(value = "{invitationId}/resend")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void resendClientGroupInvite(@PathVariable UUID clientGroupId,
                                        @PathVariable UUID invitationId,
                                        @VerifiedClientToken ClientGroupToken clientGroupToken) {
        clientGroupIdVerificationService.verify(clientGroupToken, clientGroupId);
        clientGroupAdminService.resendClientGroupInvite(clientGroupToken, invitationId);
    }

    @PostMapping(value = "/remove-authorization", consumes = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void removeAuthorization(@PathVariable UUID clientGroupId,
                                    @VerifiedClientToken ClientGroupToken clientGroupToken,
                                    @RequestBody PortalUserIdDTO portalUserId) {
        clientGroupIdVerificationService.verify(clientGroupToken, clientGroupId);
        clientGroupAdminService.removeAuthorization(clientGroupToken, portalUserId.getPortalUserId());
    }
}

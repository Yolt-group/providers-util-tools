package com.yolt.clients.client.admins;

import com.yolt.clients.admins.dto.NewInviteDTO;
import com.yolt.clients.admins.dto.PortalUserIdDTO;
import com.yolt.clients.client.admins.models.ClientAdminDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.clienttokens.annotations.NonDeletedClient;
import nl.ing.lovebird.clienttokens.annotations.VerifiedClientToken;
import nl.ing.lovebird.clienttokens.verification.ClientIdVerificationService;
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
@RequestMapping("/internal/clients/{clientId}/admins")
public class ClientAdminController {
    private final ClientAdminService clientAdminService;
    private final ClientIdVerificationService clientIdVerificationService;

    @GetMapping(produces = APPLICATION_JSON_VALUE)
    public List<ClientAdminDTO> getClientAdmins(@PathVariable UUID clientId,
                                                @VerifiedClientToken ClientToken clientToken) {
        clientIdVerificationService.verify(clientToken, clientId);
        return clientAdminService.getClientAdmins(clientToken);
    }

    @NonDeletedClient
    @PostMapping(consumes = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void createClientInvite(@PathVariable UUID clientId,
                                   @VerifiedClientToken ClientToken clientToken,
                                   @RequestBody NewInviteDTO newInviteDTO) {
        clientIdVerificationService.verify(clientToken, clientId);
        clientAdminService.inviteClientAdmin(clientToken, newInviteDTO);
    }

    @NonDeletedClient
    @PostMapping(value = "{invitationId}/resend")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void resendClientInvite(@PathVariable UUID clientId,
                                   @PathVariable UUID invitationId,
                                   @VerifiedClientToken ClientToken clientToken) {
        clientIdVerificationService.verify(clientToken, clientId);
        clientAdminService.resendClientInvite(clientToken, invitationId);
    }

    @PostMapping(value = "/remove-authorization", consumes = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void removeAuthorization(@PathVariable UUID clientId,
                                    @VerifiedClientToken ClientToken clientToken,
                                    @RequestBody PortalUserIdDTO portalUserId) {
        clientIdVerificationService.verify(clientToken, clientId);
        clientAdminService.removeAuthorization(clientToken, portalUserId.getPortalUserId());
    }
}

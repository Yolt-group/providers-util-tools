package com.yolt.clients.client;

import com.yolt.clients.client.dto.ClientDTO;
import com.yolt.clients.client.dto.ClientOnboardingStatusDTO;
import com.yolt.clients.client.dto.UpdateClientDTO;
import com.yolt.clients.clientgroup.ClientGroupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.clienttokens.annotations.NonDeletedClient;
import nl.ing.lovebird.clienttokens.annotations.VerifiedClientToken;
import nl.ing.lovebird.clienttokens.verification.ClientIdVerificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/internal/clients")
public class ClientController {

    private static final String ASSISTANCE_PORTAL_YTS = "assistance-portal-yts";
    private static final String DEV_PORTAL = "dev-portal";

    private final ClientService clientService;
    private final ClientIdVerificationService clientIdVerificationService;
    private final ClientOnboardingStatusService clientOnboardingStatusService;
    private final DeleteClientService deleteClientService;
    private final ClientGroupService clientGroupService;

    @GetMapping(produces = APPLICATION_JSON_VALUE)
    public List<ClientDTO> getClients(@RequestParam(defaultValue = "false") boolean deleted) {
        return clientService.getClients(deleted);
    }

    @GetMapping(value = "/{clientId}", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<ClientDTO> getClient(@PathVariable UUID clientId,
                                               @VerifiedClientToken(restrictedTo = {ASSISTANCE_PORTAL_YTS, DEV_PORTAL}) ClientToken clientToken) {

        clientIdVerificationService.verify(clientToken, clientId);
        return new ResponseEntity<>(clientService.getClient(clientToken), HttpStatus.OK);
    }

    @DeleteMapping(value = "/{clientId}")
    public void deleteClient(@PathVariable UUID clientId,
                             @VerifiedClientToken(restrictedTo = {ASSISTANCE_PORTAL_YTS}) ClientToken clientToken) {
        clientIdVerificationService.verify(clientToken, clientId);
        deleteClientService.deleteClient(clientToken);
    }

    @NonDeletedClient
    @PutMapping(value = "/{clientId}", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<ClientDTO> updateClient(@PathVariable UUID clientId,
                                                  @RequestBody @Valid UpdateClientDTO updateClientDTO,
                                                  @VerifiedClientToken(restrictedTo = { ASSISTANCE_PORTAL_YTS, DEV_PORTAL }) ClientToken clientToken) {
        clientIdVerificationService.verify(clientToken, clientId);
        return new ResponseEntity<>(clientService.updateClient(clientToken, updateClientDTO), HttpStatus.OK);
    }

    @GetMapping(path = "/{clientId}/onboarding-status", produces = APPLICATION_JSON_VALUE)
    public ClientOnboardingStatusDTO getOnboardingStatus(@PathVariable UUID clientId,
                                                         @VerifiedClientToken ClientToken clientToken) {
        clientIdVerificationService.verify(clientToken, clientId);
        return clientOnboardingStatusService.getClientOnboardingStatus(clientToken);
    }

    @Async("syncClientsExecutor")
    @PostMapping("/batch/sync")
    public void syncClients() {
        clientGroupService.syncClientGroups();
        clientService.syncClients();
    }
}

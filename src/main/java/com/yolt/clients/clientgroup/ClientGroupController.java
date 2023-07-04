package com.yolt.clients.clientgroup;

import com.yolt.clients.clientgroup.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.clienttokens.ClientGroupToken;
import nl.ing.lovebird.clienttokens.annotations.VerifiedClientToken;
import nl.ing.lovebird.clienttokens.verification.ClientGroupIdVerificationService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/internal/client-groups")
public class ClientGroupController {
    private final ClientGroupService clientGroupService;
    private final ClientGroupIdVerificationService clientGroupIdVerificationService;

    @PostMapping(produces = APPLICATION_JSON_VALUE)
    public ClientGroupDTO addClientGroup(@Validated @RequestBody NewClientGroupDTO clientGroupDTO) {
        return clientGroupService.addClientGroup(clientGroupDTO);
    }

    @PatchMapping(value = "/{clientGroupId}", produces = APPLICATION_JSON_VALUE)
    public ClientGroupDTO updateClientGroup(@Validated @RequestBody UpdateClientGroupDTO clientGroupDTO,
                                            @PathVariable UUID clientGroupId,
                                            @VerifiedClientToken ClientGroupToken clientGroupToken) {
        clientGroupIdVerificationService.verify(clientGroupToken, clientGroupId);
        return clientGroupService.updateClientGroup(clientGroupToken, clientGroupId, clientGroupDTO);
    }

    @GetMapping(value = "/{clientGroupId}", produces = APPLICATION_JSON_VALUE)
    public ClientGroupDetailsDTO getClientGroup(@PathVariable UUID clientGroupId,
                                                @VerifiedClientToken ClientGroupToken clientGroupToken) {
        clientGroupIdVerificationService.verify(clientGroupToken, clientGroupId);
        return clientGroupService.getClientGroupDetails(clientGroupId);
    }

    @GetMapping(produces = APPLICATION_JSON_VALUE)
    public List<ClientGroupDTO> getClientGroups() {
        return clientGroupService.getClientGroups();
    }

    @PostMapping(value = "/{clientGroupId}/allowed-domains", produces = APPLICATION_JSON_VALUE)
    public void addDomain(@PathVariable UUID clientGroupId,
                          @VerifiedClientToken ClientGroupToken clientGroupToken,
                          @RequestBody DomainDTO domain) {
        clientGroupIdVerificationService.verify(clientGroupToken, clientGroupId);
        clientGroupService.addDomain(clientGroupId, domain);
    }

    @DeleteMapping(value = "/{clientGroupId}/allowed-domains", produces = APPLICATION_JSON_VALUE)
    public void deleteDomain(@PathVariable UUID clientGroupId,
                             @VerifiedClientToken ClientGroupToken clientGroupToken,
                             @RequestBody DomainDTO domain) {
        clientGroupIdVerificationService.verify(clientGroupToken, clientGroupId);
        clientGroupService.deleteDomain(clientGroupId, domain);
    }
}

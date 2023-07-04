package com.yolt.clients.client.creditoraccounts;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.clienttokens.annotations.NonDeletedClient;
import nl.ing.lovebird.clienttokens.annotations.VerifiedClientToken;
import nl.ing.lovebird.clienttokens.verification.ClientIdVerificationService;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/internal/clients/{clientId}/creditor-accounts")
public class CreditorAccountController {

    private final CreditorAccountService creditorAccountService;
    private final ClientIdVerificationService clientIdVerificationService;

    @GetMapping(produces = APPLICATION_JSON_VALUE)
    public List<@Valid CreditorAccountDTO> getCreditorAccountsForClient(@PathVariable UUID clientId,
                                                                 @VerifiedClientToken ClientToken clientToken) {
        clientIdVerificationService.verify(clientToken, clientId);
        return creditorAccountService.getCreditorAccounts(clientToken.getClientIdClaim());
    }

    @NonDeletedClient
    @PostMapping(consumes = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public @Valid CreditorAccountDTO createCreditorAccountForClient(@PathVariable UUID clientId,
                                               @VerifiedClientToken ClientToken clientToken,
                                               @Valid @RequestBody CreateCreditorAccountDTO creditorAccountDTO) {
        clientIdVerificationService.verify(clientToken, clientId);
        return creditorAccountService.addCreditorAccount(clientToken, creditorAccountDTO);
    }

    @DeleteMapping(consumes = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void removeAuthorization(@PathVariable UUID clientId,
                                    @VerifiedClientToken ClientToken clientToken,
                                    @Valid @RequestBody RemoveCreditorAccountDTO removeCreditorAccountDTO) {
        clientIdVerificationService.verify(clientToken, clientId);
        creditorAccountService.removeCreditorAccount(clientToken, removeCreditorAccountDTO.getId());
    }
}

package com.yolt.clients.client.requesttokenpublickeys;

import com.yolt.clients.client.requesttokenpublickeys.dto.AddRequestTokenPublicKeyDTO;
import com.yolt.clients.client.requesttokenpublickeys.dto.RequestTokenPublicKeyDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.clienttokens.annotations.NonDeletedClient;
import nl.ing.lovebird.clienttokens.annotations.VerifiedClientToken;
import nl.ing.lovebird.clienttokens.verification.ClientIdVerificationService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/internal/clients/{clientId}/request-token-public-keys")
public class RequestTokenPublicKeyController {

    private final RequestTokenPublicKeyService requestTokenPublicKeyService;
    private final ClientIdVerificationService clientIdVerificationService;


    @GetMapping(produces = APPLICATION_JSON_VALUE)
    public List<RequestTokenPublicKeyDTO> list(@PathVariable final UUID clientId, @VerifiedClientToken ClientToken clientToken) {
        log.info("Getting list of request tokens  keys for client with id {}", clientId);
        clientIdVerificationService.verify(clientToken, clientId);
        return requestTokenPublicKeyService.getRequestTokenPublicKeys(clientId);
    }

    @GetMapping(value = "/{keyId}", produces = APPLICATION_JSON_VALUE)
    public RequestTokenPublicKeyDTO getKey(@PathVariable final UUID clientId, @PathVariable @Size(max = 100) final String keyId, @VerifiedClientToken ClientToken clientToken) {
        log.info("Getting request tokens  key with id {} for client with id {}", keyId, clientId);
        clientIdVerificationService.verify(clientToken, clientId);
        return requestTokenPublicKeyService.getRequestTokenPublicKey(clientId, keyId);
    }

    @NonDeletedClient
    @PostMapping(value = "/{keyId}", produces = APPLICATION_JSON_VALUE)
    public void saveVerificationKey(@PathVariable final UUID clientId,
                                    @PathVariable @Size(max = 100) final String keyId,
                                    @RequestBody final AddRequestTokenPublicKeyDTO addRequestTokenPublicKeyDTO,
                                    @VerifiedClientToken ClientToken clientToken) {
        log.info("About to save request token public key with keyId {} for client with id {}", keyId, clientId);
        clientIdVerificationService.verify(clientToken, clientId);
        requestTokenPublicKeyService.createRequestTokenPublicKey(clientToken, clientId, keyId, addRequestTokenPublicKeyDTO.getRequestTokenPublicKey());
    }

    @NonDeletedClient
    @PutMapping(value = "/{keyId}", produces = APPLICATION_JSON_VALUE)
    public void updateVerificationKey(@PathVariable final UUID clientId,
                                      @PathVariable @Size(max = 100) final String keyId,
                                      @RequestBody final AddRequestTokenPublicKeyDTO addRequestTokenPublicKeyDTO,
                                      @VerifiedClientToken ClientToken clientToken) {
        log.info("About to update request token public key with keyId {} for client with id {}", keyId, clientId);
        clientIdVerificationService.verify(clientToken, clientId);
        requestTokenPublicKeyService.updateRequestTokenPublicKey(clientToken, clientId, keyId, addRequestTokenPublicKeyDTO.getRequestTokenPublicKey());
    }

    @DeleteMapping(value = "/{keyId}", produces = APPLICATION_JSON_VALUE)
    public void delete(@PathVariable final UUID clientId,
                       @PathVariable @Size(max = 100) final String keyId,
                       @VerifiedClientToken ClientToken clientToken) {
        log.info("Deleting request token public key with keyId {} for client with id {}", keyId, clientId);
        clientIdVerificationService.verify(clientToken, clientId);
        requestTokenPublicKeyService.deleteRequestTokenPublicKey(clientToken, keyId);
    }
}

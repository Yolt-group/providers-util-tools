package com.yolt.clients.client.redirecturls;

import com.yolt.clients.client.redirecturls.dto.RedirectURLDTO;
import com.yolt.clients.exceptions.CannotDeleteRedirectUrlException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.clienttokens.annotations.NonDeletedClient;
import nl.ing.lovebird.clienttokens.annotations.VerifiedClientToken;
import nl.ing.lovebird.clienttokens.verification.ClientIdVerificationService;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/internal/clients/{clientId}/redirect-urls")
public class RedirectURLController {

    private final RedirectURLService redirectURLService;
    private final ClientIdVerificationService clientIdVerificationService;

    @GetMapping(produces = APPLICATION_JSON_VALUE)
    public List<RedirectURLDTO> list(@PathVariable UUID clientId) {
        return redirectURLService.findAll(clientId);
    }

    @NonDeletedClient
    @PostMapping(produces = APPLICATION_JSON_VALUE)
    public RedirectURLDTO create(@VerifiedClientToken final ClientToken clientToken,
                                 @PathVariable final UUID clientId,
                                 @Valid @RequestBody final RedirectURLDTO redirectURLDTO
    ) {
        clientIdVerificationService.verify(clientToken, clientId);

        var redirectURLId = redirectURLDTO.getRedirectURLId() != null
                ? redirectURLDTO.getRedirectURLId()
                : UUID.randomUUID();

        return redirectURLService.create(clientToken, redirectURLId, redirectURLDTO.getRedirectURL());
    }

    @GetMapping(value = "/{redirectURLId}", produces = APPLICATION_JSON_VALUE)
    public RedirectURLDTO get(@VerifiedClientToken final ClientToken clientToken,
                              @PathVariable final UUID clientId,
                              @PathVariable final UUID redirectURLId) {

        clientIdVerificationService.verify(clientToken, clientId);
        return redirectURLService.get(clientToken, redirectURLId);
    }

    @NonDeletedClient
    @PutMapping(value = "{redirectURLId}", produces = APPLICATION_JSON_VALUE)
    public RedirectURLDTO update(@VerifiedClientToken final ClientToken clientToken,
                                 @PathVariable final UUID clientId,
                                 @PathVariable final UUID redirectURLId,
                                 @Valid @RequestBody final RedirectURLDTO redirectURLDTO) {

        clientIdVerificationService.verify(clientToken, clientId);
        return redirectURLService.update(clientToken, redirectURLId, redirectURLDTO.getRedirectURL());
    }

    @NonDeletedClient
    @DeleteMapping(value = "{redirectURLId}", produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResponseEntity<RedirectURLDTO> delete(@VerifiedClientToken final ClientToken clientToken,
                                                 @PathVariable final UUID clientId,
                                                 @PathVariable final UUID redirectURLId) {
        clientIdVerificationService.verify(clientToken, clientId);
        try {
            return ResponseEntity.accepted().body(redirectURLService.delete(clientToken, redirectURLId));
        } catch (ConstraintViolationException e) {
            throw new CannotDeleteRedirectUrlException(e);
        }
    }

}

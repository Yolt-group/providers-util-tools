package com.yolt.clients.client.redirecturls;

import com.yolt.clients.client.redirecturls.dto.*;
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
@RequestMapping("/internal/clients/{clientId}/redirect-urls-changelog")
public class RedirectURLChangelogController {

    private static final String ASSISTANCE_PORTAL_YTS = "assistance-portal-yts";
    private static final String DEV_PORTAL = "dev-portal";

    private final RedirectURLChangelogService service;
    private final ClientIdVerificationService clientIdVerificationService;

    @GetMapping(produces = APPLICATION_JSON_VALUE)
    public List<RedirectURLChangelogDTO> list(@VerifiedClientToken(restrictedTo = {DEV_PORTAL, ASSISTANCE_PORTAL_YTS}) final ClientToken clientToken,
                                              @PathVariable UUID clientId) {

        clientIdVerificationService.verify(clientToken, clientId);
        return service.findAll(clientId);
    }

    @NonDeletedClient
    @PostMapping(value = "/add-request", produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public RedirectURLChangelogDTO createAddRequest(@VerifiedClientToken(restrictedTo = {DEV_PORTAL}) final ClientToken clientToken,
                                                    @PathVariable UUID clientId,
                                                    @Valid @RequestBody final NewAddRequestDTO payload) {

        clientIdVerificationService.verify(clientToken, clientId);
        return service.createAddRequest(clientToken, payload);
    }

    @NonDeletedClient
    @PostMapping(value = "/update-request", produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public RedirectURLChangelogDTO createUpdateRequest(@VerifiedClientToken(restrictedTo = {DEV_PORTAL}) final ClientToken clientToken,
                                                       @PathVariable UUID clientId,
                                                       @Valid @RequestBody final NewUpdateRequestDTO payload) {

        clientIdVerificationService.verify(clientToken, clientId);
        return service.createUpdateRequest(clientToken, payload);
    }

    @NonDeletedClient
    @PostMapping(value = "/delete-request", produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public RedirectURLChangelogDTO createDeleteRequest(@VerifiedClientToken(restrictedTo = {DEV_PORTAL}) final ClientToken clientToken,
                                                       @PathVariable UUID clientId,
                                                       @Valid @RequestBody final NewDeleteRequestDTO payload) {

        clientIdVerificationService.verify(clientToken, clientId);
        return service.createDeleteRequest(clientToken, payload);
    }

    @PostMapping(value = "/apply-request", produces = APPLICATION_JSON_VALUE)
    public List<RedirectURLChangelogDTO> markApplied(@VerifiedClientToken(restrictedTo = {ASSISTANCE_PORTAL_YTS}) final ClientToken clientToken,
                            @PathVariable UUID clientId,
                            @Valid @RequestBody final RedirectURLChangeRequestListDTO payload) {
        clientIdVerificationService.verify(clientToken, clientId);
        return service.markApplied(clientId, payload);
    }

    @PostMapping(value = "/deny-request", produces = APPLICATION_JSON_VALUE)
    public List<RedirectURLChangelogDTO> markDenied(@VerifiedClientToken(restrictedTo = {ASSISTANCE_PORTAL_YTS}) final ClientToken clientToken,
                                                     @PathVariable UUID clientId,
                                                     @Valid @RequestBody final RedirectURLChangeRequestListDTO payload) {
        clientIdVerificationService.verify(clientToken, clientId);
        return service.markDenied(clientId, payload);
    }
}

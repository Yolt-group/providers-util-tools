package com.yolt.clients.client.webhooks;

import com.yolt.clients.client.webhooks.dto.WebhookDTO;
import com.yolt.clients.client.webhooks.dto.WebhookURLDTO;
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
@RequestMapping("/internal/clients/{clientId}/webhooks")
public class WebhookController {
    private static final String SERVICE_DEV_PORTAL = "dev-portal";
    private static final String SERVICE_ASSISTANCE_PORTAL_YTS = "assistance-portal-yts";

    private final WebhookService webhookService;
    private final ClientIdVerificationService clientIdVerificationService;

    @GetMapping(produces = APPLICATION_JSON_VALUE)
    public List<WebhookDTO> list(
            @VerifiedClientToken(restrictedTo = {SERVICE_ASSISTANCE_PORTAL_YTS, SERVICE_DEV_PORTAL}) final ClientToken clientToken,
            @PathVariable final UUID clientId
    ) {
        clientIdVerificationService.verify(clientToken, clientId);
        return webhookService.findAll(clientId);
    }

    @NonDeletedClient
    @PostMapping(produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public WebhookDTO upsert(
            @VerifiedClientToken(restrictedTo = {SERVICE_ASSISTANCE_PORTAL_YTS, SERVICE_DEV_PORTAL}) final ClientToken clientToken,
            @PathVariable final UUID clientId,
            @Valid @RequestBody final WebhookDTO webhookDTO
    ) {
        clientIdVerificationService.verify(clientToken, clientId);
        return webhookService.upsert(clientToken, webhookDTO.getUrl(), webhookDTO.isEnabled());
    }

    @NonDeletedClient
    @DeleteMapping(produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @VerifiedClientToken(restrictedTo = {SERVICE_ASSISTANCE_PORTAL_YTS, SERVICE_DEV_PORTAL}) final ClientToken clientToken,
            @PathVariable final UUID clientId,
            @Valid @RequestBody final WebhookURLDTO webhookDTO
    ) {
        clientIdVerificationService.verify(clientToken, clientId);
        webhookService.delete(clientToken, webhookDTO.getUrl());
    }
}

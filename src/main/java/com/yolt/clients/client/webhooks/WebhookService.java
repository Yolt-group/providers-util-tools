package com.yolt.clients.client.webhooks;

import com.yolt.clients.client.webhooks.dto.WebhookDTO;
import com.yolt.clients.client.outboundallowlist.OutboundAllowListService;
import com.yolt.clients.client.webhooks.exceptions.OutboundHostNotAllowedException;
import com.yolt.clients.client.webhooks.exceptions.WebhookMalformedException;
import com.yolt.clients.client.webhooks.exceptions.WebhookNotFoundException;
import com.yolt.clients.client.webhooks.repository.Webhook;
import com.yolt.clients.client.webhooks.repository.WebhookRepository;
import nl.ing.lovebird.clienttokens.ClientToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
public class WebhookService {

    private final WebhookRepository webhookRepository;
    private final Optional<WebhookConfigurationEventProducer> webhookProducer;
    private final OutboundAllowListService outboundAllowListService;
    private final boolean outboundAllowListCheckEnabled;

    public WebhookService(
            WebhookRepository webhookRepository,
            Optional<WebhookConfigurationEventProducer> webhookProducer,
            OutboundAllowListService outboundAllowListService,
            @Value("${yolt.webhooks.outbound-check}") boolean outboundAllowListCheckEnabled
    ) {
        this.webhookRepository = webhookRepository;
        this.webhookProducer = webhookProducer;
        this.outboundAllowListService = outboundAllowListService;
        this.outboundAllowListCheckEnabled = outboundAllowListCheckEnabled;
    }

    public List<WebhookDTO> findAll(UUID clientId) {
        return webhookRepository
                .findAllByClientId(clientId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public boolean hasWebhooksConfigured(ClientToken clientToken) {
        return webhookRepository.existsByClientId(clientToken.getClientIdClaim());
    }

    public void delete(ClientToken clientToken, String webhookURL) {
        var webhook = webhookRepository.findByClientIdAndWebhookURL(clientToken.getClientIdClaim(), webhookURL)
                .orElseThrow(() -> new WebhookNotFoundException(clientToken.getClientIdClaim(), webhookURL));
        webhookRepository.delete(webhook);
        var webhookDTO = mapToDTO(webhook);
        webhookProducer.ifPresent(producer -> producer.sendMessage(clientToken, webhookDTO, WebhookMessageType.WEBHOOK_DELETED));
    }

    public WebhookDTO upsert(ClientToken clientToken, String webhookURL, boolean isEnabled) {
        final var newWebHook = new AtomicBoolean(false);

        var webhook = webhookRepository.findByClientIdAndWebhookURL(clientToken.getClientIdClaim(), webhookURL)
                .orElseGet(() -> {
                    newWebHook.set(true);
                    validateHostWhitelisted(clientToken, webhookURL);
                    return new Webhook(clientToken.getClientIdClaim(), webhookURL, isEnabled);
                });
        webhook.setEnabled(isEnabled);
        var webhookDTO = mapToDTO(webhookRepository.save(webhook));
        webhookProducer.ifPresent(producer -> producer.sendMessage(
                clientToken,
                webhookDTO,
                newWebHook.get() ? WebhookMessageType.WEBHOOK_CREATED : WebhookMessageType.WEBHOOK_UPDATED
        ));
        return webhookDTO;
    }

    private void validateHostWhitelisted(ClientToken clientToken, String webhookURL) {
        var host = getUri(clientToken, webhookURL).getHost();
        if (outboundAllowListCheckEnabled && !outboundAllowListService.hasAllowedOutboundHost(clientToken.getClientIdClaim(), host)) {
            throw new OutboundHostNotAllowedException(clientToken.getClientIdClaim(), webhookURL, host);
        }
    }

    private URI getUri(ClientToken clientToken, String webhookURL) {
        try {
            return new URI(webhookURL);
        } catch (URISyntaxException e) {
            throw new WebhookMalformedException(clientToken.getClientIdClaim(), webhookURL, e);
        }
    }

    private WebhookDTO mapToDTO(Webhook webhook) {
        return new WebhookDTO(webhook.getWebhookURL(), webhook.isEnabled());
    }
}

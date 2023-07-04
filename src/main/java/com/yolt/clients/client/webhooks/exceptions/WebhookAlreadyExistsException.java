package com.yolt.clients.client.webhooks.exceptions;

import java.util.UUID;

public class WebhookAlreadyExistsException extends RuntimeException {
    public WebhookAlreadyExistsException(UUID clientId, String webhookURL) {
        super(String.format("Webhook with URL %s for client %s already exists in our system.", webhookURL, clientId));
    }
}

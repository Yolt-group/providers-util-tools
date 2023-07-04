package com.yolt.clients.client.webhooks.exceptions;

import java.util.UUID;

public class WebhookNotFoundException extends RuntimeException {
    public WebhookNotFoundException(UUID clientId, String webhookURL) {
        super(String.format("webhook %s not found for client %s", webhookURL, clientId));
    }
}

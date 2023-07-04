package com.yolt.clients.client.webhooks.exceptions;

import java.util.UUID;

public class WebhookMalformedException extends RuntimeException {
    public WebhookMalformedException(UUID clientId, String webhookURL, Throwable cause) {
        super(String.format("Webhook with URL %s for client %s could not be parsed as URI.", webhookURL, clientId), cause);
    }
}

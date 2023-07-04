package com.yolt.clients.client.webhooks.exceptions;

import java.util.UUID;

public class OutboundHostNotAllowedException extends RuntimeException {
    public OutboundHostNotAllowedException(UUID clientId, String webhookURL, String host) {
        super(String.format("Webhook host %s with URL %s for client %s is not in the allowlist.", host, webhookURL, clientId));
    }
}

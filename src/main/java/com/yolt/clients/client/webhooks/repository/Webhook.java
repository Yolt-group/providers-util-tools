package com.yolt.clients.client.webhooks.repository;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "webhooks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@IdClass(Webhook.WebhookId.class)
public class Webhook {

    @Id
    @Column(name = "client_id")
    private UUID clientId;

    @Id
    @Column(name = "webhook_url")
    private String webhookURL;

    @Column(name = "enabled")
    private boolean enabled;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class WebhookId implements Serializable {
        UUID clientId;
        String webhookURL;
    }
}

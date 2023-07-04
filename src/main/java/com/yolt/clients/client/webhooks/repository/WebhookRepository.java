package com.yolt.clients.client.webhooks.repository;

import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WebhookRepository extends CrudRepository<Webhook, UUID> {

    boolean existsByClientIdAndWebhookURL(UUID clientId, String webhookURL);

    boolean existsByClientId(UUID clientId);

    List<Webhook> findAllByClientId(UUID clientId);

    Optional<Webhook> findByClientIdAndWebhookURL(UUID clientId, String webhookURL);
}

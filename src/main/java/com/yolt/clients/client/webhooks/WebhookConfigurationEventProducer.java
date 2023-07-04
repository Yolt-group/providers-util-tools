package com.yolt.clients.client.webhooks;

import com.yolt.clients.client.webhooks.dto.WebhookDTO;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.clienttokens.ClientToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;

import static nl.ing.lovebird.clienttokens.constants.ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "yolt.kafka.topics.client-webhooks-configuration", name = "topic-name")
public class WebhookConfigurationEventProducer {
    private final KafkaTemplate<String, WebhookMessageDTO> webhooksKafkaTemplate;
    private final String topic;

    public WebhookConfigurationEventProducer(KafkaTemplate<String, WebhookMessageDTO> webhooksKafkaTemplate,
                                             @Value("${yolt.kafka.topics.client-webhooks-configuration.topic-name}") String topic) {
        this.webhooksKafkaTemplate = webhooksKafkaTemplate;
        this.topic = topic;
    }

    public void sendMessage(final ClientToken clientToken, final WebhookDTO webhook, final WebhookMessageType messageType) {
        Message<WebhookMessageDTO> message = MessageBuilder
                .withPayload(new WebhookMessageDTO(clientToken, webhook))
                .setHeader(CLIENT_TOKEN_HEADER_NAME, clientToken.getSerialized())
                .setHeader(KafkaHeaders.TOPIC, topic)
                .setHeader(KafkaHeaders.MESSAGE_KEY, clientToken.getClientIdClaim().toString())
                .setHeader("message_type", messageType.name())
                .build();

        ListenableFuture<SendResult<String, WebhookMessageDTO>> future = webhooksKafkaTemplate.send(message);
        future.addCallback(
                result -> log.debug("Sent webhooks update."),
                ex -> log.error("Failed to send webhooks update.", ex)
        );

    }

    @lombok.Value
    @AllArgsConstructor
    static class WebhookMessageDTO {
        String clientId;
        String url;
        boolean enabled;

        public WebhookMessageDTO(ClientToken clientToken, WebhookDTO webhook) {
            this.clientId = clientToken.getClientIdClaim().toString();
            this.url = webhook.getUrl();
            this.enabled = webhook.isEnabled();
        }
    }
}

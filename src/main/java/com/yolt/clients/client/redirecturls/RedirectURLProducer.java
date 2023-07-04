package com.yolt.clients.client.redirecturls;

import com.yolt.clients.client.redirecturls.dto.RedirectURLDTO;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.clienttokens.ClientToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;

import java.util.UUID;

import static nl.ing.lovebird.clienttokens.constants.ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME;

@Slf4j
@Component
public class RedirectURLProducer {
    static final String REDIRECT_URLS_KAFKA_KEY = "ClientRedirectUrlsKafkaKey";

    private final KafkaTemplate<String, RedirectURLMessageDTO> redirectURLsKafkaTemplate;
    private final String topic;

    public RedirectURLProducer(KafkaTemplate<String, RedirectURLMessageDTO> redirectURLsKafkaTemplate,
                               @Value("${yolt.kafka.topics.clientRedirectUrls.topic-name}") String topic) {
        this.redirectURLsKafkaTemplate = redirectURLsKafkaTemplate;
        this.topic = topic;
    }

    public void sendMessage(final ClientToken clientToken, final RedirectURLDTO redirectURL, RedirectURLMessageType messageType) {
        // The key can be a static string since we only have 1 partition. The load on this topic is not significant.
        Message<RedirectURLMessageDTO> message = MessageBuilder
                .withPayload(new RedirectURLMessageDTO(clientToken, redirectURL))
                .setHeader(CLIENT_TOKEN_HEADER_NAME, clientToken.getSerialized())
                .setHeader(KafkaHeaders.TOPIC, topic)
                .setHeader(KafkaHeaders.MESSAGE_KEY, REDIRECT_URLS_KAFKA_KEY)
                .setHeader("message_type", messageType.name())
                .build();

        ListenableFuture<SendResult<String, RedirectURLMessageDTO>> future = redirectURLsKafkaTemplate.send(message);
        future.addCallback(
                result -> log.debug("Sent client redirect url update."),
                ex -> log.error("Failed to send client redirect url update.", ex)
        );

    }

    @Getter
    private class RedirectURLMessageDTO {
        private UUID clientId;
        private UUID redirectUrlId;
        private String url;
        public RedirectURLMessageDTO(ClientToken clientToken, RedirectURLDTO redirectURL) {
            this.clientId = clientToken.getClientIdClaim();
            this.redirectUrlId = redirectURL.getRedirectURLId();
            this.url = redirectURL.getRedirectURL();
        }
    }
}

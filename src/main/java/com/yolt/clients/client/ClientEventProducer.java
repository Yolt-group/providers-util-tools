package com.yolt.clients.client;

import com.yolt.clients.events.ClientEvent;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.clienttokens.AbstractClientToken;
import nl.ing.lovebird.clienttokens.constants.ClientTokenConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ClientEventProducer {

    private final KafkaTemplate<String, ClientEvent> kafkaTemplate;
    private final String topic;

    public ClientEventProducer(KafkaTemplate<String, ClientEvent> kafkaTemplate,
                               @Value("${yolt.kafka.topics.ycs-client-events.topic-name}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void sendMessage(ClientEvent payload) {
        sendMessage(null, payload);
    }

    public void sendMessage(AbstractClientToken clientToken, ClientEvent payload) {
        MessageBuilder<ClientEvent> message = MessageBuilder
                .withPayload(payload)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .setHeader(KafkaHeaders.MESSAGE_KEY, payload.getClientId().toString());
        if (clientToken != null) {
            message.setHeader(ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME, clientToken.getSerialized());
        }
        kafkaTemplate.send(message.build());
    }
}

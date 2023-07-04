package com.yolt.clients.client;

import com.yolt.clients.events.ClientDeletedEvent;
import nl.ing.lovebird.clienttokens.AbstractClientToken;
import nl.ing.lovebird.clienttokens.constants.ClientTokenConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Service
public class ClientDeletedEventProducer {

    private final KafkaTemplate<String, ClientDeletedEvent> kafkaTemplate;
    private final String topic;

    public ClientDeletedEventProducer(KafkaTemplate<String, ClientDeletedEvent> kafkaTemplate,
                                      @Value("${yolt.kafka.topics.ycs-client-deleted-events.topic-name}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void sendMessage(AbstractClientToken clientToken, ClientDeletedEvent payload) {
        Message<ClientDeletedEvent> message = MessageBuilder
                .withPayload(payload)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .setHeader(ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME, clientToken.getSerialized())
                .setHeader(KafkaHeaders.MESSAGE_KEY, payload.getClientId().toString())
                .build();
        kafkaTemplate.send(message);
    }
}

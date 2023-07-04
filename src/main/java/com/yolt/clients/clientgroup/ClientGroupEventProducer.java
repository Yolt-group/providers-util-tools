package com.yolt.clients.clientgroup;

import com.yolt.clients.events.ClientGroupEvent;
import nl.ing.lovebird.clienttokens.ClientGroupToken;
import nl.ing.lovebird.clienttokens.constants.ClientTokenConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Service
public class ClientGroupEventProducer {

    private final KafkaTemplate<String, ClientGroupEvent> kafkaTemplate;
    private final String topic;

    public ClientGroupEventProducer(KafkaTemplate<String, ClientGroupEvent> kafkaTemplate,
                                    @Value("${yolt.kafka.topics.ycs-client-group-events.topic-name}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void sendMessage(ClientGroupToken clientGroupToken, ClientGroupEvent payload) {
        Message<ClientGroupEvent> message = MessageBuilder
                .withPayload(payload)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .setHeader(ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME, clientGroupToken.getSerialized())
                .setHeader(KafkaHeaders.MESSAGE_KEY, payload.getClientGroupId().toString())
                .build();
        kafkaTemplate.send(message);
    }

    public void sendMessage(ClientGroupEvent payload) {
        Message<ClientGroupEvent> message = MessageBuilder
                .withPayload(payload)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .setHeader(KafkaHeaders.MESSAGE_KEY, payload.getClientGroupId().toString())
                .build();
        kafkaTemplate.send(message);
    }
}

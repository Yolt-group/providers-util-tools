package com.yolt.clients.client.requesttokenpublickeys;

import com.yolt.clients.client.requesttokenpublickeys.dto.RequestTokenPublicKeyDTO;
import com.yolt.clients.client.requesttokenpublickeys.events.RequestTokenPublicKeyEvent;
import com.yolt.clients.events.ClientEvent;
import nl.ing.lovebird.clienttokens.AbstractClientToken;
import nl.ing.lovebird.clienttokens.constants.ClientTokenConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Service
public class RequestTokenPublicKeyProducer {

    private final KafkaTemplate<String, RequestTokenPublicKeyEvent> kafkaTemplate;
    private final String topic;

    public RequestTokenPublicKeyProducer(KafkaTemplate<String, RequestTokenPublicKeyEvent> kafkaTemplate,
                               @Value("${yolt.kafka.topics.ycs-request-token-public-key.topic-name}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void sendMessage(AbstractClientToken clientToken, RequestTokenPublicKeyEvent payload) {
        Message<RequestTokenPublicKeyEvent> message = MessageBuilder
                .withPayload(payload)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .setHeader(ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME, clientToken.getSerialized())
                .setHeader(KafkaHeaders.MESSAGE_KEY, payload.getClientId().toString())
                .build();
        kafkaTemplate.send(message);
    }
}

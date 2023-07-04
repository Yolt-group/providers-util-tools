package com.yolt.clients.clientsite;

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
public class ClientSitesUpdateProducer {

    private final KafkaTemplate<String, ClientEvent> kafkaTemplate;
    private final String topic;

    public ClientSitesUpdateProducer(KafkaTemplate<String, ClientEvent> kafkaTemplate,
                                     @Value("${yolt.kafka.topics.client-sites-updates.topic-name}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void sendMessage() {
        Message<String> message = MessageBuilder
                .withPayload("{}")
                .setHeader(KafkaHeaders.TOPIC, topic)
                .setHeader(KafkaHeaders.MESSAGE_KEY, "1")
                .build();
        kafkaTemplate.send(message);
    }
}

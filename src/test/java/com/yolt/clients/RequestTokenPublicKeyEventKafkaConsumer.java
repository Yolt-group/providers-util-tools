package com.yolt.clients;

import com.yolt.clients.client.requesttokenpublickeys.events.RequestTokenPublicKeyEvent;
import lombok.Getter;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.clienttokens.ClientToken;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.concurrent.ConcurrentLinkedQueue;

import static nl.ing.lovebird.clienttokens.constants.ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME;

@Slf4j
@Getter
@Component
public class RequestTokenPublicKeyEventKafkaConsumer {
    private final ConcurrentLinkedQueue<Record> eventQueue = new ConcurrentLinkedQueue<>();


    @KafkaListener(topics = "${yolt.kafka.topics.ycs-request-token-public-key.topic-name}")
    public void receive(@Payload @Validated RequestTokenPublicKeyEvent event,
                        @Header(value = CLIENT_TOKEN_HEADER_NAME) final ClientToken clientToken) {
        log.info("received payload='{}'", event.toString());
        eventQueue.add(new Record(event, clientToken));
    }

    @Value
    public static class Record {
        RequestTokenPublicKeyEvent event;
        ClientToken clientToken;
    }
}

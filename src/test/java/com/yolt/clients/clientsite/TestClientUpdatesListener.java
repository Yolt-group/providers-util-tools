package com.yolt.clients.clientsite;

import lombok.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TestClientUpdatesListener {
    private final List<Consumed> consumed = new ArrayList<>();

    @KafkaListener(
            topics = "${yolt.kafka.topics.client-sites-updates.topic-name}",
            concurrency = "${yolt.kafka.topics.client-sites-updates.listener-concurrency}",
            groupId = "test-consumer"
    )
    public void consume(
            @Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String key,
            @Payload String payload
    ) {
        consumed.add(new Consumed(key, payload));
    }

    public List<Consumed> getConsumed() {
        return consumed;
    }

    public void reset() {
        consumed.clear();
    }

    @Value
    public static class Consumed {
        String key;
        String payload;
    }
}
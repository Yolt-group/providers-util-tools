package com.yolt.clients.requesttokenpublickey;

import com.yolt.clients.TestConfiguration;
import com.yolt.clients.client.requesttokenpublickeys.RequestTokenPublicKeyProducer;
import com.yolt.clients.client.requesttokenpublickeys.events.RequestTokenPublicKeyEvent;
import nl.ing.lovebird.clienttokens.ClientToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.Message;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RequestTokenPublicKeyEventProducerTest {

    private static final String TOPIC = "kafka_topic";
    private static final String SERIALIZED_CLIENT_TOKEN = "serialized request token public key client token";

    private RequestTokenPublicKeyProducer producer;

    @Mock
    private KafkaTemplate<String, RequestTokenPublicKeyEvent> kafkaTemplate;
    private RequestTokenPublicKeyEvent payload;
    private UUID clientId;
    @Mock
    private ClientToken clientToken;
    @Captor
    private ArgumentCaptor<Message<RequestTokenPublicKeyEvent>> messageCaptor;

    @BeforeEach
    void setUp() {
        producer = new RequestTokenPublicKeyProducer(kafkaTemplate, TOPIC);
        clientId = UUID.randomUUID();
        LocalDateTime created = LocalDateTime.now(TestConfiguration.FIXED_CLOCK);
        payload = new RequestTokenPublicKeyEvent(RequestTokenPublicKeyEvent.Action.ADD, clientId, "kid", "test key", created);
    }

    @Test
    void testSendMessage() {
        when(clientToken.getSerialized()).thenReturn(SERIALIZED_CLIENT_TOKEN);

        producer.sendMessage(clientToken, payload);

        verify(kafkaTemplate).send(messageCaptor.capture());

        Message<RequestTokenPublicKeyEvent> message = messageCaptor.getValue();
        assertThat(message.getPayload()).isEqualTo(payload);
        assertThat(message.getHeaders()).contains(
                entry("kafka_topic", TOPIC),
                entry("client-token", SERIALIZED_CLIENT_TOKEN),
                entry("kafka_messageKey", clientId.toString())
        );
    }
}

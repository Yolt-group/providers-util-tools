package com.yolt.clients.client;

import com.yolt.clients.events.ClientEvent;
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

import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientEventProducerTest {
    private static final String TOPIC = "kafka_topic";
    private static final String SERIALIZED_CLIENT_TOKEN = "serialized client token";

    private ClientEventProducer producer;

    @Mock
    private KafkaTemplate<String, ClientEvent> kafkaTemplate;
    private ClientEvent payload;
    private UUID clientGroupId;
    private UUID clientId;
    @Mock
    private ClientToken clientToken;
    @Captor
    private ArgumentCaptor<Message<ClientEvent>> messageCaptor;


    @BeforeEach
    void setUp() {
        producer = new ClientEventProducer(kafkaTemplate, TOPIC);
        clientGroupId = UUID.randomUUID();
        clientId = UUID.randomUUID();
        payload = new ClientEvent(
                ClientEvent.Action.ADD,
                clientId,
                clientGroupId,
                "name",
                "NL",
                10,
                true,
                true,
                "12.1",
                new ClientEvent.ClientUsersKyc(true, true),
                new ClientEvent.DataEnrichment(true, true, true, true),
                true,
                true,
                true,
                true,
                true,
                Collections.emptyList()
        );
    }

    @Test
    void testSendMessage() {
        when(clientToken.getSerialized()).thenReturn(SERIALIZED_CLIENT_TOKEN);

        producer.sendMessage(clientToken, payload);

        verify(kafkaTemplate).send(messageCaptor.capture());

        Message<ClientEvent> message = messageCaptor.getValue();
        assertThat(message.getPayload()).isEqualTo(payload);
        assertThat(message.getHeaders()).contains(
                entry("kafka_topic", TOPIC),
                entry("client-token", SERIALIZED_CLIENT_TOKEN),
                entry("kafka_messageKey", clientId.toString())
        );
    }
}

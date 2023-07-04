package com.yolt.clients.clientgroup;

import com.yolt.clients.events.ClientGroupEvent;
import nl.ing.lovebird.clienttokens.ClientGroupToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.Message;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientGroupEventProducerTest {
    private static final String TOPIC = "kafka_topic";
    private static final String SERIALIZED_CLIENT_GROUP_TOKEN = "serialized client token";

    private ClientGroupEventProducer producer;

    @Mock
    private KafkaTemplate<String, ClientGroupEvent> kafkaTemplate;
    private ClientGroupEvent payload;
    private UUID clientGroupId;
    @Mock
    private ClientGroupToken clientGroupToken;
    @Captor
    private ArgumentCaptor<Message<ClientGroupEvent>> messageCaptor;

    @BeforeEach
    void setUp() {
        producer = new ClientGroupEventProducer(kafkaTemplate, TOPIC);
        clientGroupId = UUID.randomUUID();
        payload = new ClientGroupEvent(ClientGroupEvent.Action.UPDATE, clientGroupId, "name");
    }

    @Test
    void testSendMessage() {
        producer.sendMessage(payload);

        verify(kafkaTemplate).send(messageCaptor.capture());

        Message<ClientGroupEvent> message = messageCaptor.getValue();
        assertThat(message.getPayload()).isEqualTo(payload);
        assertThat(message.getHeaders()).contains(
                entry("kafka_topic", TOPIC),
                entry("kafka_messageKey", clientGroupId.toString())
        );
    }

    @Test
    void testSendMessageWithToken() {
        when(clientGroupToken.getSerialized()).thenReturn(SERIALIZED_CLIENT_GROUP_TOKEN);

        producer.sendMessage(clientGroupToken, payload);

        verify(kafkaTemplate).send(messageCaptor.capture());

        Message<ClientGroupEvent> message = messageCaptor.getValue();
        assertThat(message.getPayload()).isEqualTo(payload);
        assertThat(message.getHeaders()).contains(
                entry("kafka_topic", TOPIC),
                entry("client-token", SERIALIZED_CLIENT_GROUP_TOKEN),
                entry("kafka_messageKey", clientGroupId.toString())
        );
    }
}
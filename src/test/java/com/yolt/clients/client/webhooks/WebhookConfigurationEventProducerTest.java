package com.yolt.clients.client.webhooks;

import com.yolt.clients.client.webhooks.dto.WebhookDTO;
import nl.ing.lovebird.clienttokens.ClientToken;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;
import org.springframework.util.concurrent.ListenableFuture;

import java.util.UUID;

import static nl.ing.lovebird.clienttokens.constants.ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebhookConfigurationEventProducerTest {
    private WebhookConfigurationEventProducer webhookConfigurationEventProducer;

    @Mock
    private KafkaTemplate<String, WebhookConfigurationEventProducer.WebhookMessageDTO> webhooksKafkaTemplate;

    @Captor
    private ArgumentCaptor<Message<WebhookConfigurationEventProducer.WebhookMessageDTO>> messageArgumentCaptor;

    @Mock
    private ListenableFuture<SendResult<String, WebhookConfigurationEventProducer.WebhookMessageDTO>> future;

    private String topic;


    @BeforeEach
    void setUp() {
        topic = "kafka-topic";
        webhookConfigurationEventProducer = new WebhookConfigurationEventProducer(webhooksKafkaTemplate, topic);
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void sendMessage() {
        ClientToken clientToken = mock(ClientToken.class);
        UUID clientId = UUID.randomUUID();
        String serializedClientToken = "serialized client token";
        when(clientToken.getClientIdClaim()).thenReturn(clientId);
        when(clientToken.getSerialized()).thenReturn(serializedClientToken);
        WebhookDTO webhook = new WebhookDTO("https://dummy/endpoint", true);
        WebhookMessageType messageType = WebhookMessageType.WEBHOOK_CREATED;

        when(webhooksKafkaTemplate.send(messageArgumentCaptor.capture())).thenReturn(future);

        webhookConfigurationEventProducer.sendMessage(clientToken, webhook, messageType);

        Message<WebhookConfigurationEventProducer.WebhookMessageDTO> message = messageArgumentCaptor.getValue();
        assertThat(message.getHeaders())
                .containsEntry(CLIENT_TOKEN_HEADER_NAME, serializedClientToken)
                .containsEntry(KafkaHeaders.TOPIC, topic)
                .containsEntry(KafkaHeaders.MESSAGE_KEY, clientId.toString())
                .containsEntry("message_type", messageType.name());

        assertThat(message.getPayload())
                .isEqualTo(new WebhookConfigurationEventProducer.WebhookMessageDTO(clientId.toString(), "https://dummy/endpoint", true));

    }
}
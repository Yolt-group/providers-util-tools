package com.yolt.clients;

import com.yolt.clients.client.ClientEventProducer;
import com.yolt.clients.client.webhooks.WebhookConfigurationEventProducer;
import com.yolt.clients.clientgroup.ClientGroupEventProducer;
import nl.ing.lovebird.clienttokens.requester.service.ClientTokenRequesterService;
import nl.ing.lovebird.kafka.test.EnableExternalKafkaTestCluster;
import nl.ing.lovebird.postgres.test.EnableExternalPostgresTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.ses.SesClient;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(classes = {
        ClientsApplication.class,
        TestConfiguration.class,
        RequestTokenPublicKeyEventKafkaConsumer.class,
}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("test")
@EnableExternalPostgresTestDatabase
@EnableExternalKafkaTestCluster
@MockBean({
        ClientTokenRequesterService.class,
        ClientEventProducer.class,
        ClientGroupEventProducer.class,
        WebhookConfigurationEventProducer.class,
        SesClient.class,
        S3Client.class
})
public @interface IntegrationTest {
}


package com.yolt.clients.clientsite;

import brave.Tracer;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.yolt.clients.IntegrationTest;
import com.yolt.clients.authmeans.ClientOnboardingsTestUtility;
import com.yolt.clients.client.ClientsRepository;
import com.yolt.clients.client.redirecturls.repository.RedirectURL;
import com.yolt.clients.client.redirecturls.repository.RedirectURLRepository;
import com.yolt.clients.clientgroup.ClientGroupRepository;
import com.yolt.clients.model.Client;
import com.yolt.clients.model.ClientGroup;
import com.yolt.clients.sites.SitesProvider;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.clienttokens.test.TestClientTokens;
import nl.ing.lovebird.providerdomain.ServiceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.util.Collections;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.http.HttpStatus.OK;

@Slf4j
@IntegrationTest
class ClientSitesUpdateIntegrationTest {

    @Autowired
    TestClientUpdatesListener testClientUpdatesListener;

    @org.springframework.beans.factory.annotation.Value("${yolt.kafka.topics.clientAuthenticationMeans.topic-name}")
    private String authMeansTopic;

    @Autowired
    private KafkaTemplate<String, ClientAuthenticationMeansDTO> authenticationMeansDTOKafkaTemplate;

    @Autowired
    private WireMockServer wireMockServer;

    @Autowired
    private SitesProvider sitesProvider;

    @Autowired
    private ClientOnboardingsTestUtility clientOnboardingsTestUtility;

    @Autowired
    private TestClientTokens testClientTokens;

    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private ClientGroupRepository clientGroupRepository;
    @Autowired
    private ClientsRepository clientRepository;
    @Autowired
    private RedirectURLRepository redirectURLRepository;

    @Autowired
    Tracer tracer;


    Client client = new Client(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "client name",
            "NL",
            false,
            true,
            "10.71",
            null,
            false,
            true,
            false,
            true,
            false,
            false,
            true,
            true,
            false,
            true,
            true,
            true,
            1L,
            Collections.emptySet()
    );

    @BeforeEach
    void beforeEach() {
        testClientUpdatesListener.reset();
    }

    @ParameterizedTest
    @CsvSource(value = {"CLIENT_AUTHENTICATION_MEANS_UPDATED,CLIENT_AUTHENTICATION_MEANS_DELETED",
            "CLIENT_GROUP_AUTHENTICATION_MEANS_UPDATED,CLIENT_GROUP_AUTHENTICATION_MEANS_DELETED"})
    void given_aDeletedOrUpdatedOnboarding_then_theClientSitesUpdatesProducerShouldHaveSignalledConsumers(String createdType, String deletedType) {
        UUID redirectUrlId = UUID.randomUUID();
        clientGroupRepository.save(new ClientGroup(client.getClientGroupId(), "client-group-name"));
        clientRepository.save(client);
        redirectURLRepository.save(new RedirectURL(client.getClientId(), redirectUrlId, "https://example.com"));

        Message<ClientAuthenticationMeansDTO> create = MessageBuilder
                .withPayload(new ClientAuthenticationMeansDTO(client.getClientId(), redirectUrlId, client.getClientGroupId(), "provider", ServiceType.AIS))
                .setHeader(KafkaHeaders.TOPIC, authMeansTopic)
                .setHeader(KafkaHeaders.MESSAGE_KEY, client.getClientId().toString())
                .setHeader("payload-type", createdType)
                .build();

        Message<ClientAuthenticationMeansDTO> delete = MessageBuilder
                .withPayload(new ClientAuthenticationMeansDTO(client.getClientId(), redirectUrlId, client.getClientGroupId(), "provider", ServiceType.AIS))
                .setHeader(KafkaHeaders.TOPIC, authMeansTopic)
                .setHeader(KafkaHeaders.MESSAGE_KEY, client.getClientId().toString())
                .setHeader("payload-type", deletedType)
                .build();

        authenticationMeansDTOKafkaTemplate.send(create);
        authenticationMeansDTOKafkaTemplate.send(delete);

        await().untilAsserted(() -> assertThat(testClientUpdatesListener.getConsumed()).hasSize(2));

    }

    @Test
    void given_AChangeInSitesListFromProviders_then_theClientSitesUpdatesProducerShouldHaveSignalledConsumers() {
        wireMockServer.stubFor(WireMock.get(urlMatching("/providers/sites-details"))
                .willReturn(aResponse()
                        .withBodyFile("providers/sites-details-from-providers-2022-03-29.json")
                        .withHeader("content-type", "application/json")
                        .withStatus(OK.value())
                ));
        sitesProvider.update();

        wireMockServer.stubFor(WireMock.get(urlMatching("/providers/sites-details"))
                .willReturn(aResponse()
                        .withBodyFile("providers/sites-details-single-site.json")
                        .withHeader("content-type", "application/json")
                        .withStatus(OK.value())
                ));
        sitesProvider.update();
        await().untilAsserted(() -> assertThat(testClientUpdatesListener.getConsumed()).isNotEmpty());

    }

    @Test
    void given_AnyChangeOnAClientSiteMetaData_then_theClientSitesUpdatesProducerShouldHaveSignalledConsumers() {
        UUID monzoSiteId = UUID.fromString("82c16668-4d59-4be8-be91-1d52792f48e3");
        UUID redirectUrlId = UUID.randomUUID();

        ClientToken clientToken = testClientTokens.createClientToken(client.getClientGroupId(), client.getClientId());

        // make sure the provider is enabled first
        clientGroupRepository.save(new ClientGroup(client.getClientGroupId(), "client-group-name"));
        clientRepository.save(client);
        redirectURLRepository.save(new RedirectURL(client.getClientId(), redirectUrlId, "https://example.com"));
        clientOnboardingsTestUtility.addOnboardedAISProviderForClient(client.getClientId(), redirectUrlId, "MONZO");

        wireMockServer.stubFor(WireMock.get(urlMatching("/providers/sites-details"))
                .willReturn(aResponse()
                        .withBodyFile("providers/sites-details-from-providers-2022-03-29.json")
                        .withHeader("content-type", "application/json")
                        .withStatus(OK.value())
                ));
        sitesProvider.update();

        // Mark site available
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add("client-token", clientToken.getSerialized());
        ResponseEntity<Void> response = restTemplate.postForEntity(
                "/client-sites/{siteId}/available/{available}",
                new HttpEntity<>(requestHeaders),
                Void.class, monzoSiteId, true);
        assertThat(response.getStatusCode().value()).isEqualTo(200);

        await().untilAsserted(() -> assertThat(testClientUpdatesListener.getConsumed()).isNotEmpty());

    }

    @Value
    static class ClientAuthenticationMeansDTO {
        UUID clientId;
        UUID redirectUrlId;
        UUID clientGroupId;
        String provider;
        ServiceType serviceType;
    }
}

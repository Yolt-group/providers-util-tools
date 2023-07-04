package com.yolt.clients.authmeans;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.yolt.clients.IntegrationTest;
import com.yolt.clients.client.ClientsRepository;
import com.yolt.clients.client.redirecturls.repository.RedirectURL;
import com.yolt.clients.client.redirecturls.repository.RedirectURLRepository;
import com.yolt.clients.clientgroup.ClientGroupRepository;
import com.yolt.clients.model.Client;
import com.yolt.clients.model.ClientGroup;
import lombok.SneakyThrows;
import nl.ing.lovebird.providerdomain.ServiceType;
import org.assertj.core.api.AssertionsForClassTypes;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@IntegrationTest
class SynchronizeWithProvidersServiceTest {

    // For setup
    @Autowired
    ClientGroupRepository clientGroupRepository;
    @Autowired
    ClientsRepository clientRepository;
    @Autowired
    RedirectURLRepository redirectUrlRepository;
    @Autowired
    WireMockServer wireMockServer;
    @Autowired
    Jackson2ObjectMapperBuilder objectMapperBuilder;

    // Under test
    @Autowired
    SynchronizeWithProvidersService synchronizeWithProvidersService;

    // For checks
    @Autowired
    ClientGroupOnboardedProviderRepository clientGroupOnboardedProviderRepository;
    @Autowired
    ClientOnboardedProviderRepository clientOnboardedProviderRepository;
    @Autowired
    ClientOnboardedScrapingProviderRepository clientOnboardedScrapingProviderRepository;

    protected Appender<ILoggingEvent> mockAppender;
    protected ArgumentCaptor<ILoggingEvent> captorLoggingEvent;

    @BeforeEach
    public void setup() {
        this.mockAppender = (Appender) Mockito.mock(Appender.class);
        this.captorLoggingEvent = ArgumentCaptor.forClass(ILoggingEvent.class);
        Logger logger = (Logger) LoggerFactory.getLogger("ROOT");
        logger.addAppender(this.mockAppender);
    }

    @Test
    @SneakyThrows
    void testSynchronizeWithProvidersService() {
        // setup
        // for ClientGroupOnboardedProvider
        var clientGroupId1 = UUID.randomUUID();
        clientGroupRepository.save(new ClientGroup(clientGroupId1, "garbage-" + clientGroupId1));
        // for ClientOnboardedProvider
        var clientGroupId2 = UUID.randomUUID();
        clientGroupRepository.save(new ClientGroup(clientGroupId2, "garbage-" + clientGroupId2));
        var clientId2 = UUID.randomUUID();
        clientRepository.save(makeClient(clientGroupId2, clientId2));
        final UUID redirectUrlId2 = UUID.randomUUID();
        redirectUrlRepository.save(new RedirectURL(clientId2, redirectUrlId2, "https://example.com"));
        // for ClientOnboardedScrapingProvider
        var clientGroupId3 = UUID.randomUUID();
        clientGroupRepository.save(new ClientGroup(clientGroupId3, "garbage-" + clientGroupId3));
        var clientId3 = UUID.randomUUID();
        clientRepository.save(makeClient(clientGroupId3, clientId3));
        var responseJson = objectMapperBuilder.build().writeValueAsString(List.of(
                // for ClientGroupOnboardedProvider
                new ProvidersClient.OnboardedProvider(clientGroupId1, null, "test1", ServiceType.AIS, null),
                // for ClientOnboardedProvider
                new ProvidersClient.OnboardedProvider(null, clientId2, "test2", ServiceType.AIS, redirectUrlId2),
                // for ClientOnboardedScrapingProvider
                new ProvidersClient.OnboardedProvider(null, clientId3, "test3", ServiceType.AIS, null)
        ));
        wireMockServer.stubFor(
                WireMock.get("/providers/all-onboarded-providers")
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody(responseJson))
        );
        // Add a few garbage rows to the db that will be cleared out by the method under test.
        clientGroupOnboardedProviderRepository.save(ClientGroupOnboardedProvider.builder()
                .clientGroupOnboardedProviderId(ClientGroupOnboardedProvider.ClientGroupOnboardedProviderId.builder()
                        .clientGroupId(clientGroupId1)
                        .serviceType(com.yolt.clients.authmeans.ServiceType.PIS)
                        .provider("garbage1")
                        .build())
                .build());
        clientOnboardedProviderRepository.save(ClientOnboardedProvider.builder()
                .clientOnboardedProviderId(ClientOnboardedProvider.ClientOnboardedProviderId.builder()
                        .clientId(clientId2)
                        .redirectUrlId(redirectUrlId2)
                        .provider("garbage2")
                        .serviceType(com.yolt.clients.authmeans.ServiceType.PIS)
                        .build())
                .build());
        clientOnboardedScrapingProviderRepository.save(ClientOnboardedScrapingProvider.builder()
                .clientOnboardedScrapingProviderId(ClientOnboardedScrapingProvider.ClientOnboardedScrapingProviderId.builder()
                        .clientId(clientId3)
                        .provider("garbage3")
                        .serviceType(com.yolt.clients.authmeans.ServiceType.AIS)
                        .build())
                .build());

        // test the dry run functionality
        synchronizeWithProvidersService.synchronizeWithProviders(true);

        // Check that the garbage is there still after a dry run.
        assertThat(clientGroupOnboardedProviderRepository.findById(ClientGroupOnboardedProvider.ClientGroupOnboardedProviderId.builder()
                .clientGroupId(clientGroupId1)
                .serviceType(com.yolt.clients.authmeans.ServiceType.PIS)
                .provider("garbage1")
                .build()))
                .isPresent();

        // test the functionality (no dry run)
        synchronizeWithProvidersService.synchronizeWithProviders(false);

        // Check that the garbage is gone and that the data retrieved from providers has been inserted.
        assertThat(clientGroupOnboardedProviderRepository.findById(ClientGroupOnboardedProvider.ClientGroupOnboardedProviderId.builder()
                .clientGroupId(clientGroupId1)
                .serviceType(com.yolt.clients.authmeans.ServiceType.PIS)
                .provider("garbage1")
                .build()))
                .isEmpty();
        assertThat(clientGroupOnboardedProviderRepository.findById(ClientGroupOnboardedProvider.ClientGroupOnboardedProviderId.builder()
                .clientGroupId(clientGroupId1)
                .serviceType(com.yolt.clients.authmeans.ServiceType.AIS)
                .provider("test1")
                .build()))
                .isPresent();

        // Ensure that our garbage row is gone and that we have only a single row (as expected).
        assertThat(clientOnboardedProviderRepository.findById(ClientOnboardedProvider.ClientOnboardedProviderId.builder()
                        .clientId(clientId2)
                        .redirectUrlId(redirectUrlId2)
                .serviceType(com.yolt.clients.authmeans.ServiceType.PIS)
                .provider("garbage2")
                .build()))
                .isEmpty();
        assertThat(clientOnboardedProviderRepository.findById(ClientOnboardedProvider.ClientOnboardedProviderId.builder()
                .clientId(clientId2)
                .redirectUrlId(redirectUrlId2)
                .serviceType(com.yolt.clients.authmeans.ServiceType.AIS)
                .provider("test2")
                .build()))
                .isPresent();

        // Ensure that our garbage row is gone and that we have only a single row (as expected).
        assertThat(clientOnboardedScrapingProviderRepository.findById(ClientOnboardedScrapingProvider.ClientOnboardedScrapingProviderId.builder()
                .clientId(clientId3)
                .provider("garbage3")
                .serviceType(com.yolt.clients.authmeans.ServiceType.AIS)
                .build()))
                .isEmpty();
        assertThat(clientOnboardedScrapingProviderRepository.findById(ClientOnboardedScrapingProvider.ClientOnboardedScrapingProviderId.builder()
                .clientId(clientId3)
                .provider("test3")
                .serviceType(com.yolt.clients.authmeans.ServiceType.AIS)
                .build()))
                .isPresent();
    }

    @Test
    @SneakyThrows
    void given_authenticationMeans_forUnknownForeignKeyRelationShip_then_theyShouldBeDroppedAndLogged() {
        var clientGroupId1 = UUID.randomUUID();
        clientGroupRepository.save(new ClientGroup(clientGroupId1, "garbage-" + clientGroupId1));
        var clientId = UUID.randomUUID();
        clientRepository.save(makeClient(clientGroupId1, clientId));

        UUID unknownClientGroupId = UUID.randomUUID();
        UUID unknownRedirectUrlId = UUID.randomUUID();
        UUID unknownClientId = UUID.randomUUID();

        var responseJson = objectMapperBuilder.build().writeValueAsString(List.of(
                // for ClientGroupOnboardedProvider
                new ProvidersClient.OnboardedProvider(unknownClientGroupId, null, "test1", ServiceType.AIS, null),
                // for ClientOnboardedProvider
                new ProvidersClient.OnboardedProvider(null, clientId, "test2", ServiceType.AIS, unknownRedirectUrlId),
                // for ClientOnboardedScrapingProvider
                new ProvidersClient.OnboardedProvider(null, unknownClientId, "test3", ServiceType.AIS, null)
        ));
        wireMockServer.stubFor(
                WireMock.get("/providers/all-onboarded-providers")
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody(responseJson))
        );

        // test the functionality (no dry run)
        synchronizeWithProvidersService.synchronizeWithProviders(false);

        // assert that nothing was inserted and everything is clean now:
        assertThat(clientOnboardedScrapingProviderRepository.findAll()).isEmpty();
        assertThat(clientOnboardedProviderRepository.findAll()).isEmpty();
        assertThat(clientGroupOnboardedProviderRepository.findAll()).isEmpty();
        assertLogMessageDoesContain("Dropping ClientGroupOnboardedProvider(clientGroupOnboardedProviderId=ClientGroupOnboardedProvider.ClientGroupOnboardedProviderId(clientGroupId=" + unknownClientGroupId + ", provider=test1, serviceType=AIS), createdAt=null) because the client group Id does not exist in clients.");
        assertLogMessageDoesContain("Dropping ClientOnboardedProvider(clientOnboardedProviderId=ClientOnboardedProvider.ClientOnboardedProviderId(clientId=" + clientId + ", redirectUrlId=" + unknownRedirectUrlId + ", serviceType=AIS, provider=test2), createdAt=null) because the client redirect url does not exist in clients.");
        assertLogMessageDoesContain("Dropping ClientOnboardedScrapingProvider(clientOnboardedScrapingProviderId=ClientOnboardedScrapingProvider.ClientOnboardedScrapingProviderId(clientId=" + unknownClientId +", provider=test3, serviceType=AIS), createdAt=null) because the client group Id does not exist in clients.");
    }


    @NotNull
    private Client makeClient(UUID clientGroupId, UUID clientId) {
        return new Client(
                clientId,
                clientGroupId,
                "garbage-" + clientId,
                "NL",
                false,
                false,
                "10.71",
                null,
                false,
                false,
                false,
                false,
                false,
                false,
                true,
                true,
                false,
                false,
                false,
                false,
                1L,
                Collections.emptySet()
        );
    }


    private void assertLogMessageDoesContain(String messagePart) {
        verify(this.mockAppender, Mockito.atLeastOnce()).doAppend(this.captorLoggingEvent.capture());
        long count = captorLoggingEvent.getAllValues()
                .stream()
                .filter(iLoggingEvent -> !iLoggingEvent.getFormattedMessage().contains(messagePart))
                .count();
        AssertionsForClassTypes.assertThat(count).isGreaterThan(0);
    }
}

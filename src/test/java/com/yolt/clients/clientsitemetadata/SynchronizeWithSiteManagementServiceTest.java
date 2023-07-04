package com.yolt.clients.clientsitemetadata;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.yolt.clients.IntegrationTest;
import com.yolt.clients.client.ClientsRepository;
import com.yolt.clients.clientgroup.ClientGroupRepository;
import com.yolt.clients.model.Client;
import com.yolt.clients.model.ClientGroup;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class SynchronizeWithSiteManagementServiceTest {

    // For setup
    @Autowired
    ClientGroupRepository clientGroupRepository;
    @Autowired
    ClientsRepository clientRepository;
    @Autowired
    WireMockServer wireMockServer;
    @Autowired
    Jackson2ObjectMapperBuilder objectMapperBuilder;

    // Under test
    @Autowired
    SynchronizeWithSiteManagementService synchronizeWithSiteManagementService;

    // For checks
    @Autowired
    ClientSiteMetadataRepository clientSiteMetadataRepository;

    @Test
    @SneakyThrows
    void testSynchronizeWithSiteManagementService() {
        clientSiteMetadataRepository.deleteAll();
        // setup
        // for ClientOnboardedProvider
        var clientGroupId = UUID.randomUUID();
        clientGroupRepository.save(new ClientGroup(clientGroupId, "garbage-" + clientGroupId));
        var clientId = UUID.randomUUID();
        clientRepository.save(makeClient(clientGroupId, clientId));
        final UUID newSiteId1 = UUID.randomUUID();
        final UUID newSiteId2 = UUID.randomUUID();
        final UUID newSiteId3 = UUID.randomUUID();
        var responseJson = objectMapperBuilder.build().writeValueAsString(List.of(
                new SiteManagementClient.ClientSiteFromSiteManagement(clientId, newSiteId1, true, false, false, List.of()),
                new SiteManagementClient.ClientSiteFromSiteManagement(clientId, newSiteId2, false, true, false, List.of("a")),
                new SiteManagementClient.ClientSiteFromSiteManagement(clientId, newSiteId3, false, false, true, List.of("a", "b"))
        ));
        wireMockServer.stubFor(
                WireMock.get("/site-management/internal/client-site-entities")
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody(responseJson))
        );
        // Add a few garbage rows to the db that will be cleared out by the method under test.
        final UUID oldSiteId = UUID.randomUUID();
        clientSiteMetadataRepository.save(ClientSiteMetadata.builder()
                .id(ClientSiteMetadata.ClientSiteMetadataId.builder()
                        .clientId(clientId)
                        .siteId(oldSiteId)
                        .build())
                .provider("TEST")
                .useExperimentalVersion(false)
                .available(true)
                .enabled(false)
                .tags(
                        Set.of(ClientSiteMetadataTags.builder()
                                .clientSiteMetadataTagsId(ClientSiteMetadataTags.ClientSiteMetadataTagsId.builder()
                                        .clientId(clientId)
                                        .siteId(oldSiteId)
                                        .tag("old tag")
                                        .build())
                                .build()))
                .build());

        // test the dry run functionality
        synchronizeWithSiteManagementService.synchronizeWithSiteManagement(true);

        // Check that the garbage is there still after a dry run.
        assertThat(clientSiteMetadataRepository.findAll().iterator().next().getId())
                .isEqualTo(ClientSiteMetadata.ClientSiteMetadataId.builder()
                        .clientId(clientId)
                        .siteId(oldSiteId)
                        .build()
                );

        // test the functionality (no dry run)
        synchronizeWithSiteManagementService.synchronizeWithSiteManagement(false);

        // Check that the garbage is gone and that the data retrieved from providers has been inserted.
        var clientSiteMetadata = StreamSupport.stream(clientSiteMetadataRepository.findAll().spliterator(), false).collect(Collectors.toList());
        // Ensure that our garbage row is gone and that we have 3 rows (as expected).
        assertThat(clientSiteMetadata).hasSize(3);
        assertThat(clientSiteMetadata)
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("provider", "createdAt", "tags")
                .containsExactlyInAnyOrder(
                        ClientSiteMetadata.builder()
                                .id(ClientSiteMetadata.ClientSiteMetadataId.builder()
                                        .clientId(clientId)
                                        .siteId(newSiteId1)
                                        .build())
                                .enabled(true)
                                .useExperimentalVersion(false)
                                .available(false)
                                .tags(Set.of())
                                .build(),
                        ClientSiteMetadata.builder()
                                .id(ClientSiteMetadata.ClientSiteMetadataId.builder()
                                        .clientId(clientId)
                                        .siteId(newSiteId2)
                                        .build())
                                .enabled(false)
                                .useExperimentalVersion(true)
                                .available(false)
                                .build(),
                        ClientSiteMetadata.builder()
                                .id(ClientSiteMetadata.ClientSiteMetadataId.builder()
                                        .clientId(clientId)
                                        .siteId(newSiteId3)
                                        .build())
                                .enabled(false)
                                .useExperimentalVersion(false)
                                .available(true)
                                .build()
                );
        assertThat(clientSiteMetadata.stream().filter(it -> it.getId().getSiteId().equals(newSiteId1))
                .flatMap(it -> it.getTags().stream()).map(it -> it.getClientSiteMetadataTagsId().getTag())).isEmpty();
        assertThat(clientSiteMetadata.stream().filter(it -> it.getId().getSiteId().equals(newSiteId2))
                .flatMap(it -> it.getTags().stream()).map(it -> it.getClientSiteMetadataTagsId().getTag())).containsExactlyInAnyOrder("a");
        assertThat(clientSiteMetadata.stream().filter(it -> it.getId().getSiteId().equals(newSiteId3))
                .flatMap(it -> it.getTags().stream()).map(it -> it.getClientSiteMetadataTagsId().getTag())).containsExactlyInAnyOrder("a", "b");

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

}

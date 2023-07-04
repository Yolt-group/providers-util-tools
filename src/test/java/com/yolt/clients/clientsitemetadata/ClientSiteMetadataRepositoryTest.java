package com.yolt.clients.clientsitemetadata;

import com.vladmihalcea.sql.SQLStatementCountValidator;
import com.yolt.clients.IntegrationTest;
import com.yolt.clients.client.ClientsRepository;
import com.yolt.clients.clientgroup.ClientGroupRepository;
import com.yolt.clients.model.Client;
import com.yolt.clients.model.ClientGroup;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.vladmihalcea.sql.SQLStatementCountValidator.assertSelectCount;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@IntegrationTest
class ClientSiteMetadataRepositoryTest {

    // For setup
    @Autowired
    ClientGroupRepository clientGroupRepository;
    @Autowired
    ClientsRepository clientRepository;

    // Under test
    @Autowired
    ClientSiteMetadataRepository clientSiteMetadataRepository;

    @Test
    void given_client_when_insertClientSiteMetadata_then_success() {
        // given a Client ..
        var clientGroupId = UUID.randomUUID();
        clientGroupRepository.save(new ClientGroup(clientGroupId, "garbage-" + clientGroupId));
        var clientId = UUID.randomUUID();
        clientRepository.save(makeClient(clientGroupId, clientId));

        // Insert some metadata.
        var siteId = UUID.randomUUID();
        var key = ClientSiteMetadata.ClientSiteMetadataId.builder()
                .clientId(clientId)
                .siteId(siteId)
                .build();
        clientSiteMetadataRepository.save(ClientSiteMetadata.builder()
                .id(key)
                .provider("TEST")
                .available(true)
                .enabled(true)
                .useExperimentalVersion(false)
                .tags(Set.of(
                        new ClientSiteMetadataTags(new ClientSiteMetadataTags.ClientSiteMetadataTagsId(clientId, siteId, "test"), Instant.now()),
                        new ClientSiteMetadataTags(new ClientSiteMetadataTags.ClientSiteMetadataTagsId(clientId, siteId, "test2"), Instant.now())
                ))
                .build());

        // Check that it indeed exists.
        var createdEntity = clientSiteMetadataRepository.findById(key);
        assertThat(createdEntity).isPresent();
        createdEntity.ifPresent(o -> {
            assertThat(o.getId()).isEqualTo(key);
            assertThat(o.getCreatedAt()).isNotNull();
            assertThat(o.getTags().stream().map(it -> it.getClientSiteMetadataTagsId().getTag())).containsExactlyInAnyOrder("test", "test2");
        });

        // Delete the entity.
        clientSiteMetadataRepository.deleteById(key);
        // Check that it is gone.
        assertThat(clientSiteMetadataRepository.findById(key)).isEmpty();
    }

    @Test
    void given_nonExistingClient_when_insertClientSiteMetadata_then_failure() {
        // test unsuccessful insert (fk failure)
        var key = ClientSiteMetadata.ClientSiteMetadataId.builder()
                .clientId(/* does not exist*/ UUID.randomUUID())
                .siteId(/* value doesn't matter */ UUID.randomUUID())
                .build();
        assertThatCode(() -> clientSiteMetadataRepository.save(ClientSiteMetadata.builder()
                .id(key)
                .provider("TEST")
                .enabled(true)
                .available(true)
                .useExperimentalVersion(false)
                .build()
        )).hasMessageContaining("client_site_metadata_client_id_fkey");
    }

    @Test
    void given_multipLeTags_then_ASearchQueryShouldEagerlyFetchAllTagsInsteadOfNPlus1Queries_andNotReturnDuplicatedRows() {
        // given a Client ..
        var clientGroupId = UUID.randomUUID();
        clientGroupRepository.save(new ClientGroup(clientGroupId, "garbage-" + clientGroupId));
        var clientId = UUID.randomUUID();
        clientRepository.save(makeClient(clientGroupId, clientId));

        // Insert some metadata.
        var siteWithTag1Tag2 = UUID.randomUUID();
        var siteWithTag3Tag4 = UUID.randomUUID();

        var key1 = ClientSiteMetadata.ClientSiteMetadataId.builder()
                .clientId(clientId)
                .siteId(siteWithTag1Tag2)
                .build();
        ClientSiteMetadata metadata1 = ClientSiteMetadata.builder()
                .id(key1)
                .provider("TEST")
                .available(true)
                .enabled(true)
                .useExperimentalVersion(false)
                .tags(Set.of(
                        new ClientSiteMetadataTags(new ClientSiteMetadataTags.ClientSiteMetadataTagsId(clientId, siteWithTag1Tag2, "tag1"), Instant.now()),
                        new ClientSiteMetadataTags(new ClientSiteMetadataTags.ClientSiteMetadataTagsId(clientId, siteWithTag1Tag2, "tag2"), Instant.now())
                ))
                .build();
        clientSiteMetadataRepository.save(metadata1);
        ClientSiteMetadata metdata2 = ClientSiteMetadata.builder()
                .id(ClientSiteMetadata.ClientSiteMetadataId.builder()
                        .clientId(clientId)
                        .siteId(siteWithTag3Tag4)
                        .build())
                .provider("TEST")
                .available(true)
                .enabled(true)
                .useExperimentalVersion(false)
                .tags(Set.of(
                        new ClientSiteMetadataTags(new ClientSiteMetadataTags.ClientSiteMetadataTagsId(clientId, siteWithTag3Tag4, "tag3"), Instant.now()),
                        new ClientSiteMetadataTags(new ClientSiteMetadataTags.ClientSiteMetadataTagsId(clientId, siteWithTag3Tag4, "tag4"), Instant.now())
                ))
                .build();
        clientSiteMetadataRepository.save(metdata2);

        // Unfortunately it's not really easy to assert the query that has been performed.
        SQLStatementCountValidator.reset();
        List<ClientSiteMetadata> all = clientSiteMetadataRepository.findAll();
        SQLStatementCountValidator.assertSelectCount(1);
        assertThat(all.stream().filter(it -> it.getId().getSiteId().equals(siteWithTag1Tag2))
                .flatMap(it -> it.getTags().stream()).map(ClientSiteMetadataTags::getClientSiteMetadataTagsId)
                .map(ClientSiteMetadataTags.ClientSiteMetadataTagsId::getTag)).containsExactlyInAnyOrder("tag1", "tag2");
        assertThat(all.stream().filter(it -> it.getId().getSiteId().equals(siteWithTag3Tag4))
                .flatMap(it -> it.getTags().stream()).map(ClientSiteMetadataTags::getClientSiteMetadataTagsId)
                .map(ClientSiteMetadataTags.ClientSiteMetadataTagsId::getTag)).containsExactlyInAnyOrder("tag3", "tag4");

        SQLStatementCountValidator.reset();
        List<ClientSiteMetadata> allByIdClientId = clientSiteMetadataRepository.findAllByIdClientId(clientId);
        assertThat(allByIdClientId.stream().filter(it -> it.getId().getSiteId().equals(siteWithTag1Tag2))
                .flatMap(it -> it.getTags().stream()).map(ClientSiteMetadataTags::getClientSiteMetadataTagsId)
                .map(ClientSiteMetadataTags.ClientSiteMetadataTagsId::getTag)).containsExactlyInAnyOrder("tag1", "tag2");
        assertThat(allByIdClientId.stream().filter(it -> it.getId().getSiteId().equals(siteWithTag3Tag4))
                .flatMap(it -> it.getTags().stream()).map(ClientSiteMetadataTags::getClientSiteMetadataTagsId)
                .map(ClientSiteMetadataTags.ClientSiteMetadataTagsId::getTag)).containsExactlyInAnyOrder("tag3", "tag4");
        SQLStatementCountValidator.assertSelectCount(1);

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

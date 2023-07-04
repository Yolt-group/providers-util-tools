package com.yolt.clients.clientsitemetadata;

import com.yolt.clients.client.ClientsRepository;
import com.yolt.clients.model.Client;
import com.yolt.clients.sites.Site;
import com.yolt.clients.sites.SitesProvider;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * This service clears out the following tables and re-fills them after retrieving a snapshot
 * from the site-management service.
 * - {@link ClientSiteMetadata}
 * - {@link ClientSiteMetadataTags}
 */
@Slf4j
@Service
@RequiredArgsConstructor
class SynchronizeWithSiteManagementService {

    private final SiteManagementClient siteManagementClient;
    private final ClientSiteMetadataRepository clientSiteMetadataRepository;
    private final TransactionTemplate transactionTemplate;
    private final SitesProvider sitesProvider;
    private final ClientsRepository clientsRepository;

    private String providerFromSiteId(@NonNull UUID siteId) {
        return sitesProvider.findById(new Site.SiteId(siteId)).map(Site::getProvider).orElse("UNKNOWN-DELETED-SITE");
    }

    /**
     * @param dryrun if set to true the db transaction that replaces the data will be rolled back
     */
    public void synchronizeWithSiteManagement(boolean dryrun) {
        Set<UUID> clientIdsInClients = clientsRepository.findAll().stream().map(Client::getClientId).collect(Collectors.toSet());
        siteManagementClient.retrieveAllClientSiteMetadata().ifPresentOrElse(
                clientSiteMetadataFromSM -> {
                    Set<UUID> clientIdsFromSM = clientSiteMetadataFromSM.stream().map(SiteManagementClient.ClientSiteFromSiteManagement::getClientId).collect(Collectors.toSet());
                    Set<UUID> ignoredClients = new HashSet<>(clientIdsFromSM);
                    ignoredClients.removeAll(clientIdsInClients);
                    log.info("All clients from site-management {}, Only syncing data for clients {}. ignoring {}", clientIdsFromSM, clientIdsInClients, ignoredClients);
                    var clientSiteMetadata = clientSiteMetadataFromSM.stream()
                            .filter(it -> clientIdsInClients.contains(it.getClientId()))
                            .map(o -> ClientSiteMetadata.builder()
                                    .id(ClientSiteMetadata.ClientSiteMetadataId.builder()
                                            .clientId(o.getClientId())
                                            .siteId(o.getSiteId())
                                            .build())
                                    .provider(providerFromSiteId(o.getSiteId()))
                                    .available(o.isAvailable())
                                    .tags(
                                            o.getTags().stream()
                                                    .map(tag -> ClientSiteMetadataTags.builder().clientSiteMetadataTagsId(new ClientSiteMetadataTags.ClientSiteMetadataTagsId(o.getClientId(), o.getSiteId(), tag)).build())
                                                    .collect(Collectors.toSet()))
                                    .enabled(o.isEnabled())
                                    .useExperimentalVersion(o.isUsingExperimentalVersion())
                                    .build())
                            .collect(Collectors.toList());

                    replaceAllData(
                            clientSiteMetadata,
                            dryrun
                    );
                },
                () -> log.warn("synchronizeWithSiteManagement: not doing anything, failed to retrieve a snapshot from site-management.")
        );
    }

    /**
     * Replace all data in a single transaction.
     */
    public void replaceAllData(
            Collection<ClientSiteMetadata> clientSiteMetadata,
            boolean dryrun
    ) {
        transactionTemplate.execute(ctx -> {
            // Retrieve the current state.
            List<ClientSiteMetadata> oldClientSiteMetadata = StreamSupport.stream(clientSiteMetadataRepository.findAll().spliterator(), false).collect(Collectors.toList());

            // Delete all the data.
            clientSiteMetadataRepository.deleteAll();

            // Insert all the new data.
            clientSiteMetadataRepository.saveAll(clientSiteMetadata);

            // Retrieve the new state.
            List<ClientSiteMetadata> newClientSiteMetadata = StreamSupport.stream(clientSiteMetadataRepository.findAll().spliterator(), false).collect(Collectors.toList());

            // Output a diff.
            logDiff(dryrun, diff(
                    oldClientSiteMetadata,
                    newClientSiteMetadata,
                    ClientSiteMetadata::getId
            ));

            if (dryrun) {
                ctx.setRollbackOnly();
            }

            return null;
        });
    }

    static <E, K> Pair<Collection<K>, Collection<K>> diff(Collection<E> lhs, Collection<E> rhs, Function<E, K> extractKey) {
        var lhsKeys = lhs.stream().map(extractKey).collect(Collectors.toSet());
        var rhsKeys = rhs.stream().map(extractKey).collect(Collectors.toSet());
        var deleted = lhsKeys.stream().filter(k -> !rhsKeys.contains(k)).collect(Collectors.toList());
        var added = rhsKeys.stream().filter(k -> !lhsKeys.contains(k)).collect(Collectors.toList());
        return Pair.of(deleted, added);
    }

    static <K> void logDiff(boolean dryrun, Pair<Collection<K>, Collection<K>> diff) {
        log.info("{} {}", dryrun ? "would remove" : "did remove", diff.getLeft());
        log.info("{} {}", dryrun ? "would add" : "did add", diff.getRight());
    }
}

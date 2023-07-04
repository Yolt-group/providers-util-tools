package com.yolt.clients.authmeans;

import com.yolt.clients.client.ClientsRepository;
import com.yolt.clients.client.redirecturls.repository.RedirectURL;
import com.yolt.clients.client.redirecturls.repository.RedirectURLRepository;
import com.yolt.clients.clientgroup.ClientGroupRepository;
import com.yolt.clients.model.Client;
import com.yolt.clients.model.ClientGroup;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * This service clears out the following tables and re-fills them after retrieving a snapshot
 * from the providers service.
 * - {@link ClientGroupOnboardedProvider}
 * - {@link ClientOnboardedProvider}
 * - {@link ClientOnboardedScrapingProvider}
 */
@Value
@Slf4j
@Service
@RequiredArgsConstructor
class SynchronizeWithProvidersService {

    ProvidersClient providersClient;
    ClientGroupOnboardedProviderRepository clientGroupOnboardedProviderRepository;
    ClientOnboardedProviderRepository clientOnboardedProviderRepository;
    ClientOnboardedScrapingProviderRepository clientOnboardedScrapingProviderRepository;
    ClientGroupRepository clientGroupRepository;
    RedirectURLRepository redirectURLRepository;
    ClientsRepository clientsRepository;
    TransactionTemplate transactionTemplate;


    /**
     * @param dryrun if set to true the db transaction that replaces the data will be rolled back, if set to true
     *               the method will output a 'delta'
     */
    public void synchronizeWithProviders(boolean dryrun) {
        providersClient.retrieveAllAuthenticationMeans().ifPresentOrElse(
                onboardedProviders -> {
                    var clientGroupOnboardedProviders = onboardedProviders.stream()
                            .filter(ProvidersClient.OnboardedProvider::isClientGroupOnboardedProvider)
                            .map(o -> ClientGroupOnboardedProvider.builder()
                                    .clientGroupOnboardedProviderId(ClientGroupOnboardedProvider.ClientGroupOnboardedProviderId.builder()
                                            .clientGroupId(o.getClientGroupId())
                                            .provider(o.getProvider())
                                            .serviceType(ServiceType.valueOf(o.getServiceType().name()))
                                            .build())
                                    .build())
                            .collect(Collectors.toList());
                    var clientOnboardedProviders = onboardedProviders.stream()
                            .filter(ProvidersClient.OnboardedProvider::isClientOnboardedProvider)
                            .map(o -> ClientOnboardedProvider.builder()
                                    .clientOnboardedProviderId(ClientOnboardedProvider.ClientOnboardedProviderId.builder()
                                            .clientId(o.getClientId())
                                            .provider(o.getProvider())
                                            .redirectUrlId(o.getRedirectUrlId())
                                            .serviceType(ServiceType.valueOf(o.getServiceType().name()))
                                            .build())
                                    .build())
                            .collect(Collectors.toList());
                    var clientOnboardedScrapingProviders = onboardedProviders.stream()
                            .filter(ProvidersClient.OnboardedProvider::isClientOnboardedScrapingProvider)
                            .map(o -> ClientOnboardedScrapingProvider.builder()
                                    .clientOnboardedScrapingProviderId(ClientOnboardedScrapingProvider.ClientOnboardedScrapingProviderId.builder()
                                            .clientId(o.getClientId())
                                            .provider(o.getProvider())
                                            .serviceType(ServiceType.valueOf(o.getServiceType().name()))
                                            .build())
                                    .build())
                            .collect(Collectors.toList());

                    List<RedirectURL> allRedirectUrls = StreamSupport.stream(redirectURLRepository.findAll().spliterator(), false).collect(Collectors.toList());
                    List<UUID> allClientGroupIds = clientGroupRepository.findAll().stream().map(ClientGroup::getId).collect(Collectors.toList());
                    List<UUID> allClientIds = clientsRepository.findAll().stream().map(Client::getClientId).collect(Collectors.toList());
                    replaceAllData(
                            filterUnknownClientGroups(clientGroupOnboardedProviders, allClientGroupIds),
                            filterUnknownRedirectUrlReferences(clientOnboardedProviders, allRedirectUrls),
                            filterUnknownClients(clientOnboardedScrapingProviders, allClientIds),
                            dryrun
                    );
                },
                () -> log.warn("synchronizeWithProviders: not doing anything, failed to retrieve a snapshot from providers.")
        );
    }

    private List<ClientOnboardedScrapingProvider> filterUnknownClients(List<ClientOnboardedScrapingProvider> clientOnboardedScrapingProviders, List<UUID> allClientIds) {
        return clientOnboardedScrapingProviders.stream().filter(it -> {
            if (!allClientIds.contains(it.getClientOnboardedScrapingProviderId().getClientId())) {
                log.info("Dropping {} because the client group Id does not exist in clients.", it); //NOSHERIFF
                return false;
            }
            return true;
        }).collect(Collectors.toList());
    }

    private List<ClientOnboardedProvider> filterUnknownRedirectUrlReferences(List<ClientOnboardedProvider> clientOnboardedProviders, List<RedirectURL> allRedirectUrls) {
        return clientOnboardedProviders.stream().filter(it -> {
            ClientOnboardedProvider.ClientOnboardedProviderId id = it.getClientOnboardedProviderId();
            UUID clientId = id.getClientId();
            UUID redirectUrlId = id.getRedirectUrlId();
            if (allRedirectUrls.stream().noneMatch(redirectURL -> redirectURL.getRedirectURLId().equals(redirectUrlId) && redirectURL.getClientId().equals(clientId))) {
                log.info("Dropping {} because the client redirect url does not exist in clients.", it); //NOSHERIFF
                return false;
            }
            return true;
        }).collect(Collectors.toList());
    }

    private List<ClientGroupOnboardedProvider> filterUnknownClientGroups(List<ClientGroupOnboardedProvider> clientGroupOnboardedProviders, List<UUID> allClientGroupIds) {
        return clientGroupOnboardedProviders.stream().filter(it -> {
            if (!allClientGroupIds.contains(it.getClientGroupOnboardedProviderId().getClientGroupId())) {
                log.info("Dropping {} because the client group Id does not exist in clients.", it); //NOSHERIFF
                return false;
            }
            return true;
        }).collect(Collectors.toList());
    }

    /**
     * Replace all data in a single transaction.
     */
    public void replaceAllData(
            Collection<ClientGroupOnboardedProvider> clientGroupOnboardedProviders,
            Collection<ClientOnboardedProvider> clientOnboardedProviders,
            Collection<ClientOnboardedScrapingProvider> clientOnboardedScrapingProviders,
            boolean dryrun
    ) {
        transactionTemplate.execute(ctx -> {
            // Retrieve the current state.
            List<ClientGroupOnboardedProvider> oldClientGroupOnboardedProviders = StreamSupport.stream(clientGroupOnboardedProviderRepository.findAll().spliterator(), false).collect(Collectors.toList());
            List<ClientOnboardedProvider> oldClientOnboardedProviders = StreamSupport.stream(clientOnboardedProviderRepository.findAll().spliterator(), false).collect(Collectors.toList());
            List<ClientOnboardedScrapingProvider> oldClientOnboardedScrapingProviders = StreamSupport.stream(clientOnboardedScrapingProviderRepository.findAll().spliterator(), false).collect(Collectors.toList());

            // Delete all the data.
            clientGroupOnboardedProviderRepository.deleteAll();
            clientOnboardedProviderRepository.deleteAll();
            clientOnboardedScrapingProviderRepository.deleteAll();

            // Insert all the new data.
            clientGroupOnboardedProviderRepository.saveAll(clientGroupOnboardedProviders);
            clientOnboardedProviderRepository.saveAll(clientOnboardedProviders);
            clientOnboardedScrapingProviderRepository.saveAll(clientOnboardedScrapingProviders);

            // Retrieve the new state.
            List<ClientGroupOnboardedProvider> newClientGroupOnboardedProviders = StreamSupport.stream(clientGroupOnboardedProviderRepository.findAll().spliterator(), false).collect(Collectors.toList());
            List<ClientOnboardedProvider> newClientOnboardedProviders = StreamSupport.stream(clientOnboardedProviderRepository.findAll().spliterator(), false).collect(Collectors.toList());
            List<ClientOnboardedScrapingProvider> newClientOnboardedScrapingProviders = StreamSupport.stream(clientOnboardedScrapingProviderRepository.findAll().spliterator(), false).collect(Collectors.toList());

            // Output a diff.
            logDiff(dryrun, diff(
                    oldClientGroupOnboardedProviders,
                    newClientGroupOnboardedProviders,
                    ClientGroupOnboardedProvider::getClientGroupOnboardedProviderId
            ));
            logDiff(dryrun, diff(
                    oldClientOnboardedProviders,
                    newClientOnboardedProviders,
                    ClientOnboardedProvider::getClientOnboardedProviderId
            ));
            logDiff(dryrun, diff(
                    oldClientOnboardedScrapingProviders,
                    newClientOnboardedScrapingProviders,
                    ClientOnboardedScrapingProvider::getClientOnboardedScrapingProviderId
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

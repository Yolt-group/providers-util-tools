package com.yolt.clients.clientsite;

import com.yolt.clients.authmeans.OnboardedProviderView;
import com.yolt.clients.authmeans.OnboardedProviderViewRepository;
import com.yolt.clients.authmeans.ServiceType;
import com.yolt.clients.clientsite.dto.ProviderClientSitesDTO;
import com.yolt.clients.clientsitemetadata.ClientSiteMetadata;
import com.yolt.clients.clientsitemetadata.ClientSiteMetadataRepository;
import com.yolt.clients.clientsitemetadata.ClientSiteMetadataTags;
import com.yolt.clients.sites.Site;
import com.yolt.clients.sites.SitesProvider;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.clienttokens.ClientToken;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientSiteService {

    private final ClientSiteMetadataRepository clientSiteMetadataRepository;
    private final ClientSitesUpdateProducer clientSitesUpdateProducer;
    private final OnboardedProviderViewRepository onboardedProviderViewRepository;
    private final ClientSiteDTOMapper clientSiteDTOMapper;
    private final SitesProvider sitesProvider;

    /**
     * Provide tags to a site for a specific client (ClientSite).
     * If a ClientSite does not yet exist for the provided client and site a new ClientSite is created.
     *
     * @param clientToken The client token identifying the client.
     * @param siteId      The site id.
     * @param newTags     The list of tags to be assigned to the client site.
     */
    @Transactional
    void tagSite(final ClientToken clientToken, final UUID siteId, final List<String> newTags) {
        ClientSiteMetadata clientSiteMetadata = getOrCreateClientSiteMetaData(clientToken, siteId);

        clientSiteMetadata.setTags(newTags.stream()
                        .map(t -> ClientSiteMetadataTags.builder()
                                .clientSiteMetadataTagsId(ClientSiteMetadataTags.ClientSiteMetadataTagsId.builder()
                                        .clientId(clientToken.getClientIdClaim())
                                        .siteId(siteId)
                                        .tag(t)
                                        .build())
                                .build())
                        .collect(Collectors.toSet()));
        save(clientSiteMetadata);
    }

    /**
     * Mark a site (contractually) available (default is unavailable) for use by a client, this is stored in a ClientSite.
     * If a ClientSite does not yet exist for the provided client and site a new ClientSite is created.
     *
     * @param clientToken The client token identifying the client.
     * @param siteId      The site id.
     */
    @Transactional
    public void markSiteAvailable(final ClientToken clientToken, final UUID siteId) {
        ClientSiteMetadata clientSiteMetadata = getOrCreateClientSiteMetaData(clientToken, siteId);

        clientSiteMetadata.setAvailable(true);
        save(clientSiteMetadata);
    }


    /**
     * Mark a site (contractually) unavailable (default) for use by a client, this is stored in a ClientSite.
     * If a ClientSite does not yet exist for the provided client and site a new ClientSite is created.
     *
     * @param clientToken The client token identifying the client.
     * @param siteId      The site id.
     */
    @Transactional
    void markSiteUnavailable(final ClientToken clientToken, final UUID siteId) {
        ClientSiteMetadata clientSiteMetadata = getOrCreateClientSiteMetaData(clientToken, siteId);

        clientSiteMetadata.setAvailable(false);
        save(clientSiteMetadata);
    }

    /**
     * Enable the site for the client.
     * Requires a provider to be enabled for the site, if this is not the case the {@link ProviderNotEnabledException} is thrown.
     * Requires the site to be (contractually) available see {@link #markSiteAvailable}.
     * If this is not the case, or if there is no ClientSite yet, the {@link ClientSiteNotAvailableException} is thrown.
     *
     * @param clientToken The client token identifying the client.
     * @param siteId      The site id.
     */
    @Transactional
    public void enableSite(final ClientToken clientToken, final UUID siteId) {

        ClientSiteMetadata clientSiteMetadata = getOrCreateClientSiteMetaData(clientToken, siteId);

        if (!clientSiteMetadata.isAvailable()) {
            String msg = String.format("Trying to enable a client site that is not available for the client. ClientId: %s, SiteId: %s", clientToken.getClientIdClaim(), siteId);
            throw new ClientSiteNotAvailableException(msg);
        }
        clientSiteMetadata.setEnabled(true);
        save(clientSiteMetadata);
    }

    /**
     * Disable the site for the client.
     * Requires the site to be enabled see {@link #enableSite}.
     * If this is not the case, or if there is no ClientSite yet, the {@link ClientSiteConfigurationException} is thrown.
     *
     * @param clientToken The client token identifying the client.
     * @param siteId      The site id.
     */
    @Transactional
    void disableSite(final ClientToken clientToken, final UUID siteId) {
        ClientSiteMetadata clientSiteMetadata = getOrCreateClientSiteMetaData(clientToken, siteId);

        if (!clientSiteMetadata.isEnabled()) {
            String msg = String.format("Trying to disable a client site that has not been enabled yet. ClientId: %s, SiteId: %s", clientToken.getClientIdClaim(), siteId);
            throw new ClientSiteConfigurationException(msg);
        }
        clientSiteMetadata.setEnabled(false);
        save(clientSiteMetadata);
    }

    /**
     * Provide the useExperimentalVersion for a site and a specific client (ClientSite).
     * If a ClientSite does not yet exist for the provided client and site a new ClientSite is created.
     *
     * @param clientToken            The client token identifying the client.
     * @param siteId                 The site id.
     * @param useExperimentalVersion flag indicating that we want to use the experimental version of the api.
     */
    @Transactional
    void setExperimentalVersionForSite(final ClientToken clientToken, final UUID siteId, final boolean useExperimentalVersion) {

        ClientSiteMetadata clientSiteMetadata = getOrCreateClientSiteMetaData(clientToken, siteId);

        clientSiteMetadata.setUseExperimentalVersion(useExperimentalVersion);

        save(clientSiteMetadata);
    }

    ClientSite getEnabledClientSite(final ClientToken clientToken, final UUID siteUUID) {
        UUID clientId = clientToken.getClientIdClaim();
        Site.SiteId siteId = new Site.SiteId(siteUUID);
        Site site = sitesProvider.findByIdOrThrow(siteId);

        Optional<ClientSiteMetadata> optionalEnabledSite = clientSiteMetadataRepository.findById(new ClientSiteMetadata.ClientSiteMetadataId(clientToken.getClientIdClaim(), site.getId().unwrap()));
        if (optionalEnabledSite.isEmpty() || !optionalEnabledSite.get().isEnabled()) {
            throw new ClientSiteNotAvailableException(clientId, siteId);
        }
        List<OnboardedProviderView> onboardings = onboardedProviderViewRepository.selectAllForClientAndProvider(clientToken.getClientIdClaim(),  site.getProvider());
        return toClientSite(optionalEnabledSite.get(), onboardings).orElseThrow(() -> new ClientSiteNotAvailableException(clientId, siteId));
    }


    List<ClientSite> listEnabledClientSites(@NonNull final ClientToken clientToken, @Nullable UUID redirectUrlId, @Nullable List<String> tags) {
        UUID clientId = clientToken.getClientIdClaim();

        Map<String, List<OnboardedProviderView>> onboardingsByProvider = onboardedProviderViewRepository.selectAllForClient(clientId).stream()
                .collect(Collectors.groupingBy(OnboardedProviderView::getProvider));

        return clientSiteMetadataRepository.findAllByIdClientId(clientId)
                .stream()
                .filter(ClientSiteMetadata::isEnabled)
                .map(metaData -> toClientSite(metaData, onboardingsByProvider.get(metaData.getProvider())))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(entry -> {
                    if (redirectUrlId != null) {
                        return entry.getRedirectUrlIds().contains(redirectUrlId);
                    }
                    return true;
                })
                .filter(entry -> {
                    if (tags != null && !tags.isEmpty()) {
                        return entry.getTags().containsAll(tags);
                    }
                    return true;
                })
                .collect(toList());
    }

    private Set<String> toSimpleTags(ClientSiteMetadata metaData) {
        if (metaData == null || metaData.getTags() == null || metaData.getTags().isEmpty()) {
            return Set.of();
        }
        return metaData.getTags().stream().map(it -> it.getClientSiteMetadataTagsId().getTag()).collect(Collectors.toSet());
    }

    /**
     * This method returns all onboarded client sites. Also for those client sites that are *not* enabled, or where there is no site meta data.
     * This has an important implication. For example: client X has onboarded provider YODLEE, but did not enable any site yet.
     */
    List<ProviderClientSitesDTO> listAllClientSitesForInternalUsage(ClientToken clientToken, boolean onlyAvailable) {
        UUID clientId = clientToken.getClientIdClaim();
        Map<String, List<OnboardedProviderView>> onboardingsByProvider = onboardedProviderViewRepository.selectAllForClient(clientId).stream()
                .collect(Collectors.groupingBy(OnboardedProviderView::getProvider));
        Map<UUID, ClientSiteMetadata> metaDataPerSiteId = clientSiteMetadataRepository.findAllByIdClientId(clientId)
                .stream().collect(Collectors.toMap(it -> it.getId().getSiteId(), Function.identity()));


        return onboardingsByProvider.entrySet().stream()
                .map(providerEntry -> {
                    String provider = providerEntry.getKey();
                    // The authentication means for this provider entry.
                    List<AuthenticationMeansScope> authenticationMeansScopes = toAuthenticationMeansScopes(providerEntry.getValue());
                    List<ClientSite> clientSitesForProvider = sitesProvider.findByProvider(provider)
                            .stream()
                            .map(site -> {
                                UUID siteId = site.getId().unwrap();
                                ClientSiteMetadata metadata = metaDataPerSiteId.getOrDefault(siteId, new ClientSiteMetadata(clientId, siteId, provider));
                                return new ClientSite(site, metadata.isAvailable(), metadata.isEnabled(), metadata.isUseExperimentalVersion(),
                                        toSimpleTags(metadata),
                                        authenticationMeansScopes);
                            })
                            .filter(it -> {
                                if (onlyAvailable) {
                                    return it.isAvailable();
                                }
                                return true;
                            })

                             .collect(toList());
                    return new ProviderClientSitesDTO(provider, authenticationMeansScopes, clientSiteDTOMapper.mapClientSiteDTO(clientSitesForProvider));

                })
                .collect(toList());
    }

    Map<UUID, List<ClientSite>> listEnabledSitesPerClient() {

        Map<UUID, Map<String, List<OnboardedProviderView>>> onboardingsPerClientPerProvider = onboardedProviderViewRepository.selectAll().stream()
                .collect(groupingBy(OnboardedProviderView::getClientId, groupingBy(OnboardedProviderView::getProvider)));

        return clientSiteMetadataRepository.findAll().stream()
                .collect(groupingBy(it -> it.getId().getClientId()))
                .entrySet()
                .stream()
                .map(metaDataForClient -> {
                    UUID clientId = metaDataForClient.getKey();
                    List<ClientSite> clientSitesForClient = metaDataForClient.getValue().stream()
                            .map(metaData -> {
                                List<OnboardedProviderView> onboardings = onboardingsPerClientPerProvider.getOrDefault(clientId, Map.of()).getOrDefault(metaData.getProvider(), List.of());
                                return toClientSite(metaData, onboardings);
                            })
                            .filter(Optional::isPresent)
                            .map(Optional::get).collect(toList());
                    return Map.entry(clientId, clientSitesForClient);
                }).collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private ClientSiteMetadata getOrCreateClientSiteMetaData(ClientToken clientToken, UUID siteId) {
        UUID clientId = clientToken.getClientIdClaim();
        Site site = sitesProvider.findByIdOrThrow(new Site.SiteId(siteId));

        return clientSiteMetadataRepository.findById(new ClientSiteMetadata.ClientSiteMetadataId(clientId, siteId))
                .orElse(new ClientSiteMetadata(clientId, siteId, site.getProvider()));
    }

    private List<AuthenticationMeansScope> toAuthenticationMeansScopes(List<OnboardedProviderView> onboardings) {
        return onboardings.stream()
                .collect(groupingBy(OnboardedProviderView::getServiceType)).entrySet()
                .stream()
                .map(onboardingsForServiceType -> {
                    final var serviceType = onboardingsForServiceType.getKey() == ServiceType.AIS ? nl.ing.lovebird.providerdomain.ServiceType.AIS : nl.ing.lovebird.providerdomain.ServiceType.PIS;
                    // This is a little nasty...
                    // OnboardedProviderView::getRedirectUrlId can only return 'null' for scrapers. If we encounter no redirect urls, we assume we're dealing with a scraper,
                    // and for backwards compatibility we set the scopeType to 'CLIENT', meaning the site is onboarded for the entire CLIENT, regardless of any redirect url id.
                    final var redirectUrls = onboardingsForServiceType.getValue().stream()
                            .map(OnboardedProviderView::getRedirectUrlId)
                            .filter(Objects::nonNull)
                            .collect(toList());
                    final var scopeType = redirectUrls.isEmpty() ? AuthenticationMeansScope.Type.CLIENT : AuthenticationMeansScope.Type.REDIRECT_URL;
                    return new AuthenticationMeansScope(scopeType, redirectUrls, serviceType);
                })
                .collect(toList());
    }

    private void save(ClientSiteMetadata clientSiteMetadata) {
        clientSiteMetadataRepository.save(clientSiteMetadata);
        clientSitesUpdateProducer.sendMessage();
    }

    private Optional<ClientSite> toClientSite(ClientSiteMetadata metaData, List<OnboardedProviderView> onboardings) {
        Optional<Site> site = sitesProvider.findById(new Site.SiteId(metaData.getId().getSiteId()));
        if (onboardings == null || onboardings.isEmpty() || site.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(
                new ClientSite(site.get(), metaData.isAvailable(), metaData.isEnabled(), metaData.isUseExperimentalVersion(),
                        toSimpleTags(metaData),
                        toAuthenticationMeansScopes(onboardings)));
    }
}

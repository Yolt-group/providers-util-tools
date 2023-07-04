package com.yolt.clients.clientsite;

import com.yolt.clients.authmeans.OnboardedProviderView;
import com.yolt.clients.authmeans.OnboardedProviderViewRepository;
import com.yolt.clients.authmeans.ServiceType;
import com.yolt.clients.clientsite.dto.ClientSiteDTO;
import com.yolt.clients.clientsite.dto.ProviderClientSitesDTO;
import com.yolt.clients.clientsitemetadata.ClientSiteMetadata;
import com.yolt.clients.clientsitemetadata.ClientSiteMetadataRepository;
import com.yolt.clients.clientsitemetadata.ClientSiteMetadataTags;
import com.yolt.clients.sites.Site;
import com.yolt.clients.sites.SiteCreatorUtil;
import com.yolt.clients.sites.SiteNotFoundException;
import com.yolt.clients.sites.SitesProvider;
import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.clienttokens.test.TestJwtClaims;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@MockitoSettings(strictness = Strictness.WARN)
@ExtendWith(MockitoExtension.class)
class ClientSiteServiceTest {

    @Mock
    private ClientSiteMetadataRepository clientSiteMetadataRepository;
    @Mock
    private OnboardedProviderViewRepository onboardedProviderViewRepository;
    @Spy
    private ClientSiteDTOMapper clientSiteDTOMapper = new ClientSiteDTOMapper("https://icon.com");
    @Mock
    private SitesProvider sitesProvider;

    @InjectMocks
    ClientSiteService clientSiteService;

    final UUID CLIENT_ID = UUID.randomUUID();

    @Test
    void listEnabledClientSites_given_redirectUrlAsFilter_then_listShouldBeFiltered() {
        UUID siteWithOnboardedRedirectUrl1 = UUID.randomUUID();
        UUID siteWithOnboardedRedirectUrl2 = UUID.randomUUID();
        UUID siteWithRandomOnboardedRedirectUrl = UUID.randomUUID();
        UUID siteWithoutOnboardedRedirectUrl = UUID.randomUUID();

        UUID redirect1 = UUID.randomUUID();
        UUID redirect2 = UUID.randomUUID();

        when(sitesProvider.findById(any())).thenAnswer(invocationOnMock -> {
            Site.SiteId siteId = (Site.SiteId) invocationOnMock.getArguments()[0];
            return Optional.of(randomSite(siteId));
        });

        when(onboardedProviderViewRepository.selectAllForClient(CLIENT_ID)).thenReturn(
                List.of(
                        new OnboardedProviderView(CLIENT_ID, "provider", ServiceType.AIS, redirect1),
                        new OnboardedProviderView(CLIENT_ID, "provider2", ServiceType.AIS, redirect2),
                        new OnboardedProviderView(CLIENT_ID, "providerWithRandomRedirectUrlOnboarded", ServiceType.AIS, UUID.randomUUID()),
                        new OnboardedProviderView(CLIENT_ID, "providerWithoutRedirectOnboarded", ServiceType.AIS, null)
                )
        );

        when(clientSiteMetadataRepository.findAllByIdClientId(CLIENT_ID)).thenReturn(
                List.of(new ClientSiteMetadata(new ClientSiteMetadata.ClientSiteMetadataId(CLIENT_ID, siteWithOnboardedRedirectUrl1), "provider", true, true, true, Instant.now(), Set.of()),
                        new ClientSiteMetadata(new ClientSiteMetadata.ClientSiteMetadataId(CLIENT_ID, siteWithOnboardedRedirectUrl2), "provider2", true, true, true, Instant.now(), Set.of()),
                        new ClientSiteMetadata(new ClientSiteMetadata.ClientSiteMetadataId(CLIENT_ID, siteWithRandomOnboardedRedirectUrl), "providerWithRandomRedirectUrlOnboarded", true, true, true, Instant.now(), Set.of()),
                        new ClientSiteMetadata(new ClientSiteMetadata.ClientSiteMetadataId(CLIENT_ID, siteWithoutOnboardedRedirectUrl), "providerWithoutRedirectOnboarded", true, true, true, Instant.now(), Set.of()))
        );

        UUID randomRedirectUrlId = UUID.randomUUID();
        ClientToken clientToken = new ClientToken("stubbed-client-token", TestJwtClaims.createClientClaims("junit", UUID.randomUUID(), CLIENT_ID));
        List<ClientSite> noClientSites = clientSiteService.listEnabledClientSites(clientToken, randomRedirectUrlId, null);
        assertThat(noClientSites.stream().map(it -> it.getSite().getId().unwrap())).isEmpty();

        List<ClientSite> clientSites = clientSiteService.listEnabledClientSites(clientToken, redirect1, null);
        assertThat(clientSites.stream().map(it -> it.getSite().getId().unwrap())).containsExactly(siteWithOnboardedRedirectUrl1);

        List<ClientSite> allClientSites = clientSiteService.listEnabledClientSites(clientToken, null, null);
        assertThat(allClientSites.stream().map(it -> it.getSite().getId().unwrap())).containsExactlyInAnyOrder(siteWithOnboardedRedirectUrl1, siteWithOnboardedRedirectUrl2, siteWithRandomOnboardedRedirectUrl, siteWithoutOnboardedRedirectUrl);
    }

    @Test
    void listEnabledClientSites_given_tagsAsFilter_then_listShouldBeFiltered() {
        UUID siteWith2Tags = UUID.randomUUID();
        UUID siteWith1Tag = UUID.randomUUID();
        UUID siteWithNoTag = UUID.randomUUID();

        when(sitesProvider.findById(any())).thenAnswer(invocationOnMock -> {
            Site.SiteId siteId = (Site.SiteId) invocationOnMock.getArguments()[0];
            return Optional.of(randomSite(siteId));
        });

        when(onboardedProviderViewRepository.selectAllForClient(CLIENT_ID)).thenReturn(
                List.of(new OnboardedProviderView(CLIENT_ID, "provider", ServiceType.AIS, UUID.randomUUID()))
        );

        when(clientSiteMetadataRepository.findAllByIdClientId(CLIENT_ID)).thenReturn(
                List.of(ClientSiteMetadata.builder().id(ClientSiteMetadata.ClientSiteMetadataId.builder()
                                        .clientId(CLIENT_ID)
                                        .siteId(siteWith2Tags)
                                        .build())
                                .provider("provider")
                                .enabled(true)
                                .tags(Set.of(
                                        new ClientSiteMetadataTags(CLIENT_ID, siteWith2Tags, "my-tag"), new ClientSiteMetadataTags(CLIENT_ID, siteWith2Tags, "another-tag")
                                ))
                                .build(),
                        ClientSiteMetadata.builder().id(ClientSiteMetadata.ClientSiteMetadataId.builder()
                                        .clientId(CLIENT_ID)
                                        .siteId(siteWith1Tag)
                                        .build())
                                .provider("provider")
                                .enabled(true)
                                .tags(Set.of(
                                        new ClientSiteMetadataTags(CLIENT_ID, siteWith2Tags, "my-tag")
                                ))
                                .build(),
                        ClientSiteMetadata.builder().id(ClientSiteMetadata.ClientSiteMetadataId.builder()
                                        .clientId(CLIENT_ID)
                                        .siteId(siteWithNoTag)
                                        .build())
                                .provider("provider")
                                .enabled(true)
                                .tags(Set.of())
                                .build()));

        ClientToken clientToken = new ClientToken("stubbed-client-token", TestJwtClaims.createClientClaims("junit", UUID.randomUUID(), CLIENT_ID));
        List<ClientSite> onlyTaggedSite = clientSiteService.listEnabledClientSites(clientToken, null, List.of("my-tag"));
        assertThat(onlyTaggedSite.stream().map(it -> it.getSite().getId().unwrap())).containsExactlyInAnyOrder(siteWith2Tags, siteWith1Tag);

        List<ClientSite> allSites = clientSiteService.listEnabledClientSites(clientToken, null, null);
        assertThat(allSites.stream().map(it -> it.getSite().getId().unwrap())).containsExactlyInAnyOrder(siteWith2Tags, siteWith1Tag, siteWithNoTag);
    }

    @Test
    void getEnabledClientSite_given_siteNotFound_shouldThrowException() {
        UUID siteId = UUID.randomUUID();

        when(sitesProvider.findByIdOrThrow(new Site.SiteId(siteId))).thenThrow(SiteNotFoundException.class);
        ClientToken clientToken = new ClientToken("stubbed-client-token", TestJwtClaims.createClientClaims("junit", UUID.randomUUID(), CLIENT_ID));
        assertThrows(SiteNotFoundException.class, () -> clientSiteService.getEnabledClientSite(clientToken, siteId));
    }

    @Test
    void listEnabledClientSites_given_siteNotFound_shouldNotReturnThoseSite() {
        UUID siteThatExist = UUID.randomUUID();
        UUID siteThatDoesNotExist = UUID.randomUUID();

        when(sitesProvider.findById(new Site.SiteId(siteThatExist))).thenReturn(Optional.of(randomSite(new Site.SiteId(siteThatExist))));
        when(sitesProvider.findById(new Site.SiteId(siteThatDoesNotExist))).thenReturn(Optional.empty());


        when(onboardedProviderViewRepository.selectAllForClient(CLIENT_ID)).thenReturn(
                List.of(new OnboardedProviderView(CLIENT_ID, "provider", ServiceType.AIS, UUID.randomUUID()))
        );

        when(clientSiteMetadataRepository.findAllByIdClientId(CLIENT_ID)).thenReturn(
                List.of(ClientSiteMetadata.builder().id(ClientSiteMetadata.ClientSiteMetadataId.builder()
                                        .clientId(CLIENT_ID)
                                        .siteId(siteThatExist)
                                        .build())
                                .provider("provider")
                                .enabled(true)
                                .build(),
                        ClientSiteMetadata.builder().id(ClientSiteMetadata.ClientSiteMetadataId.builder()
                                        .clientId(CLIENT_ID)
                                        .siteId(siteThatDoesNotExist)
                                        .build())
                                .provider("provider")
                                .enabled(true)
                                .build()));

        ClientToken clientToken = new ClientToken("stubbed-client-token", TestJwtClaims.createClientClaims("junit", UUID.randomUUID(), CLIENT_ID));
        List<ClientSite> onlySite1 = clientSiteService.listEnabledClientSites(clientToken, null, null);
        assertThat(onlySite1.stream().map(it -> it.getSite().getId().unwrap())).containsExactly(siteThatExist);
    }

    @Test
    void listEnabledSitesPerClient_given_siteNotFound_shouldNotReturnThoseSite() {
        UUID site1 = UUID.randomUUID();
        UUID siteThatNoLongerExists = UUID.randomUUID();
        when(sitesProvider.findById(new Site.SiteId(site1))).thenReturn(Optional.of(randomSite(new Site.SiteId(site1))));
        when(sitesProvider.findById(new Site.SiteId(siteThatNoLongerExists))).thenReturn(Optional.empty());


        when(onboardedProviderViewRepository.selectAll()).thenReturn(
                List.of(new OnboardedProviderView(CLIENT_ID, "provider", ServiceType.AIS, UUID.randomUUID()))
        );

        when(clientSiteMetadataRepository.findAll()).thenReturn(
                List.of(ClientSiteMetadata.builder().id(ClientSiteMetadata.ClientSiteMetadataId.builder()
                                        .clientId(CLIENT_ID)
                                        .siteId(site1)
                                        .build())
                                .provider("provider")
                                .enabled(true)
                                .build(),
                        ClientSiteMetadata.builder().id(ClientSiteMetadata.ClientSiteMetadataId.builder()
                                        .clientId(CLIENT_ID)
                                        .siteId(siteThatNoLongerExists)
                                        .build())
                                .provider("provider")
                                .enabled(true)
                                .build()));

        Map<UUID, List<ClientSite>> sitesPerClient = clientSiteService.listEnabledSitesPerClient();
        assertThat(sitesPerClient.size()).isEqualTo(1);
        assertThat(sitesPerClient.get(CLIENT_ID).stream().map(it -> it.getSite().getId().unwrap())).doesNotContain(siteThatNoLongerExists).containsExactlyInAnyOrder(site1);
    }

    @Test
    void listAllClientSitesForInternalUsage_given_siteNotFound_shouldNotReturnThoseSite() {
        UUID siteThatExistForProvider1 = UUID.randomUUID();
        UUID siteThatExistsForProvider2 = UUID.randomUUID();
        UUID siteThatNoLongerExists = UUID.randomUUID();
        when(sitesProvider.findByProvider("provider1")).thenReturn(List.of(
                randomSite(new Site.SiteId(siteThatExistForProvider1), "provider1")
        ));
        when(sitesProvider.findByProvider("provider2")).thenReturn(List.of(
                randomSite(new Site.SiteId(siteThatExistsForProvider2), "provider2")
        ));
        when(onboardedProviderViewRepository.selectAllForClient(CLIENT_ID)).thenReturn(
                List.of(
                        new OnboardedProviderView(CLIENT_ID, "provider1", ServiceType.AIS, UUID.randomUUID()),
                        new OnboardedProviderView(CLIENT_ID, "provider2", ServiceType.AIS, UUID.randomUUID()))
        );

        when(clientSiteMetadataRepository.findAllByIdClientId(CLIENT_ID)).thenReturn(
                List.of(ClientSiteMetadata.builder().id(ClientSiteMetadata.ClientSiteMetadataId.builder()
                                        .clientId(CLIENT_ID)
                                        .siteId(siteThatExistForProvider1)
                                        .build())
                                .provider("provider1")
                                .enabled(true)
                                .build(),
                        ClientSiteMetadata.builder().id(ClientSiteMetadata.ClientSiteMetadataId.builder()
                                        .clientId(CLIENT_ID)
                                        .siteId(siteThatNoLongerExists)
                                        .build())
                                .provider("provider1")
                                .enabled(true)
                                .build(),
                        ClientSiteMetadata.builder().id(ClientSiteMetadata.ClientSiteMetadataId.builder()
                                        .clientId(CLIENT_ID)
                                        .siteId(siteThatExistsForProvider2)
                                        .build())
                                .provider("provider2")
                                .enabled(true)
                                .build()));

        ClientToken clientToken = new ClientToken("stubbed-client-token", TestJwtClaims.createClientClaims("junit", UUID.randomUUID(), CLIENT_ID));
        List<ProviderClientSitesDTO> providerClientSitesDTOS = clientSiteService.listAllClientSitesForInternalUsage(clientToken, false);
        assertThat(providerClientSitesDTOS).hasSize(2);
        assertThat(providerClientSitesDTOS.stream().flatMap(it -> it.getSites().stream()).map(ClientSiteDTO::getId)).doesNotContain(siteThatNoLongerExists);
        assertThat(providerClientSitesDTOS.stream().filter(it -> it.getProvider().equals("provider1")).flatMap(it -> it.getSites().stream()).map(ClientSiteDTO::getId)).containsExactly(siteThatExistForProvider1);
        assertThat(providerClientSitesDTOS.stream().filter(it -> it.getProvider().equals("provider2")).flatMap(it -> it.getSites().stream()).map(ClientSiteDTO::getId)).containsExactly(siteThatExistsForProvider2);
    }

    @Test
    void listAllClientSitesForInternalUsage_givenAnOnboardedProvider_and_multipleSitesWithoutMetaData_shouldBeReturnedWithDefaultDisabledMetaData() {
        UUID site1 = UUID.randomUUID();
        UUID site2 = UUID.randomUUID();
        UUID site3 = UUID.randomUUID();
        when(sitesProvider.findByProvider("YODLEE")).thenReturn(List.of(
                randomSite(new Site.SiteId(site1), "YODLEE"),
                randomSite(new Site.SiteId(site2), "YODLEE"),
                randomSite(new Site.SiteId(site3), "YODLEE")

        ));

        when(onboardedProviderViewRepository.selectAllForClient(CLIENT_ID)).thenReturn(
                List.of(new OnboardedProviderView(CLIENT_ID, "YODLEE", ServiceType.AIS, UUID.randomUUID())));

        when(clientSiteMetadataRepository.findAllByIdClientId(CLIENT_ID)).thenReturn(List.of());

        ClientToken clientToken = new ClientToken("stubbed-client-token", TestJwtClaims.createClientClaims("junit", UUID.randomUUID(), CLIENT_ID));
        List<ProviderClientSitesDTO> providerClientSitesDTOS = clientSiteService.listAllClientSitesForInternalUsage(clientToken, false);
        assertThat(providerClientSitesDTOS).hasSize(1);
        ProviderClientSitesDTO providerEntry = providerClientSitesDTOS.get(0);
        assertThat(providerEntry.getProvider()).isEqualTo("YODLEE");
        assertThat(providerEntry.getSites().stream().map(ClientSiteDTO::getId)).containsExactlyInAnyOrder(site1, site2, site3);
        assertThat(providerEntry.getSites().stream().map(ClientSiteDTO::isEnabled)).allMatch(it -> !it);
        assertThat(providerEntry.getSites().stream().map(ClientSiteDTO::isAvailable)).allMatch(it -> !it);
    }

    private Site randomSite(Site.SiteId siteId) {
        return randomSite(siteId, "provider");
    }

    private Site randomSite(Site.SiteId siteId, String provider) {
        return SiteCreatorUtil.createTestSite(siteId.unwrap(), "name", provider, List.of(), List.of(), Map.of());
    }
}
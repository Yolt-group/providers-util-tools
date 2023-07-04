package com.yolt.clients.sites;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.yolt.clients.IntegrationTest;
import com.yolt.clients.sites.ais.ConsentBehavior;
import com.yolt.clients.sites.ais.LoginRequirement;
import nl.ing.lovebird.providerdomain.AccountType;
import nl.ing.lovebird.providerdomain.ServiceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

@IntegrationTest
class SitesProviderTest {

    @Autowired
    private SitesProvider sitesProvider;

    @Autowired
    private WireMockServer wireMockServer;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    public void beforeEach() {
        WireMock.removeAllMappings();
    }

    @Test
    public void update_sitesAreNoteUpdatedWhenDuplicatesAreReturned() throws JsonProcessingException {

        List<Site> initialSites = sitesProvider.allSites();

        wireMockServer.stubFor(WireMock.get(urlMatching("/providers/sites-details"))
                .willReturn(aResponse()
                        .withBody(objectMapper.writeValueAsString(createExampleDuplicatedProvidersSites(2)))
                        .withHeader("content-type", "application/json")
                        .withStatus(OK.value())
                ));

        // when
        sitesProvider.update();

        // then
        assertThat(sitesProvider.allSites()).isEqualTo(initialSites);
    }

    @Test
    public void allSites_allSitesAreAvailableAfterProvidersFail() throws JsonProcessingException {

        wireMockServer.stubFor(WireMock.get(urlMatching("/providers/sites-details"))
                .willReturn(aResponse()
                        .withBody(objectMapper.writeValueAsString(createExampleProvidersSites(1)))
                        .withHeader("content-type", "application/json")
                        .withStatus(OK.value())
                ));

        sitesProvider.update();
        int siteNumberAfterFirstUpdate = sitesProvider.allSites().size();

        wireMockServer.stubFor(WireMock.get(urlMatching("/providers/sites-details"))
                .willReturn(aResponse()
                        .withBody(objectMapper.writeValueAsString(createExampleProvidersSites(2)))
                        .withHeader("content-type", "application/json")
                        .withStatus(OK.value())
                ));

        sitesProvider.update();
        int siteNumberAfterSecondUpdate = sitesProvider.allSites().size();

        wireMockServer.stubFor(WireMock.get(urlMatching("/providers/sites-details"))
                .willReturn(aResponse()
                        .withStatus(NOT_FOUND.value())
                ));

        sitesProvider.update();
        int siteNumberAfterFailure = sitesProvider.allSites().size();

        assertThat(siteNumberAfterFirstUpdate).isEqualTo(1);
        assertThat(siteNumberAfterSecondUpdate).isEqualTo(2);
        assertThat(siteNumberAfterFailure).isEqualTo(2);
    }

    @Test
    public void when_providersReturnsAllSites_thenNothingShouldCrash() {
        wireMockServer.stubFor(WireMock.get(urlMatching("/providers/sites-details"))
                .willReturn(aResponse()
                        .withBodyFile("providers/sites-details-from-providers-2022-03-29.json")
                        .withHeader("content-type", "application/json")
                        .withStatus(OK.value())
                ));

        sitesProvider.update();

        assertThat(sitesProvider.allSites().size()).isEqualTo(295);
    }

    private ProvidersSites createExampleProvidersSites(int numberOfSites) {
        List<ProvidersSites.RegisteredSite> sites = new ArrayList<>();
        for (int i = 0; i < numberOfSites; i++) {
            sites.add(new ProvidersSites.RegisteredSite("name",
                    "BARCLAYS",
                    "groupingBy",
                    UUID.randomUUID(),
                    List.of(AccountType.CURRENT_ACCOUNT),
                    List.of(CountryCode.PL),
                    90,
                    Set.of(ConsentBehavior.CONSENT_PER_ACCOUNT),
                    "externalId",
                    Map.of(ServiceType.AIS, List.of(LoginRequirement.REDIRECT)))
            );
        }
        return new ProvidersSites(sites, Collections.emptyList());
    }

    private ProvidersSites createExampleDuplicatedProvidersSites(int numberOfSites) {
        List<ProvidersSites.RegisteredSite> sites = new ArrayList<>();
        UUID id = UUID.randomUUID();
        for (int i = 0; i < numberOfSites; i++) {
            sites.add(TestProviderSites.ABN_AMRO);
        }
        return new ProvidersSites(sites, Collections.emptyList());
    }
}

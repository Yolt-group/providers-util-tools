package com.yolt.clients.clientsite;


import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.yolt.clients.IntegrationTest;
import com.yolt.clients.authmeans.ClientOnboardingsTestUtility;
import com.yolt.clients.client.ClientsRepository;
import com.yolt.clients.client.redirecturls.RedirectURLService;
import com.yolt.clients.clientgroup.ClientGroupRepository;
import com.yolt.clients.clientsite.dto.ClientSiteDTO;
import com.yolt.clients.clientsitemetadata.ClientSiteMetadata;
import com.yolt.clients.clientsitemetadata.ClientSiteMetadataRepository;
import com.yolt.clients.model.Client;
import com.yolt.clients.model.ClientGroup;
import com.yolt.clients.sites.CountryCode;
import com.yolt.clients.sites.SitesProvider;
import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.clienttokens.test.TestClientTokens;
import nl.ing.lovebird.errorhandling.ErrorDTO;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.*;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.OK;

@IntegrationTest
class ClientSiteControllerIntegrationTest {

    private static final ClientId CLIENT_ID = new ClientId(UUID.randomUUID());
    private static final UUID CLIENT_GROUP_ID = UUID.randomUUID();
    private static final UUID REDIRECT_URL_ID = UUID.randomUUID();
    private static final UUID SITE_ID = UUID.fromString("82c16668-4d59-4be8-be91-1d52792f48e3");
    private static final String PROVIDER = "MONZO";

    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private WireMockServer wireMockServer;
    @Autowired
    private SitesProvider sitesProvider;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private ClientOnboardingsTestUtility clientOnboardingsTestUtility;

    @Autowired
    private ClientGroupRepository clientGroupRepository;
    @Autowired
    private ClientsRepository clientRepository;
    @Autowired
    private RedirectURLService redirectURLService;
    @Autowired
    private TestClientTokens testClientTokens;

    @Autowired
    private ClientSiteMetadataRepository clientSiteMetadataRepository;

    private ClientToken clientToken;

    @BeforeEach
    void setUp() {
        clientToken = testClientTokens.createClientToken(CLIENT_GROUP_ID, CLIENT_ID.unwrap());
        wireMockServer.stubFor(WireMock.get(urlMatching("/providers/sites-details"))
                .willReturn(aResponse()
                        .withBodyFile("providers/sites-details-from-providers-2022-03-29.json")
                        .withHeader("content-type", "application/json")
                        .withStatus(OK.value())
                ));

        sitesProvider.update();
        clientGroupRepository.save(new ClientGroup(CLIENT_GROUP_ID, "client-group-name"));
        clientRepository.save(makeClient(CLIENT_GROUP_ID, CLIENT_ID.unwrap()));
        redirectURLService.create(clientToken, REDIRECT_URL_ID, "https://example.com");

    }

    @AfterEach
    public void cleanup() {
        transactionTemplate.executeWithoutResult(transactionStatus -> entityManager.createNativeQuery("truncate table client_group cascade").executeUpdate());
        transactionTemplate.executeWithoutResult(transactionStatus -> entityManager.createNativeQuery("truncate table client_site_metadata cascade").executeUpdate());
    }

    @Test
    void testEnableWhenProviderNotEnabled() throws URISyntaxException {
        ResponseEntity<Void> response = enableSite(SITE_ID);
        assertThat(response.getStatusCode().value()).isEqualTo(400); // Provider is not enabled
    }

    @Test
    void when_aSiteIsOnboarded_then_itShouldOnlyBeReturnedIfItsEnabled() throws URISyntaxException {
        // make sure the provider is enabled first
        clientOnboardingsTestUtility.addOnboardedAISProviderForClient(CLIENT_ID.unwrap(), REDIRECT_URL_ID, PROVIDER);

        // Check that it s not yet returned
        ResponseEntity<ErrorDTO> response = restTemplate.exchange("/v2/sites/{siteId}",
                HttpMethod.GET,
                createHttpEntity(null),
                ErrorDTO.class, SITE_ID);
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().getMessage()).isEqualTo("The client site is not available.");

        // Enable it
        ResponseEntity<Void> availableResponse = updateSiteAvailable(SITE_ID, true);
        assertThat(availableResponse.getStatusCode().value()).isEqualTo(200);
        var responseEntity = enableSite(SITE_ID);
        assertThat(responseEntity.getStatusCode().value()).isEqualTo(200);

        // Check that it is now
        ResponseEntity<ClientSiteDTO> finalResponse = restTemplate.exchange("/v2/sites/{siteId}",
                HttpMethod.GET,
                createHttpEntity(null),
                ClientSiteDTO.class, SITE_ID);
        assertThat(finalResponse.getStatusCode().value()).isEqualTo(200);
        assertThat(finalResponse.getBody().getId()).isEqualTo(SITE_ID);
        System.out.println(finalResponse.getBody());
    }

    @Test
    void testTagSite() throws URISyntaxException {
        // make sure the provider is enabled first
        clientOnboardingsTestUtility.addOnboardedAISProviderForClient(CLIENT_ID.unwrap(), REDIRECT_URL_ID, PROVIDER);

        // Mark site available
        ResponseEntity<Void> availableResponseEntity = updateSiteAvailable(SITE_ID, true);
        assertThat(availableResponseEntity.getStatusCode().value()).isEqualTo(200);

        // Save the enable site
        ResponseEntity<Void> responseEntity = enableSite(SITE_ID);
        assertThat(responseEntity.getStatusCode().value()).isEqualTo(200);

        // Add tags
        String putTagsUri = String.format("/client-sites/%s/tags", SITE_ID);
        String body = "[\"Ralph\", \"NL\", \"oldTag\"]";
        HttpEntity<?> tagsEntity = createHttpEntity(body);
        ResponseEntity<Void> addTagResponse = restTemplate.exchange(new URI(putTagsUri), HttpMethod.PUT, tagsEntity, Void.class);
        assertThat(addTagResponse.getStatusCode().value()).isEqualTo(202);

        // Add another tags
        ResponseEntity<Void> updateTag = restTemplate.exchange(new URI(putTagsUri), HttpMethod.PUT, createHttpEntity("[\"Ralph\", \"NL\", \"newTag\"]"), Void.class);
        assertThat(updateTag.getStatusCode().value()).isEqualTo(202);

        // Check site is returned by tag
        String uri = String.format("/v2/sites?redirectUrlId=%s", REDIRECT_URL_ID) + "&tag=Ralph&tag=NL";
        ResponseEntity<List<ClientSiteDTO>> enabledSitesResponse = restTemplate.exchange(new URI(uri), HttpMethod.GET, createHttpEntity(null),
                new ParameterizedTypeReference<>() {
                });
        assertThat(enabledSitesResponse.getStatusCode().value()).isEqualTo(200);
        assertThat(enabledSitesResponse.getBody()).hasSize(1);
        ClientSiteDTO enabledSite = enabledSitesResponse.getBody().get(0);
        assertThat(enabledSite.getId()).isEqualTo(SITE_ID);
        assertThat(enabledSite.getName()).isEqualTo("Monzo");
        assertThat(enabledSite.getAvailableInCountries()).containsExactlyInAnyOrder(CountryCode.GB, CountryCode.NL);
        assertThat(enabledSite.getServices().getAis()).isNotNull();
        assertThat(enabledSite.getServices().getPis()).isNotNull();
        assertThat(enabledSite.getTags()).containsExactlyInAnyOrder("Ralph", "NL", "newTag");
        assertThat(enabledSite.getServices().getAis().getOnboarded().getRedirectUrlIds()).containsExactlyInAnyOrder(REDIRECT_URL_ID);

        // Check site is returned without tags
        uri = String.format("/v2/sites?redirectUrlId=%s", REDIRECT_URL_ID);
        enabledSitesResponse = restTemplate.exchange(new URI(uri), HttpMethod.GET, createHttpEntity(null),
                new ParameterizedTypeReference<>() {
                });
        assertThat(enabledSitesResponse.getStatusCode().value()).isEqualTo(200);
        assertThat(enabledSitesResponse.getBody()).hasSize(1);
        enabledSite = enabledSitesResponse.getBody().get(0);
        assertThat(enabledSite.getId()).isEqualTo(SITE_ID);
        assertThat(enabledSite.getName()).isEqualTo("Monzo");

        // Check site is not returned by for unknowntag
        uri = String.format("/v2/sites?redirectUrlId=%s", REDIRECT_URL_ID) + "&tag=Robin";
        ResponseEntity<List<ClientSiteDTO>> response = restTemplate.exchange(new URI(uri), HttpMethod.GET, createHttpEntity(null),
                new ParameterizedTypeReference<>() {
                });
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void testEnableAndDisableSite() throws URISyntaxException {
        // make sure the provider is enabled first
        clientOnboardingsTestUtility.addOnboardedAISProviderForClient(CLIENT_ID.unwrap(), REDIRECT_URL_ID, PROVIDER);

        // Mark site available
        ResponseEntity<Void> availableResponseEntity = updateSiteAvailable(SITE_ID, true);
        assertThat(availableResponseEntity.getStatusCode().value()).isEqualTo(200);

        // Save the enable site
        ResponseEntity<Void> responseEntity = enableSite(SITE_ID);
        assertThat(responseEntity.getStatusCode().value()).isEqualTo(200);

        // Check site is enabled
        String uri = String.format("/v2/sites?redirectUrlId=%s", REDIRECT_URL_ID);
        ResponseEntity<List<ClientSiteDTO>> enabledSitesResponse = restTemplate.exchange(new URI(uri), HttpMethod.GET, createHttpEntity(null),
                new ParameterizedTypeReference<>() {
                });
        assertThat(enabledSitesResponse.getStatusCode().value()).isEqualTo(200);
        assertThat(enabledSitesResponse.getBody()).hasSize(1);
        ClientSiteDTO enabledSite = enabledSitesResponse.getBody().get(0);
        assertThat(enabledSite.getId()).isEqualTo(SITE_ID);
        assertThat(enabledSite.getName()).isEqualTo("Monzo");
        assertThat(enabledSite.getServices().getAis()).isNotNull();
        assertThat(enabledSite.getServices().getPis()).isNotNull();
        assertThat(enabledSite.getServices().getAis().getOnboarded().getRedirectUrlIds()).containsExactlyInAnyOrder(REDIRECT_URL_ID);

        // disable the enabled site
        uri = String.format("/client-sites/%s", SITE_ID);
        restTemplate.exchange(new URI(uri), HttpMethod.DELETE, createHttpEntity(null), Void.class);
        assertThat(responseEntity.getStatusCode().value()).isEqualTo(200);

        // Check site is disabled
        Optional<ClientSiteMetadata> clientSite = clientSiteMetadataRepository.findById(new ClientSiteMetadata.ClientSiteMetadataId(CLIENT_ID.unwrap(), SITE_ID));
        assertThat(clientSite.get().isEnabled()).isFalse();
    }

    @Test
    void testEnableSiteGroupLevel() throws URISyntaxException {
        // make sure the provider is enabled first
        clientOnboardingsTestUtility.addOnboardedAISProviderForClientGroup(CLIENT_GROUP_ID, PROVIDER);

        // Mark site available
        ResponseEntity<Void> availableResponseEntity = updateSiteAvailable(SITE_ID, true);
        assertThat(availableResponseEntity.getStatusCode().value()).isEqualTo(200);

        // Save the enable site
        ResponseEntity<Void> responseEntity = enableSite(SITE_ID);
        assertThat(responseEntity.getStatusCode().value()).isEqualTo(200);

        // Check site is enabled
        String uri = String.format("/v2/sites?redirectUrlId=%s", REDIRECT_URL_ID);
        ResponseEntity<List<ClientSiteDTO>> enabledSitesResponse = restTemplate.exchange(new URI(uri), HttpMethod.GET, createHttpEntity(null),
                new ParameterizedTypeReference<List<ClientSiteDTO>>() {
                });
        assertThat(enabledSitesResponse.getStatusCode().value()).isEqualTo(200);
        assertThat(enabledSitesResponse.getBody()).hasSize(1);
        ClientSiteDTO enabledSite = enabledSitesResponse.getBody().get(0);
        assertThat(enabledSite.getId()).isEqualTo(SITE_ID);
        assertThat(enabledSite.getName()).isEqualTo("Monzo");
        assertThat(enabledSite.getServices().getAis()).isNotNull();
        assertThat(enabledSite.getServices().getPis()).isNotNull();
    }

    @Test
    void testEnableDisableExperimentalSite() throws URISyntaxException {
        // make sure the provider is enabled
        clientOnboardingsTestUtility.addOnboardedAISProviderForClient(CLIENT_ID.unwrap(), REDIRECT_URL_ID, PROVIDER);

        // Mark site available
        ResponseEntity<Void> availableResponseEntity = updateSiteAvailable(SITE_ID, true);
        assertThat(availableResponseEntity.getStatusCode().value()).isEqualTo(200);

        // Save the enable site
        ResponseEntity<Void> responseEntity = enableSite(SITE_ID);
        assertThat(responseEntity.getStatusCode().value()).isEqualTo(200);

        // Enable experimental version for site
        String uri = String.format("/client-sites/%s/experimental/%s", SITE_ID, true);
        HttpEntity<?> httpEntity = createHttpEntity(null);
        ResponseEntity<Void> response = restTemplate.postForEntity(
                new URI(uri),
                httpEntity,
                Void.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);

        // Check if experimental version is enabled
        uri = String.format("/v2/sites?redirectUrlId=%s", REDIRECT_URL_ID);
        ResponseEntity<List<ClientSiteDTO>> enabledSitesResponse = restTemplate.exchange(new URI(uri), HttpMethod.GET, httpEntity,
                new ParameterizedTypeReference<>() {
                });
        assertThat(enabledSitesResponse.getStatusCode().value()).isEqualTo(200);
        assertThat(enabledSitesResponse.getBody()).hasSize(1);
        ClientSiteDTO site = enabledSitesResponse.getBody().get(0);
        assertThat(site.getId()).isEqualTo(SITE_ID);
        assertThat(site.isUseExperimentalVersion()).isTrue();

        // disable experimental version for site
        uri = String.format("/client-sites/%s/experimental/%s", SITE_ID, false);
        restTemplate.exchange(new URI(uri), HttpMethod.POST, createHttpEntity(null), Void.class);
        assertThat(responseEntity.getStatusCode().value()).isEqualTo(200);

        // Check if experimental version is disabled
        Optional<ClientSiteMetadata> clientSite = clientSiteMetadataRepository.findById(new ClientSiteMetadata.ClientSiteMetadataId(CLIENT_ID.unwrap(), SITE_ID));
        assertThat(clientSite.get().isUseExperimentalVersion()).isFalse();
    }

    @Test
    void testEnableDisableAvailabilitySite() throws URISyntaxException {
        // make sure the provider is enabled
        clientOnboardingsTestUtility.addOnboardedAISProviderForClient(CLIENT_ID.unwrap(), REDIRECT_URL_ID, PROVIDER);

        // Enable available for site
        ResponseEntity<Void> availableResponseEntity = updateSiteAvailable(SITE_ID, true);
        assertThat(availableResponseEntity.getStatusCode().value()).isEqualTo(200);

        // Check if available version is enabled
        Optional<ClientSiteMetadata> clientSite = clientSiteMetadataRepository.findById(new ClientSiteMetadata.ClientSiteMetadataId(CLIENT_ID.unwrap(), SITE_ID));
        assertThat(clientSite.get().isAvailable()).isTrue();


        // disable available for site
        String uri = String.format("/client-sites/%s/available/%s", SITE_ID, false);
        ResponseEntity<Void> disabled = restTemplate.exchange(new URI(uri), HttpMethod.POST, createHttpEntity(null), Void.class);
        assertThat(disabled.getStatusCode().value()).isEqualTo(200);

        // Check if available is disabled
        Optional<ClientSiteMetadata> clientSite2 = clientSiteMetadataRepository.findById(new ClientSiteMetadata.ClientSiteMetadataId(CLIENT_ID.unwrap(), SITE_ID));
        assertThat(clientSite2.get().isAvailable()).isFalse();

    }


    private HttpEntity<?> createHttpEntity(String body) {
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.setContentType(MediaType.APPLICATION_JSON);
        List<Charset> charSets = new ArrayList<>();
        charSets.add(Charset.defaultCharset());
        requestHeaders.setAcceptCharset(charSets);
        List<MediaType> mediaTypes = new ArrayList<>();
        mediaTypes.add(MediaType.APPLICATION_JSON);
        requestHeaders.setAccept(mediaTypes);
        requestHeaders.setContentType(MediaType.APPLICATION_JSON);
        requestHeaders.add("client-token", clientToken.getSerialized());
        return new HttpEntity<>(body, requestHeaders);
    }

    private ResponseEntity<Void> enableSite(UUID siteId) throws URISyntaxException {
        String uri = String.format("/client-sites/%s", siteId);
        HttpEntity<?> httpEntity = createHttpEntity(null);
        return restTemplate.postForEntity(
                new URI(uri),
                httpEntity,
                Void.class);
    }

    private ResponseEntity<Void> updateSiteAvailable(UUID siteId, boolean available) throws URISyntaxException {
        String availableURI = String.format("/client-sites/%s/available/%s", siteId, available);
        HttpEntity<?> availableHttpEntity = createHttpEntity(null);
        return restTemplate.postForEntity(
                new URI(availableURI),
                availableHttpEntity,
                Void.class);
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

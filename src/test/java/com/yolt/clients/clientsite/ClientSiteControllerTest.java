package com.yolt.clients.clientsite;

import com.yolt.clients.clientsite.dto.ClientSiteDTO;
import com.yolt.clients.clientsite.dto.ConnectionType;
import com.yolt.clients.clientsite.dto.LoginType;
import com.yolt.clients.clientsite.dto.ProviderClientSitesDTO;
import com.yolt.clients.config.ExceptionHandlers;
import com.yolt.clients.sites.CountryCode;
import com.yolt.clients.sites.Site;
import com.yolt.clients.sites.SiteCreatorUtil;
import com.yolt.clients.sites.ais.LoginRequirement;
import com.yolt.clients.sites.pis.*;
import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.clienttokens.test.TestClientTokens;
import nl.ing.lovebird.providerdomain.AccountType;
import nl.ing.lovebird.providerdomain.ServiceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static com.yolt.clients.sites.SiteCreatorUtil.AIS_WITH_REDIRECT_STEPS;
import static com.yolt.clients.sites.SiteCreatorUtil.CURRENT_CREDIT_SAVINGS;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = ClientSiteController.class)
@ActiveProfiles("test")
@Import({
        ExceptionHandlers.class,
        ClientSiteDTOMapper.class
})
class ClientSiteControllerTest {

    private static final UUID CLIENT_GROUP_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final ClientId CLIENT_ID = new ClientId(UUID.randomUUID());
    @MockBean
    private ClientSiteService clientSiteService;

    private HttpHeaders headers;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private TestClientTokens testClientTokens;
    private ClientToken clientToken;

    @BeforeEach
    void setUp() {
        clientToken = testClientTokens.createClientToken(CLIENT_GROUP_ID, CLIENT_ID.unwrap());
        headers = new HttpHeaders();
        headers.add("client-token", clientToken.getSerialized());
        headers.put("user-id", Collections.singletonList(USER_ID.toString()));
        headers.put("cbms-profile-id", Collections.singletonList("yolt"));
    }

    @Test
    void postSites_BadMethod() throws Exception {
        mockMvc.perform(post("/client-sites")
                        .headers(headers)
                        .content("{}")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().json("{\"code\":\"CLS1001\",\"message\":\"Method not supported\"}"))
                .andReturn();
    }

    @Test
    void testListClientSites() throws Exception {
        // Prep the params
        UUID siteId = UUID.randomUUID();
        String siteName = "name";
        String uri = "/client-sites";

        // prep mock
        List<ProviderClientSitesDTO> clientSites = new LinkedList<>();
        clientSites.add(new ProviderClientSitesDTO("YOLT_PROVIDER",
                Collections.singletonList(new AuthenticationMeansScope(AuthenticationMeansScope.Type.CLIENT, null, ServiceType.AIS)),
                Collections.singletonList(new ClientSiteDTO(siteId, siteName, Collections.singletonList(AccountType.CURRENT_ACCOUNT), LoginType.URL, ConnectionType.DIRECT_CONNECTION, new ClientSiteDTO.Services(new ClientSiteDTO.Services.AIS(new ClientSiteDTO.Services.Onboarded(Collections.emptySet(), true), true, false, Collections.emptySortedSet()),
                        null), null, Collections.singletonList("tag"), false, false, false, null, null, null, "/path/to/icons"))));
        when(clientSiteService.listAllClientSitesForInternalUsage(clientToken, false)).thenReturn(clientSites);

        // Hit the controller and verify
        this.mockMvc.perform(get(uri)
                        .headers(headers)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].provider").value("YOLT_PROVIDER"))
                .andExpect(jsonPath("$[0].registeredAuthenticationMeans").isArray())
                .andExpect(jsonPath("$[0].registeredAuthenticationMeans[0].type").value("CLIENT"))
                .andExpect(jsonPath("$[0].sites[0].id").value(siteId.toString()))
                .andExpect(jsonPath("$[0].sites[0].loginType").value("URL"))
                .andExpect(jsonPath("$[0].sites[0].connectionType").value("DIRECT_CONNECTION"))
                .andExpect(jsonPath("$[0].sites[0].name").value(siteName))
                .andExpect(jsonPath("$[0].sites[0].tags[0]").value("tag"))
                .andExpect(jsonPath("$[0].sites[0].services.pis").doesNotExist())
                .andExpect(jsonPath("$[0].sites[0].services.ais").exists())
                .andExpect(jsonPath("$[0].sites[0].services.ais.onboarded.client").value(true))
                .andExpect(jsonPath("$[0].sites[0].services.ais.onboarded.redirectUrlIds").isArray())
                .andExpect(jsonPath("$[0].sites[0].services.ais.onboarded.redirectUrlIds").isEmpty());

        // verify mock
        verify(clientSiteService).listAllClientSitesForInternalUsage(clientToken, false);
    }

    @Test
    void testListClientSitesOnlyAvailableSet() throws Exception {
        // Prep the params
        UUID siteId = UUID.randomUUID();
        String siteName = "name";
        String uri = "/client-sites";

        // prep mock
        List<ProviderClientSitesDTO> clientSites = new LinkedList<>();
        clientSites.add(new ProviderClientSitesDTO("YOLT_PROVIDER",
                Collections.singletonList(new AuthenticationMeansScope(AuthenticationMeansScope.Type.CLIENT, null, ServiceType.AIS)),
                Collections.singletonList(new ClientSiteDTO(siteId, siteName, Collections.singletonList(AccountType.CURRENT_ACCOUNT), LoginType.URL, ConnectionType.DIRECT_CONNECTION, new ClientSiteDTO.Services(new ClientSiteDTO.Services.AIS(new ClientSiteDTO.Services.Onboarded(Collections.emptySet(), true), true, false, Collections.emptySortedSet()),
                        null), null, Collections.singletonList("tag"), false, false, false, null, null, null, "/path/to/icons"))));
        when(clientSiteService.listAllClientSitesForInternalUsage(clientToken, true)).thenReturn(clientSites);

        // Hit the controller and verify
        this.mockMvc.perform(get(uri)
                        .headers(headers)
                        .contentType(MediaType.APPLICATION_JSON)
                        .queryParam("only-available", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].provider").value("YOLT_PROVIDER"))
                .andExpect(jsonPath("$[0].registeredAuthenticationMeans").isArray())
                .andExpect(jsonPath("$[0].registeredAuthenticationMeans[0].type").value("CLIENT"))
                .andExpect(jsonPath("$[0].sites[0].id").value(siteId.toString()))
                .andExpect(jsonPath("$[0].sites[0].loginType").value("URL"))
                .andExpect(jsonPath("$[0].sites[0].connectionType").value("DIRECT_CONNECTION"))
                .andExpect(jsonPath("$[0].sites[0].name").value(siteName))
                .andExpect(jsonPath("$[0].sites[0].tags[0]").value("tag"))
                .andExpect(jsonPath("$[0].sites[0].services.pis").doesNotExist())
                .andExpect(jsonPath("$[0].sites[0].services.ais").exists())
                .andExpect(jsonPath("$[0].sites[0].services.ais.onboarded.client").value(true))
                .andExpect(jsonPath("$[0].sites[0].services.ais.onboarded.redirectUrlIds").isArray())
                .andExpect(jsonPath("$[0].sites[0].services.ais.onboarded.redirectUrlIds").isEmpty());

        // verify mock
        verify(clientSiteService).listAllClientSitesForInternalUsage(clientToken, true);
    }

    @Test
    void testEnableSiteOk() throws Exception {
        // Prep the params
        UUID siteId = UUID.randomUUID();
        String uri = String.format("/client-sites/%s", siteId);

        // Hit the controller and verify
        this.mockMvc.perform(post(uri)
                        .content("{}")
                        .headers(headers)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // verify mock
        verify(clientSiteService).enableSite(clientToken, siteId);
    }

    @Test
    void testEnableSiteWithProviderNotEnabled() throws Exception {
        // Prep the params
        UUID siteId = UUID.randomUUID();
        String uri = String.format("/client-sites/%s", siteId);
        doThrow(new ProviderNotEnabledException("test")).when(clientSiteService).enableSite(clientToken, siteId);

        // Hit the controller and verify
        this.mockMvc.perform(post(uri)
                        .content("{}")
                        .headers(headers)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        // verify mock
        verify(clientSiteService).enableSite(clientToken, siteId);
    }

    @Test
    void testDisableSiteOk() throws Exception {
        // Prep the params
        UUID siteId = UUID.randomUUID();
        String uri = String.format("/client-sites/%s", siteId);

        // Hit the controller and verify
        this.mockMvc.perform(delete(uri)
                        .content("{}")
                        .headers(headers)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // verify mock
        verify(clientSiteService).disableSite(clientToken, siteId);
    }

    @Test
    void testDisableSiteWhenSiteNotEnabled() throws Exception {
        // Prep the params
        UUID siteId = UUID.randomUUID();
        String uri = String.format("/client-sites/%s", siteId);
        doThrow(new ClientSiteConfigurationException("test")).when(clientSiteService).disableSite(clientToken, siteId);

        // Hit the controller and verify
        this.mockMvc.perform(delete(uri)
                        .content("{}")
                        .headers(headers)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        // verify mock
        verify(clientSiteService).disableSite(clientToken, siteId);
    }

    @Test
    void testSetExperimentalSiteOk() throws Exception {
        // Prep the params
        UUID siteId = UUID.randomUUID();
        String uri = String.format("/client-sites/%s/experimental/%s", siteId, true);

        // Hit the controller and verify
        this.mockMvc.perform(post(uri)
                        .content("{}")
                        .headers(headers)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // verify mock
        verify(clientSiteService).setExperimentalVersionForSite(clientToken, siteId, true);
    }

    @Test
    void testGetSitesV2() throws Exception {
        // Prep the params
        UUID redirectUrlId = UUID.randomUUID();
        UUID siteId1 = UUID.randomUUID();
        String siteName1 = "test site 1";
        UUID siteId2 = UUID.randomUUID();
        String siteName2 = "test site 2";
        String uri = String.format("/v2/sites?redirectUrlId=%s", redirectUrlId);

        // prep mock
        Site site1 = SiteCreatorUtil.createTestSite(siteId1, siteName1, "YODLEE", CURRENT_CREDIT_SAVINGS, List.of(CountryCode.GB), AIS_WITH_REDIRECT_STEPS);
        ClientSite clientSite1 = new ClientSite(
                site1,
                true, false, true,
                Collections.emptySet(),
                List.of(
                        new AuthenticationMeansScope(AuthenticationMeansScope.Type.CLIENT, null, ServiceType.AIS),
                        new AuthenticationMeansScope(AuthenticationMeansScope.Type.REDIRECT_URL, Collections.singletonList(redirectUrlId), ServiceType.AIS))
        );

        Site site2 = SiteCreatorUtil.createTestSite(siteId2, siteName2, "YODLEE", CURRENT_CREDIT_SAVINGS, List.of(CountryCode.GB), AIS_WITH_REDIRECT_STEPS);

        ClientSite clientSite2 = new ClientSite(
                site2,
                true, false, true,
                Collections.emptySet(),
                Collections.singletonList(new AuthenticationMeansScope(AuthenticationMeansScope.Type.CLIENT, null, ServiceType.AIS)));

        when(clientSiteService.listEnabledClientSites(eq(clientToken), eq(redirectUrlId), anyList())).thenReturn(List.of(clientSite1, clientSite2));

        // Hit the controller and verify
        this.mockMvc.perform(get(uri)
                        .content("{}")
                        .headers(headers)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(siteId1.toString()))
                .andExpect(jsonPath("$[0].name").value(siteName1))
                .andExpect(jsonPath("$[0].services.ais.onboarded.redirectUrlIds").isArray())
                .andExpect(jsonPath("$[0].services.ais.onboarded.redirectUrlIds", hasItem(redirectUrlId.toString())))
                .andExpect(jsonPath("$[0].services.ais.onboarded.client").value(true))
                .andExpect(jsonPath("$[0].services.ais.hasRedirectSteps").value(true))
                .andExpect(jsonPath("$[0].services.ais.hasFormSteps").value(false))
                .andExpect(jsonPath("$[0].services.pis").doesNotExist())
                .andExpect(jsonPath("$[1].id").value(siteId2.toString()))
                .andExpect(jsonPath("$[1].name").value(siteName2));

        // verify mock
        verify(clientSiteService).listEnabledClientSites(eq(clientToken), eq(redirectUrlId), anyList());
    }

    @Test
    void testGetPaymentInformationForV2List() throws Exception {
        UUID redirectUrlId = UUID.randomUUID();
        UUID siteId = UUID.fromString("33aca8b9-281a-4259-8492-1b37706af6db"); // Yolt Provider site id
        String siteName = "payment test site";

        String uri = String.format("/v2/sites?redirectUrlId=%s", redirectUrlId);

        // prep mock
        Site site1 = SiteCreatorUtil.create(siteId, siteName, "YOLT_PROVIDER", CURRENT_CREDIT_SAVINGS, List.of(CountryCode.GB), Map.of(ServiceType.PIS, List.of(LoginRequirement.REDIRECT)), null, null, null, null,
                new SepaSinglePaymentDetails(true, PaymentType.SEPA_SINGLE, true, DynamicFields.builder().creditorAgentBic(new DynamicFields.CreditorAgentBic(true)).creditorAgentName(new DynamicFields.CreditorAgentName(false)).build()),
                null, new SepaPeriodicPaymentDetails(List.of(Frequency.WEEKLY, Frequency.MONTHLY), true, PaymentType.SEPA_PERIODIC, true, null), null, null, null);

        ClientSite clientSite1 = new ClientSite(
                site1,
                true, false, true,
                Collections.emptySet(),
                List.of(
                        new AuthenticationMeansScope(AuthenticationMeansScope.Type.CLIENT, null, ServiceType.PIS),
                        new AuthenticationMeansScope(AuthenticationMeansScope.Type.REDIRECT_URL, Collections.singletonList(redirectUrlId), ServiceType.PIS)
                )
        );

        when(clientSiteService.listEnabledClientSites(eq(clientToken), eq(redirectUrlId), anyList())).thenReturn(List.of(clientSite1));

        // Hit the controller and verify
        this.mockMvc.perform(get(uri)
                        .content("{}")
                        .headers(headers)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(siteId.toString()))
                .andExpect(jsonPath("$[0].name").value(siteName))
                .andExpect(jsonPath("$[0].services.pis.sepaSingle").isNotEmpty())
                .andExpect(jsonPath("$[0].services.pis.sepaSingle.supported").value(true))
                .andExpect(jsonPath("$[0].services.pis.sepaSingle.dynamicFields.creditorAgentBic.required").value(true))
                .andExpect(jsonPath("$[0].services.pis.sepaSingle.dynamicFields.creditorAgentName.required").value(false))
                .andExpect(jsonPath("$[0].services.pis.sepaPeriodic").isNotEmpty())
                .andExpect(jsonPath("$[0].services.pis.sepaPeriodic.supported").value(true))
                .andExpect(jsonPath("$[0].services.pis.sepaPeriodic.supportedFrequencies", hasSize(2)))
                .andExpect(jsonPath("$[0].services.pis.sepaPeriodic.supportedFrequencies", hasItem("WEEKLY")))
                .andExpect(jsonPath("$[0].services.pis.sepaPeriodic.supportedFrequencies", hasItem("MONTHLY")))
                .andDo(print());
    }
}

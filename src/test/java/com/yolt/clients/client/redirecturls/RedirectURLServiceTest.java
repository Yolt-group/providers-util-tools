package com.yolt.clients.client.redirecturls;

import com.yolt.clients.authmeans.ClientOnboardedProviderRepository;
import com.yolt.clients.client.redirecturls.dto.RedirectURLDTO;
import com.yolt.clients.client.redirecturls.exceptions.RedirectURLAlreadyExistsException;
import com.yolt.clients.client.redirecturls.exceptions.RedirectURLCardinalityException;
import com.yolt.clients.client.redirecturls.repository.RedirectURL;
import com.yolt.clients.client.redirecturls.repository.RedirectURLRepository;
import com.yolt.clients.clientgroup.certificatemanagement.CertificateService;
import com.yolt.clients.clientgroup.certificatemanagement.dto.CertificateDTO;
import com.yolt.clients.clientgroup.certificatemanagement.providers.ProvidersService;
import com.yolt.clients.clientgroup.certificatemanagement.providers.YoltProvider;
import com.yolt.clients.clientgroup.certificatemanagement.yoltbank.YoltbankService;
import com.yolt.clients.clientgroup.certificatemanagement.yoltbank.dto.CertificateSigningResponse;
import com.yolt.clients.clientsite.ClientSiteService;
import nl.ing.lovebird.clienttokens.ClientToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static com.yolt.clients.client.redirecturls.RedirectURLMessageType.CLIENT_REDIRECT_URL_CREATED;
import static com.yolt.clients.clientgroup.certificatemanagement.dto.CertificateType.*;
import static com.yolt.clients.clientgroup.certificatemanagement.dto.CertificateUsageType.SIGNING;
import static com.yolt.clients.clientgroup.certificatemanagement.dto.CertificateUsageType.TRANSPORT;
import static java.util.Collections.emptySet;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedirectURLServiceTest {

    private static final UUID CLIENT_ID = randomUUID();
    private static final UUID CLIENT_GROUP_ID = randomUUID();
    private static final List<RedirectURL> REDIRECT_URLS = List.of(
            new RedirectURL(CLIENT_ID, randomUUID(), "https://junit.test_01"),
            new RedirectURL(CLIENT_ID, randomUUID(), "https://junit.test_02"),
            new RedirectURL(CLIENT_ID, randomUUID(), "https://junit.test_03"),
            new RedirectURL(CLIENT_ID, randomUUID(), "https://junit.test_04"),
            new RedirectURL(CLIENT_ID, randomUUID(), "https://junit.test_05")
    );

    // <editor-fold desc="Certificates used in tests" defaultstate="collapsed">
    private static final String YOLTBANK_ROOT_CERTIFICATE = """
            -----BEGIN CERTIFICATE-----
            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
            sNwt1HjY5aKZ7tg=
            -----END CERTIFICATE-----
            """;

    private static final String CERTIFICATE_CHAIN_TRANSPORT = YOLTBANK_ROOT_CERTIFICATE + "\n" + """
            -----BEGIN CERTIFICATE-----
            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
            -----END CERTIFICATE-----
            """;

    private static final String CERTIFICATE_CHAIN_SIGNING = YOLTBANK_ROOT_CERTIFICATE + "\n" + """
            -----BEGIN CERTIFICATE-----
            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
            sNwt1HjY5aKZ7tg=
            -----END CERTIFICATE-----
            """;

    private static final String CERTIFICATE_PEM_TRANSPORT = """
            -----BEGIN CERTIFICATE-----
            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
            sNwt1HjY5aKZ7tg=
            -----END CERTIFICATE-----
            """;

    private static final String CERTIFICATE_PEM_SIGNING = """
            -----BEGIN CERTIFICATE-----
            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
            sNwt1HjY5aKZ7tg=
            -----END CERTIFICATE-----
            """;
    // </editor-fold>

    @Mock
    private RedirectURLRepository redirectURLRepository;
    @Mock
    private ClientOnboardedProviderRepository clientOnboardedProviderRepository;
    @Mock
    private RedirectURLProducer redirectURLProducer;
    @Mock
    private CertificateService certificateService;
    @Mock
    private ProvidersService providersService;
    @Mock
    private YoltbankService yoltbankService;
    @Mock
    private ClientSiteService clientSiteService;
    @Mock
    private ClientToken clientToken;

    private RedirectURLService serviceWithoutAutoRegistration;
    private RedirectURLService serviceWithAutoRegistration;

    @BeforeEach
    void setUp() {
        when(clientToken.getClientIdClaim()).thenReturn(CLIENT_ID);
        serviceWithoutAutoRegistration = new RedirectURLService(
                redirectURLRepository,
                clientOnboardedProviderRepository,
                redirectURLProducer,
                certificateService,
                providersService,
                yoltbankService,
                clientSiteService,
                false
        );
        serviceWithAutoRegistration = new RedirectURLService(
                redirectURLRepository,
                clientOnboardedProviderRepository,
                redirectURLProducer,
                certificateService,
                providersService,
                yoltbankService,
                clientSiteService,
                true
        );
    }

    @Test
    void shouldCreateNewRedirectURL_NoAutoRegistration() {
        UUID redirectUrlId = randomUUID();
        String redirectURL = "https://junit.test_new";

        when(redirectURLRepository.findAllByClientId(CLIENT_ID)).thenReturn(REDIRECT_URLS);
        when(redirectURLRepository.save(any(RedirectURL.class))).thenAnswer(input -> input.getArguments()[0]);

        RedirectURLDTO result = serviceWithoutAutoRegistration.create(clientToken, redirectUrlId, redirectURL);

        verify(redirectURLRepository, times(1)).findAllByClientId(CLIENT_ID);
        verify(redirectURLRepository, times(1)).save(any(RedirectURL.class));
        verify(redirectURLProducer, times(1)).sendMessage(eq(clientToken), any(RedirectURLDTO.class), eq(CLIENT_REDIRECT_URL_CREATED));

        assertThat(result).isNotNull()
                .hasFieldOrPropertyWithValue("redirectURLId", redirectUrlId)
                .hasFieldOrPropertyWithValue("redirectURL", redirectURL);
    }

    @Test
    void shouldCreateNewRedirectURL_NoAutoRegistration_ExistingId() {
        String redirectURL = "https://junit.test_new";

        when(redirectURLRepository.findAllByClientId(CLIENT_ID)).thenReturn(REDIRECT_URLS);

        assertThrows(
                RedirectURLAlreadyExistsException.class,
                () -> serviceWithoutAutoRegistration.create(clientToken, REDIRECT_URLS.get(0).getRedirectURLId(), redirectURL)
        );

        verify(redirectURLRepository, times(1)).findAllByClientId(CLIENT_ID);
        verifyNoMoreInteractions(redirectURLRepository);
        verifyNoInteractions(redirectURLProducer);
    }

    @Test
    void shouldCreateNewRedirectURL_NoAutoRegistration_ExistingUrl() {
        UUID redirectUrlId = randomUUID();

        when(redirectURLRepository.findAllByClientId(CLIENT_ID)).thenReturn(REDIRECT_URLS);

        assertThrows(
                RedirectURLAlreadyExistsException.class,
                () -> serviceWithoutAutoRegistration.create(clientToken, redirectUrlId, REDIRECT_URLS.get(0).getRedirectURL())
        );

        verify(redirectURLRepository, times(1)).findAllByClientId(CLIENT_ID);
        verifyNoMoreInteractions(redirectURLRepository);
        verifyNoInteractions(redirectURLProducer);
    }

    @Test
    void shouldCreateNewRedirectURL_AutoRegistration_TooManyRedirectURLs() {
        UUID redirectUrlId = randomUUID();
        String redirectURL = "https://junit.test_new";

        when(redirectURLRepository.findAllByClientId(CLIENT_ID)).thenReturn(REDIRECT_URLS);

        assertThrows(
                RedirectURLCardinalityException.class,
                () -> serviceWithAutoRegistration.create(clientToken, redirectUrlId, redirectURL)
        );

        verify(redirectURLRepository, times(1)).findAllByClientId(CLIENT_ID);
        verifyNoMoreInteractions(redirectURLRepository);
        verifyNoInteractions(redirectURLProducer);
    }

    @Test
    void shouldCreateNewRedirectURL_AutoRegistration() {
        UUID redirectUrlId = randomUUID();
        String redirectURL = "https://junit.test_new";
        CertificateDTO transportKey = new CertificateDTO("auto-created YOLT_PROVIDER-TRANSPORT-AIS", EIDAS, "kid_1", TRANSPORT, emptySet(), "keyAlg_1", "signature_1", "signingRequest_1", "certChain_1");
        CertificateDTO signingKey = new CertificateDTO("auto-created YOLT_PROVIDER-SIGNING-AIS", OB_LEGACY, "kid_3", SIGNING, emptySet(), "keyAlg_3", "signature_3", "signingRequest_3", "certChain_3");
        CertificateSigningResponse sepaPIS = new CertificateSigningResponse("kid_7", "signedCert_3");

        when(clientToken.getClientGroupIdClaim()).thenReturn(CLIENT_GROUP_ID);
        when(redirectURLRepository.findAllByClientId(CLIENT_ID)).thenReturn(REDIRECT_URLS.subList(0, 2));
        when(redirectURLRepository.save(any(RedirectURL.class))).thenAnswer(input -> input.getArguments()[0]);
        when(certificateService.getCertificatesByCertificateType(CLIENT_GROUP_ID, EIDAS)).thenReturn(List.of(
                transportKey
        ));
        when(certificateService.getCertificatesByCertificateType(CLIENT_GROUP_ID, OB_ETSI)).thenReturn(List.of(
                new CertificateDTO("cert_2", OB_ETSI, "kid_2", null, emptySet(), "keyAlg_2", "signature_2", "signingRequest_2", "certChain_2")
        ));
        when(certificateService.getCertificatesByCertificateType(CLIENT_GROUP_ID, OB_LEGACY)).thenReturn(List.of(
                signingKey
        ));
        when(certificateService.getCertificatesByCertificateType(CLIENT_GROUP_ID, OTHER)).thenReturn(List.of(
                new CertificateDTO("cert_4", OTHER, "kid_4", null, emptySet(), "keyAlg_4", "signature_4", "signingRequest_4", "certChain_4")
        ));
        when(yoltbankService.signCSR("signingRequest_1")).thenReturn(CERTIFICATE_CHAIN_TRANSPORT);
        when(yoltbankService.signCSR("signingRequest_3")).thenReturn(CERTIFICATE_CHAIN_SIGNING);
        when(yoltbankService.signSepaPIS("signingRequest_3")).thenReturn(sepaPIS);

        RedirectURLDTO result = serviceWithAutoRegistration.create(clientToken, redirectUrlId, redirectURL);

        verify(redirectURLRepository, times(1)).findAllByClientId(CLIENT_ID);
        verify(redirectURLRepository, times(1)).save(any(RedirectURL.class));
        verify(redirectURLProducer, times(1)).sendMessage(eq(clientToken), any(RedirectURLDTO.class), eq(CLIENT_REDIRECT_URL_CREATED));
        verify(providersService, times(1)).uploadAISAuthenticationMeansForTestBank(clientToken, transportKey, CERTIFICATE_PEM_TRANSPORT, signingKey, CERTIFICATE_PEM_SIGNING, redirectUrlId);
        verify(providersService, times(1)).uploadPISAuthenticationMeansForTestBank(clientToken, signingKey, sepaPIS, redirectUrlId);
        verify(clientSiteService, times(1)).markSiteAvailable(clientToken, YoltProvider.YOLT_TEST_BANK_SITE_ID);
        verify(clientSiteService, times(1)).enableSite(clientToken, YoltProvider.YOLT_TEST_BANK_SITE_ID);

        assertThat(result).isNotNull()
                .hasFieldOrPropertyWithValue("redirectURLId", redirectUrlId)
                .hasFieldOrPropertyWithValue("redirectURL", redirectURL);
    }

    @Test
    void shouldCreateNewRedirectURL_AutoRegistration_MissingTransportKey() {
        UUID redirectUrlId = randomUUID();
        String redirectURL = "https://junit.test_new";
        CertificateDTO signingKey = new CertificateDTO("auto-created YOLT_PROVIDER-SIGNING-AIS", OB_LEGACY, "kid_3", SIGNING, emptySet(), "keyAlg_3", "signature_3", "signingRequest_3", "certChain_3");

        when(clientToken.getClientGroupIdClaim()).thenReturn(CLIENT_GROUP_ID);
        when(redirectURLRepository.findAllByClientId(CLIENT_ID)).thenReturn(REDIRECT_URLS.subList(0, 2));
        when(redirectURLRepository.save(any(RedirectURL.class))).thenAnswer(input -> input.getArguments()[0]);
        when(certificateService.getCertificatesByCertificateType(CLIENT_GROUP_ID, EIDAS)).thenReturn(List.of(
                new CertificateDTO("cert1", EIDAS, "kid_1", null, emptySet(), "keyAlg_1", "signature_1", "signingRequest_1", "certChain_1")
        ));
        when(certificateService.getCertificatesByCertificateType(CLIENT_GROUP_ID, OB_ETSI)).thenReturn(List.of(
                new CertificateDTO("cert_2", OB_ETSI, "kid_2", null, emptySet(), "keyAlg_2", "signature_2", "signingRequest_2", "certChain_2")
        ));
        when(certificateService.getCertificatesByCertificateType(CLIENT_GROUP_ID, OB_LEGACY)).thenReturn(List.of(
                signingKey
        ));
        when(certificateService.getCertificatesByCertificateType(CLIENT_GROUP_ID, OTHER)).thenReturn(List.of(
                new CertificateDTO("cert_4", OTHER, "kid_4", null, emptySet(), "keyAlg_4", "signature_4", "signingRequest_4", "certChain_4")
        ));

        assertThrows(
                RuntimeException.class,
                () -> serviceWithAutoRegistration.create(clientToken, redirectUrlId, redirectURL),
                "Missing transport or signing key for client %s"
        );

        verify(redirectURLRepository, times(1)).findAllByClientId(CLIENT_ID);
        verify(redirectURLRepository, times(1)).save(any(RedirectURL.class));
        verify(redirectURLProducer, times(1)).sendMessage(eq(clientToken), any(RedirectURLDTO.class), eq(CLIENT_REDIRECT_URL_CREATED));

        verifyNoInteractions(providersService, clientSiteService);
    }

}

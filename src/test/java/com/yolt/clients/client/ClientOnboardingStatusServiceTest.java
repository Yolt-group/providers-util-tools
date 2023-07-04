package com.yolt.clients.client;

import com.yolt.clients.client.dto.ClientOnboardingStatusDTO.Status;
import com.yolt.clients.client.ipallowlist.IPAllowListService;
import com.yolt.clients.client.mtlscertificates.ClientMTLSCertificateService;
import com.yolt.clients.client.mtlsdn.DistinguishedNameService;
import com.yolt.clients.client.outboundallowlist.OutboundAllowListService;
import com.yolt.clients.client.redirecturls.RedirectURLService;
import com.yolt.clients.client.requesttokenpublickeys.RequestTokenPublicKeyService;
import com.yolt.clients.client.webhooks.WebhookService;
import nl.ing.lovebird.clienttokens.ClientToken;
import org.jose4j.jwt.JwtClaims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static com.yolt.clients.client.dto.ClientOnboardingStatusDTO.Status.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientOnboardingStatusServiceTest {


    @Mock RedirectURLService redirectURLService;
    @Mock WebhookService webhookService;
    @Mock IPAllowListService ipAllowListService;
    @Mock RequestTokenPublicKeyService requestTokenPublicKeyService;
    @Mock OutboundAllowListService outboundAllowListService;
    @Mock ClientMTLSCertificateService clientMTLSCertificateService;
    @Mock DistinguishedNameService distinguishedNameService;

    @InjectMocks ClientOnboardingStatusService service;

    private UUID clientId;

    @BeforeEach
    void setUp() throws Exception {
        clientId = UUID.randomUUID();
    }

    @Test
    void getMTLSCertificatesStatus_NotConfigured() throws Exception {
        // GIVEN no TLS cert

        // WHEN
        Status result = service.getMTLSCertificatesStatus(clientId);

        // THEN
        assertThat(result).isEqualTo(NOT_CONFIGURED);
    }

    @Test
    void getMTLSCertificatesStatus_Configured() throws Exception {
        // GIVEN
        when(clientMTLSCertificateService.hasClientMTLSCertificate(any())).thenReturn(true);

        // WHEN
        Status result = service.getMTLSCertificatesStatus(clientId);

        // THEN
        assertThat(result).isEqualTo(CONFIGURED);
    }


    @Test
    void getDistinguishedNameStatus_NotConfigured() throws Exception {
        // GIVEN no TLS cert

        // WHEN
        Status result = service.getDistinguishedNameStatus(clientId);

        // THEN
        assertThat(result).isEqualTo(NOT_CONFIGURED);
    }

    @Test
    void getDistinguishedNameStatus_Configured() throws Exception {
        // GIVEN
        when(distinguishedNameService.hasActiveCertificates(any())).thenReturn(true);

        // WHEN
        Status result = service.getDistinguishedNameStatus(clientId);

        // THEN
        assertThat(result).isEqualTo(CONFIGURED);
    }

    @Test
    void getDistinguishedNameStatus_Pending() throws Exception {
        // GIVEN
        when(distinguishedNameService.hasActiveCertificates(any())).thenReturn(false);
        when(distinguishedNameService.hasPendingCertificates(any())).thenReturn(true);

        // WHEN
        Status result = service.getDistinguishedNameStatus(clientId);

        // THEN
        assertThat(result).isEqualTo(PENDING);
    }
}
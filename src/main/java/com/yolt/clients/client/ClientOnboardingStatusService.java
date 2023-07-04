package com.yolt.clients.client;

import com.yolt.clients.client.dto.ClientOnboardingStatusDTO;
import com.yolt.clients.client.dto.ClientOnboardingStatusDTO.Status;
import com.yolt.clients.client.ipallowlist.IPAllowListService;
import com.yolt.clients.client.mtlscertificates.ClientMTLSCertificateService;
import com.yolt.clients.client.mtlsdn.DistinguishedNameService;
import com.yolt.clients.client.outboundallowlist.OutboundAllowListService;
import com.yolt.clients.client.redirecturls.RedirectURLService;
import com.yolt.clients.client.requesttokenpublickeys.RequestTokenPublicKeyService;
import com.yolt.clients.client.webhooks.WebhookService;
import lombok.RequiredArgsConstructor;
import nl.ing.lovebird.clienttokens.ClientToken;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static com.yolt.clients.client.dto.ClientOnboardingStatusDTO.Status.*;

@Service
@RequiredArgsConstructor
public class ClientOnboardingStatusService {

    private final RedirectURLService redirectURLService;
    private final WebhookService webhookService;
    private final IPAllowListService ipAllowListService;
    private final RequestTokenPublicKeyService requestTokenPublicKeyService;
    private final OutboundAllowListService outboundAllowListService;
    private final ClientMTLSCertificateService clientMTLSCertificateService;
    private final DistinguishedNameService distinguishedNameService;

    public ClientOnboardingStatusDTO getClientOnboardingStatus(ClientToken clientToken) {
        UUID clientId = clientToken.getClientIdClaim();
        return ClientOnboardingStatusDTO.builder()
                .distinguishedNameConfigured(getDistinguishedNameStatus(clientId))
                .mutualTlsCertificatesConfigured(getMTLSCertificatesStatus(clientId))
                .redirectUrlConfigured(redirectURLService.hasRedirectUrlsConfigured(clientToken) ? CONFIGURED : NOT_CONFIGURED)
                .webhookUrlConfigured(webhookService.hasWebhooksConfigured(clientToken) ? CONFIGURED : NOT_CONFIGURED)
                .ipAllowListConfigured(getIPAllowListStatus(clientToken))
                .requestTokenConfigured(requestTokenPublicKeyService.hasRequestTokenPublicKeysConfigured(clientToken) ? CONFIGURED : NOT_CONFIGURED)
                .webhookDomainAllowListConfigured(getWebhookDomainAllowListStatus(clientToken))
                .build();
    }

    public Status getMTLSCertificatesStatus(UUID clientId) {
        if (clientMTLSCertificateService.hasClientMTLSCertificate(clientId)) {
            return CONFIGURED;
        } else {
            return NOT_CONFIGURED;
        }
    }

    public Status getDistinguishedNameStatus(UUID clientId) {
        if (distinguishedNameService.hasActiveCertificates(clientId)) {
            return CONFIGURED;
        } else if (distinguishedNameService.hasPendingCertificates(clientId)) {
            return PENDING;
        } else {
            return NOT_CONFIGURED;
        }
    }

    public ClientOnboardingStatusDTO.Status getIPAllowListStatus(ClientToken clientToken) {
        if (ipAllowListService.hasAddedIPAllowListItems(clientToken)) {
            return CONFIGURED;
        } else if (ipAllowListService.hasPendingAdditionIPAllowListItems(clientToken)) {
            return PENDING;
        } else {
            return NOT_CONFIGURED;
        }
    }

    public ClientOnboardingStatusDTO.Status getWebhookDomainAllowListStatus(ClientToken clientToken) {
        if (outboundAllowListService.hasAddedWebhookDomainAllowListItems(clientToken)) {
            return CONFIGURED;
        } else if (outboundAllowListService.hasPendingAdditionWebhookDomainAllowListItems(clientToken)) {
            return PENDING;
        } else {
            return NOT_CONFIGURED;
        }
    }
}

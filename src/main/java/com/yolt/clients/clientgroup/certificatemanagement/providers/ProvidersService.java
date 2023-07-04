package com.yolt.clients.clientgroup.certificatemanagement.providers;

import com.yolt.clients.clientgroup.certificatemanagement.dto.CertificateDTO;
import com.yolt.clients.clientgroup.certificatemanagement.providers.dto.ProviderAuthenticationMeansBatch;
import com.yolt.clients.clientgroup.certificatemanagement.providers.dto.ProviderInfo;
import com.yolt.clients.clientgroup.certificatemanagement.providers.exceptions.ProviderInfoFetchException;
import com.yolt.clients.clientgroup.certificatemanagement.yoltbank.dto.CertificateSigningResponse;
import nl.ing.lovebird.clienttokens.ClientToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.yolt.clients.clientgroup.certificatemanagement.providers.YoltProvider.*;
import static java.util.Collections.singleton;
import static nl.ing.lovebird.clienttokens.constants.ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME;
import static nl.ing.lovebird.providerdomain.ServiceType.AIS;
import static nl.ing.lovebird.providerdomain.ServiceType.PIS;

@Service
public class ProvidersService {
    private final RestTemplate restTemplate;

    public ProvidersService(@Value("${service.providers.url}") String providersUrl, RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.rootUri(providersUrl).build();
    }

    public ProviderInfo getProviderInfo(String providerKey) {
        try {
            ResponseEntity<ProviderInfo> responseEntity = restTemplate.getForEntity("/provider-info/{providerKey}", ProviderInfo.class, providerKey);
            return Optional.ofNullable(responseEntity.getBody()).orElseThrow(() -> new ProviderInfoFetchException("No response body"));
        } catch (HttpStatusCodeException e) {
            String error = String.format("Error getting provider info for providerKey: %s, status code: %d, body: %s",
                    providerKey, e.getRawStatusCode(), e.getResponseBodyAsString());
            throw new ProviderInfoFetchException(error);
        }
    }

    public void uploadAISAuthenticationMeansForTestBank(
            ClientToken clientToken,
            CertificateDTO transportKey,
            String transportSignedClientCertificate,
            CertificateDTO signingKey,
            String signingSignedCertificate,
            UUID redirectUrlId
    ) {
        ProviderAuthenticationMeansBatch providerAuthenticationMeansBatch = new ProviderAuthenticationMeansBatch(
                YOLT_PROVIDER,
                singleton(redirectUrlId),
                Set.of(AIS),
                makeAISAuthenticationMeansForYoltProvider(
                        transportKey.getKid(),
                        transportSignedClientCertificate,
                        signingKey.getKid(),
                        signingSignedCertificate
                ),
                false
        );

        HttpEntity<ProviderAuthenticationMeansBatch> httpRequest = toEntity(clientToken, providerAuthenticationMeansBatch);
        restTemplate.postForEntity("/clients/{clientId}/provider-authentication-means/batch", httpRequest, Void.class, clientToken.getClientIdClaim());
    }

    public void uploadPISAuthenticationMeansForTestBank(
            ClientToken clientToken,
            CertificateDTO signingKey,
            CertificateSigningResponse signingCertificate,
            UUID redirectUrlId
    ) {
        ProviderAuthenticationMeansBatch providerAuthenticationMeansBatch = new ProviderAuthenticationMeansBatch(
                YOLT_PROVIDER,
                singleton(redirectUrlId),
                singleton(PIS),
                makePISAuthenticationMeansForYoltProvider(
                        signingKey.getKid(),
                        signingCertificate.getKid()
                ),
                false
        );

        HttpEntity<ProviderAuthenticationMeansBatch> httpRequest = toEntity(clientToken, providerAuthenticationMeansBatch);
        restTemplate.postForEntity("/clients/{clientId}/provider-authentication-means/batch", httpRequest, Void.class, clientToken.getClientIdClaim());
    }

    private <T> HttpEntity<T> toEntity(ClientToken clientToken, T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(CLIENT_TOKEN_HEADER_NAME, clientToken.getSerialized());
        return new HttpEntity<>(body, headers);
    }

}

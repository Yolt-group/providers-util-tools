package com.yolt.clients.clientgroup.certificatemanagement.crypto;

import com.yolt.clients.clientgroup.certificatemanagement.crypto.dto.*;
import com.yolt.clients.clientgroup.certificatemanagement.dto.CertificateUsageType;
import com.yolt.clients.clientgroup.certificatemanagement.dto.SimpleDistinguishedNameElement;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.clienttokens.AbstractClientToken;
import nl.ing.lovebird.clienttokens.ClientGroupToken;
import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.logging.AuditLogger;
import nl.ing.lovebird.logging.MDCContextCreator;
import nl.ing.lovebird.providerdomain.ServiceType;
import org.bouncycastle.util.encoders.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static nl.ing.lovebird.clienttokens.constants.ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME;

@Service
@Slf4j
public class CryptoService {
    private final RestTemplate restTemplate;

    public CryptoService(@Value("${service.crypto.url}") String cryptoUrl, RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.rootUri(cryptoUrl).build();
    }

    public String createPrivateKey(ClientGroupToken clientGroupToken, String keyAlgorithm, CertificateUsageType usageType) {
        log.info("Creating private key for KeyAlgo: {}, usageType: {}", keyAlgorithm, usageType);

        HttpHeaders headers = getHeaders(clientGroupToken.getSerialized());
        KeyRequirementsDTO keyRequirementsDto = new KeyRequirementsDTO(usageType, keyAlgorithm);
        HttpEntity<KeyRequirementsDTO> request = new HttpEntity<>(keyRequirementsDto, headers);
        ResponseEntity<KidDTO> responseEntity;

        try {
            responseEntity = restTemplate.postForEntity("/key", request, KidDTO.class);
        } catch (HttpStatusCodeException e) {
            throw new IllegalStateException(String.format("Creating the private key failed with http error code: %s, body: %s", e.getStatusCode(), e.getResponseBodyAsString()));
        }

        if (responseEntity.getBody() == null) {
            throw new IllegalStateException("Creating the private key failed, null kid returned.");
        }

        return responseEntity.getBody().getKid();
    }

    public String createCSR(ClientGroupToken clientGroupToken,
                            String kid,
                            CertificateUsageType usageType,
                            String signatureAlgorithm,
                            List<SimpleDistinguishedNameElement> distinguishedNames,
                            boolean addQcStatements,
                            Set<ServiceType> serviceTypes,
                            Set<String> subjectAlternativeNames) {
        log.info("Creating CSR for keyId: {}, signatureAlgo: {}, addQcStatements: {}", kid, signatureAlgorithm, addQcStatements);

        HttpHeaders headers = getHeaders(clientGroupToken.getSerialized());
        CSRRequirementsDTO dto = new CSRRequirementsDTO(usageType, serviceTypes, signatureAlgorithm, distinguishedNames, addQcStatements, subjectAlternativeNames);
        HttpEntity<CSRRequirementsDTO> request = new HttpEntity<>(dto, headers);
        ResponseEntity<CSRDTO> responseEntity;
        try {
            responseEntity = restTemplate.postForEntity("/key/{kid}/csr", request, CSRDTO.class, kid);
        } catch (HttpStatusCodeException e) {
            throw new IllegalStateException(String.format("Creating the CSR failed with http error code: %s, body: %s", e.getStatusCode(), e.getResponseBodyAsString()));
        }

        if (responseEntity.getBody() == null) {
            throw new IllegalStateException("Creating the CSR failed, nothing was returned");
        }

        return responseEntity.getBody().getCertificateSigningRequest();
    }

    public String sign(AbstractClientToken clientToken, CertificateUsageType usageType, String privateKid, SignatureAlgorithm signatureAlgorithm, byte[] bytesToSign) {
        String encodedSigningInput = Base64.toBase64String(bytesToSign);

        SignRequestDTO signRequestDTO = new SignRequestDTO(UUID.fromString(privateKid), signatureAlgorithm, encodedSigningInput, usageType);
        HttpHeaders headers = new HttpHeaders();
        headers.add(CLIENT_TOKEN_HEADER_NAME, clientToken.getSerialized());
        if (clientToken instanceof ClientToken) {
            headers.add(MDCContextCreator.CLIENT_ID_HEADER_NAME, ((ClientToken) clientToken).getClientIdClaim().toString());
        }

        HttpEntity<SignRequestDTO> request = new HttpEntity<>(signRequestDTO, headers);

        try {
            ResponseEntity<SignatureDTO> responseEntity = restTemplate.postForEntity("/sign", request, SignatureDTO.class);
            return responseEntity.getBody().getEncodedSignature();
        } catch (HttpStatusCodeException e) {
            AuditLogger.logError("Failed signing bytes, something went wrong when talking to the crypto service", signRequestDTO, e);
            throw new IllegalArgumentException("Signing on the crypto service went wrong, status code: " + e.getStatusCode().value());
        } catch (RestClientException e) {
            AuditLogger.logError("Failed signing bytes, something went wrong when talking to the crypto service", signRequestDTO, e);
            throw new IllegalArgumentException("Signing on the crypto service went wrong");
        }
    }

    public void deletePrivateKey(ClientGroupToken clientGroupToken, String kid) {
        log.info("Deleting private key for clientGroupId: {}, kid: {}", clientGroupToken.getClientGroupIdClaim(), kid);
        try {
            HttpHeaders headers = getHeaders(clientGroupToken.getSerialized());
            HttpEntity<?> requestEntity = new HttpEntity<>(headers);
            restTemplate.exchange("/key/{kid}", HttpMethod.DELETE, requestEntity, Void.class, kid);
        } catch (HttpStatusCodeException e) {
            throw new IllegalStateException(String.format("Deleting the private key failed with http error code: %s, body: %s", e.getStatusCode(), e.getResponseBodyAsString()));
        }
    }

    private HttpHeaders getHeaders(String serializedClientToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(CLIENT_TOKEN_HEADER_NAME, serializedClientToken);

        return headers;
    }
}

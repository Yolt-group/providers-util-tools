package com.yolt.clients.clientgroup.certificatemanagement.yoltbank;

import com.yolt.clients.clientgroup.certificatemanagement.yoltbank.dto.CertificateSigningResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Service
public class YoltbankService {
    private final RestTemplate yoltbank;

    public YoltbankService(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${service.yoltbank.url}") String yoltbankUrl) {
        yoltbank = restTemplateBuilder.rootUri(yoltbankUrl).build();
    }

    public String signCSR(String csr) {
        ResponseEntity<String> responseEntity;

        try {
            responseEntity = yoltbank.postForEntity("/csr", csr, String.class);
        } catch (HttpStatusCodeException e) {
            throw new IllegalStateException(String.format("Signing the csr failed with http error code: %s, body: %s", e.getStatusCode(), e.getResponseBodyAsString()));
        }

        if (!responseEntity.hasBody()) {
            throw new IllegalStateException("Creating the PEM chain failed, no body returned");
        }

        return responseEntity.getBody();
    }

    public CertificateSigningResponse signSepaPIS(String certificateSigningRequest) {
        return yoltbank.postForEntity("/yolt-test-bank/pis/sepa/csr", certificateSigningRequest, CertificateSigningResponse.class).getBody();
    }
}

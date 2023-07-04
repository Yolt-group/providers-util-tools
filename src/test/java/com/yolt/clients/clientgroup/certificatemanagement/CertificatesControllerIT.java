package com.yolt.clients.clientgroup.certificatemanagement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.yolt.clients.IntegrationTest;
import com.yolt.clients.clientgroup.ClientGroupRepository;
import com.yolt.clients.clientgroup.certificatemanagement.dto.CertificateDTO;
import com.yolt.clients.clientgroup.certificatemanagement.dto.CertificateType;
import com.yolt.clients.clientgroup.certificatemanagement.dto.CertificateUsageType;
import com.yolt.clients.clientgroup.certificatemanagement.dto.serviceinfo.KeyMaterialRequirements;
import com.yolt.clients.clientgroup.certificatemanagement.dto.serviceinfo.KeyRequirementsWrapper;
import com.yolt.clients.clientgroup.certificatemanagement.dto.serviceinfo.ServiceInfo;
import com.yolt.clients.clientgroup.certificatemanagement.eidas.EIDASHelper;
import com.yolt.clients.clientgroup.certificatemanagement.providers.dto.ProviderInfo;
import com.yolt.clients.clientgroup.certificatemanagement.repository.Certificate;
import com.yolt.clients.clientgroup.certificatemanagement.repository.CertificateRepository;
import com.yolt.clients.clientgroup.dto.ClientGroupDTO;
import com.yolt.clients.model.Client;
import com.yolt.clients.model.ClientGroup;
import nl.ing.lovebird.clienttokens.ClientGroupToken;
import nl.ing.lovebird.clienttokens.constants.ClientTokenConstants;
import nl.ing.lovebird.clienttokens.test.TestClientTokens;
import nl.ing.lovebird.errorhandling.ErrorDTO;
import nl.ing.lovebird.providerdomain.ServiceType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class CertificatesControllerIT {
    private UUID clientGroupId;
    private ClientGroupToken clientGroupToken;

    @Autowired
    private TestClientTokens testClientTokens;

    @Autowired
    private ClientGroupRepository clientGroupRepository;

    @Autowired
    private CertificateRepository certificateRepository;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private WireMockServer wireMockServer;

    @Autowired
    private ObjectMapper objectMapper;


    private CertificateType certificateType;
    private String keyId;

    private HttpHeaders getHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME, clientGroupToken.getSerialized());
        return headers;
    }

    @BeforeEach
    void setUp() {
        clientGroupId = UUID.randomUUID();
        clientGroupToken = testClientTokens.createClientGroupToken(clientGroupId,
                claims -> claims.setClaim(ClientTokenConstants.EXTRA_CLAIM_ISSUED_FOR, "assistance-portal-yts"));
        certificateType = CertificateType.EIDAS;
        keyId = UUID.randomUUID().toString();
    }

    @AfterEach
    void cleanUp() {
        try {
            certificateRepository.deleteById(new Certificate.CertificateId(certificateType, keyId, clientGroupId));
        } catch (Exception e) {
            // ignored
        }

        try {
            clientGroupRepository.deleteById(clientGroupId);
        } catch (Exception e) {
            // ignored
        }
    }

    @Test
    void findCompatibleCertificates() throws Exception {
        UUID clientId = UUID.randomUUID();
        String providerKey = "EIDAS";
        String certName = "certName";
        Set<ServiceType> serviceTypes = Set.of(ServiceType.AIS);
        CertificateUsageType certificateUsageType = CertificateUsageType.TRANSPORT;
        String keyAlgorithm = "RSA2048";
        String signatureAlgorithm = "SHA256_WITH_RSA";
        Set<String> subjectAlternativeNames = Set.of("SAN1", "SAN2");
        String certificateSigningRequest = "CSR string";
        String signedCertificate = "signed cert";
        Certificate certificate = new Certificate(
                certificateType,
                keyId,
                clientGroupId,
                certName,
                serviceTypes,
                certificateUsageType,
                keyAlgorithm,
                signatureAlgorithm,
                subjectAlternativeNames,
                certificateSigningRequest,
                signedCertificate,
                providerKey
        );

        CertificateDTO certificateDTO = new CertificateDTO(
                certName,
                certificateType,
                keyId,
                certificateUsageType,
                serviceTypes,
                keyAlgorithm,
                signatureAlgorithm,
                certificateSigningRequest,
                signedCertificate
        );
        clientGroupRepository.save(new ClientGroup(clientGroupId, "client group 2",
                Set.of(new Client(clientId,
                        clientGroupId,
                        "Test Client",
                        "NL",
                        false,
                        true,
                        "12.1",
                        120,
                        false,
                        false,
                        false,
                        false,
                        true,
                        true,
                        true,
                        true,
                        false,
                        true,
                        true,
                        true,
                        1L,
                        Collections.emptySet())),
                Collections.emptySet(), Collections.emptySet()));
        certificateRepository.save(certificate);

        ProviderInfo providerInfo = new ProviderInfo(
                "EIDAS",
                EIDASHelper.getServiceInfoMap()
        );

        this.wireMockServer.stubFor(
                WireMock.get("/providers/provider-info/" + providerKey)
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody(objectMapper.writeValueAsBytes(providerInfo)))
        );

        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/internal/client-groups/{clientGroupId}/certificates")
                .uriVariables(Map.of("clientGroupId", clientGroupId))
                .queryParam("providerKey", providerKey)
                .queryParam("serviceTypes", serviceTypes)
                .queryParam("usageType", certificateUsageType);


        HttpEntity<Void> entity = new HttpEntity<>(getHttpHeaders());
        ResponseEntity<List<CertificateDTO>> responseEntity = restTemplate.exchange(builder.toUriString(), HttpMethod.GET, entity, new ParameterizedTypeReference<>() {
        });
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).contains(certificateDTO);
    }

    @Test
    void findCompatibleCertificates_no_key_constraints_found_for_filters() throws Exception {
        String providerKey = "EIDAS";
        Set<ServiceType> serviceTypes = Set.of(ServiceType.IC);
        CertificateUsageType certificateUsageType = CertificateUsageType.TRANSPORT;

        ServiceInfo serviceInfo = new ServiceInfo(
                new KeyRequirementsWrapper(new KeyMaterialRequirements(
                        Set.of("keyAlg1", "keyAlg2"),
                        Set.of("sigAlg1", "sigAlg2"),
                        List.of()
                )),
                new KeyRequirementsWrapper(new KeyMaterialRequirements(
                        Set.of("keyAlg3", "keyAlg2"),
                        Set.of("sigAlg3", "sigAlg2"),
                        List.of()
                ))
        );

        Map<ServiceType, ServiceInfo> services = new HashMap<>();
        services.put(ServiceType.AIS, serviceInfo);
        services.put(ServiceType.PIS, serviceInfo);
        services.put(ServiceType.AS, serviceInfo);

        ProviderInfo providerInfo = new ProviderInfo(
                "EIDAS",
                services
        );

        this.wireMockServer.stubFor(
                WireMock.get("/providers/provider-info/" + providerKey)
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody(objectMapper.writeValueAsBytes(providerInfo)))
        );

        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/internal/client-groups/{clientGroupId}/certificates")
                .uriVariables(Map.of("clientGroupId", clientGroupId))
                .queryParam("providerKey", providerKey)
                .queryParam("serviceTypes", serviceTypes)
                .queryParam("usageType", certificateUsageType);


        HttpEntity<Void> entity = new HttpEntity<>(getHttpHeaders());
        ResponseEntity<List<CertificateDTO>> responseEntity = restTemplate.exchange(builder.toUriString(), HttpMethod.GET, entity, new ParameterizedTypeReference<>() {
        });
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody())
                .isNotNull()
                .isEmpty();
    }

    @Test
    void findCompatibleCertificates_providers_replies_badly() {
        String providerKey = "EIDAS";
        Set<ServiceType> serviceTypes = Set.of(ServiceType.AIS);
        CertificateUsageType certificateUsageType = CertificateUsageType.TRANSPORT;


        this.wireMockServer.stubFor(
                WireMock.get("/providers/provider-info/" + providerKey)
                        .willReturn(aResponse()
                                .withStatus(400)
                                .withBody("error"))
        );

        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/internal/client-groups/{clientGroupId}/certificates")
                .uriVariables(Map.of("clientGroupId", clientGroupId))
                .queryParam("providerKey", providerKey)
                .queryParam("serviceTypes", serviceTypes)
                .queryParam("usageType", certificateUsageType);


        HttpEntity<Void> entity = new HttpEntity<>(getHttpHeaders());
        ResponseEntity<ErrorDTO> responseEntity = restTemplate.exchange(builder.toUriString(), HttpMethod.GET, entity, new ParameterizedTypeReference<>() {
        });
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(responseEntity.getBody()).isNotNull().extracting("code").isEqualTo("CLS1000");
    }

    @Test
    void findCompatibleCertificates_invalid_provider_key() {
        String providerKey = "invalid";
        Set<ServiceType> serviceTypes = Set.of(ServiceType.AIS);
        CertificateUsageType certificateUsageType = CertificateUsageType.TRANSPORT;

        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/internal/client-groups/{clientGroupId}/certificates")
                .uriVariables(Map.of("clientGroupId", clientGroupId))
                .queryParam("providerKey", providerKey)
                .queryParam("serviceTypes", serviceTypes)
                .queryParam("usageType", certificateUsageType);


        HttpEntity<Void> entity = new HttpEntity<>(getHttpHeaders());
        ResponseEntity<ErrorDTO> responseEntity = restTemplate.exchange(builder.toUriString(), HttpMethod.GET, entity, new ParameterizedTypeReference<>() {
        });
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(responseEntity.getBody()).isNotNull().extracting("code").isEqualTo("CLS1008");
    }

    @Test
    void findCompatibleCertificates_provider_key_null() {
        String providerKey = null;
        Set<ServiceType> serviceTypes = Set.of(ServiceType.AIS);
        CertificateUsageType certificateUsageType = CertificateUsageType.TRANSPORT;

        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/internal/client-groups/{clientGroupId}/certificates")
                .uriVariables(Map.of("clientGroupId", clientGroupId))
                .queryParam("providerKey", providerKey)
                .queryParam("serviceTypes", serviceTypes)
                .queryParam("usageType", certificateUsageType);


        HttpEntity<Void> entity = new HttpEntity<>(getHttpHeaders());
        ResponseEntity<ErrorDTO> responseEntity = restTemplate.exchange(builder.toUriString(), HttpMethod.GET, entity, new ParameterizedTypeReference<>() {
        });
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(responseEntity.getBody()).isNotNull().extracting("code").isEqualTo("CLS1008");
    }

    @Test
    void findCompatibleCertificates_provider_key_not_provided() {
        Set<ServiceType> serviceTypes = Set.of(ServiceType.AIS);
        CertificateUsageType certificateUsageType = CertificateUsageType.TRANSPORT;

        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/internal/client-groups/{clientGroupId}/certificates")
                .uriVariables(Map.of("clientGroupId", clientGroupId))
                .queryParam("serviceTypes", serviceTypes)
                .queryParam("usageType", certificateUsageType);


        HttpEntity<List<ClientGroupDTO>> entity = new HttpEntity<>(getHttpHeaders());
        ResponseEntity<ErrorDTO> responseEntity = restTemplate.exchange(builder.toUriString(), HttpMethod.GET, entity, new ParameterizedTypeReference<>() {
        });
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(responseEntity.getBody()).isNotNull().extracting("code").isEqualTo("CLS1006");
    }

    @Test
    void findCompatibleCertificates_empty_service_types() {
        String providerKey = "KEY";
        Set<ServiceType> serviceTypes = Set.of();
        CertificateUsageType certificateUsageType = CertificateUsageType.TRANSPORT;

        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/internal/client-groups/{clientGroupId}/certificates")
                .uriVariables(Map.of("clientGroupId", clientGroupId))
                .queryParam("providerKey", providerKey)
                .queryParam("serviceTypes", serviceTypes)
                .queryParam("usageType", certificateUsageType);


        HttpEntity<Void> entity = new HttpEntity<>(getHttpHeaders());
        ResponseEntity<ErrorDTO> responseEntity = restTemplate.exchange(builder.toUriString(), HttpMethod.GET, entity, new ParameterizedTypeReference<>() {
        });
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(responseEntity.getBody()).isNotNull().extracting("code").isEqualTo("CLS1008");
    }

    @Test
    void findCompatibleCertificates_wrong_service_types() {
        String providerKey = "KEY";
        Set<String> serviceTypes = Set.of("wrong");
        CertificateUsageType certificateUsageType = CertificateUsageType.TRANSPORT;

        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/internal/client-groups/{clientGroupId}/certificates")
                .uriVariables(Map.of("clientGroupId", clientGroupId))
                .queryParam("providerKey", providerKey)
                .queryParam("serviceTypes", serviceTypes)
                .queryParam("usageType", certificateUsageType);


        HttpEntity<Void> entity = new HttpEntity<>(getHttpHeaders());
        ResponseEntity<ErrorDTO> responseEntity = restTemplate.exchange(builder.toUriString(), HttpMethod.GET, entity, new ParameterizedTypeReference<>() {
        });
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(responseEntity.getBody()).isNotNull().extracting("code").isEqualTo("CLS1005");
    }

    @Test
    void findCompatibleCertificates_null_service_types() {
        String providerKey = "KEY";
        Set<ServiceType> serviceTypes = null;
        CertificateUsageType certificateUsageType = CertificateUsageType.TRANSPORT;

        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/internal/client-groups/{clientGroupId}/certificates")
                .uriVariables(Map.of("clientGroupId", clientGroupId))
                .queryParam("providerKey", providerKey)
                .queryParam("serviceTypes", serviceTypes)
                .queryParam("usageType", certificateUsageType);


        HttpEntity<Void> entity = new HttpEntity<>(getHttpHeaders());
        ResponseEntity<ErrorDTO> responseEntity = restTemplate.exchange(builder.toUriString(), HttpMethod.GET, entity, new ParameterizedTypeReference<>() {
        });
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(responseEntity.getBody()).isNotNull().extracting("code").isEqualTo("CLS1008");
    }

    @Test
    void findCompatibleCertificates_no_service_types() {
        String providerKey = "KEY";
        CertificateUsageType certificateUsageType = CertificateUsageType.TRANSPORT;

        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/internal/client-groups/{clientGroupId}/certificates")
                .uriVariables(Map.of("clientGroupId", clientGroupId))
                .queryParam("providerKey", providerKey)
                .queryParam("usageType", certificateUsageType);


        HttpEntity<Void> entity = new HttpEntity<>(getHttpHeaders());
        ResponseEntity<ErrorDTO> responseEntity = restTemplate.exchange(builder.toUriString(), HttpMethod.GET, entity, new ParameterizedTypeReference<>() {
        });
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(responseEntity.getBody()).isNotNull().extracting("code").isEqualTo("CLS1006");
    }

    @Test
    void findCompatibleCertificates_invalid_usage_type() {
        String providerKey = "KEY";
        Set<ServiceType> serviceTypes = Set.of(ServiceType.AIS);

        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/internal/client-groups/{clientGroupId}/certificates")
                .uriVariables(Map.of("clientGroupId", clientGroupId))
                .queryParam("providerKey", providerKey)
                .queryParam("serviceTypes", serviceTypes)
                .queryParam("usageType", "wrong");


        HttpEntity<Void> entity = new HttpEntity<>(getHttpHeaders());
        ResponseEntity<ErrorDTO> responseEntity = restTemplate.exchange(builder.toUriString(), HttpMethod.GET, entity, new ParameterizedTypeReference<>() {
        });
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(responseEntity.getBody()).isNotNull().extracting("code").isEqualTo("CLS1005");
    }

    @Test
    void findCompatibleCertificates_null_usage_type() {
        String providerKey = "KEY";
        Set<ServiceType> serviceTypes = Set.of(ServiceType.AIS);
        CertificateUsageType certificateUsageType = null;

        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/internal/client-groups/{clientGroupId}/certificates")
                .uriVariables(Map.of("clientGroupId", clientGroupId))
                .queryParam("providerKey", providerKey)
                .queryParam("serviceTypes", serviceTypes)
                .queryParam("usageType", certificateUsageType);


        HttpEntity<Void> entity = new HttpEntity<>(getHttpHeaders());
        ResponseEntity<ErrorDTO> responseEntity = restTemplate.exchange(builder.toUriString(), HttpMethod.GET, entity, new ParameterizedTypeReference<>() {
        });
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(responseEntity.getBody()).isNotNull().extracting("code").isEqualTo("CLS1006");
    }

    @Test
    void findCompatibleCertificates_missing_usage_type() {
        String providerKey = "KEY";
        Set<ServiceType> serviceTypes = Set.of(ServiceType.AIS);

        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/internal/client-groups/{clientGroupId}/certificates")
                .uriVariables(Map.of("clientGroupId", clientGroupId))
                .queryParam("providerKey", providerKey)
                .queryParam("serviceTypes", serviceTypes);


        HttpEntity<Void> entity = new HttpEntity<>(getHttpHeaders());
        ResponseEntity<ErrorDTO> responseEntity = restTemplate.exchange(builder.toUriString(), HttpMethod.GET, entity, new ParameterizedTypeReference<>() {
        });
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(responseEntity.getBody()).isNotNull().extracting("code").isEqualTo("CLS1006");
    }

    @Test
    void deleteCertificate() {
        UUID clientId = UUID.randomUUID();
        String providerKey = "EIDAS";
        String certName = "certName";
        Set<ServiceType> serviceTypes = Set.of(ServiceType.AIS);
        CertificateUsageType certificateUsageType = CertificateUsageType.TRANSPORT;
        String keyAlgorithm = "RSA2048";
        String signatureAlgorithm = "SHA256_WITH_RSA";
        Set<String> subjectAlternativeNames = Set.of("SAN1", "SAN2");
        String certificateSigningRequest = "CSR string";
        String signedCertificate = "signed cert";
        Certificate certificate = new Certificate(
                certificateType,
                keyId,
                clientGroupId,
                certName,
                serviceTypes,
                certificateUsageType,
                keyAlgorithm,
                signatureAlgorithm,
                subjectAlternativeNames,
                certificateSigningRequest,
                signedCertificate,
                providerKey
        );

        clientGroupRepository.save(new ClientGroup(clientGroupId, "client group 2",
                Set.of(new Client(clientId,
                        clientGroupId,
                        "Test Client",
                        "NL",
                        false,
                        true,
                        "12.1",
                        120,
                        false,
                        false,
                        false,
                        false,
                        true,
                        true,
                        true,
                        true,
                        false,
                        true,
                        true,
                        true,
                        123456789L,
                        Collections.emptySet())),
                Collections.emptySet(), Collections.emptySet()));
        certificateRepository.save(certificate);

        assertThat(certificateRepository.findCertificateByClientGroupIdAndKid(clientGroupId, keyId)).isNotEmpty();

        this.wireMockServer.stubFor(
                WireMock.delete("/crypto/key/" + keyId)
                        .willReturn(aResponse().withStatus(200))
        );

        HttpEntity<Void> entity = new HttpEntity<>(getHttpHeaders());
        ResponseEntity<Void> responseEntity = restTemplate.exchange("/internal/client-groups/{clientGroupId}/certificates/{certificateId}", HttpMethod.DELETE, entity, new ParameterizedTypeReference<>() {
        }, clientGroupId, keyId);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(certificateRepository.findCertificateByClientGroupIdAndKid(clientGroupId, keyId)).isEmpty();
    }

    @Test
    void deleteCertificate_certificate_not_found() {
        HttpEntity<Void> entity = new HttpEntity<>(getHttpHeaders());
        ResponseEntity<ErrorDTO> responseEntity = restTemplate.exchange("/internal/client-groups/{clientGroupId}/certificates/{certificateId}", HttpMethod.DELETE, entity, new ParameterizedTypeReference<>() {
        }, clientGroupId, keyId);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().getCode()).isEqualTo("CLS012");
    }
}

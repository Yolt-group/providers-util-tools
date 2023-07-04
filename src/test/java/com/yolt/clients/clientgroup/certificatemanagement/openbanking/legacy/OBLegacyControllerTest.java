package com.yolt.clients.clientgroup.certificatemanagement.openbanking.legacy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yolt.clients.clientgroup.certificatemanagement.KeyUtil;
import com.yolt.clients.clientgroup.certificatemanagement.dto.*;
import com.yolt.clients.clientgroup.certificatemanagement.exceptions.CertificateAlreadySignedException;
import com.yolt.clients.clientgroup.certificatemanagement.exceptions.CertificateNotFoundException;
import com.yolt.clients.clientgroup.certificatemanagement.exceptions.CertificateValidationException;
import com.yolt.clients.clientgroup.certificatemanagement.exceptions.NameIsAlreadyUsedException;
import nl.ing.lovebird.clienttokens.ClientGroupToken;
import nl.ing.lovebird.clienttokens.constants.ClientTokenConstants;
import nl.ing.lovebird.clienttokens.test.TestClientTokens;
import nl.ing.lovebird.clienttokens.verification.ClientGroupIdVerificationService;
import nl.ing.lovebird.providerdomain.ServiceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Stream;

import static nl.ing.lovebird.clienttokens.constants.ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME;
import static nl.ing.lovebird.clienttokens.constants.ClientTokenConstants.EXTRA_CLAIM_ISSUED_FOR;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OBLegacyController.class)
class OBLegacyControllerTest {
    private static final String BASE_URL = "/internal/client-groups/{clientGroupId}/open-banking-legacy-certificates";
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestClientTokens testClientTokens;

    @MockBean
    private ClientGroupIdVerificationService clientGroupIdVerificationService;

    @MockBean
    private OBLegacyService obLegacyService;

    @ParameterizedTest
    @MethodSource
    void createCertificateSigningRequest(OBLegacyCertificateSigningRequestDTO inputDTO) throws Exception {
        UUID clientGroupId = UUID.randomUUID();
        ClientGroupToken clientGroupToken = testClientTokens.createClientGroupToken(clientGroupId,
                claims -> claims.setClaim(EXTRA_CLAIM_ISSUED_FOR, "dev-portal"));
        String keyId = UUID.randomUUID().toString();
        CertificateDTO certificateDTO = new CertificateDTO(
                "my signing-key",
                CertificateType.EIDAS,
                keyId,
                CertificateUsageType.SIGNING,
                Set.of(ServiceType.AIS),
                "RSA2048",
                "SHA256_WITH_RSA",
                "signingRequest",
                null
        );

        when(obLegacyService.createCertificateSigningRequest(clientGroupToken, inputDTO)).thenReturn(certificateDTO);

        mockMvc
                .perform(
                        post(BASE_URL, clientGroupId)
                                .header(CLIENT_TOKEN_HEADER_NAME, clientGroupToken.getSerialized())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(inputDTO))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kid", is(keyId)));
        verify(clientGroupIdVerificationService).verify(clientGroupToken, clientGroupId);
    }

    static Stream<OBLegacyCertificateSigningRequestDTO> createCertificateSigningRequest() {
        return Stream.of(
                new OBLegacyCertificateSigningRequestDTO(
                        "certificateName",
                        Set.of(),
                        CertificateUsageType.SIGNING,
                        "RSA2048",
                        "SHA256_WITH_RSA",
                        List.of(
                                new SimpleDistinguishedNameElement("O", "OpenBanking"),
                                new SimpleDistinguishedNameElement("C", "GB"),
                                new SimpleDistinguishedNameElement("CN", "Common Name"),
                                new SimpleDistinguishedNameElement("OU", "Organisational Unit")
                        ),
                        Set.of()
                ),
                new OBLegacyCertificateSigningRequestDTO(
                        "certificateName",
                        Set.of(),
                        CertificateUsageType.TRANSPORT,
                        "RSA2048",
                        "SHA256_WITH_RSA",
                        List.of(
                                new SimpleDistinguishedNameElement("O", "OpenBanking"),
                                new SimpleDistinguishedNameElement("C", "GB"),
                                new SimpleDistinguishedNameElement("CN", "Common Name"),
                                new SimpleDistinguishedNameElement("OU", "Organisational Unit")
                        ),
                        Set.of()
                ),
                new OBLegacyCertificateSigningRequestDTO(
                        "certificateName",
                        Set.of(),
                        CertificateUsageType.SIGNING,
                        "RSA2048",
                        "SHA256_WITH_RSA",
                        List.of(
                                new SimpleDistinguishedNameElement("O", "OpenBanking"),
                                new SimpleDistinguishedNameElement("C", "GB"),
                                new SimpleDistinguishedNameElement("CN", "Common Name"),
                                new SimpleDistinguishedNameElement("OU", "Organisational Unit")
                        ),
                        Set.of("SAN1", "SAN2")
                ),
                new OBLegacyCertificateSigningRequestDTO(
                        "certificateName",
                        Set.of(),
                        CertificateUsageType.TRANSPORT,
                        "RSA2048",
                        "SHA256_WITH_RSA",
                        List.of(
                                new SimpleDistinguishedNameElement("O", "OpenBanking"),
                                new SimpleDistinguishedNameElement("C", "GB"),
                                new SimpleDistinguishedNameElement("CN", "Common Name"),
                                new SimpleDistinguishedNameElement("OU", "Organisational Unit")
                        ),
                        Set.of("SAN1", "SAN2")
                ),
                new OBLegacyCertificateSigningRequestDTO(
                        "certificateName",
                        Set.of(),
                        CertificateUsageType.SIGNING,
                        "RSA4096",
                        "SHA256_WITH_RSA",
                        List.of(
                                new SimpleDistinguishedNameElement("O", "OpenBanking"),
                                new SimpleDistinguishedNameElement("C", "GB"),
                                new SimpleDistinguishedNameElement("CN", "other CN"),
                                new SimpleDistinguishedNameElement("OU", "other OU")
                        ),
                        null
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void createCertificateSigningRequest_invalid_input(OBLegacyCertificateSigningRequestDTO inputDTO) throws Exception {
        UUID clientGroupId = UUID.randomUUID();
        ClientGroupToken clientGroupToken = testClientTokens.createClientGroupToken(clientGroupId);


        mockMvc
                .perform(
                        post(BASE_URL, clientGroupId)
                                .header(CLIENT_TOKEN_HEADER_NAME, clientGroupToken.getSerialized())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(inputDTO))
                )
                .andExpect(status().isBadRequest());
    }

    static Stream<OBLegacyCertificateSigningRequestDTO> createCertificateSigningRequest_invalid_input() {
        return Stream.of(
                new OBLegacyCertificateSigningRequestDTO(
                        " ",
                        Set.of(),
                        CertificateUsageType.SIGNING,
                        "RSA2048",
                        "SHA256_WITH_RSA",
                        List.of(
                                new SimpleDistinguishedNameElement("O", "OpenBanking"),
                                new SimpleDistinguishedNameElement("C", "GB"),
                                new SimpleDistinguishedNameElement("CN", "Common Name"),
                                new SimpleDistinguishedNameElement("OU", "Organisational Unit")
                        ),
                        Set.of()
                ),
                new OBLegacyCertificateSigningRequestDTO(
                        null,
                        Set.of(),
                        CertificateUsageType.SIGNING,
                        "RSA2048",
                        "SHA256_WITH_RSA",
                        List.of(
                                new SimpleDistinguishedNameElement("O", "OpenBanking"),
                                new SimpleDistinguishedNameElement("C", "GB"),
                                new SimpleDistinguishedNameElement("CN", "Common Name"),
                                new SimpleDistinguishedNameElement("OU", "Organisational Unit")
                        ),
                        Set.of()
                ),
                new OBLegacyCertificateSigningRequestDTO(
                        "name with Illegal characters!",
                        Set.of(),
                        CertificateUsageType.SIGNING,
                        "RSA2048",
                        "SHA256_WITH_RSA",
                        List.of(
                                new SimpleDistinguishedNameElement("O", "OpenBanking"),
                                new SimpleDistinguishedNameElement("C", "GB"),
                                new SimpleDistinguishedNameElement("CN", "Common Name"),
                                new SimpleDistinguishedNameElement("OU", "Organisational Unit")
                        ),
                        Set.of()
                ),
                new OBLegacyCertificateSigningRequestDTO(
                        "too long name 123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890",
                        Set.of(),
                        CertificateUsageType.SIGNING,
                        "RSA2048",
                        "SHA256_WITH_RSA",
                        List.of(
                                new SimpleDistinguishedNameElement("O", "OpenBanking"),
                                new SimpleDistinguishedNameElement("C", "GB"),
                                new SimpleDistinguishedNameElement("CN", "Common Name"),
                                new SimpleDistinguishedNameElement("OU", "Organisational Unit")
                        ),
                        Set.of()
                ),
                new OBLegacyCertificateSigningRequestDTO(
                        "service types filled, while required empty",
                        Set.of(ServiceType.AIS),
                        CertificateUsageType.SIGNING,
                        "RSA2048",
                        "SHA256_WITH_RSA",
                        List.of(
                                new SimpleDistinguishedNameElement("O", "OpenBanking"),
                                new SimpleDistinguishedNameElement("C", "GB"),
                                new SimpleDistinguishedNameElement("CN", "Common Name"),
                                new SimpleDistinguishedNameElement("OU", "Organisational Unit")
                        ),
                        Set.of()
                ),
                new OBLegacyCertificateSigningRequestDTO(
                        "service types missing while required",
                        null,
                        CertificateUsageType.SIGNING,
                        "RSA2048",
                        "SHA256_WITH_RSA",
                        List.of(
                                new SimpleDistinguishedNameElement("O", "OpenBanking"),
                                new SimpleDistinguishedNameElement("C", "GB"),
                                new SimpleDistinguishedNameElement("CN", "Common Name"),
                                new SimpleDistinguishedNameElement("OU", "Organisational Unit")
                        ),
                        Set.of()
                ),
                new OBLegacyCertificateSigningRequestDTO(
                        "key type missing",
                        Set.of(),
                        null,
                        "RSA2048",
                        "SHA256_WITH_RSA",
                        List.of(
                                new SimpleDistinguishedNameElement("O", "OpenBanking"),
                                new SimpleDistinguishedNameElement("C", "GB"),
                                new SimpleDistinguishedNameElement("CN", "Common Name"),
                                new SimpleDistinguishedNameElement("OU", "Organisational Unit")
                        ),
                        Set.of()
                ),
                new OBLegacyCertificateSigningRequestDTO(
                        "too long name 123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890",
                        Set.of(ServiceType.AS),
                        CertificateUsageType.SIGNING,
                        "RSA2048",
                        "SHA256_WITH_RSA",
                        List.of(
                                new SimpleDistinguishedNameElement("O", "OpenBanking"),
                                new SimpleDistinguishedNameElement("C", "GB"),
                                new SimpleDistinguishedNameElement("CN", "Common Name"),
                                new SimpleDistinguishedNameElement("OU", "Organisational Unit")
                        ),
                        Set.of()
                ),
                new OBLegacyCertificateSigningRequestDTO(
                        "key algorithm missing",
                        Set.of(ServiceType.AS),
                        CertificateUsageType.SIGNING,
                        null,
                        "SHA256_WITH_RSA",
                        List.of(
                                new SimpleDistinguishedNameElement("O", "OpenBanking"),
                                new SimpleDistinguishedNameElement("C", "GB"),
                                new SimpleDistinguishedNameElement("CN", "Common Name"),
                                new SimpleDistinguishedNameElement("OU", "Organisational Unit")
                        ),
                        Set.of()
                ),
                new OBLegacyCertificateSigningRequestDTO(
                        "signature algorithm missing",
                        Set.of(ServiceType.AS),
                        CertificateUsageType.SIGNING,
                        "RSA2048",
                        null,
                        List.of(
                                new SimpleDistinguishedNameElement("O", "OpenBanking"),
                                new SimpleDistinguishedNameElement("C", "GB"),
                                new SimpleDistinguishedNameElement("CN", "Common Name"),
                                new SimpleDistinguishedNameElement("OU", "Organisational Unit")
                        ),
                        Set.of()
                ),
                new OBLegacyCertificateSigningRequestDTO(
                        "signature algorithm wrong",
                        Set.of(ServiceType.AS),
                        CertificateUsageType.SIGNING,
                        "RSA2048",
                        "wrong",
                        List.of(
                                new SimpleDistinguishedNameElement("O", "OpenBanking"),
                                new SimpleDistinguishedNameElement("C", "GB"),
                                new SimpleDistinguishedNameElement("CN", "Common Name"),
                                new SimpleDistinguishedNameElement("OU", "Organisational Unit")
                        ),
                        Set.of()
                ),
                new OBLegacyCertificateSigningRequestDTO(
                        "DN Organisation wrong",
                        Set.of(ServiceType.AS),
                        CertificateUsageType.SIGNING,
                        "RSA2048",
                        "SHA256_WITH_RSA",
                        List.of(
                                new SimpleDistinguishedNameElement("O", "Wrong"),
                                new SimpleDistinguishedNameElement("C", "GB"),
                                new SimpleDistinguishedNameElement("CN", "Common Name"),
                                new SimpleDistinguishedNameElement("OU", "Organisational Unit")
                        ),
                        Set.of()
                ),
                new OBLegacyCertificateSigningRequestDTO(
                        "DN country wrong",
                        Set.of(ServiceType.AS),
                        CertificateUsageType.SIGNING,
                        "RSA2048",
                        "SHA256_WITH_RSA",
                        List.of(
                                new SimpleDistinguishedNameElement("O", "OpenBanking"),
                                new SimpleDistinguishedNameElement("C", "NL"),
                                new SimpleDistinguishedNameElement("CN", "Common Name"),
                                new SimpleDistinguishedNameElement("OU", "Organisational Unit")
                        ),
                        Set.of()
                ),
                new OBLegacyCertificateSigningRequestDTO(
                        "DN element missing",
                        Set.of(ServiceType.AS),
                        CertificateUsageType.SIGNING,
                        "RSA2048",
                        "SHA256_WITH_RSA",
                        List.of(
                                new SimpleDistinguishedNameElement("O", "OpenBanking"),
                                new SimpleDistinguishedNameElement("C", "GB"),
                                new SimpleDistinguishedNameElement("CN", "Common Name")
                        ),
                        Set.of()
                ),
                new OBLegacyCertificateSigningRequestDTO(
                        "extra DN element",
                        Set.of(ServiceType.AS),
                        CertificateUsageType.SIGNING,
                        "RSA2048",
                        "SHA256_WITH_RSA",
                        List.of(
                                new SimpleDistinguishedNameElement("O", "OpenBanking"),
                                new SimpleDistinguishedNameElement("C", "GB"),
                                new SimpleDistinguishedNameElement("CN", "Common Name"),
                                new SimpleDistinguishedNameElement("OU", "Organisational Unit"),
                                new SimpleDistinguishedNameElement("extra", "some value")
                        ),
                        Set.of()
                ),
                new OBLegacyCertificateSigningRequestDTO(
                        "DNs missing",
                        Set.of(ServiceType.AS),
                        CertificateUsageType.SIGNING,
                        "RSA2048",
                        "SHA256_WITH_RSA",
                        null,
                        Set.of()
                )
        );
    }

    @Test
    void getAllCertificates() throws Exception {
        UUID clientGroupId = UUID.randomUUID();
        ClientGroupToken clientGroupToken = testClientTokens.createClientGroupToken(clientGroupId);
        String keyId = UUID.randomUUID().toString();
        CertificateDTO certificateDTO = new CertificateDTO(
                "my signing-key",
                CertificateType.EIDAS,
                keyId,
                CertificateUsageType.SIGNING,
                Set.of(ServiceType.AIS),
                "RSA2048",
                "SHA256_WITH_RSA",
                "signingRequest",
                null
        );

        when(obLegacyService.getCertificates(clientGroupToken)).thenReturn(List.of(certificateDTO));

        mockMvc
                .perform(
                        get(BASE_URL, clientGroupId)
                                .header(CLIENT_TOKEN_HEADER_NAME, clientGroupToken.getSerialized())
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].kid", is(keyId)));
        verify(clientGroupIdVerificationService).verify(clientGroupToken, clientGroupId);
    }

    @Test
    void getCertificate() throws Exception {
        UUID clientGroupId = UUID.randomUUID();
        ClientGroupToken clientGroupToken = testClientTokens.createClientGroupToken(clientGroupId);
        String keyId = UUID.randomUUID().toString();
        CertificateDTO certificateDTO = new CertificateDTO(
                "my signing-key",
                CertificateType.EIDAS,
                keyId,
                CertificateUsageType.SIGNING,
                Set.of(ServiceType.AIS),
                "RSA2048",
                "SHA256_WITH_RSA",
                "signingRequest",
                null
        );

        when(obLegacyService.getCertificate(clientGroupToken, keyId)).thenReturn(certificateDTO);

        mockMvc
                .perform(
                        get(BASE_URL + "/{certificateId}", clientGroupId, keyId)
                                .header(CLIENT_TOKEN_HEADER_NAME, clientGroupToken.getSerialized())
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kid", is(keyId)));
        verify(clientGroupIdVerificationService).verify(clientGroupToken, clientGroupId);
    }

    @Test
    void getCertificate_certificate_not_found() throws Exception {
        UUID clientGroupId = UUID.randomUUID();
        ClientGroupToken clientGroupToken = testClientTokens.createClientGroupToken(clientGroupId);
        String keyId = UUID.randomUUID().toString();

        when(obLegacyService.getCertificate(clientGroupToken, keyId)).thenThrow(new CertificateNotFoundException("oops"));

        mockMvc
                .perform(
                        get(BASE_URL + "/{certificateId}", clientGroupId, keyId)
                                .header(CLIENT_TOKEN_HEADER_NAME, clientGroupToken.getSerialized())
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("CLS012")));
        verify(clientGroupIdVerificationService).verify(clientGroupToken, clientGroupId);
    }

    @Test
    void updateCertificateName() throws Exception {
        UUID clientGroupId = UUID.randomUUID();
        ClientGroupToken clientGroupToken = testClientTokens.createClientGroupToken(clientGroupId,
                claims -> claims.setClaim(ClientTokenConstants.EXTRA_CLAIM_ISSUED_FOR, "dev-portal"));
        String keyId = UUID.randomUUID().toString();
        CertificateDTO certificateDTO = new CertificateDTO(
                "my signing-key",
                CertificateType.EIDAS,
                keyId,
                CertificateUsageType.SIGNING,
                Set.of(ServiceType.AIS),
                "RSA2048",
                "SHA256_WITH_RSA",
                "signingRequest",
                null
        );

        NewCertificateNameDTO inputDTO = new NewCertificateNameDTO("New Name");

        when(obLegacyService.updateName(clientGroupToken, keyId, inputDTO)).thenReturn(certificateDTO);

        mockMvc
                .perform(
                        put(BASE_URL + "/{certificateId}/name", clientGroupId, keyId)
                                .header(CLIENT_TOKEN_HEADER_NAME, clientGroupToken.getSerialized())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(inputDTO))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kid", is(keyId)));
        verify(clientGroupIdVerificationService).verify(clientGroupToken, clientGroupId);
    }

    @Test
    void updateCertificateName_name_same() throws Exception {
        UUID clientGroupId = UUID.randomUUID();
        ClientGroupToken clientGroupToken = testClientTokens.createClientGroupToken(clientGroupId,
                claims -> claims.setClaim(ClientTokenConstants.EXTRA_CLAIM_ISSUED_FOR, "dev-portal"));
        String keyId = UUID.randomUUID().toString();
        CertificateDTO certificateDTO = new CertificateDTO(
                "my signing-key",
                CertificateType.EIDAS,
                keyId,
                CertificateUsageType.SIGNING,
                Set.of(ServiceType.AIS),
                "RSA2048",
                "SHA256_WITH_RSA",
                "signingRequest",
                null
        );

        NewCertificateNameDTO inputDTO = new NewCertificateNameDTO("my signing-key");

        when(obLegacyService.updateName(clientGroupToken, keyId, inputDTO)).thenReturn(certificateDTO);

        mockMvc
                .perform(
                        put(BASE_URL + "/{certificateId}/name", clientGroupId, keyId)
                                .header(CLIENT_TOKEN_HEADER_NAME, clientGroupToken.getSerialized())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(inputDTO))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kid", is(keyId)));
        verify(clientGroupIdVerificationService).verify(clientGroupToken, clientGroupId);
    }

    @Test
    void updateCertificateName_name_taken() throws Exception {
        UUID clientGroupId = UUID.randomUUID();
        ClientGroupToken clientGroupToken = testClientTokens.createClientGroupToken(clientGroupId,
                claims -> claims.setClaim(ClientTokenConstants.EXTRA_CLAIM_ISSUED_FOR, "dev-portal"));
        String keyId = UUID.randomUUID().toString();

        NewCertificateNameDTO inputDTO = new NewCertificateNameDTO("New Name");

        when(obLegacyService.updateName(clientGroupToken, keyId, inputDTO)).thenThrow(new NameIsAlreadyUsedException());

        mockMvc
                .perform(
                        put(BASE_URL + "/{certificateId}/name", clientGroupId, keyId)
                                .header(CLIENT_TOKEN_HEADER_NAME, clientGroupToken.getSerialized())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(inputDTO))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("CLS013")));
        verify(clientGroupIdVerificationService).verify(clientGroupToken, clientGroupId);
    }

    @Test
    void updateCertificateName_certificate_not_found() throws Exception {
        UUID clientGroupId = UUID.randomUUID();
        ClientGroupToken clientGroupToken = testClientTokens.createClientGroupToken(clientGroupId,
                claims -> claims.setClaim(ClientTokenConstants.EXTRA_CLAIM_ISSUED_FOR, "dev-portal"));
        String keyId = UUID.randomUUID().toString();

        NewCertificateNameDTO inputDTO = new NewCertificateNameDTO("New Name");

        when(obLegacyService.updateName(clientGroupToken, keyId, inputDTO)).thenThrow(new CertificateNotFoundException("oops"));

        mockMvc
                .perform(
                        put(BASE_URL + "/{certificateId}/name", clientGroupId, keyId)
                                .header(CLIENT_TOKEN_HEADER_NAME, clientGroupToken.getSerialized())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(inputDTO))
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("CLS012")));
        verify(clientGroupIdVerificationService).verify(clientGroupToken, clientGroupId);
    }

    @Test
    void updateCertificateName_wrong_token() throws Exception {
        UUID clientGroupId = UUID.randomUUID();
        ClientGroupToken clientGroupToken = testClientTokens.createClientGroupToken(clientGroupId,
                claims -> claims.setClaim(ClientTokenConstants.EXTRA_CLAIM_ISSUED_FOR, "other"));
        String keyId = UUID.randomUUID().toString();

        NewCertificateNameDTO inputDTO = new NewCertificateNameDTO("New Name");

        mockMvc
                .perform(
                        put(BASE_URL + "/{certificateId}/name", clientGroupId, keyId)
                                .header(CLIENT_TOKEN_HEADER_NAME, clientGroupToken.getSerialized())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(inputDTO))
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("CLS9002")));
    }

    @Test
    void validateCertificateChain() throws Exception {
        UUID clientGroupId = UUID.randomUUID();
        ClientGroupToken clientGroupToken = testClientTokens.createClientGroupToken(clientGroupId,
                claims -> claims.setClaim(ClientTokenConstants.EXTRA_CLAIM_ISSUED_FOR, "dev-portal"));
        String keyId = UUID.randomUUID().toString();
        byte[] pem = this.getClass().getResourceAsStream("valid-sandbox-chain.pem").readAllBytes();
        List<X509Certificate> certificateChain = KeyUtil.parseCertificateChain(new String(pem));
        CertificateChainDTO inputDTO = new CertificateChainDTO(Base64.getEncoder().encodeToString(pem));
        ValidatedCertificateChainDTO validatedCertificateChainDTO = new ValidatedCertificateChainDTO(true, null, null);

        when(obLegacyService.verifyCertificateChain(clientGroupToken, keyId, certificateChain)).thenReturn(validatedCertificateChainDTO);
        try {
            mockMvc
                    .perform(
                            post(BASE_URL + "/{certificateId}/validate-certificate-chain", clientGroupId, keyId)
                                    .header(CLIENT_TOKEN_HEADER_NAME, clientGroupToken.getSerialized())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsBytes(inputDTO))
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.valid", is(true)));
        } finally {
            verify(clientGroupIdVerificationService).verify(clientGroupToken, clientGroupId);
        }
    }

    @Test
    void testValidateCertificateChain_certificate_not_found() throws Exception {
        UUID clientGroupId = UUID.randomUUID();
        ClientGroupToken clientGroupToken = testClientTokens.createClientGroupToken(clientGroupId,
                claims -> claims.setClaim(ClientTokenConstants.EXTRA_CLAIM_ISSUED_FOR, "dev-portal"));
        String keyId = UUID.randomUUID().toString();
        byte[] pem = this.getClass().getResourceAsStream("valid-sandbox-chain.pem").readAllBytes();
        List<X509Certificate> certificateChain = KeyUtil.parseCertificateChain(new String(pem));
        CertificateChainDTO inputDTO = new CertificateChainDTO(Base64.getEncoder().encodeToString(pem));

        when(obLegacyService.verifyCertificateChain(clientGroupToken, keyId, certificateChain)).thenThrow(new CertificateNotFoundException("oops"));
        try {
            mockMvc
                    .perform(
                            post(BASE_URL + "/{certificateId}/validate-certificate-chain", clientGroupId, keyId)
                                    .header(CLIENT_TOKEN_HEADER_NAME, clientGroupToken.getSerialized())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsBytes(inputDTO))
                    )
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code", is("CLS012")));
        } finally {
            verify(clientGroupIdVerificationService).verify(clientGroupToken, clientGroupId);
        }
    }

    @Test
    void testValidateCertificateChain_invalidInput() throws Exception {
        UUID clientGroupId = UUID.randomUUID();
        ClientGroupToken clientGroupToken = testClientTokens.createClientGroupToken(clientGroupId);
        String keyId = UUID.randomUUID().toString();
        CertificateChainDTO inputDTO = new CertificateChainDTO(" ");

        mockMvc
                .perform(
                        post(BASE_URL + "/{certificateId}/validate-certificate-chain", clientGroupId, keyId)
                                .header(CLIENT_TOKEN_HEADER_NAME, clientGroupToken.getSerialized())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(inputDTO))
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdateCertificateChain() throws Exception {
        UUID clientGroupId = UUID.randomUUID();
        String certificateId = UUID.randomUUID().toString();
        ClientGroupToken clientGroupToken = testClientTokens.createClientGroupToken(clientGroupId,
                claims -> claims.setClaim(ClientTokenConstants.EXTRA_CLAIM_ISSUED_FOR, "dev-portal"));
        byte[] certificateChainPem = this.getClass().getResourceAsStream("valid-sandbox-chain.pem").readAllBytes();
        List<X509Certificate> certificateChain = KeyUtil.parseCertificateChain(new String(certificateChainPem));
        CertificateChainDTO certificateChainDTO = new CertificateChainDTO(Base64.getEncoder().encodeToString(certificateChainPem));
        CertificateInfoDTO certificateInfoDTO = new CertificateInfoDTO(
                certificateId,
                BigInteger.valueOf(456L),
                "subject",
                "issuer",
                Date.from(Instant.now().minus(5, ChronoUnit.DAYS)),
                Date.from(Instant.now().plus(5, ChronoUnit.DAYS)),
                "keyAlg",
                "signingAlg"
        );
        when(obLegacyService.updateCertificateChain(clientGroupToken, certificateId, certificateChain)).thenReturn(List.of(certificateInfoDTO));
        try {
            mockMvc
                    .perform(
                            put(BASE_URL + "/{certificateId}/certificate-chain", clientGroupId, certificateId)
                                    .header(CLIENT_TOKEN_HEADER_NAME, clientGroupToken.getSerialized())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsBytes(certificateChainDTO))
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].kid", is(certificateId)));
        } finally {
            verify(clientGroupIdVerificationService).verify(clientGroupToken, clientGroupId);
        }
    }

    @Test
    void testUpdateCertificateChain_invalid_certificate() throws Exception {
        UUID clientGroupId = UUID.randomUUID();
        String certificateId = UUID.randomUUID().toString();
        ClientGroupToken clientGroupToken = testClientTokens.createClientGroupToken(clientGroupId,
                claims -> claims.setClaim(ClientTokenConstants.EXTRA_CLAIM_ISSUED_FOR, "dev-portal"));
        byte[] certificateChainPem = this.getClass().getResourceAsStream("valid-sandbox-chain.pem").readAllBytes();
        List<X509Certificate> certificateChain = KeyUtil.parseCertificateChain(new String(certificateChainPem));
        CertificateChainDTO certificateChainDTO = new CertificateChainDTO(Base64.getEncoder().encodeToString(certificateChainPem));

        when(obLegacyService.updateCertificateChain(clientGroupToken, certificateId, certificateChain)).thenThrow(new CertificateValidationException("oops"));
        try {
            mockMvc
                    .perform(
                            put(BASE_URL + "/{certificateId}/certificate-chain", clientGroupId, certificateId)
                                    .header(CLIENT_TOKEN_HEADER_NAME, clientGroupToken.getSerialized())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsBytes(certificateChainDTO))
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code", is("CLS014")));
        } finally {
            verify(clientGroupIdVerificationService).verify(clientGroupToken, clientGroupId);
        }
    }

    @Test
    void testUpdateCertificateChain_already_signed_certificate() throws Exception {
        UUID clientGroupId = UUID.randomUUID();
        String certificateId = UUID.randomUUID().toString();
        ClientGroupToken clientGroupToken = testClientTokens.createClientGroupToken(clientGroupId,
                claims -> claims.setClaim(ClientTokenConstants.EXTRA_CLAIM_ISSUED_FOR, "dev-portal"));
        byte[] certificateChainPem = this.getClass().getResourceAsStream("valid-sandbox-chain.pem").readAllBytes();
        List<X509Certificate> certificateChain = KeyUtil.parseCertificateChain(new String(certificateChainPem));
        CertificateChainDTO certificateChainDTO = new CertificateChainDTO(Base64.getEncoder().encodeToString(certificateChainPem));

        when(obLegacyService.updateCertificateChain(clientGroupToken, certificateId, certificateChain)).thenThrow(new CertificateAlreadySignedException());
        try {
            mockMvc
                    .perform(
                            put(BASE_URL + "/{certificateId}/certificate-chain", clientGroupId, certificateId)
                                    .header(CLIENT_TOKEN_HEADER_NAME, clientGroupToken.getSerialized())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsBytes(certificateChainDTO))
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code", is("CLS015")));
        } finally {
            verify(clientGroupIdVerificationService).verify(clientGroupToken, clientGroupId);
        }
    }

    @Test
    void testUpdateCertificateChain_wrong_token() throws Exception {
        UUID clientGroupId = UUID.randomUUID();
        UUID certificateId = UUID.randomUUID();
        ClientGroupToken clientGroupToken = testClientTokens.createClientGroupToken(clientGroupId);
        CertificateChainDTO certificateChainDTO = new CertificateChainDTO(Base64.getEncoder().encodeToString(this.getClass().getResourceAsStream("valid-sandbox-chain.pem").readAllBytes()));

        mockMvc
                .perform(
                        put(BASE_URL + "/{certificateId}/certificate-chain", clientGroupId, certificateId)
                                .header(CLIENT_TOKEN_HEADER_NAME, clientGroupToken.getSerialized())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(certificateChainDTO))
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void testUpdateCertificateChain_blank_certificate() throws Exception {
        UUID clientGroupId = UUID.randomUUID();
        UUID certificateId = UUID.randomUUID();
        ClientGroupToken clientGroupToken = testClientTokens.createClientGroupToken(clientGroupId);
        CertificateChainDTO certificateChainDTO = new CertificateChainDTO("");

        mockMvc
                .perform(
                        put(BASE_URL + "/{certificateId}/certificate-chain", clientGroupId, certificateId)
                                .header(CLIENT_TOKEN_HEADER_NAME, clientGroupToken.getSerialized())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(certificateChainDTO))
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdateCertificateChain_no_certificate() throws Exception {
        UUID clientGroupId = UUID.randomUUID();
        UUID certificateId = UUID.randomUUID();
        ClientGroupToken clientGroupToken = testClientTokens.createClientGroupToken(clientGroupId);

        mockMvc
                .perform(
                        put(BASE_URL + "/{certificateId}/certificate-chain", clientGroupId, certificateId)
                                .header(CLIENT_TOKEN_HEADER_NAME, clientGroupToken.getSerialized())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(null))
                )
                .andExpect(status().isBadRequest());
    }
}
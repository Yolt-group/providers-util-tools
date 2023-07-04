package com.yolt.clients.clientgroup.certificatemanagement.openbanking.etsi;

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

@WebMvcTest(OBEtsiController.class)
class OBEtsiControllerTest {
    private static final String BASE_URL = "/internal/client-groups/{clientGroupId}/open-banking-etsi-certificates";
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestClientTokens testClientTokens;

    @MockBean
    private ClientGroupIdVerificationService clientGroupIdVerificationService;

    @MockBean
    private OBEtsiService obEtsiService;

    @ParameterizedTest
    @MethodSource
    void createCertificateSigningRequest(OBEtsiCertificateSigningRequestDTO inputDTO) throws Exception {
        UUID clientGroupId = UUID.randomUUID();
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
        ClientGroupToken clientGroupToken = testClientTokens.createClientGroupToken(clientGroupId,
                claims -> claims.setClaim(ClientTokenConstants.EXTRA_CLAIM_ISSUED_FOR, "dev-portal"));

        when(obEtsiService.createCertificateSigningRequest(clientGroupToken, inputDTO)).thenReturn(certificateDTO);

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

    static Stream<OBEtsiCertificateSigningRequestDTO> createCertificateSigningRequest() {
        return Stream.of(
                new OBEtsiCertificateSigningRequestDTO(
                        "certificateName",
                        Set.of(ServiceType.AS),
                        CertificateUsageType.SIGNING,
                        "RSA2048",
                        "SHA256_WITH_RSA",
                        List.of(
                                new SimpleDistinguishedNameElement("O", "organisation"),
                                new SimpleDistinguishedNameElement("C", "NL"),
                                new SimpleDistinguishedNameElement("CN", "common name"),
                                new SimpleDistinguishedNameElement("2.5.4.97", "NL")
                        ),
                        Set.of()
                ),
                new OBEtsiCertificateSigningRequestDTO(
                        "certificateName",
                        Set.of(ServiceType.AS, ServiceType.PIS),
                        CertificateUsageType.TRANSPORT,
                        "RSA4096",
                        "SHA256_WITH_RSA",
                        List.of(
                                new SimpleDistinguishedNameElement("O", "organisation"),
                                new SimpleDistinguishedNameElement("C", "NL"),
                                new SimpleDistinguishedNameElement("CN", "common name"),
                                new SimpleDistinguishedNameElement("2.5.4.97", "NL")
                        ),
                        null
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void createCertificateSigningRequest_invalid_input(OBEtsiCertificateSigningRequestDTO inputDTO) throws Exception {
        UUID clientGroupId = UUID.randomUUID();

        ClientGroupToken clientGroupToken = testClientTokens.createClientGroupToken(clientGroupId,
                claims -> claims.setClaim(ClientTokenConstants.EXTRA_CLAIM_ISSUED_FOR, "dev-portal"));

        mockMvc
                .perform(
                        post(BASE_URL, clientGroupId)
                                .header(CLIENT_TOKEN_HEADER_NAME, clientGroupToken.getSerialized())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(inputDTO))
                )
                .andExpect(status().isBadRequest());
    }

    static Stream<OBEtsiCertificateSigningRequestDTO> createCertificateSigningRequest_invalid_input() {
        return Stream.of(
                new OBEtsiCertificateSigningRequestDTO(
                        " ",
                        Set.of(ServiceType.AS),
                        CertificateUsageType.SIGNING,
                        "RSA2048",
                        "SHA256_WITH_RSA",
                        List.of(
                                new SimpleDistinguishedNameElement("O", "organisation"),
                                new SimpleDistinguishedNameElement("C", "NL"),
                                new SimpleDistinguishedNameElement("CN", "common name"),
                                new SimpleDistinguishedNameElement("2.5.4.97", "NL")
                        ),
                        Set.of()
                ),
                new OBEtsiCertificateSigningRequestDTO(
                        null,
                        Set.of(ServiceType.AS),
                        CertificateUsageType.SIGNING,
                        "RSA2048",
                        "SHA256_WITH_RSA",
                        List.of(
                                new SimpleDistinguishedNameElement("O", "organisation"),
                                new SimpleDistinguishedNameElement("C", "NL"),
                                new SimpleDistinguishedNameElement("CN", "common name"),
                                new SimpleDistinguishedNameElement("2.5.4.97", "NL")
                        ),
                        Set.of()
                ),
                new OBEtsiCertificateSigningRequestDTO(
                        "name with Illegal characters!",
                        Set.of(ServiceType.AS),
                        CertificateUsageType.SIGNING,
                        "RSA2048",
                        "SHA256_WITH_RSA",
                        List.of(
                                new SimpleDistinguishedNameElement("O", "organisation"),
                                new SimpleDistinguishedNameElement("C", "NL"),
                                new SimpleDistinguishedNameElement("CN", "common name"),
                                new SimpleDistinguishedNameElement("2.5.4.97", "NL")
                        ),
                        Set.of()
                ),
                new OBEtsiCertificateSigningRequestDTO(
                        "too long name 123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890",
                        Set.of(ServiceType.AS),
                        CertificateUsageType.SIGNING,
                        "RSA2048",
                        "SHA256_WITH_RSA",
                        List.of(
                                new SimpleDistinguishedNameElement("O", "organisation"),
                                new SimpleDistinguishedNameElement("C", "NL"),
                                new SimpleDistinguishedNameElement("CN", "common name"),
                                new SimpleDistinguishedNameElement("2.5.4.97", "NL")
                        ),
                        Set.of()
                ),
                new OBEtsiCertificateSigningRequestDTO(
                        "service types empty while required",
                        Set.of(),
                        CertificateUsageType.SIGNING,
                        "RSA2048",
                        "SHA256_WITH_RSA",
                        List.of(
                                new SimpleDistinguishedNameElement("O", "organisation"),
                                new SimpleDistinguishedNameElement("C", "NL"),
                                new SimpleDistinguishedNameElement("CN", "common name"),
                                new SimpleDistinguishedNameElement("2.5.4.97", "NL")
                        ),
                        Set.of()
                ),
                new OBEtsiCertificateSigningRequestDTO(
                        "service types missing while required",
                        null,
                        CertificateUsageType.SIGNING,
                        "RSA2048",
                        "SHA256_WITH_RSA",
                        List.of(
                                new SimpleDistinguishedNameElement("O", "organisation"),
                                new SimpleDistinguishedNameElement("C", "NL"),
                                new SimpleDistinguishedNameElement("CN", "common name"),
                                new SimpleDistinguishedNameElement("2.5.4.97", "NL")
                        ),
                        Set.of()
                ),
                new OBEtsiCertificateSigningRequestDTO(
                        "key type missing",
                        Set.of(ServiceType.AS),
                        null,
                        "RSA2048",
                        "SHA256_WITH_RSA",
                        List.of(
                                new SimpleDistinguishedNameElement("O", "organisation"),
                                new SimpleDistinguishedNameElement("C", "NL"),
                                new SimpleDistinguishedNameElement("CN", "common name"),
                                new SimpleDistinguishedNameElement("2.5.4.97", "NL")
                        ),
                        Set.of()
                ),
                new OBEtsiCertificateSigningRequestDTO(
                        "too long name 123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890",
                        Set.of(ServiceType.AS),
                        CertificateUsageType.SIGNING,
                        "RSA2048",
                        "SHA256_WITH_RSA",
                        List.of(
                                new SimpleDistinguishedNameElement("O", "organisation"),
                                new SimpleDistinguishedNameElement("C", "NL"),
                                new SimpleDistinguishedNameElement("CN", "common name"),
                                new SimpleDistinguishedNameElement("2.5.4.97", "NL")
                        ),
                        Set.of()
                ),
                new OBEtsiCertificateSigningRequestDTO(
                        "key algorithm missing",
                        Set.of(ServiceType.AS),
                        CertificateUsageType.SIGNING,
                        null,
                        "SHA256_WITH_RSA",
                        List.of(
                                new SimpleDistinguishedNameElement("O", "organisation"),
                                new SimpleDistinguishedNameElement("C", "NL"),
                                new SimpleDistinguishedNameElement("CN", "common name"),
                                new SimpleDistinguishedNameElement("2.5.4.97", "NL")
                        ),
                        Set.of()
                ),
                new OBEtsiCertificateSigningRequestDTO(
                        "key algorithm wrong",
                        Set.of(ServiceType.AS),
                        CertificateUsageType.SIGNING,
                        "wrong",
                        "SHA256_WITH_RSA",
                        List.of(
                                new SimpleDistinguishedNameElement("O", "organisation"),
                                new SimpleDistinguishedNameElement("C", "NL"),
                                new SimpleDistinguishedNameElement("CN", "common name"),
                                new SimpleDistinguishedNameElement("2.5.4.97", "NL")
                        ),
                        Set.of()
                ),
                new OBEtsiCertificateSigningRequestDTO(
                        "signature algorithm missing",
                        Set.of(ServiceType.AS),
                        CertificateUsageType.SIGNING,
                        "RSA2048",
                        null,
                        List.of(
                                new SimpleDistinguishedNameElement("O", "organisation"),
                                new SimpleDistinguishedNameElement("C", "NL"),
                                new SimpleDistinguishedNameElement("CN", "common name"),
                                new SimpleDistinguishedNameElement("2.5.4.97", "NL")
                        ),
                        Set.of()
                ),
                new OBEtsiCertificateSigningRequestDTO(
                        "signature algorithm wrong",
                        Set.of(ServiceType.AS),
                        CertificateUsageType.SIGNING,
                        "RSA2048",
                        "wrong",
                        List.of(
                                new SimpleDistinguishedNameElement("O", "organisation"),
                                new SimpleDistinguishedNameElement("C", "NL"),
                                new SimpleDistinguishedNameElement("CN", "common name"),
                                new SimpleDistinguishedNameElement("2.5.4.97", "NL")
                        ),
                        Set.of()
                ),
                new OBEtsiCertificateSigningRequestDTO(
                        "DN element missing",
                        Set.of(ServiceType.AS),
                        CertificateUsageType.SIGNING,
                        "RSA2048",
                        "SHA256_WITH_RSA",
                        List.of(
                                new SimpleDistinguishedNameElement("O", "organisation"),
                                new SimpleDistinguishedNameElement("C", "NL"),
                                new SimpleDistinguishedNameElement("CN", "common name")
                        ),
                        Set.of()
                ),
                new OBEtsiCertificateSigningRequestDTO(
                        "extra DN element",
                        Set.of(ServiceType.AS),
                        CertificateUsageType.SIGNING,
                        "RSA2048",
                        "SHA256_WITH_RSA",
                        List.of(
                                new SimpleDistinguishedNameElement("O", "organisation"),
                                new SimpleDistinguishedNameElement("C", "NL"),
                                new SimpleDistinguishedNameElement("CN", "common name"),
                                new SimpleDistinguishedNameElement("2.5.4.97", "NL"),
                                new SimpleDistinguishedNameElement("extra", "some value")
                        ),
                        Set.of()
                ),
                new OBEtsiCertificateSigningRequestDTO(
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
        ClientGroupToken clientGroupToken = testClientTokens.createClientGroupToken(clientGroupId);

        when(obEtsiService.getCertificates(clientGroupToken)).thenReturn(List.of(certificateDTO));

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
        ClientGroupToken clientGroupToken = testClientTokens.createClientGroupToken(clientGroupId);

        when(obEtsiService.getCertificate(clientGroupToken, keyId)).thenReturn(certificateDTO);

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
        String keyId = UUID.randomUUID().toString();
        ClientGroupToken clientGroupToken = testClientTokens.createClientGroupToken(clientGroupId);

        when(obEtsiService.getCertificate(clientGroupToken, keyId)).thenThrow(new CertificateNotFoundException("oops"));

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
        ClientGroupToken clientGroupToken = testClientTokens.createClientGroupToken(clientGroupId,
                claims -> claims.setClaim(EXTRA_CLAIM_ISSUED_FOR, "dev-portal"));

        NewCertificateNameDTO inputDTO = new NewCertificateNameDTO("New Name");

        when(obEtsiService.updateName(clientGroupToken, keyId, inputDTO)).thenReturn(certificateDTO);

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
        ClientGroupToken clientGroupToken = testClientTokens.createClientGroupToken(clientGroupId,
                claims -> claims.setClaim(EXTRA_CLAIM_ISSUED_FOR, "dev-portal"));

        NewCertificateNameDTO inputDTO = new NewCertificateNameDTO("my signing-key");

        when(obEtsiService.updateName(clientGroupToken, keyId, inputDTO)).thenReturn(certificateDTO);

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
        String keyId = UUID.randomUUID().toString();
        ClientGroupToken clientGroupToken = testClientTokens.createClientGroupToken(clientGroupId,
                claims -> claims.setClaim(EXTRA_CLAIM_ISSUED_FOR, "dev-portal"));

        NewCertificateNameDTO inputDTO = new NewCertificateNameDTO("New Name");

        when(obEtsiService.updateName(clientGroupToken, keyId, inputDTO)).thenThrow(new NameIsAlreadyUsedException());

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
        String keyId = UUID.randomUUID().toString();
        ClientGroupToken clientGroupToken = testClientTokens.createClientGroupToken(clientGroupId,
                claims -> claims.setClaim(EXTRA_CLAIM_ISSUED_FOR, "dev-portal"));

        NewCertificateNameDTO inputDTO = new NewCertificateNameDTO("New Name");

        when(obEtsiService.updateName(clientGroupToken, keyId, inputDTO)).thenThrow(new CertificateNotFoundException("oops"));

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
        String keyId = UUID.randomUUID().toString();
        ClientGroupToken clientGroupToken = testClientTokens.createClientGroupToken(clientGroupId,
                claims -> claims.setClaim(EXTRA_CLAIM_ISSUED_FOR, "other"));

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
        String keyId = UUID.randomUUID().toString();
        byte[] pem = this.getClass().getResourceAsStream("valid-sandbox-chain.pem").readAllBytes();
        List<X509Certificate> certificateChain = KeyUtil.parseCertificateChain(new String(pem));
        CertificateChainDTO inputDTO = new CertificateChainDTO(Base64.getEncoder().encodeToString(pem));
        ValidatedCertificateChainDTO validatedCertificateChainDTO = new ValidatedCertificateChainDTO(true, null, null);
        ClientGroupToken clientGroupToken = testClientTokens.createClientGroupToken(clientGroupId,
                claims -> claims.setClaim(EXTRA_CLAIM_ISSUED_FOR, "dev-portal"));

        when(obEtsiService.verifyCertificateChain(clientGroupToken, keyId, certificateChain)).thenReturn(validatedCertificateChainDTO);
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
        String keyId = UUID.randomUUID().toString();
        byte[] pem = this.getClass().getResourceAsStream("valid-sandbox-chain.pem").readAllBytes();
        List<X509Certificate> certificateChain = KeyUtil.parseCertificateChain(new String(pem));
        CertificateChainDTO inputDTO = new CertificateChainDTO(Base64.getEncoder().encodeToString(pem));
        ClientGroupToken clientGroupToken = testClientTokens.createClientGroupToken(clientGroupId,
                claims -> claims.setClaim(EXTRA_CLAIM_ISSUED_FOR, "dev-portal"));

        when(obEtsiService.verifyCertificateChain(clientGroupToken, keyId, certificateChain)).thenThrow(new CertificateNotFoundException("oops"));
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
        String keyId = UUID.randomUUID().toString();
        CertificateChainDTO inputDTO = new CertificateChainDTO(" ");
        ClientGroupToken clientGroupToken = testClientTokens.createClientGroupToken(clientGroupId);

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
        ClientGroupToken clientGroupToken = testClientTokens.createClientGroupToken(clientGroupId,
                claims -> claims.setClaim(EXTRA_CLAIM_ISSUED_FOR, "dev-portal"));

        when(obEtsiService.updateCertificateChain(clientGroupToken, certificateId, certificateChain)).thenReturn(List.of(certificateInfoDTO));
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
        byte[] certificateChainPem = this.getClass().getResourceAsStream("valid-sandbox-chain.pem").readAllBytes();
        List<X509Certificate> certificateChain = KeyUtil.parseCertificateChain(new String(certificateChainPem));
        CertificateChainDTO certificateChainDTO = new CertificateChainDTO(Base64.getEncoder().encodeToString(certificateChainPem));

        ClientGroupToken clientGroupToken = testClientTokens.createClientGroupToken(clientGroupId,
                claims -> claims.setClaim(EXTRA_CLAIM_ISSUED_FOR, "dev-portal"));

        when(obEtsiService.updateCertificateChain(clientGroupToken, certificateId, certificateChain)).thenThrow(new CertificateValidationException("oops"));
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
        byte[] certificateChainPem = this.getClass().getResourceAsStream("valid-sandbox-chain.pem").readAllBytes();
        List<X509Certificate> certificateChain = KeyUtil.parseCertificateChain(new String(certificateChainPem));
        CertificateChainDTO certificateChainDTO = new CertificateChainDTO(Base64.getEncoder().encodeToString(certificateChainPem));

        ClientGroupToken clientGroupToken = testClientTokens.createClientGroupToken(clientGroupId,
                claims -> claims.setClaim(EXTRA_CLAIM_ISSUED_FOR, "dev-portal"));

        when(obEtsiService.updateCertificateChain(clientGroupToken, certificateId, certificateChain)).thenThrow(new CertificateAlreadySignedException());
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
        CertificateChainDTO certificateChainDTO = new CertificateChainDTO(Base64.getEncoder().encodeToString(this.getClass().getResourceAsStream("valid-sandbox-chain.pem").readAllBytes()));
        ClientGroupToken clientGroupToken = testClientTokens.createClientGroupToken(clientGroupId,
                claims -> claims.setClaim(EXTRA_CLAIM_ISSUED_FOR, "other"));

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
        CertificateChainDTO certificateChainDTO = new CertificateChainDTO("");
        ClientGroupToken clientGroupToken = testClientTokens.createClientGroupToken(clientGroupId);

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
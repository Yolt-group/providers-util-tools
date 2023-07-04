package com.yolt.clients.clientgroup.certificatemanagement.eidas;

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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.IntStream.range;
import static nl.ing.lovebird.clienttokens.constants.ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EIDASController.class)
class EIDASControllerTest {
    private static final String BASE_URL = "/internal/client-groups/{clientGroupId}/eidas-certificates";
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestClientTokens testClientTokens;

    @MockBean
    private ClientGroupIdVerificationService clientGroupIdVerificationService;

    @MockBean
    private EIDASService eidasService;

    @AfterEach
    public void validateServices() {
        verifyNoMoreInteractions(ignoreStubs(
                clientGroupIdVerificationService,
                eidasService
        ));
    }

    @Test
    void testGetServices() throws Exception {
        UUID clientGroupId = UUID.randomUUID();
        ClientGroupToken clientGroupToken = testClientTokens.createClientGroupToken(clientGroupId);

        try {
            ResultActions result = mockMvc
                    .perform(
                            get(BASE_URL + "/services", clientGroupId)
                                    .header(CLIENT_TOKEN_HEADER_NAME, clientGroupToken.getSerialized())
                                    .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(status().isOk());

            result.andExpect(jsonPath("$.AIS.signing.keyRequirements.keyAlgorithms", containsInAnyOrder("RSA4096", "RSA2048")))
                    .andExpect(jsonPath("$.AIS.signing.keyRequirements.signatureAlgorithms", containsInAnyOrder("SHA256_WITH_RSA")))
                    .andExpect(jsonPath("$.AIS.signing.keyRequirements.distinguishedNames", hasSize(8)))
                    .andExpect(jsonPath("$.AIS.signing.keyRequirements.distinguishedNames[?(@.type=='C')].value", containsInAnyOrder("")))
                    .andExpect(jsonPath("$.AIS.signing.keyRequirements.distinguishedNames[?(@.type=='C')].placeholder", containsInAnyOrder("Country Code")))
                    .andExpect(jsonPath("$.AIS.signing.keyRequirements.distinguishedNames[?(@.type=='C')].editable", containsInAnyOrder(true)))
                    .andExpect(jsonPath("$.AIS.signing.keyRequirements.distinguishedNames[?(@.type=='ST')].value", containsInAnyOrder("")))
                    .andExpect(jsonPath("$.AIS.signing.keyRequirements.distinguishedNames[?(@.type=='ST')].placeholder", containsInAnyOrder("State / Province")))
                    .andExpect(jsonPath("$.AIS.signing.keyRequirements.distinguishedNames[?(@.type=='ST')].editable", containsInAnyOrder(true)))
                    .andExpect(jsonPath("$.AIS.signing.keyRequirements.distinguishedNames[?(@.type=='L')].value", containsInAnyOrder("")))
                    .andExpect(jsonPath("$.AIS.signing.keyRequirements.distinguishedNames[?(@.type=='L')].placeholder", containsInAnyOrder("City (Locality Name)")))
                    .andExpect(jsonPath("$.AIS.signing.keyRequirements.distinguishedNames[?(@.type=='L')].editable", containsInAnyOrder(true)))
                    .andExpect(jsonPath("$.AIS.signing.keyRequirements.distinguishedNames[?(@.type=='STREET')].value", containsInAnyOrder("")))
                    .andExpect(jsonPath("$.AIS.signing.keyRequirements.distinguishedNames[?(@.type=='STREET')].placeholder", containsInAnyOrder("Street / First line of address")))
                    .andExpect(jsonPath("$.AIS.signing.keyRequirements.distinguishedNames[?(@.type=='STREET')].editable", containsInAnyOrder(true)))
                    .andExpect(jsonPath("$.AIS.signing.keyRequirements.distinguishedNames[?(@.type=='O')].value", containsInAnyOrder("")))
                    .andExpect(jsonPath("$.AIS.signing.keyRequirements.distinguishedNames[?(@.type=='O')].placeholder", containsInAnyOrder("Organization name")))
                    .andExpect(jsonPath("$.AIS.signing.keyRequirements.distinguishedNames[?(@.type=='O')].editable", containsInAnyOrder(true)))
                    .andExpect(jsonPath("$.AIS.signing.keyRequirements.distinguishedNames[?(@.type=='OU')].value", containsInAnyOrder("")))
                    .andExpect(jsonPath("$.AIS.signing.keyRequirements.distinguishedNames[?(@.type=='OU')].placeholder", containsInAnyOrder("Organizational unit")))
                    .andExpect(jsonPath("$.AIS.signing.keyRequirements.distinguishedNames[?(@.type=='OU')].editable", containsInAnyOrder(true)))
                    .andExpect(jsonPath("$.AIS.signing.keyRequirements.distinguishedNames[?(@.type=='2.5.4.97')].value", containsInAnyOrder("")))
                    .andExpect(jsonPath("$.AIS.signing.keyRequirements.distinguishedNames[?(@.type=='2.5.4.97')].placeholder", containsInAnyOrder("National PSP Identifier (XXXXX-XXX-123456)")))
                    .andExpect(jsonPath("$.AIS.signing.keyRequirements.distinguishedNames[?(@.type=='2.5.4.97')].editable", containsInAnyOrder(true)))
                    .andExpect(jsonPath("$.AIS.signing.keyRequirements.distinguishedNames[?(@.type=='CN')].value", containsInAnyOrder("")))
                    .andExpect(jsonPath("$.AIS.signing.keyRequirements.distinguishedNames[?(@.type=='CN')].placeholder", containsInAnyOrder("Common name (should be equal to 'O')")))
                    .andExpect(jsonPath("$.AIS.signing.keyRequirements.distinguishedNames[?(@.type=='CN')].editable", containsInAnyOrder(true)))
                    .andExpect(jsonPath("$.AIS.transport.keyRequirements.distinguishedNames", hasSize(8)))
                    .andExpect(jsonPath("$.AIS.transport.keyRequirements.distinguishedNames[?(@.type=='C')].value", containsInAnyOrder("")))
                    .andExpect(jsonPath("$.AIS.transport.keyRequirements.distinguishedNames[?(@.type=='C')].placeholder", containsInAnyOrder("Country Code")))
                    .andExpect(jsonPath("$.AIS.transport.keyRequirements.distinguishedNames[?(@.type=='C')].editable", containsInAnyOrder(true)))
                    .andExpect(jsonPath("$.AIS.transport.keyRequirements.distinguishedNames[?(@.type=='ST')].value", containsInAnyOrder("")))
                    .andExpect(jsonPath("$.AIS.transport.keyRequirements.distinguishedNames[?(@.type=='ST')].placeholder", containsInAnyOrder("State / Province")))
                    .andExpect(jsonPath("$.AIS.transport.keyRequirements.distinguishedNames[?(@.type=='ST')].editable", containsInAnyOrder(true)))
                    .andExpect(jsonPath("$.AIS.transport.keyRequirements.distinguishedNames[?(@.type=='L')].value", containsInAnyOrder("")))
                    .andExpect(jsonPath("$.AIS.transport.keyRequirements.distinguishedNames[?(@.type=='L')].placeholder", containsInAnyOrder("City (Locality Name)")))
                    .andExpect(jsonPath("$.AIS.transport.keyRequirements.distinguishedNames[?(@.type=='L')].editable", containsInAnyOrder(true)))
                    .andExpect(jsonPath("$.AIS.transport.keyRequirements.distinguishedNames[?(@.type=='STREET')].value", containsInAnyOrder("")))
                    .andExpect(jsonPath("$.AIS.transport.keyRequirements.distinguishedNames[?(@.type=='STREET')].placeholder", containsInAnyOrder("Street / First line of address")))
                    .andExpect(jsonPath("$.AIS.transport.keyRequirements.distinguishedNames[?(@.type=='STREET')].editable", containsInAnyOrder(true)))
                    .andExpect(jsonPath("$.AIS.transport.keyRequirements.distinguishedNames[?(@.type=='O')].value", containsInAnyOrder("")))
                    .andExpect(jsonPath("$.AIS.transport.keyRequirements.distinguishedNames[?(@.type=='O')].placeholder", containsInAnyOrder("Organization name")))
                    .andExpect(jsonPath("$.AIS.transport.keyRequirements.distinguishedNames[?(@.type=='O')].editable", containsInAnyOrder(true)))
                    .andExpect(jsonPath("$.AIS.transport.keyRequirements.distinguishedNames[?(@.type=='OU')].value", containsInAnyOrder("")))
                    .andExpect(jsonPath("$.AIS.transport.keyRequirements.distinguishedNames[?(@.type=='OU')].placeholder", containsInAnyOrder("Organizational unit")))
                    .andExpect(jsonPath("$.AIS.transport.keyRequirements.distinguishedNames[?(@.type=='OU')].editable", containsInAnyOrder(true)))
                    .andExpect(jsonPath("$.AIS.transport.keyRequirements.distinguishedNames[?(@.type=='2.5.4.97')].value", containsInAnyOrder("")))
                    .andExpect(jsonPath("$.AIS.transport.keyRequirements.distinguishedNames[?(@.type=='2.5.4.97')].placeholder", containsInAnyOrder("National PSP Identifier (XXXXX-XXX-123456)")))
                    .andExpect(jsonPath("$.AIS.transport.keyRequirements.distinguishedNames[?(@.type=='2.5.4.97')].editable", containsInAnyOrder(true)))
                    .andExpect(jsonPath("$.AIS.transport.keyRequirements.distinguishedNames[?(@.type=='CN')].value", containsInAnyOrder("yolt.io")))
                    .andExpect(jsonPath("$.AIS.transport.keyRequirements.distinguishedNames[?(@.type=='CN')].placeholder", containsInAnyOrder("Common name (SAN)")))
                    .andExpect(jsonPath("$.AIS.transport.keyRequirements.distinguishedNames[?(@.type=='CN')].editable", containsInAnyOrder(true)))
            ;
        } finally {
            verify(clientGroupIdVerificationService).verify(clientGroupToken, clientGroupId);
        }
    }

    @Test
    void testCreateCertificate_valid_input() throws Exception {
        UUID clientGroupId = UUID.randomUUID();
        ClientGroupToken clientGroupToken = testClientTokens.createClientGroupToken(clientGroupId,
                claims -> claims.setClaim(ClientTokenConstants.EXTRA_CLAIM_ISSUED_FOR, "dev-portal"));
        String keyId = UUID.randomUUID().toString();
        EIDASCertificateSigningRequestDTO inputDTO = new EIDASCertificateSigningRequestDTO(
                "certificateName",
                Set.of(ServiceType.AS),
                CertificateUsageType.SIGNING,
                "RSA2048",
                "SHA256_WITH_RSA",
                List.of(new SimpleDistinguishedNameElement("C", "NL")),
                Set.of()
        );
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
        when(eidasService.createCertificateSigningRequest(any(), any())).thenReturn(certificateDTO);

        mockMvc
                .perform(
                        post(BASE_URL, clientGroupId)
                                .header(CLIENT_TOKEN_HEADER_NAME, clientGroupToken.getSerialized())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(inputDTO))
                )
                .andExpect(status().isOk());
        verify(clientGroupIdVerificationService).verify(clientGroupToken, clientGroupId);
    }

    @ParameterizedTest
    @MethodSource
    void testCreateCertificate_bad_inputs(EIDASCertificateSigningRequestDTO inputDTO) throws Exception {
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

    static Stream<EIDASCertificateSigningRequestDTO> testCreateCertificate_bad_inputs() {
        return Stream.of(
                new EIDASCertificateSigningRequestDTO(
                        "invalid signature algorithm",
                        Set.of(ServiceType.AS),
                        CertificateUsageType.TRANSPORT,
                        "RSA4096",
                        "invalid",
                        List.of(new SimpleDistinguishedNameElement("C", "NL")),
                        Set.of("SAN1")
                ),
                new EIDASCertificateSigningRequestDTO(
                        "invalid key algorithm",
                        Set.of(ServiceType.AS),
                        CertificateUsageType.TRANSPORT,
                        "invalid",
                        "SHA256_WITH_RSA",
                        List.of(new SimpleDistinguishedNameElement("C", "NL")),
                        Set.of("SAN1")
                ),
                new EIDASCertificateSigningRequestDTO(
                        "transport key without SANs",
                        Set.of(ServiceType.AS),
                        CertificateUsageType.TRANSPORT,
                        "RSA4096",
                        "SHA256_WITH_RSA",
                        List.of(new SimpleDistinguishedNameElement("C", "NL")),
                        Set.of()
                ),
                new EIDASCertificateSigningRequestDTO(
                        "transport key without SANs",
                        Set.of(ServiceType.AS),
                        CertificateUsageType.TRANSPORT,
                        "RSA4096",
                        "SHA256_WITH_RSA",
                        List.of(new SimpleDistinguishedNameElement("C", "NL")),
                        null
                ),
                new EIDASCertificateSigningRequestDTO(
                        "",
                        Set.of(ServiceType.AS),
                        CertificateUsageType.SIGNING,
                        "RSA2048",
                        "SHA256_WITH_RSA",
                        List.of(new SimpleDistinguishedNameElement("C", "NL")),
                        Set.of()
                ),
                new EIDASCertificateSigningRequestDTO(
                        null,
                        Set.of(ServiceType.AS),
                        CertificateUsageType.SIGNING,
                        "RSA2048",
                        "SHA256_WITH_RSA",
                        List.of(new SimpleDistinguishedNameElement("C", "NL")),
                        Set.of()
                ),
                new EIDASCertificateSigningRequestDTO(
                        "no service types",
                        null,
                        CertificateUsageType.SIGNING,
                        "RSA2048",
                        "SHA256_WITH_RSA",
                        List.of(new SimpleDistinguishedNameElement("C", "NL")),
                        Set.of()
                ),
                new EIDASCertificateSigningRequestDTO(
                        "no key type",
                        Set.of(ServiceType.AS),
                        null,
                        "RSA2048",
                        "SHA256_WITH_RSA",
                        List.of(new SimpleDistinguishedNameElement("C", "NL")),
                        Set.of()
                ),
                new EIDASCertificateSigningRequestDTO(
                        "no key algorithm",
                        Set.of(ServiceType.AS),
                        CertificateUsageType.SIGNING,
                        null,
                        "SHA256_WITH_RSA",
                        List.of(new SimpleDistinguishedNameElement("C", "NL")),
                        Set.of()
                ),
                new EIDASCertificateSigningRequestDTO(
                        "no signing algorithm",
                        Set.of(ServiceType.AS),
                        CertificateUsageType.SIGNING,
                        "RSA2048",
                        null,
                        List.of(new SimpleDistinguishedNameElement("C", "NL")),
                        Set.of()
                ),
                new EIDASCertificateSigningRequestDTO(
                        "empty sdn list",
                        Set.of(ServiceType.AS),
                        CertificateUsageType.SIGNING,
                        "RSA2048",
                        "SHA256_WITH_RSA",
                        List.of(),
                        Set.of()
                ),
                new EIDASCertificateSigningRequestDTO(
                        "too many sdns",
                        Set.of(ServiceType.AS),
                        CertificateUsageType.SIGNING,
                        "RSA2048",
                        "SHA256_WITH_RSA",
                        range(0, 21).mapToObj(i -> new SimpleDistinguishedNameElement("C" + i, "NL" + i)).collect(Collectors.toList()),
                        Set.of()
                ),
                new EIDASCertificateSigningRequestDTO(
                        "no sdn list",
                        Set.of(ServiceType.AS),
                        CertificateUsageType.SIGNING,
                        "RSA2048",
                        "SHA256_WITH_RSA",
                        null,
                        Set.of()
                ),
                new EIDASCertificateSigningRequestDTO(
                        "name with invalid characters!",
                        Set.of(ServiceType.AS),
                        CertificateUsageType.SIGNING,
                        "RSA2048",
                        "SHA256_WITH_RSA",
                        List.of(new SimpleDistinguishedNameElement("C", "NL")),
                        Set.of()
                ),
                new EIDASCertificateSigningRequestDTO(
                        "Way too long name that exceeds the 128 characters length constraints 123456789012345678901234567890123456789012345678901234567890",
                        Set.of(ServiceType.AS),
                        CertificateUsageType.SIGNING,
                        "RSA2048",
                        "SHA256_WITH_RSA",
                        List.of(new SimpleDistinguishedNameElement("C", "NL")),
                        Set.of()
                ),
                new EIDASCertificateSigningRequestDTO(
                        "Too long SAN name",
                        Set.of(ServiceType.AS),
                        CertificateUsageType.SIGNING,
                        "RSA2048",
                        "SHA256_WITH_RSA",
                        List.of(new SimpleDistinguishedNameElement("C", "NL")),
                        Set.of("TooLongSAN1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890")
                ),
                new EIDASCertificateSigningRequestDTO(
                        "Too many SANs",
                        Set.of(ServiceType.AS),
                        CertificateUsageType.SIGNING,
                        "RSA2048",
                        "SHA256_WITH_RSA",
                        List.of(new SimpleDistinguishedNameElement("C", "NL")),
                        range(0, 101).mapToObj(i -> "SAN" + i).collect(Collectors.toSet())
                )
        );
    }

    @Test
    void testGetCertificates() throws Exception {
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
                null,
                null
        );

        when(eidasService.getCertificates(clientGroupToken)).thenReturn(List.of(certificateDTO));
        try {
            mockMvc
                    .perform(
                            get(BASE_URL, clientGroupId)
                                    .header(CLIENT_TOKEN_HEADER_NAME, clientGroupToken.getSerialized())
                                    .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].kid", is(keyId)));
        } finally {
            verify(clientGroupIdVerificationService).verify(clientGroupToken, clientGroupId);
        }
    }

    @Test
    void testGetCertificate() throws Exception {
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
                null,
                null
        );

        when(eidasService.getCertificate(clientGroupToken, keyId)).thenReturn(certificateDTO);
        try {
            mockMvc
                    .perform(
                            get(BASE_URL + "/{certificateId}", clientGroupId, keyId)
                                    .header(CLIENT_TOKEN_HEADER_NAME, clientGroupToken.getSerialized())
                                    .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.kid", is(keyId)));
        } finally {
            verify(clientGroupIdVerificationService).verify(clientGroupToken, clientGroupId);
        }
    }

    @Test
    void testGetCertificate_not_found() throws Exception {
        UUID clientGroupId = UUID.randomUUID();
        ClientGroupToken clientGroupToken = testClientTokens.createClientGroupToken(clientGroupId);
        String keyId = UUID.randomUUID().toString();

        when(eidasService.getCertificate(clientGroupToken, keyId)).thenThrow(new CertificateNotFoundException("oops"));
        try {
            mockMvc
                    .perform(
                            get(BASE_URL + "/{certificateId}", clientGroupId, keyId)
                                    .header(CLIENT_TOKEN_HEADER_NAME, clientGroupToken.getSerialized())
                                    .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code", is("CLS012")));
        } finally {
            verify(clientGroupIdVerificationService).verify(clientGroupToken, clientGroupId);
        }
    }

    @Test
    void testUpdateCertificateName() throws Exception {
        UUID clientGroupId = UUID.randomUUID();
        ClientGroupToken clientGroupToken = testClientTokens.createClientGroupToken(clientGroupId);
        String keyId = UUID.randomUUID().toString();
        NewCertificateNameDTO inputDTO = new NewCertificateNameDTO("new name");

        CertificateDTO certificateDTO = new CertificateDTO(
                "my signing-key",
                CertificateType.EIDAS,
                keyId,
                CertificateUsageType.SIGNING,
                Set.of(ServiceType.AIS),
                "RSA2048",
                "SHA256_WITH_RSA",
                null,
                null
        );

        when(eidasService.updateName(clientGroupToken, keyId, inputDTO)).thenReturn(certificateDTO);
        try {
            mockMvc
                    .perform(
                            put(BASE_URL + "/{certificateId}/name", clientGroupId, keyId)
                                    .header(CLIENT_TOKEN_HEADER_NAME, clientGroupToken.getSerialized())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsBytes(inputDTO))
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.kid", is(keyId)));
        } finally {
            verify(clientGroupIdVerificationService).verify(clientGroupToken, clientGroupId);
        }
    }

    @Test
    void testUpdateCertificateName_no_name() throws Exception {
        UUID clientGroupId = UUID.randomUUID();
        ClientGroupToken clientGroupToken = testClientTokens.createClientGroupToken(clientGroupId);
        String keyId = UUID.randomUUID().toString();
        NewCertificateNameDTO inputDTO = new NewCertificateNameDTO("");

        mockMvc
                .perform(
                        put(BASE_URL + "/{certificateId}/name", clientGroupId, keyId)
                                .header(CLIENT_TOKEN_HEADER_NAME, clientGroupToken.getSerialized())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(inputDTO))
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdateCertificateName_certificate_not_found() throws Exception {
        UUID clientGroupId = UUID.randomUUID();
        ClientGroupToken clientGroupToken = testClientTokens.createClientGroupToken(clientGroupId);
        String keyId = UUID.randomUUID().toString();
        NewCertificateNameDTO inputDTO = new NewCertificateNameDTO("new name");

        when(eidasService.updateName(clientGroupToken, keyId, inputDTO)).thenThrow(new CertificateNotFoundException("oops"));
        try {
            mockMvc
                    .perform(
                            put(BASE_URL + "/{certificateId}/name", clientGroupId, keyId)
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
    void testUpdateCertificateName_name_in_use() throws Exception {
        UUID clientGroupId = UUID.randomUUID();
        ClientGroupToken clientGroupToken = testClientTokens.createClientGroupToken(clientGroupId);
        String keyId = UUID.randomUUID().toString();
        NewCertificateNameDTO inputDTO = new NewCertificateNameDTO("new name");

        when(eidasService.updateName(clientGroupToken, keyId, inputDTO)).thenThrow(new NameIsAlreadyUsedException());
        try {
            mockMvc
                    .perform(
                            put(BASE_URL + "/{certificateId}/name", clientGroupId, keyId)
                                    .header(CLIENT_TOKEN_HEADER_NAME, clientGroupToken.getSerialized())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsBytes(inputDTO))
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code", is("CLS013")));
        } finally {
            verify(clientGroupIdVerificationService).verify(clientGroupToken, clientGroupId);
        }
    }

    @Test
    void testValidateCertificateChain() throws Exception {
        UUID clientGroupId = UUID.randomUUID();
        ClientGroupToken clientGroupToken = testClientTokens.createClientGroupToken(clientGroupId,
                claims -> claims.setClaim(ClientTokenConstants.EXTRA_CLAIM_ISSUED_FOR, "dev-portal"));
        String keyId = UUID.randomUUID().toString();
        byte[] pem = this.getClass().getResourceAsStream("valid-sandbox-chain.pem").readAllBytes();
        List<X509Certificate> certificateChain = KeyUtil.parseCertificateChain(new String(pem));
        CertificateChainDTO inputDTO = new CertificateChainDTO(Base64.getEncoder().encodeToString(pem));
        ValidatedCertificateChainDTO validatedCertificateChainDTO = new ValidatedCertificateChainDTO(true, null, null);

        when(eidasService.verifyCertificateChain(clientGroupToken, keyId, certificateChain)).thenReturn(validatedCertificateChainDTO);
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

        when(eidasService.verifyCertificateChain(clientGroupToken, keyId, certificateChain)).thenThrow(new CertificateNotFoundException("oops"));
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

        when(eidasService.updateCertificateChain(clientGroupToken, certificateId, certificateChain)).thenReturn(List.of(certificateInfoDTO));
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

        when(eidasService.updateCertificateChain(clientGroupToken, certificateId, certificateChain)).thenThrow(new CertificateValidationException("oops"));
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

        when(eidasService.updateCertificateChain(clientGroupToken, certificateId, certificateChain)).thenThrow(new CertificateAlreadySignedException());
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
    void testUpdateCertificateChain_invalid_chain_format() throws Exception {
        UUID clientGroupId = UUID.randomUUID();
        String certificateId = UUID.randomUUID().toString();
        ClientGroupToken clientGroupToken = testClientTokens.createClientGroupToken(clientGroupId,
                claims -> claims.setClaim(ClientTokenConstants.EXTRA_CLAIM_ISSUED_FOR, "dev-portal"));
        byte[] certificateChainPem = "not a pem".getBytes(StandardCharsets.UTF_8);
        CertificateChainDTO certificateChainDTO = new CertificateChainDTO(Base64.getEncoder().encodeToString(certificateChainPem));

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
    void testUpdateCertificateChain_wrong_token() throws Exception {
        UUID clientGroupId = UUID.randomUUID();
        UUID certificateId = UUID.randomUUID();
        ClientGroupToken clientGroupToken = testClientTokens.createClientGroupToken(clientGroupId,
                claims -> claims.setClaim(ClientTokenConstants.EXTRA_CLAIM_ISSUED_FOR, "other"));
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

    @Test
    void testSignEIDASCSR() throws Exception {
        UUID clientGroupId = UUID.randomUUID();
        String certificateId = UUID.randomUUID().toString();
        ClientGroupToken clientGroupToken = testClientTokens.createClientGroupToken(clientGroupId,
                claims -> claims.setClaim(ClientTokenConstants.EXTRA_CLAIM_ISSUED_FOR, "dev-portal"));
        String certChain = "certificate chain";

        when(eidasService.sign(clientGroupToken, certificateId)).thenReturn(certChain);
        mockMvc
                .perform(
                        post(BASE_URL + "/{certificateId}/sign", clientGroupId, certificateId)
                                .header(CLIENT_TOKEN_HEADER_NAME, clientGroupToken.getSerialized())
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.certificateChain", is(certChain)));

        verify(clientGroupIdVerificationService).verify(clientGroupToken, clientGroupId);
    }

    @Test
    void testSignEIDASCSR_certificate_already_signed() throws Exception {
        UUID clientGroupId = UUID.randomUUID();
        String certificateId = UUID.randomUUID().toString();
        ClientGroupToken clientGroupToken = testClientTokens.createClientGroupToken(clientGroupId,
                claims -> claims.setClaim(ClientTokenConstants.EXTRA_CLAIM_ISSUED_FOR, "dev-portal"));
        when(eidasService.sign(clientGroupToken, certificateId)).thenThrow(new CertificateAlreadySignedException());
        mockMvc
                .perform(
                        post(BASE_URL + "/{certificateId}/sign", clientGroupId, certificateId)
                                .header(CLIENT_TOKEN_HEADER_NAME, clientGroupToken.getSerialized())
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("CLS015")));

        verify(clientGroupIdVerificationService).verify(clientGroupToken, clientGroupId);
    }
}
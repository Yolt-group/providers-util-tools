package com.yolt.clients.clientgroup.certificatemanagement.other;

import com.yolt.clients.clientgroup.certificatemanagement.CertificateService;
import com.yolt.clients.clientgroup.certificatemanagement.dto.CertificateDTO;
import com.yolt.clients.clientgroup.certificatemanagement.dto.CertificateType;
import com.yolt.clients.clientgroup.certificatemanagement.dto.CertificateUsageType;
import com.yolt.clients.clientgroup.certificatemanagement.exceptions.CertificateNotFoundException;
import nl.ing.lovebird.clienttokens.ClientGroupToken;
import nl.ing.lovebird.clienttokens.test.TestClientTokens;
import nl.ing.lovebird.clienttokens.verification.ClientGroupIdVerificationService;
import nl.ing.lovebird.providerdomain.ServiceType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static nl.ing.lovebird.clienttokens.constants.ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OtherCertificatesController.class)
class OtherCertificatesControllerTest {
    private static final String BASE_URL = "/internal/client-groups/{clientGroupId}/other-certificates";
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestClientTokens testClientTokens;

    @MockBean
    private ClientGroupIdVerificationService clientGroupIdVerificationService;

    @MockBean
    private CertificateService certificateService;

    @AfterEach
    public void validateServices() {
        verifyNoMoreInteractions(ignoreStubs(
                clientGroupIdVerificationService,
                certificateService
        ));
    }

    @Test
    void testGetCertificates() throws Exception {
        UUID clientGroupId = UUID.randomUUID();
        ClientGroupToken clientGroupToken = testClientTokens.createClientGroupToken(clientGroupId);
        String keyId = UUID.randomUUID().toString();

        CertificateDTO certificateDTO = new CertificateDTO(
                "my signing-key",
                CertificateType.OTHER,
                keyId,
                CertificateUsageType.SIGNING,
                Set.of(ServiceType.AIS),
                "RSA2048",
                "SHA256_WITH_RSA",
                null,
                null
        );

        when(certificateService.getCertificatesByCertificateType(clientGroupId, CertificateType.OTHER)).thenReturn(List.of(certificateDTO));
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

        when(certificateService.getCertificateByIdAndType(clientGroupToken, CertificateType.OTHER, keyId)).thenReturn(certificateDTO);
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

        when(certificateService.getCertificateByIdAndType(clientGroupToken, CertificateType.OTHER, keyId)).thenThrow(new CertificateNotFoundException("oops"));
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
}
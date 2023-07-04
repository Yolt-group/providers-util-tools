package com.yolt.clients.clientgroup.certificatemanagement.eidas;

import com.yolt.clients.clientgroup.certificatemanagement.CertificateValidationService;
import com.yolt.clients.clientgroup.certificatemanagement.KeyUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EIDASValidationServiceTest {

    private EIDASValidationService eidasValidationService;
    private EIDASValidationService eidasValidationServiceWithYoltCA;

    @Mock
    private CertificateValidationService certificateValidationService;

    @Captor
    private ArgumentCaptor<X509Certificate[]> certificatesCaptor;

    @BeforeEach
    void setUp() {
        URL truststore = this.getClass().getResource("truststore.jks");
        eidasValidationService = new EIDASValidationService(
                false,
                truststore.toString(),
                certificateValidationService
        );
        eidasValidationServiceWithYoltCA = new EIDASValidationService(
                true,
                truststore.toString(),
                certificateValidationService
        );
    }

    @Test
    void validateCertificateChain() throws Exception {
        byte[] pem = this.getClass().getResourceAsStream("valid-sandbox-chain.pem").readAllBytes();
        List<X509Certificate> certificateChain = KeyUtil.parseCertificateChain(new String(pem));

        eidasValidationService.validateCertificateChain(certificateChain);

        verify(certificateValidationService).validateCertPath(certificatesCaptor.capture(), eq(certificateChain));
        verify(certificateValidationService).hasQCStatementsExtension(certificateChain.get(0));

        assertThat(certificatesCaptor.getValue()).extracting(X509Certificate::getSubjectX500Principal).asString().doesNotContain(
                "CN=Root, OU=Yolt, O=Yolt, L=Amsterdam, ST=Noord-Holland, C=NL"
        );
    }

    @Test
    void validateCertificateChainWithYoltCA() throws Exception {
        byte[] pem = this.getClass().getResourceAsStream("valid-sandbox-chain.pem").readAllBytes();
        List<X509Certificate> certificateChain = KeyUtil.parseCertificateChain(new String(pem));

        eidasValidationServiceWithYoltCA.validateCertificateChain(certificateChain);

        verify(certificateValidationService).validateCertPath(certificatesCaptor.capture(), eq(certificateChain));
        verify(certificateValidationService).hasQCStatementsExtension(certificateChain.get(0));

        assertThat(certificatesCaptor.getValue()).extracting(X509Certificate::getSubjectX500Principal).asString().contains(
                "CN=Root, OU=Yolt, O=Yolt, L=Amsterdam, ST=Noord-Holland, C=NL"
        );
    }
}
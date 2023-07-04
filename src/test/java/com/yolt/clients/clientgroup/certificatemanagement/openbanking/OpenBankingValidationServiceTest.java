package com.yolt.clients.clientgroup.certificatemanagement.openbanking;

import com.yolt.clients.clientgroup.certificatemanagement.CertificateValidationService;
import com.yolt.clients.clientgroup.certificatemanagement.exceptions.CertificateValidationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.security.auth.x500.X500Principal;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OpenBankingValidationServiceTest {

    private OpenBankingValidationService openBankingValidationService;
    @Mock
    private CertificateValidationService certificateValidationService;
    @Captor
    private ArgumentCaptor<X509Certificate[]> certificatesCaptor;

    @BeforeEach
    void setUp() {
        URL truststore = this.getClass().getResource("truststore.jks");
        openBankingValidationService = new OpenBankingValidationService(
                truststore.toString(),
                certificateValidationService
        );
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void validateLegacyCertificateChain() throws Exception {
        X509Certificate mockedCertificate = mock(X509Certificate.class);
        when(mockedCertificate.getSubjectX500Principal()).thenReturn(new X500Principal("CN=test,C=GB,O=OpenBanking,OU=JUnit"));
        List<X509Certificate> certificateChain = List.of(mockedCertificate);

        openBankingValidationService.validateLegacyCertificateChain(certificateChain);

        verify(certificateValidationService).validateCertPath(certificatesCaptor.capture(), eq(certificateChain));

        assertThat(certificatesCaptor.getValue()).extracting(X509Certificate::getSubjectX500Principal).asString().contains(
                "CN=Root, OU=Yolt, O=Yolt, L=Amsterdam, ST=Noord-Holland, C=NL"
        );
    }

    @Test
    void validateLegacyCertificateChain_invalid_country() throws Exception {
        X509Certificate mockedCertificate = mock(X509Certificate.class);
        when(mockedCertificate.getSubjectX500Principal()).thenReturn(new X500Principal("CN=test,C=NL,O=OpenBanking,OU=JUnit"));
        List<X509Certificate> certificateChain = List.of(mockedCertificate);

        CertificateValidationException exception = assertThrows(CertificateValidationException.class, () -> openBankingValidationService.validateLegacyCertificateChain(certificateChain));

        assertThat(exception.getMessage()).isEqualTo("Expected C to be GB for certificate");
        verify(certificateValidationService).validateCertPath(certificatesCaptor.capture(), eq(certificateChain));
        assertThat(certificatesCaptor.getValue()).extracting(X509Certificate::getSubjectX500Principal).asString().contains(
                "CN=Root, OU=Yolt, O=Yolt, L=Amsterdam, ST=Noord-Holland, C=NL"
        );
    }

    @Test
    void validateLegacyCertificateChain_invalid_organisation() throws Exception {
        X509Certificate mockedCertificate = mock(X509Certificate.class);
        when(mockedCertificate.getSubjectX500Principal()).thenReturn(new X500Principal("CN=test,C=GB,O=other,OU=JUnit"));
        List<X509Certificate> certificateChain = List.of(mockedCertificate);

        CertificateValidationException exception = assertThrows(CertificateValidationException.class, () -> openBankingValidationService.validateLegacyCertificateChain(certificateChain));

        assertThat(exception.getMessage()).isEqualTo("Expected O to be OpenBanking for certificate");
        verify(certificateValidationService).validateCertPath(certificatesCaptor.capture(), eq(certificateChain));
        assertThat(certificatesCaptor.getValue()).extracting(X509Certificate::getSubjectX500Principal).asString().contains(
                "CN=Root, OU=Yolt, O=Yolt, L=Amsterdam, ST=Noord-Holland, C=NL"
        );
    }

    @Test
    void validateEtsiCertificateChain() throws Exception {
        X509Certificate mockedCertificate = mock(X509Certificate.class);
        List<X509Certificate> certificateChain = List.of(mockedCertificate);

        openBankingValidationService.validateEtsiCertificateChain(certificateChain);

        verify(certificateValidationService).validateCertPath(certificatesCaptor.capture(), eq(certificateChain));
        verify(certificateValidationService).hasQCStatementsExtension(certificateChain.get(0));

        assertThat(certificatesCaptor.getValue()).extracting(X509Certificate::getSubjectX500Principal).asString().contains(
                "CN=Root, OU=Yolt, O=Yolt, L=Amsterdam, ST=Noord-Holland, C=NL"
        );
    }
}
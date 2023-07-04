package com.yolt.clients.clientgroup.certificatemanagement.eidas;

import com.yolt.clients.clientgroup.certificatemanagement.CertificateService;
import com.yolt.clients.clientgroup.certificatemanagement.aws.S3StorageClient;
import com.yolt.clients.clientgroup.certificatemanagement.dto.*;
import com.yolt.clients.clientgroup.certificatemanagement.exceptions.CertificateAlreadySignedException;
import com.yolt.clients.clientgroup.certificatemanagement.exceptions.CertificateValidationException;
import com.yolt.clients.clientgroup.certificatemanagement.yoltbank.YoltbankService;
import nl.ing.lovebird.clienttokens.ClientGroupToken;
import nl.ing.lovebird.providerdomain.ServiceType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EIDASServiceTest {
    private static final CertificateType CERTIFICATE_TYPE = CertificateType.EIDAS;

    @Mock
    private EIDASValidationService eidasValidationService;
    @Mock
    private CertificateService certificateService;
    @Mock
    private S3StorageClient s3StorageClient;
    @Mock
    private JWKSService jwksService;
    @Mock
    private YoltbankService yoltbankService;
    @Mock
    private ClientGroupToken clientGroupToken;

    @InjectMocks
    private EIDASService eidasService;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(ignoreStubs(
                eidasValidationService,
                certificateService,
                s3StorageClient,
                jwksService,
                yoltbankService
        ));
    }

    @Test
    void createCertificateSigningRequest() {
        CertificateSigningRequestDTO certificateSigningRequestDTO = new CertificateSigningRequestDTO(
                "invalid signature algorithm",
                Set.of(ServiceType.AS),
                CertificateUsageType.TRANSPORT,
                "RSA4096",
                "invalid",
                List.of(new SimpleDistinguishedNameElement("C", "NL")),
                Set.of("SAN1")
        );
        CertificateDTO certificateDTO = new CertificateDTO(
                "my signing-key",
                CertificateType.EIDAS,
                UUID.randomUUID().toString(),
                CertificateUsageType.SIGNING,
                Set.of(ServiceType.AIS),
                "RSA2048",
                "SHA256_WITH_RSA",
                "signingRequest",
                null
        );
        when(certificateService.createCertificateSigningRequest(clientGroupToken, certificateSigningRequestDTO, CERTIFICATE_TYPE))
                .thenReturn(certificateDTO);

        CertificateDTO result = eidasService.createCertificateSigningRequest(clientGroupToken, certificateSigningRequestDTO);

        assertThat(result).isSameAs(certificateDTO);
    }

    @Test
    void getCertificates() {
        CertificateDTO certificateDTO = new CertificateDTO(
                "my signing-key",
                CertificateType.EIDAS,
                UUID.randomUUID().toString(),
                CertificateUsageType.SIGNING,
                Set.of(ServiceType.AIS),
                "RSA2048",
                "SHA256_WITH_RSA",
                "signingRequest",
                null
        );

        when(certificateService.getCertificatesByCertificateType(clientGroupToken.getClientGroupIdClaim(), CERTIFICATE_TYPE)).thenReturn(List.of(certificateDTO));

        List<CertificateDTO> result = eidasService.getCertificates(clientGroupToken);

        assertThat(result).containsExactlyInAnyOrder(certificateDTO);
    }

    @Test
    void getCertificate() {
        String certificateId = UUID.randomUUID().toString();
        CertificateDTO certificateDTO = new CertificateDTO(
                "my signing-key",
                CertificateType.EIDAS,
                certificateId,
                CertificateUsageType.SIGNING,
                Set.of(ServiceType.AIS),
                "RSA2048",
                "SHA256_WITH_RSA",
                "signingRequest",
                null
        );

        when(certificateService.getCertificateByIdAndType(clientGroupToken, CERTIFICATE_TYPE, certificateId)).thenReturn(certificateDTO);

        CertificateDTO result = eidasService.getCertificate(clientGroupToken, certificateId);

        assertThat(result).isSameAs(certificateDTO);
    }

    @Test
    void updateName() {
        String certificateId = UUID.randomUUID().toString();
        CertificateDTO certificateDTO = new CertificateDTO(
                "my signing-key",
                CertificateType.EIDAS,
                certificateId,
                CertificateUsageType.SIGNING,
                Set.of(ServiceType.AIS),
                "RSA2048",
                "SHA256_WITH_RSA",
                "signingRequest",
                null
        );
        NewCertificateNameDTO newCertificateNameDTO = new NewCertificateNameDTO("");

        when(certificateService.updateCertificateNameForId(clientGroupToken, CERTIFICATE_TYPE, certificateId, newCertificateNameDTO)).thenReturn(certificateDTO);

        CertificateDTO result = eidasService.updateName(clientGroupToken, certificateId, newCertificateNameDTO);

        assertThat(result).isSameAs(certificateDTO);
    }

    @Test
    void verifyCertificateChain() throws Exception {
        String certificateId = UUID.randomUUID().toString();
        X509Certificate x509Certificate = mock(X509Certificate.class);
        List<X509Certificate> certificateChain = List.of(x509Certificate);
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

        when(certificateService.validateCertificate(clientGroupToken, CERTIFICATE_TYPE, certificateId, x509Certificate)).thenReturn(certificateInfoDTO);

        ValidatedCertificateChainDTO result = eidasService.verifyCertificateChain(clientGroupToken, certificateId, certificateChain);
        assertThat(result.isValid()).isTrue();
        assertThat(result.getMessage()).isNull();
        assertThat(result.getInfo()).isSameAs(certificateInfoDTO);

        verify(eidasValidationService).validateCertificateChain(certificateChain);
    }

    @Test
    void verifyCertificateChain_invalid_leaf() throws Exception {
        String certificateId = UUID.randomUUID().toString();
        X509Certificate x509Certificate = mock(X509Certificate.class);
        List<X509Certificate> certificateChain = List.of(x509Certificate);

        when(certificateService.validateCertificate(clientGroupToken, CERTIFICATE_TYPE, certificateId, x509Certificate))
                .thenThrow(new CertificateValidationException("oops"));

        ValidatedCertificateChainDTO result = eidasService.verifyCertificateChain(clientGroupToken, certificateId, certificateChain);
        assertThat(result.isValid()).isFalse();
        assertThat(result.getMessage()).isEqualTo("oops");
        assertThat(result.getInfo()).isNull();

        verify(eidasValidationService).validateCertificateChain(certificateChain);
    }

    @Test
    void verifyCertificateChain_invalid_chain() throws Exception {
        String certificateId = UUID.randomUUID().toString();
        X509Certificate x509Certificate = mock(X509Certificate.class);
        List<X509Certificate> certificateChain = List.of(x509Certificate);

        doThrow(new CertificateValidationException("oops")).when(eidasValidationService).validateCertificateChain(certificateChain);

        ValidatedCertificateChainDTO result = eidasService.verifyCertificateChain(clientGroupToken, certificateId, certificateChain);
        assertThat(result.isValid()).isFalse();
        assertThat(result.getMessage()).isEqualTo("oops");
        assertThat(result.getInfo()).isNull();
    }

    @Test
    void updateCertificateChain() throws Exception {
        String certificateId = UUID.randomUUID().toString();
        X509Certificate x509Certificate = mock(X509Certificate.class);
        List<X509Certificate> certificateChain = List.of(x509Certificate);
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

        when(certificateService.updateCertificateChainForId(clientGroupToken, CERTIFICATE_TYPE, certificateId, certificateChain)).thenReturn(List.of(certificateInfoDTO));

        List<CertificateInfoDTO> result = eidasService.updateCertificateChain(clientGroupToken, certificateId, certificateChain);

        assertThat(result).containsExactly(certificateInfoDTO);
        verify(eidasValidationService).validateCertificateChain(certificateChain);
        verify(s3StorageClient).storeCertificate(certificateChain);
        verify(jwksService).createJWKSJsonOnS3(clientGroupToken);
    }

    @Test
    void sign() {
        String certificateId = UUID.randomUUID().toString();
        String csr = "signingCSR";
        CertificateDTO certificateDTO = new CertificateDTO(
                "my signing-key",
                CertificateType.EIDAS,
                certificateId,
                CertificateUsageType.SIGNING,
                Set.of(ServiceType.AIS),
                "RSA2048",
                "SHA256_WITH_RSA",
                csr,
                null
        );
        String signedChain = "signed cert chain";

        when(certificateService.getCertificateByIdAndType(clientGroupToken, CERTIFICATE_TYPE, certificateId)).thenReturn(certificateDTO);
        when(yoltbankService.signCSR(csr)).thenReturn(signedChain);

        String result = eidasService.sign(clientGroupToken, certificateId);

        assertThat(result).isEqualTo(signedChain);
    }

    @Test
    void sign_already_signed_certificate() {
        String certificateId = UUID.randomUUID().toString();
        String csr = "signingCSR";
        String signedChain = "signed cert chain";
        CertificateDTO certificateDTO = new CertificateDTO(
                "my signing-key",
                CertificateType.EIDAS,
                certificateId,
                CertificateUsageType.SIGNING,
                Set.of(ServiceType.AIS),
                "RSA2048",
                "SHA256_WITH_RSA",
                csr,
                signedChain
        );

        when(certificateService.getCertificateByIdAndType(clientGroupToken, CERTIFICATE_TYPE, certificateId)).thenReturn(certificateDTO);

        assertThrows(CertificateAlreadySignedException.class, () -> eidasService.sign(clientGroupToken, certificateId));
    }
}

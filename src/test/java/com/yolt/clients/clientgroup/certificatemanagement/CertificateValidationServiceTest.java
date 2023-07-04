package com.yolt.clients.clientgroup.certificatemanagement;

import com.yolt.clients.TestConfiguration;
import com.yolt.clients.clientgroup.certificatemanagement.crypto.CryptoService;
import com.yolt.clients.clientgroup.certificatemanagement.crypto.SignatureAlgorithm;
import com.yolt.clients.clientgroup.certificatemanagement.dto.CertificateUsageType;
import com.yolt.clients.clientgroup.certificatemanagement.exceptions.CertificateValidationException;
import nl.ing.lovebird.clienttokens.ClientGroupToken;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Base64;
import org.jose4j.jwt.JwtClaims;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static nl.ing.lovebird.clienttokens.constants.ClientTokenConstants.EXTRA_CLAIM_CLIENT_GROUP_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CertificateValidationServiceTest {
    private static final byte[] SECRET = "We walked in darkness, kept hittin' the walls".getBytes(StandardCharsets.UTF_8);
    private static final String CERTIFICATE = """
            -----BEGIN CERTIFICATE-----
            MIIDdjCCAl6gAwIBAgIJAN+kC5AB/WxCMA0GCSqGSIb3DQEBCwUAMFAxCzAJBgNV
            BAYTAk5MMRMwEQYDVQQIDApTb21lLVN0YXRlMQwwCgYDVQQKDANBQkMxHjAcBgNV
            BAMMFVRwcFNhbmRib3hDZXJ0aWZpY2F0ZTAeFw0yMDAyMTkxNjE5MDRaFw0zMDAy
            MTYxNjE5MDRaMFAxCzAJBgNVBAYTAk5MMRMwEQYDVQQIDApTb21lLVN0YXRlMQww
            CgYDVQQKDANBQkMxHjAcBgNVBAMMFVRwcFNhbmRib3hDZXJ0aWZpY2F0ZTCCASIw
            DQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAJkUcwLLs9dr3ngvR5ULGHoCvCpy
            aeVA9s/VBZ4q4ysbLAjdGux3mR18GKtGMkIeB1Dmw65vBdJWCBXxbPdeWA3ZRoC6
            yUCU6w5HMg0IMMq4Z5dbUs5cgvrUF1ZD12uUW/4zSQ6dw4DpyVzE2rDQ88dSGBGS
            C2U/Ql3aR8W2RaDL0Ii5MobKM1VtCrL2bjGKyPf4rViJZDrvFQBBH2WzlGJnDQVY
            xgQnINVQa1lIY+B/gNvm1iw/znAqeAN38FrNNXy6LpHXmi7viDh1/pBMbG2L6SRn
            uSOu79QrXaMPsaupklEHlyrY5s/SDvsjgEGC3IQNVduAL87zhjTLt+ElGPcCAwEA
            AaNTMFEwHQYDVR0OBBYEFC845pGDdNNAtsOkJLHxJoQc8LZdMB8GA1UdIwQYMBaA
            FC845pGDdNNAtsOkJLHxJoQc8LZdMA8GA1UdEwEB/wQFMAMBAf8wDQYJKoZIhvcN
            AQELBQADggEBAHyKoI4s7Lf0lnMOE7/FmLZf1dIOmvda2n08bxmwLGBna/o6XaND
            tt9OjXm6t1I1Wc/gVaR8mhRwXYVSgiQ6AYlgCnL+JjY5SSrGrpu0RkE4uputx+Nf
            QAC21GOSfMX22MLqkopaF4hwB0nKCEAToiM3RboUOeFBdK5AFawSYbeYXLJtHGsl
            OyZ4sFHgMo9w1ivAZNv3XpDBEA5t8qJ4hDJmERXiPx3I1hwsrO2MUtNKvSbh/L+3
            fGaVny7kTSJ04wFiOgUP6yd+N5e7nXpCXuxfaL66r78lF2wxzTGcgq89QeXBlXNy
            bJGQnWkkpwM6ANf5F4WcsAPLke7G731rmsg=
            -----END CERTIFICATE-----
            """;
    private static final String PRIVATE_KEY = """
            MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCZFHMCy7PXa954
            L0eVCxh6ArwqcmnlQPbP1QWeKuMrGywI3Rrsd5kdfBirRjJCHgdQ5sOubwXSVggV
            8Wz3XlgN2UaAuslAlOsORzINCDDKuGeXW1LOXIL61BdWQ9drlFv+M0kOncOA6clc
            xNqw0PPHUhgRkgtlP0Jd2kfFtkWgy9CIuTKGyjNVbQqy9m4xisj3+K1YiWQ67xUA
            QR9ls5RiZw0FWMYEJyDVUGtZSGPgf4Db5tYsP85wKngDd/BazTV8ui6R15ou74g4
            df6QTGxti+kkZ7kjru/UK12jD7GrqZJRB5cq2ObP0g77I4BBgtyEDVXbgC/O84Y0
            y7fhJRj3AgMBAAECggEARGydlAxVkN8IjBQmHPrer/r0/MwzhWPqbq+7WR22eRgm
            MLgURsqWyFUl+bjg0ij2ADWGFjxOD9ygtJ47pL6pAVezaesT9igagUFVn/mfRZ3z
            v/X0J4W2jkOrQsYETnP8Qr3N1Bi0wLS/axYa4pojvV52n7P2IAWMtsLQ/hEhQmPm
            7IqInzsIEtZTOfOcTqOK1xe3nXaouZVl2jXhJlBAv4tsO7e9RmWSeanB5qSZ0SmN
            Surc/9ukFoi9s/SuW5yPxe/LmHMr6CCu5U5spzW5XccWSlnEZ2wxm+Cnipu6AXna
            TAhPqDMJLGCYrxqOt3HJCCSIwESx9Nst2sS32ij+gQKBgQDOVVxRKwBzHwGhEPAG
            mdmFYKoxKE0QTmrxBlhPpAx1F1S0e9cxyRqIh1BIL/itdgbQgNyI5yKIKrgt+gFO
            KUP04Vzu5BBDYxhtCkdf184kqe2VweTLGVGMho/P2I026BHw2/VLO37yng71eP4R
            C+VLroZQBed/2z1hWrijBah6FwKBgQC97YIQM5v27/HjPgazE+jmp7Iw2RaJ7cGL
            rS70zt8T1Fec75ze9x9S3xDVJ1T8pP5E5Zq0pVwpJsPtQfvdkEsxhN+1X5fLuuYg
            1+Y1hh5SOGcB31a0NoJK2dBOuC6CjHfOJkaBjY5XQ8Q1fkXSm1DDan7+GiHnrPxU
            Thafw8MEIQKBgG4gS0SbQgMvwmvYIXQ0e0/f9xaDnxYb9KIuM8ZWFbwNNs2Z55KP
            9pR2PFg7GmxiuWJh1NNRIjIxMtp/PGEeT0INYs+ydCezZV8VhGDYSxNwivlKYrYw
            DkGFtI5H059BoAnBLJv55ljSGcPUzy4D/l81iER/0j6AorMqe6+vHmwDAoGBAIbm
            bKw/S/cQJJnIU4/cg19ZGyqw9t5O/lrMTn7ZdP8ronM4ig6gLiJ5iAYuIqI0OtoK
            z2Ch1xzviNg7Nr7/nzjz7MVxuWqePJh1YPEBawXxQ9DDplzoHpE1tkxDa92UEgBd
            lVSti72Vx4ZLQyK86Jd0S/EF9LEOYEctE8q0jA6hAoGANPZaEQ0Fljrfg7gKJugK
            AOC2QXqCzKAejwW6MaZP4b4Mb+sQBBI6J8L8KW5c0U/kYE3tNNk29qeo0jV5/q9X
            eCzGzWS+tpHdePx9P9WevQeoNNPAKVmBs+8JO5IR126H5N+Qq3VHAD19V5AKHL4d
            aI2epGY4MIV94xxhI6IvEzk=""";

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private CertificateValidationService certificateValidationService;

    @Mock
    private CryptoService cryptoService;

    private UUID clientGroupId;
    private ClientGroupToken clientGroupToken;
    private CertificateUsageType certificateUsageType;
    private String certificateId;
    private X509Certificate x509Certificate;
    private X509Certificate[] trustedCAs;

    @BeforeEach
    void setUp() {
        certificateValidationService = new CertificateValidationService(cryptoService, TestConfiguration.FIXED_CLOCK);

        clientGroupId = UUID.randomUUID();
        JwtClaims jwtClaims = new JwtClaims();
        jwtClaims.setStringClaim(EXTRA_CLAIM_CLIENT_GROUP_ID, clientGroupId.toString());
        clientGroupToken = new ClientGroupToken("serialized", jwtClaims);
        certificateUsageType = CertificateUsageType.TRANSPORT;
        certificateId = UUID.randomUUID().toString();
        x509Certificate = KeyUtil.parseCertificateChain(CERTIFICATE).get(0);
        trustedCAs = getTrustedCAs();
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void validateValidity() throws Exception {
        x509Certificate = mock(X509Certificate.class);

        certificateValidationService.validateValidity(x509Certificate);

        verify(x509Certificate).checkValidity(Date.from(Instant.now(TestConfiguration.FIXED_CLOCK)));
    }

    @Test
    void validateValidity_certificate_not_yet_valid() throws Exception {
        x509Certificate = mock(X509Certificate.class);
        Throwable cause = new CertificateNotYetValidException();
        doThrow(cause).when(x509Certificate).checkValidity(Date.from(Instant.now(TestConfiguration.FIXED_CLOCK)));

        CertificateValidationException exception = assertThrows(CertificateValidationException.class, () -> certificateValidationService.validateValidity(x509Certificate));

        assertThat(exception.getMessage()).isEqualTo("Certificate is not valid");
        assertThat(exception.getCause()).isEqualTo(cause);
        verify(x509Certificate).checkValidity(Date.from(Instant.now(TestConfiguration.FIXED_CLOCK)));
    }

    @Test
    void validateValidity_certificate_expired() throws Exception {
        x509Certificate = mock(X509Certificate.class);
        X509Certificate x509Certificate = mock(X509Certificate.class);
        Throwable cause = new CertificateExpiredException();
        doThrow(cause).when(x509Certificate).checkValidity(Date.from(Instant.now(TestConfiguration.FIXED_CLOCK)));

        CertificateValidationException exception = assertThrows(CertificateValidationException.class, () -> certificateValidationService.validateValidity(x509Certificate));

        assertThat(exception.getMessage()).isEqualTo("Certificate is not valid");
        assertThat(exception.getCause()).isEqualTo(cause);
        verify(x509Certificate).checkValidity(Date.from(Instant.now(TestConfiguration.FIXED_CLOCK)));
    }

    @Test
    void validateCertificateWithPrivateKey() throws Exception {
        when(cryptoService.sign(clientGroupToken, certificateUsageType, certificateId, SignatureAlgorithm.SHA256_WITH_RSA, SECRET))
                .thenReturn(createSignature());

        certificateValidationService.validateCertificateWithPrivateKey(clientGroupToken, certificateUsageType, certificateId, x509Certificate);

        verify(cryptoService).sign(clientGroupToken, certificateUsageType, certificateId, SignatureAlgorithm.SHA256_WITH_RSA, SECRET);
    }

    @Test
    void validateCertificateWithPrivateKey_with_non_mathing_private_key() {
        when(cryptoService.sign(clientGroupToken, certificateUsageType, certificateId, SignatureAlgorithm.SHA256_WITH_RSA, SECRET))
                .thenReturn(new String(Base64.encode("corrupted private key".getBytes())));

        CertificateValidationException exception = assertThrows(
                CertificateValidationException.class,
                () -> certificateValidationService.validateCertificateWithPrivateKey(clientGroupToken, certificateUsageType, certificateId, x509Certificate)
        );

        assertThat(exception.getMessage()).isEqualTo("Verification of certificate failed.");
        verify(cryptoService).sign(clientGroupToken, certificateUsageType, certificateId, SignatureAlgorithm.SHA256_WITH_RSA, SECRET);
    }

    @Test
    void hasQCStatementsExtension() throws Exception {
        x509Certificate = mock(X509Certificate.class);
        byte[] extensionBytes = new byte[5];

        when(x509Certificate.getExtensionValue(Extension.qCStatements.getId())).thenReturn(extensionBytes);

        certificateValidationService.hasQCStatementsExtension(x509Certificate);

        verify(x509Certificate).getExtensionValue(Extension.qCStatements.getId());
    }

    @Test
    void hasQCStatementsExtension_with_no_qc_extension() {
        x509Certificate = mock(X509Certificate.class);

        when(x509Certificate.getExtensionValue(Extension.qCStatements.getId())).thenReturn(null);

        CertificateValidationException exception = assertThrows(
                CertificateValidationException.class,
                () -> certificateValidationService.hasQCStatementsExtension(x509Certificate)
        );

        assertThat(exception.getMessage()).isEqualTo("Expected the qcStatements extension to be in the certificate");
        verify(x509Certificate).getExtensionValue(Extension.qCStatements.getId());
    }

    @Test
    void validateCertPath() throws Exception {
        List<X509Certificate> certificateChain = KeyUtil.parseCertificateChain(
                new String(this.getClass().getResourceAsStream("valid-yoltbank-chain.pem").readAllBytes())
        );
        certificateValidationService.validateCertPath(trustedCAs, certificateChain);
    }

    @Test
    void validateCertPath_untrusted_root() throws Exception {
        List<X509Certificate> certificateChain = KeyUtil.parseCertificateChain(
                new String(this.getClass().getResourceAsStream("valid-sandbox-chain.pem").readAllBytes())
        );
        CertificateValidationException exception = assertThrows(
                CertificateValidationException.class,
                () -> certificateValidationService.validateCertPath(trustedCAs, certificateChain)
        );
        assertThat(exception.getMessage()).isEqualTo("Certificate chain is not trusted");
    }

    @Test
    void validateCertPath_bad_chain() throws Exception {
        List<X509Certificate> certificateChain = KeyUtil.parseCertificateChain(
                new String(this.getClass().getResourceAsStream("invalid-chain.pem").readAllBytes())
        );
        CertificateValidationException exception = assertThrows(
                CertificateValidationException.class,
                () -> certificateValidationService.validateCertPath(trustedCAs, certificateChain)
        );
        assertThat(exception.getMessage()).isEqualTo("Certificate chain is incorrect");
    }

    private static X509Certificate[] getTrustedCAs() {
        try {
            Resource truststoreWithYoltbank = new DefaultResourceLoader().getResource("classpath:truststore-with-yoltbank.jks");
            KeyStore trustStore = KeyStore.getInstance(truststoreWithYoltbank.getFile(), "changeit".toCharArray());
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStore);

            return Arrays.stream(trustManagerFactory.getTrustManagers())
                    .filter(X509TrustManager.class::isInstance)
                    .findAny()
                    .map(trustManager -> ((X509TrustManager) trustManager).getAcceptedIssuers())
                    .orElseThrow(() -> new IllegalStateException("Could not find any X509TrustManagers in the trustManagerFactory for algorithm " + TrustManagerFactory.getDefaultAlgorithm()));
        } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException e) {
            throw new IllegalStateException("Cannot resolve JVM truststore", e);
        }
    }

    private static String createSignature() throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA", "BC");
        signature.initSign(createPrivateKeyFromString(), new SecureRandom());
        signature.update(CertificateValidationServiceTest.SECRET);
        return Base64.toBase64String(signature.sign());
    }

    private static PrivateKey createPrivateKeyFromString() throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException {
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(Base64.decode(CertificateValidationServiceTest.PRIVATE_KEY));
        KeyFactory kf = KeyFactory.getInstance("RSA", "BC");
        return kf.generatePrivate(keySpec);
    }
}
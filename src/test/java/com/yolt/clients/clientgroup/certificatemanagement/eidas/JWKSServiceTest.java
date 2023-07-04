package com.yolt.clients.clientgroup.certificatemanagement.eidas;

import com.yolt.clients.clientgroup.certificatemanagement.aws.S3StorageClient;
import com.yolt.clients.clientgroup.certificatemanagement.dto.CertificateType;
import com.yolt.clients.clientgroup.certificatemanagement.repository.Certificate;
import com.yolt.clients.clientgroup.certificatemanagement.repository.CertificateRepository;
import lombok.SneakyThrows;
import nl.ing.lovebird.clienttokens.ClientGroupToken;
import org.jose4j.jwk.JsonWebKeySet;
import org.jose4j.jwt.JwtClaims;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.security.cert.CertPathValidatorException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.yolt.clients.clientgroup.certificatemanagement.dto.CertificateUsageType.SIGNING;
import static com.yolt.clients.clientgroup.certificatemanagement.dto.CertificateUsageType.TRANSPORT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JWKSServiceTest {
    private static final String KEY_ID = "KEY-0";
    private static final UUID CLIENT_GROUP_ID = UUID.randomUUID();
    private static final ClientGroupToken CLIENT_GROUP_TOKEN;
    private static final String CERTIFICATE = "MIIDcDCCAlgCFEPpOVAIl/ww58ivKIaNafqaSZclMA0GCSqGSIb3DQEBCwUAMG4x" +
            "CzAJBgNVBAYTAk5MMRYwFAYDVQQIDA1Ob29yZC1Ib2xsYW5kMRIwEAYDVQQHDAlB" +
            "bXN0ZXJkYW0xDTALBgNVBAoMBFlvbHQxDTALBgNVBAsMBFlvbHQxFTATBgNVBAMM" +
            "DEludGVybWVkaWF0ZTAeFw0yMDAyMDcxMjI2MjRaFw0zMDAyMDQxMjI2MjRaMHsx" +
            "CzAJBgNVBAYTAk5MMRYwFAYDVQQIDA1Ob29yZC1Ib2xsYW5kMRIwEAYDVQQHDAlB" +
            "bXN0ZXJkYW0xDTALBgNVBAoMBFlvbHQxDTALBgNVBAsMBFlvbHQxIjAgBgNVBAMM" +
            "GUxlYWYtV2l0aG91dCBRY1N0YXRlbWVudHMwggEiMA0GCSqGSIb3DQEBAQUAA4IB" +
            "DwAwggEKAoIBAQCY4VwqHuZUzlNYoiulRL+HcP8o6e4xD6Wa4Beug70Q7oZptHPv" +
            "wMILwnYGFPFG7SawK4QtDZgmJ0DxMxaB7Crp5EoT5agmReGsPe8NqgPn/AQrWidS" +
            "yJUZfDhjwTVzf8QxjqBRQAuWS4t+HYpt1REp9QfhLK9k1Q89/BOTUfPtKAmTBvsH" +
            "fvLKlGrOH4d84rrUIJDrxA9bEI/qOQ8J9qyYm1E0R3JupcMM+MqX+VGq5se0+xOV" +
            "TLTGwH4nqpEPzX/nrbrwADcQqDMDZ/VzBYmzBbNdoiC7gaUodCeWcrE9h9aCpJaH" +
            "LpDJus5NHz/JQLjTEVLIa8LOGjGdq8DuR8vJAgMBAAEwDQYJKoZIhvcNAQELBQAD" +
            "ggEBAIPX4vSJJ3DQuXGq2+jsLgEbMEfNacQ9xsDpWfbyIwGSXVA4oEa7HsaYsYmI" +
            "9CfUtt5BNzwluoEBqLRm0h1gW9//utVqm9RJIyNfhuvRA0UpAcL90hheeQdPJRgU" +
            "FvGWpdiLk4uWmf/Ejr9VZnldt/pAYume0PNQqTfPWoubiHMDrUN5oa7J7XZRRcG9" +
            "fWoVKb0TTFQVblhl0lOO3q/W7xm4l40iIuIBawAgY1lB1X14fhotQPEIumHESc8c" +
            "aMWosew+74IzS9DqN1VMlri2PcAQHpL+bNMHq6+uN2zW2NUo0T9xAhUMPEM7YSQH" +
            "NjYgPsHskYHnERCbMPVYqog609Q=";
    private static final String ROOT_CERTIFICATE = "MIIDdTCCAl2gAwIBAgIUHp/eJEDoE4xxjpsqTR9yU4Ye8S0wDQYJKoZIhvcNAQEL" +
            "BQAwZjELMAkGA1UEBhMCTkwxFjAUBgNVBAgMDU5vb3JkLUhvbGxhbmQxEjAQBgNV" +
            "BAcMCUFtc3RlcmRhbTENMAsGA1UECgwEWW9sdDENMAsGA1UECwwEWW9sdDENMAsG" +
            "A1UEAwwEUm9vdDAeFw0yMDAyMDcxMjI2MjRaFw0zMDAyMDQxMjI2MjRaMG4xCzAJ" +
            "BgNVBAYTAk5MMRYwFAYDVQQIDA1Ob29yZC1Ib2xsYW5kMRIwEAYDVQQHDAlBbXN0" +
            "ZXJkYW0xDTALBgNVBAoMBFlvbHQxDTALBgNVBAsMBFlvbHQxFTATBgNVBAMMDElu" +
            "dGVybWVkaWF0ZTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAMed1DRy" +
            "p6/rGNDZMnc+ghQECMXoDAfnFqtnb5sZg0XqoJIE4+U7Q5zFm085Wg8q/qym9/It" +
            "GxUHgPpY3FPnEz82t3BgzHdwW+Fgoej0nk1DYfOMtVnBswEQ+YoUXkWleoRcnyZZ" +
            "a/r5gYhgzUCwb2miS3MMP3pvRQG42XEO41/4DKG+3gjSKL2vuj6BjyrNJzoMzus5" +
            "xiemE+iZNxaAIocOHSBPtGAZR1DyND1+IHDIpnr7XUYf4/lKqlMtv3PXvDE2M5Vs" +
            "gVv2lEa2ZO1VpgoxTUMfdiHi5OFmeJlvbPjBD86M2hkhS0U4N/iURuuWo995ytHO" +
            "55ksxMcXj7m/RqUCAwEAAaMTMBEwDwYDVR0TAQH/BAUwAwEB/zANBgkqhkiG9w0B" +
            "AQsFAAOCAQEAvvx7WQq7X6MFRCX3dyRytIo0x2bRi1s3CfxaU3hca7Fif4prDmjC" +
            "XN0rtWz22pyCSdIHVy6JW9vf448r772a3bv0QFXKP39MSW/nKOoS/go8dd09jHsj" +
            "8XmuIMlT8eOnCxMwOPykFWMXOImiOaIv0oAOyFY0Kv0cFT8nDpLmd56Dgav8wb/0" +
            "pW/n03dkhKnuwhHicZ0HcIa60UjgVCDCxVlMhG0mnzkHGt1EDjJE+WvZQT7s6EmM" +
            "fInoIN9TqPaCLeAIm+C8wNUBv+Vdh5o2LdKg/4N4sn3zbhuPPPynvopKD1KGgz1m" +
            "z+UoI8W9cD183MKjGenZkA8MPnONZbzzTw==";
    private static final String CERTIFICATE_CHAIN = "-----BEGIN CERTIFICATE-----\n" +
            CERTIFICATE + "\n" +
            "-----END CERTIFICATE-----\n" +
            "-----BEGIN CERTIFICATE-----\n" +
            ROOT_CERTIFICATE + "\n" +
            "-----END CERTIFICATE-----\n";

    static {
        JwtClaims claims = new JwtClaims();
        claims.setClaim("client-group-id", CLIENT_GROUP_ID.toString());
        CLIENT_GROUP_TOKEN = new ClientGroupToken("serialized", claims);
    }

    @InjectMocks
    private JWKSService jwksService;

    @Mock
    private CertificateRepository certificateRepository;
    @Mock
    private S3StorageClient s3StorageClient;
    @Mock
    private EIDASValidationService eidasValidationService;
    @Captor
    private ArgumentCaptor<List<X509Certificate>> certChainCaptor;
    @Captor
    ArgumentCaptor<JsonWebKeySet> keySetArgumentCaptor;

    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void fetchJWKSJsonValidSigningKey() throws Exception {
        Certificate certificate = new Certificate(
                CertificateType.EIDAS,
                KEY_ID,
                CLIENT_GROUP_ID,
                "certificateName",
                Set.of(),
                SIGNING,
                null,
                null,
                null,
                null,
                CERTIFICATE_CHAIN
        );
        when(certificateRepository.findCertificatesByClientGroupIdAndCertificateType(CLIENT_GROUP_ID, CertificateType.EIDAS)).thenReturn(List.of(certificate));

        jwksService.createJWKSJsonOnS3(CLIENT_GROUP_TOKEN);

        verify(s3StorageClient).storeJWKS(eq(CLIENT_GROUP_TOKEN), keySetArgumentCaptor.capture());
        String expectedJson = "{\"keys\":[{\"kty\":\"RSA\",\"e\":\"AQAB\",\"use\":\"sig\",\"kid\":\"KEY-0\",\"x5c\":[\"MIIDcDCCAlgCFEPpOVAIl\\/ww58ivKIaNafqaSZclMA0GCSqGSIb3DQEBCwUAMG4xCzAJBgNVBAYTAk5MMRYwFAYDVQQIDA1Ob29yZC1Ib2xsYW5kMRIwEAYDVQQHDAlBbXN0ZXJkYW0xDTALBgNVBAoMBFlvbHQxDTALBgNVBAsMBFlvbHQxFTATBgNVBAMMDEludGVybWVkaWF0ZTAeFw0yMDAyMDcxMjI2MjRaFw0zMDAyMDQxMjI2MjRaMHsxCzAJBgNVBAYTAk5MMRYwFAYDVQQIDA1Ob29yZC1Ib2xsYW5kMRIwEAYDVQQHDAlBbXN0ZXJkYW0xDTALBgNVBAoMBFlvbHQxDTALBgNVBAsMBFlvbHQxIjAgBgNVBAMMGUxlYWYtV2l0aG91dCBRY1N0YXRlbWVudHMwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCY4VwqHuZUzlNYoiulRL+HcP8o6e4xD6Wa4Beug70Q7oZptHPvwMILwnYGFPFG7SawK4QtDZgmJ0DxMxaB7Crp5EoT5agmReGsPe8NqgPn\\/AQrWidSyJUZfDhjwTVzf8QxjqBRQAuWS4t+HYpt1REp9QfhLK9k1Q89\\/BOTUfPtKAmTBvsHfvLKlGrOH4d84rrUIJDrxA9bEI\\/qOQ8J9qyYm1E0R3JupcMM+MqX+VGq5se0+xOVTLTGwH4nqpEPzX\\/nrbrwADcQqDMDZ\\/VzBYmzBbNdoiC7gaUodCeWcrE9h9aCpJaHLpDJus5NHz\\/JQLjTEVLIa8LOGjGdq8DuR8vJAgMBAAEwDQYJKoZIhvcNAQELBQADggEBAIPX4vSJJ3DQuXGq2+jsLgEbMEfNacQ9xsDpWfbyIwGSXVA4oEa7HsaYsYmI9CfUtt5BNzwluoEBqLRm0h1gW9\\/\\/utVqm9RJIyNfhuvRA0UpAcL90hheeQdPJRgUFvGWpdiLk4uWmf\\/Ejr9VZnldt\\/pAYume0PNQqTfPWoubiHMDrUN5oa7J7XZRRcG9fWoVKb0TTFQVblhl0lOO3q\\/W7xm4l40iIuIBawAgY1lB1X14fhotQPEIumHESc8caMWosew+74IzS9DqN1VMlri2PcAQHpL+bNMHq6+uN2zW2NUo0T9xAhUMPEM7YSQHNjYgPsHskYHnERCbMPVYqog609Q=\",\"MIIDdTCCAl2gAwIBAgIUHp\\/eJEDoE4xxjpsqTR9yU4Ye8S0wDQYJKoZIhvcNAQELBQAwZjELMAkGA1UEBhMCTkwxFjAUBgNVBAgMDU5vb3JkLUhvbGxhbmQxEjAQBgNVBAcMCUFtc3RlcmRhbTENMAsGA1UECgwEWW9sdDENMAsGA1UECwwEWW9sdDENMAsGA1UEAwwEUm9vdDAeFw0yMDAyMDcxMjI2MjRaFw0zMDAyMDQxMjI2MjRaMG4xCzAJBgNVBAYTAk5MMRYwFAYDVQQIDA1Ob29yZC1Ib2xsYW5kMRIwEAYDVQQHDAlBbXN0ZXJkYW0xDTALBgNVBAoMBFlvbHQxDTALBgNVBAsMBFlvbHQxFTATBgNVBAMMDEludGVybWVkaWF0ZTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAMed1DRyp6\\/rGNDZMnc+ghQECMXoDAfnFqtnb5sZg0XqoJIE4+U7Q5zFm085Wg8q\\/qym9\\/ItGxUHgPpY3FPnEz82t3BgzHdwW+Fgoej0nk1DYfOMtVnBswEQ+YoUXkWleoRcnyZZa\\/r5gYhgzUCwb2miS3MMP3pvRQG42XEO41\\/4DKG+3gjSKL2vuj6BjyrNJzoMzus5xiemE+iZNxaAIocOHSBPtGAZR1DyND1+IHDIpnr7XUYf4\\/lKqlMtv3PXvDE2M5VsgVv2lEa2ZO1VpgoxTUMfdiHi5OFmeJlvbPjBD86M2hkhS0U4N\\/iURuuWo995ytHO55ksxMcXj7m\\/RqUCAwEAAaMTMBEwDwYDVR0TAQH\\/BAUwAwEB\\/zANBgkqhkiG9w0BAQsFAAOCAQEAvvx7WQq7X6MFRCX3dyRytIo0x2bRi1s3CfxaU3hca7Fif4prDmjCXN0rtWz22pyCSdIHVy6JW9vf448r772a3bv0QFXKP39MSW\\/nKOoS\\/go8dd09jHsj8XmuIMlT8eOnCxMwOPykFWMXOImiOaIv0oAOyFY0Kv0cFT8nDpLmd56Dgav8wb\\/0pW\\/n03dkhKnuwhHicZ0HcIa60UjgVCDCxVlMhG0mnzkHGt1EDjJE+WvZQT7s6EmMfInoIN9TqPaCLeAIm+C8wNUBv+Vdh5o2LdKg\\/4N4sn3zbhuPPPynvopKD1KGgz1mz+UoI8W9cD183MKjGenZkA8MPnONZbzzTw==\"],\"alg\":\"RS256\",\"n\":\"mOFcKh7mVM5TWKIrpUS_h3D_KOnuMQ-lmuAXroO9EO6GabRz78DCC8J2BhTxRu0msCuELQ2YJidA8TMWgewq6eRKE-WoJkXhrD3vDaoD5_wEK1onUsiVGXw4Y8E1c3_EMY6gUUALlkuLfh2KbdURKfUH4SyvZNUPPfwTk1Hz7SgJkwb7B37yypRqzh-HfOK61CCQ68QPWxCP6jkPCfasmJtRNEdybqXDDPjKl_lRqubHtPsTlUy0xsB-J6qRD81_56268AA3EKgzA2f1cwWJswWzXaIgu4GlKHQnlnKxPYfWgqSWhy6QybrOTR8_yUC40xFSyGvCzhoxnavA7kfLyQ\"}]}";
        JSONAssert.assertEquals(expectedJson, keySetArgumentCaptor.getValue().toJson(), JSONCompareMode.STRICT);

        verify(eidasValidationService).validateCertificateChain(certChainCaptor.capture());
        List<String> certs = certChainCaptor.getValue().stream().map(this::x509ToPem).collect(Collectors.toList());
        assertThat(certs).containsExactlyInAnyOrder(CERTIFICATE, ROOT_CERTIFICATE);
    }

    @Test
    void fetchJWKSJsonValidTransportKey() throws Exception {
        Certificate certificate = new Certificate(
                CertificateType.EIDAS,
                KEY_ID,
                CLIENT_GROUP_ID,
                "certificateName",
                Set.of(),
                TRANSPORT,
                null,
                null,
                null,
                null,
                CERTIFICATE_CHAIN
        );
        when(certificateRepository.findCertificatesByClientGroupIdAndCertificateType(CLIENT_GROUP_ID, CertificateType.EIDAS)).thenReturn(List.of(certificate));

        jwksService.createJWKSJsonOnS3(CLIENT_GROUP_TOKEN);

        verify(s3StorageClient).storeJWKS(eq(CLIENT_GROUP_TOKEN), keySetArgumentCaptor.capture());
        String expectedJson = "{\"keys\":[{\"kty\":\"RSA\",\"e\":\"AQAB\",\"use\":\"enc\",\"kid\":\"KEY-0\",\"x5c\":[\"MIIDcDCCAlgCFEPpOVAIl\\/ww58ivKIaNafqaSZclMA0GCSqGSIb3DQEBCwUAMG4xCzAJBgNVBAYTAk5MMRYwFAYDVQQIDA1Ob29yZC1Ib2xsYW5kMRIwEAYDVQQHDAlBbXN0ZXJkYW0xDTALBgNVBAoMBFlvbHQxDTALBgNVBAsMBFlvbHQxFTATBgNVBAMMDEludGVybWVkaWF0ZTAeFw0yMDAyMDcxMjI2MjRaFw0zMDAyMDQxMjI2MjRaMHsxCzAJBgNVBAYTAk5MMRYwFAYDVQQIDA1Ob29yZC1Ib2xsYW5kMRIwEAYDVQQHDAlBbXN0ZXJkYW0xDTALBgNVBAoMBFlvbHQxDTALBgNVBAsMBFlvbHQxIjAgBgNVBAMMGUxlYWYtV2l0aG91dCBRY1N0YXRlbWVudHMwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCY4VwqHuZUzlNYoiulRL+HcP8o6e4xD6Wa4Beug70Q7oZptHPvwMILwnYGFPFG7SawK4QtDZgmJ0DxMxaB7Crp5EoT5agmReGsPe8NqgPn\\/AQrWidSyJUZfDhjwTVzf8QxjqBRQAuWS4t+HYpt1REp9QfhLK9k1Q89\\/BOTUfPtKAmTBvsHfvLKlGrOH4d84rrUIJDrxA9bEI\\/qOQ8J9qyYm1E0R3JupcMM+MqX+VGq5se0+xOVTLTGwH4nqpEPzX\\/nrbrwADcQqDMDZ\\/VzBYmzBbNdoiC7gaUodCeWcrE9h9aCpJaHLpDJus5NHz\\/JQLjTEVLIa8LOGjGdq8DuR8vJAgMBAAEwDQYJKoZIhvcNAQELBQADggEBAIPX4vSJJ3DQuXGq2+jsLgEbMEfNacQ9xsDpWfbyIwGSXVA4oEa7HsaYsYmI9CfUtt5BNzwluoEBqLRm0h1gW9\\/\\/utVqm9RJIyNfhuvRA0UpAcL90hheeQdPJRgUFvGWpdiLk4uWmf\\/Ejr9VZnldt\\/pAYume0PNQqTfPWoubiHMDrUN5oa7J7XZRRcG9fWoVKb0TTFQVblhl0lOO3q\\/W7xm4l40iIuIBawAgY1lB1X14fhotQPEIumHESc8caMWosew+74IzS9DqN1VMlri2PcAQHpL+bNMHq6+uN2zW2NUo0T9xAhUMPEM7YSQHNjYgPsHskYHnERCbMPVYqog609Q=\",\"MIIDdTCCAl2gAwIBAgIUHp\\/eJEDoE4xxjpsqTR9yU4Ye8S0wDQYJKoZIhvcNAQELBQAwZjELMAkGA1UEBhMCTkwxFjAUBgNVBAgMDU5vb3JkLUhvbGxhbmQxEjAQBgNVBAcMCUFtc3RlcmRhbTENMAsGA1UECgwEWW9sdDENMAsGA1UECwwEWW9sdDENMAsGA1UEAwwEUm9vdDAeFw0yMDAyMDcxMjI2MjRaFw0zMDAyMDQxMjI2MjRaMG4xCzAJBgNVBAYTAk5MMRYwFAYDVQQIDA1Ob29yZC1Ib2xsYW5kMRIwEAYDVQQHDAlBbXN0ZXJkYW0xDTALBgNVBAoMBFlvbHQxDTALBgNVBAsMBFlvbHQxFTATBgNVBAMMDEludGVybWVkaWF0ZTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAMed1DRyp6\\/rGNDZMnc+ghQECMXoDAfnFqtnb5sZg0XqoJIE4+U7Q5zFm085Wg8q\\/qym9\\/ItGxUHgPpY3FPnEz82t3BgzHdwW+Fgoej0nk1DYfOMtVnBswEQ+YoUXkWleoRcnyZZa\\/r5gYhgzUCwb2miS3MMP3pvRQG42XEO41\\/4DKG+3gjSKL2vuj6BjyrNJzoMzus5xiemE+iZNxaAIocOHSBPtGAZR1DyND1+IHDIpnr7XUYf4\\/lKqlMtv3PXvDE2M5VsgVv2lEa2ZO1VpgoxTUMfdiHi5OFmeJlvbPjBD86M2hkhS0U4N\\/iURuuWo995ytHO55ksxMcXj7m\\/RqUCAwEAAaMTMBEwDwYDVR0TAQH\\/BAUwAwEB\\/zANBgkqhkiG9w0BAQsFAAOCAQEAvvx7WQq7X6MFRCX3dyRytIo0x2bRi1s3CfxaU3hca7Fif4prDmjCXN0rtWz22pyCSdIHVy6JW9vf448r772a3bv0QFXKP39MSW\\/nKOoS\\/go8dd09jHsj8XmuIMlT8eOnCxMwOPykFWMXOImiOaIv0oAOyFY0Kv0cFT8nDpLmd56Dgav8wb\\/0pW\\/n03dkhKnuwhHicZ0HcIa60UjgVCDCxVlMhG0mnzkHGt1EDjJE+WvZQT7s6EmMfInoIN9TqPaCLeAIm+C8wNUBv+Vdh5o2LdKg\\/4N4sn3zbhuPPPynvopKD1KGgz1mz+UoI8W9cD183MKjGenZkA8MPnONZbzzTw==\"],\"alg\":\"RS256\",\"n\":\"mOFcKh7mVM5TWKIrpUS_h3D_KOnuMQ-lmuAXroO9EO6GabRz78DCC8J2BhTxRu0msCuELQ2YJidA8TMWgewq6eRKE-WoJkXhrD3vDaoD5_wEK1onUsiVGXw4Y8E1c3_EMY6gUUALlkuLfh2KbdURKfUH4SyvZNUPPfwTk1Hz7SgJkwb7B37yypRqzh-HfOK61CCQ68QPWxCP6jkPCfasmJtRNEdybqXDDPjKl_lRqubHtPsTlUy0xsB-J6qRD81_56268AA3EKgzA2f1cwWJswWzXaIgu4GlKHQnlnKxPYfWgqSWhy6QybrOTR8_yUC40xFSyGvCzhoxnavA7kfLyQ\"}]}";
        JSONAssert.assertEquals(expectedJson, keySetArgumentCaptor.getValue().toJson(), JSONCompareMode.STRICT);

        verify(eidasValidationService).validateCertificateChain(certChainCaptor.capture());
        List<String> certs = certChainCaptor.getValue().stream().map(this::x509ToPem).collect(Collectors.toList());
        assertThat(certs).containsExactlyInAnyOrder(CERTIFICATE, ROOT_CERTIFICATE);
    }

    @Test
    void fetchJWKSJsonInvalidSigningKey() throws Exception {
        Certificate certificate = new Certificate(
                CertificateType.EIDAS,
                KEY_ID,
                CLIENT_GROUP_ID,
                "certificateName",
                Set.of(),
                SIGNING,
                null,
                null,
                null,
                null,
                CERTIFICATE_CHAIN
        );

        CertPathValidatorException cause = new CertPathValidatorException();
        IllegalArgumentException exception = new IllegalArgumentException(cause);

        when(certificateRepository.findCertificatesByClientGroupIdAndCertificateType(CLIENT_GROUP_ID, CertificateType.EIDAS)).thenReturn(List.of(certificate));
        doThrow(exception).when(eidasValidationService).validateCertificateChain(any());

        jwksService.createJWKSJsonOnS3(CLIENT_GROUP_TOKEN);

        verify(s3StorageClient).storeJWKS(eq(CLIENT_GROUP_TOKEN), keySetArgumentCaptor.capture());
        String expectedJson = "{\"keys\":[]}";
        JSONAssert.assertEquals(expectedJson, keySetArgumentCaptor.getValue().toJson(), JSONCompareMode.STRICT);

        verify(eidasValidationService).validateCertificateChain(certChainCaptor.capture());
        List<String> certs = certChainCaptor.getValue().stream().map(this::x509ToPem).collect(Collectors.toList());
        assertThat(certs).containsExactlyInAnyOrder(CERTIFICATE, ROOT_CERTIFICATE);
    }

    @Test
    void fetchJWKSJsonInvalidTransportKey() throws Exception {
        Certificate certificate = new Certificate(
                CertificateType.EIDAS,
                KEY_ID,
                CLIENT_GROUP_ID,
                "certificateName",
                Set.of(),
                TRANSPORT,
                null,
                null,
                null,
                null,
                CERTIFICATE_CHAIN
        );

        IllegalArgumentException exception = new IllegalArgumentException();

        when(certificateRepository.findCertificatesByClientGroupIdAndCertificateType(CLIENT_GROUP_ID, CertificateType.EIDAS)).thenReturn(List.of(certificate));
        doThrow(exception).when(eidasValidationService).validateCertificateChain(any());

        jwksService.createJWKSJsonOnS3(CLIENT_GROUP_TOKEN);

        verify(s3StorageClient).storeJWKS(eq(CLIENT_GROUP_TOKEN), keySetArgumentCaptor.capture());
        String expectedJson = "{\"keys\":[]}";
        JSONAssert.assertEquals(expectedJson, keySetArgumentCaptor.getValue().toJson(), JSONCompareMode.STRICT);

        verify(eidasValidationService).validateCertificateChain(certChainCaptor.capture());
        List<String> certs = certChainCaptor.getValue().stream().map(this::x509ToPem).collect(Collectors.toList());
        assertThat(certs).containsExactlyInAnyOrder(CERTIFICATE, ROOT_CERTIFICATE);
    }

    @Test
    void fetchJWKSJsonBadSignedCert() throws Exception {
        Certificate certificate = new Certificate(
                CertificateType.EIDAS,
                KEY_ID,
                CLIENT_GROUP_ID,
                "certificateName",
                Set.of(),
                TRANSPORT,
                null,
                null,
                null,
                null,
                "bad"
        );
        when(certificateRepository.findCertificatesByClientGroupIdAndCertificateType(CLIENT_GROUP_ID, CertificateType.EIDAS)).thenReturn(List.of(certificate));

        jwksService.createJWKSJsonOnS3(CLIENT_GROUP_TOKEN);

        verify(s3StorageClient).storeJWKS(eq(CLIENT_GROUP_TOKEN), keySetArgumentCaptor.capture());

        String expectedJson = "{\"keys\":[]}";
        JSONAssert.assertEquals(expectedJson, keySetArgumentCaptor.getValue().toJson(), JSONCompareMode.STRICT);
    }

    @Test
    void fetchJWKSJsonNoSignedCert() throws Exception {
        Certificate certificate = new Certificate(
                CertificateType.EIDAS,
                KEY_ID,
                CLIENT_GROUP_ID,
                "certificateName",
                Set.of(),
                TRANSPORT,
                null,
                null,
                null,
                null,
                null
        );
        when(certificateRepository.findCertificatesByClientGroupIdAndCertificateType(CLIENT_GROUP_ID, CertificateType.EIDAS)).thenReturn(List.of(certificate));

        jwksService.createJWKSJsonOnS3(CLIENT_GROUP_TOKEN);

        verify(s3StorageClient).storeJWKS(eq(CLIENT_GROUP_TOKEN), keySetArgumentCaptor.capture());

        String expectedJson = "{\"keys\":[]}";
        JSONAssert.assertEquals(expectedJson, keySetArgumentCaptor.getValue().toJson(), JSONCompareMode.STRICT);
    }

    @SneakyThrows
    private String x509ToPem(X509Certificate x509Certificate) {
        return new String(Base64.getEncoder().encode(x509Certificate.getEncoded()));
    }
}
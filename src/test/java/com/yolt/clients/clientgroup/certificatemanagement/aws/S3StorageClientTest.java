package com.yolt.clients.clientgroup.certificatemanagement.aws;

import nl.ing.lovebird.clienttokens.ClientGroupToken;
import org.apache.commons.io.IOUtils;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMParser;
import org.jose4j.jwk.JsonWebKeySet;
import org.jose4j.jwk.RsaJsonWebKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.http.MediaType;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.StringReader;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class S3StorageClientTest {
    private static final String BUCKET_NAME = "bucketName";
    private static final String CERTIFICATE = """
            -----BEGIN CERTIFICATE-----
            MIIC9DCCAdygAwIBAgIGAWkqXD5SMA0GCSqGSIb3DQEBCwUAMDsxCzAJBgNVBAYT
            Ak5MMQwwCgYDVQQKEwNBQkMxHjAcBgNVBAMTFVRwcFNhbmRib3hDZXJ0aWZpY2F0
            ZTAeFw0xOTAyMjYxNTExMjJaFw0yMDAyMjYxNTExMjJaMDsxCzAJBgNVBAYTAk5M
            MQwwCgYDVQQKEwNBQkMxHjAcBgNVBAMTFVRwcFNhbmRib3hDZXJ0aWZpY2F0ZTCC
            ASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAJkUcwLLs9dr3ngvR5ULGHoC
            vCpyaeVA9s/VBZ4q4ysbLAjdGux3mR18GKtGMkIeB1Dmw65vBdJWCBXxbPdeWA3Z
            RoC6yUCU6w5HMg0IMMq4Z5dbUs5cgvrUF1ZD12uUW/4zSQ6dw4DpyVzE2rDQ88dS
            GBGSC2U/Ql3aR8W2RaDL0Ii5MobKM1VtCrL2bjGKyPf4rViJZDrvFQBBH2WzlGJn
            DQVYxgQnINVQa1lIY+B/gNvm1iw/znAqeAN38FrNNXy6LpHXmi7viDh1/pBMbG2L
            6SRnuSOu79QrXaMPsaupklEHlyrY5s/SDvsjgEGC3IQNVduAL87zhjTLt+ElGPcC
            AwEAATANBgkqhkiG9w0BAQsFAAOCAQEAIJkFTRn6MnnQsfSJkNvzdHypH4ha43zU
            RXypc8B5bY9VD8jzBuy5QEpuNRV8i3X22Sqldh2KZ4tp+X54f2LIfwwjd6t60eTT
            vPsH8MTpV376pUMB3pdCuA6EPlCij/qXJbi/UCeNA8jQAoOTd7oIwIrQrf6tyCa3
            d871WvFkS4GyyVuPa6QBIsbD040cT63EaB/QQc9z+1EJiAMDZ1oONotEPL2t6gQk
            rEmNlYgebDMx9NEvQYYMnQB6AMfYuxcieqDN4bngJS3IKnEuzn47PYrNLfdoWHuJ
            ffIv4MnTxl59WRV5pTOaZu776Qlgzm7RHdgbibfD5ASixT+VDebMoQ==
            -----END CERTIFICATE-----
            """;
    private static final String CERTIFICATE_CHAIN = """
            -----BEGIN CERTIFICATE-----
            MIIDcDCCAlgCFEPpOVAIl/ww58ivKIaNafqaSZclMA0GCSqGSIb3DQEBCwUAMG4x
            CzAJBgNVBAYTAk5MMRYwFAYDVQQIDA1Ob29yZC1Ib2xsYW5kMRIwEAYDVQQHDAlB
            bXN0ZXJkYW0xDTALBgNVBAoMBFlvbHQxDTALBgNVBAsMBFlvbHQxFTATBgNVBAMM
            DEludGVybWVkaWF0ZTAeFw0yMDAyMDcxMjI2MjRaFw0zMDAyMDQxMjI2MjRaMHsx
            CzAJBgNVBAYTAk5MMRYwFAYDVQQIDA1Ob29yZC1Ib2xsYW5kMRIwEAYDVQQHDAlB
            bXN0ZXJkYW0xDTALBgNVBAoMBFlvbHQxDTALBgNVBAsMBFlvbHQxIjAgBgNVBAMM
            GUxlYWYtV2l0aG91dCBRY1N0YXRlbWVudHMwggEiMA0GCSqGSIb3DQEBAQUAA4IB
            DwAwggEKAoIBAQCY4VwqHuZUzlNYoiulRL+HcP8o6e4xD6Wa4Beug70Q7oZptHPv
            wMILwnYGFPFG7SawK4QtDZgmJ0DxMxaB7Crp5EoT5agmReGsPe8NqgPn/AQrWidS
            yJUZfDhjwTVzf8QxjqBRQAuWS4t+HYpt1REp9QfhLK9k1Q89/BOTUfPtKAmTBvsH
            fvLKlGrOH4d84rrUIJDrxA9bEI/qOQ8J9qyYm1E0R3JupcMM+MqX+VGq5se0+xOV
            TLTGwH4nqpEPzX/nrbrwADcQqDMDZ/VzBYmzBbNdoiC7gaUodCeWcrE9h9aCpJaH
            LpDJus5NHz/JQLjTEVLIa8LOGjGdq8DuR8vJAgMBAAEwDQYJKoZIhvcNAQELBQAD
            ggEBAIPX4vSJJ3DQuXGq2+jsLgEbMEfNacQ9xsDpWfbyIwGSXVA4oEa7HsaYsYmI
            9CfUtt5BNzwluoEBqLRm0h1gW9//utVqm9RJIyNfhuvRA0UpAcL90hheeQdPJRgU
            FvGWpdiLk4uWmf/Ejr9VZnldt/pAYume0PNQqTfPWoubiHMDrUN5oa7J7XZRRcG9
            fWoVKb0TTFQVblhl0lOO3q/W7xm4l40iIuIBawAgY1lB1X14fhotQPEIumHESc8c
            aMWosew+74IzS9DqN1VMlri2PcAQHpL+bNMHq6+uN2zW2NUo0T9xAhUMPEM7YSQH
            NjYgPsHskYHnERCbMPVYqog609Q=
            -----END CERTIFICATE-----
            -----BEGIN CERTIFICATE-----
            MIIDdTCCAl2gAwIBAgIUHp/eJEDoE4xxjpsqTR9yU4Ye8S0wDQYJKoZIhvcNAQEL
            BQAwZjELMAkGA1UEBhMCTkwxFjAUBgNVBAgMDU5vb3JkLUhvbGxhbmQxEjAQBgNV
            BAcMCUFtc3RlcmRhbTENMAsGA1UECgwEWW9sdDENMAsGA1UECwwEWW9sdDENMAsG
            A1UEAwwEUm9vdDAeFw0yMDAyMDcxMjI2MjRaFw0zMDAyMDQxMjI2MjRaMG4xCzAJ
            BgNVBAYTAk5MMRYwFAYDVQQIDA1Ob29yZC1Ib2xsYW5kMRIwEAYDVQQHDAlBbXN0
            ZXJkYW0xDTALBgNVBAoMBFlvbHQxDTALBgNVBAsMBFlvbHQxFTATBgNVBAMMDElu
            dGVybWVkaWF0ZTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAMed1DRy
            p6/rGNDZMnc+ghQECMXoDAfnFqtnb5sZg0XqoJIE4+U7Q5zFm085Wg8q/qym9/It
            GxUHgPpY3FPnEz82t3BgzHdwW+Fgoej0nk1DYfOMtVnBswEQ+YoUXkWleoRcnyZZ
            a/r5gYhgzUCwb2miS3MMP3pvRQG42XEO41/4DKG+3gjSKL2vuj6BjyrNJzoMzus5
            xiemE+iZNxaAIocOHSBPtGAZR1DyND1+IHDIpnr7XUYf4/lKqlMtv3PXvDE2M5Vs
            gVv2lEa2ZO1VpgoxTUMfdiHi5OFmeJlvbPjBD86M2hkhS0U4N/iURuuWo995ytHO
            55ksxMcXj7m/RqUCAwEAAaMTMBEwDwYDVR0TAQH/BAUwAwEB/zANBgkqhkiG9w0B
            AQsFAAOCAQEAvvx7WQq7X6MFRCX3dyRytIo0x2bRi1s3CfxaU3hca7Fif4prDmjC
            XN0rtWz22pyCSdIHVy6JW9vf448r772a3bv0QFXKP39MSW/nKOoS/go8dd09jHsj
            8XmuIMlT8eOnCxMwOPykFWMXOImiOaIv0oAOyFY0Kv0cFT8nDpLmd56Dgav8wb/0
            pW/n03dkhKnuwhHicZ0HcIa60UjgVCDCxVlMhG0mnzkHGt1EDjJE+WvZQT7s6EmM
            fInoIN9TqPaCLeAIm+C8wNUBv+Vdh5o2LdKg/4N4sn3zbhuPPPynvopKD1KGgz1m
            z+UoI8W9cD183MKjGenZkA8MPnONZbzzTw==
            -----END CERTIFICATE-----
            """;

    private S3Properties s3Properties;
    private S3Client s3Client;
    private S3StorageClient s3StorageClient;

    @BeforeEach
    void setupTestData() {
        s3Properties = new S3Properties();
        s3Properties.setBucket(BUCKET_NAME);
        s3Client = mock(S3Client.class);

        s3StorageClient = new S3StorageClient(s3Properties, s3Client);

    }

    @Test
    void testStoreCertificateWithoutChain() throws Exception{
        X509CertificateHolder x509CertificateHolder = (X509CertificateHolder) new PEMParser(new StringReader(CERTIFICATE)).readObject();
        X509Certificate certificate = new JcaX509CertificateConverter().getCertificate(x509CertificateHolder);

        s3StorageClient.storeCertificate(singletonList(certificate));

        ArgumentCaptor<RequestBody> pemRequestBodyArgumentCaptor = ArgumentCaptor.forClass(RequestBody.class);
        ArgumentCaptor<RequestBody> derRequestBodyArgumentCaptor = ArgumentCaptor.forClass(RequestBody.class);

        verify(s3Client).putObject(eq(PutObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key("c9:b1:7f:87:cb:d1:83:f7:f1:15:78:e5:15:ef:cf:58:c2:5f:27:0b.der").build()), derRequestBodyArgumentCaptor.capture());
        verify(s3Client).putObject(eq(PutObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key("c9:b1:7f:87:cb:d1:83:f7:f1:15:78:e5:15:ef:cf:58:c2:5f:27:0b.pem").build()), pemRequestBodyArgumentCaptor.capture());

        byte[] bytes = IOUtils.toByteArray(derRequestBodyArgumentCaptor.getValue().contentStreamProvider().newStream());
        assertThat(bytes).containsExactly(certificate.getEncoded());

        String postedPem = new String(IOUtils.toByteArray(pemRequestBodyArgumentCaptor.getValue().contentStreamProvider().newStream()));
        assertThat(postedPem).isEqualToNormalizingNewlines(CERTIFICATE);
    }

    @Test
    void testStoreCertificateWithChain() throws Exception{
        List<X509Certificate> x509Certificates = parsePemChain(CERTIFICATE_CHAIN);
        X509Certificate leafCertificate = x509Certificates.get(0);
        s3StorageClient.storeCertificate(x509Certificates);

        ArgumentCaptor<RequestBody> pemRequestBodyArgumentCaptor = ArgumentCaptor.forClass(RequestBody.class);
        ArgumentCaptor<RequestBody> derRequestBodyArgumentCaptor = ArgumentCaptor.forClass(RequestBody.class);
        ArgumentCaptor<RequestBody> chainRequestBodyArgumentCaptor = ArgumentCaptor.forClass(RequestBody.class);

        verify(s3Client).putObject(eq(PutObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key("db:12:5b:53:b8:29:4c:e0:92:cd:04:07:04:6d:a9:aa:a5:ac:d1:55.der").build()), derRequestBodyArgumentCaptor.capture());
        verify(s3Client).putObject(eq(PutObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key("db:12:5b:53:b8:29:4c:e0:92:cd:04:07:04:6d:a9:aa:a5:ac:d1:55.pem").build()), pemRequestBodyArgumentCaptor.capture());
        verify(s3Client).putObject(eq(PutObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key("db:12:5b:53:b8:29:4c:e0:92:cd:04:07:04:6d:a9:aa:a5:ac:d1:55-chain.pem").build()), chainRequestBodyArgumentCaptor.capture());

        byte[] bytes = IOUtils.toByteArray(derRequestBodyArgumentCaptor.getValue().contentStreamProvider().newStream());
        assertThat(bytes).containsExactly(leafCertificate.getEncoded());

        String postedPem = new String(IOUtils.toByteArray(pemRequestBodyArgumentCaptor.getValue().contentStreamProvider().newStream()));
        assertThat(postedPem).isEqualToNormalizingNewlines(CERTIFICATE_CHAIN.substring(0, CERTIFICATE_CHAIN.lastIndexOf("-----BEGIN CERTIFICATE-----")));

        String postedChain = new String(IOUtils.toByteArray(chainRequestBodyArgumentCaptor.getValue().contentStreamProvider().newStream()));
        assertThat(postedChain).isEqualToNormalizingNewlines(CERTIFICATE_CHAIN);
    }

    @Test
    void testStoreJWKS() throws Exception{
        UUID clientGroupId = UUID.randomUUID();
        ClientGroupToken clientGroupToken = mock(ClientGroupToken.class);
        when(clientGroupToken.getClientGroupIdClaim()).thenReturn(clientGroupId);

        X509CertificateHolder x509CertificateHolder = (X509CertificateHolder) new PEMParser(new StringReader(CERTIFICATE)).readObject();
        X509Certificate certificate = new JcaX509CertificateConverter().getCertificate(x509CertificateHolder);
        RSAPublicKey rsaPublicKey = (RSAPublicKey) certificate.getPublicKey();
        RsaJsonWebKey rsaJsonWebKey = new RsaJsonWebKey(rsaPublicKey);
        JsonWebKeySet jsonWebKeySet = new JsonWebKeySet(rsaJsonWebKey);
        String expectedJWKSString = "{\"keys\":[{\"kty\":\"RSA\",\"n\":\"mRRzAsuz12veeC9HlQsYegK8KnJp5UD2z9UFnirjKxssCN0a7HeZHXwYq0YyQh4HUObDrm8F0lYIFfFs915YDdlGgLrJQJTrDkcyDQgwyrhnl1tSzlyC-tQXVkPXa5Rb_jNJDp3DgOnJXMTasNDzx1IYEZILZT9CXdpHxbZFoMvQiLkyhsozVW0KsvZuMYrI9_itWIlkOu8VAEEfZbOUYmcNBVjGBCcg1VBrWUhj4H-A2-bWLD_OcCp4A3fwWs01fLoukdeaLu-IOHX-kExsbYvpJGe5I67v1Ctdow-xq6mSUQeXKtjmz9IO-yOAQYLchA1V24AvzvOGNMu34SUY9w\",\"e\":\"AQAB\"}]}";

        PutObjectRequest expectedPutObjectRequest = PutObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .key("jwks/" + clientGroupId + "/keys").build();

        ArgumentCaptor<RequestBody> bodyArgumentCaptor = ArgumentCaptor.forClass(RequestBody.class);

        s3StorageClient.storeJWKS(clientGroupToken, jsonWebKeySet);

        verify(s3Client).putObject(eq(expectedPutObjectRequest), bodyArgumentCaptor.capture());
        String postedJWKS = new String(IOUtils.toByteArray(bodyArgumentCaptor.getValue().contentStreamProvider().newStream()));
        JSONAssert.assertEquals(expectedJWKSString, postedJWKS, JSONCompareMode.STRICT);
    }

    private List<X509Certificate> parsePemChain(String pemCertificateChain) {
        LinkedList<X509Certificate> certificateChain = new LinkedList<>();
        try (PEMParser pemParser = new PEMParser(new StringReader(pemCertificateChain))) {
            Object pemObject;
            while ((pemObject = pemParser.readObject()) != null) {
                X509CertificateHolder certificateHolder = (X509CertificateHolder) pemObject;
                X509Certificate certificate = new JcaX509CertificateConverter().getCertificate(certificateHolder);
                certificateChain.add(certificate);
            }
            return certificateChain;
        } catch (IOException | CertificateException e) {
            throw new IllegalStateException("Error occurred during PEM parsing", e);
        }
    }
}
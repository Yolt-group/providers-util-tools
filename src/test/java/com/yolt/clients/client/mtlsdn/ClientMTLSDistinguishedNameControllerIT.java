package com.yolt.clients.client.mtlsdn;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.yolt.clients.IntegrationTest;
import com.yolt.clients.client.mtlsdn.dto.DistinguishedNameDTO;
import com.yolt.clients.client.mtlsdn.dto.DistinguishedNameIdListDTO;
import com.yolt.clients.client.mtlsdn.dto.NewDistinguishedNameDTO;
import com.yolt.clients.client.mtlsdn.respository.ClientMTLSCertificateDN;
import com.yolt.clients.client.mtlsdn.respository.ClientMTLSCertificateDNRepository;
import com.yolt.clients.clientgroup.ClientGroupRepository;
import com.yolt.clients.jira.Status;
import com.yolt.clients.jira.dto.IssueResponseDTO;
import com.yolt.clients.model.Client;
import com.yolt.clients.model.ClientGroup;
import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.clienttokens.constants.ClientTokenConstants;
import nl.ing.lovebird.clienttokens.test.TestClientTokens;
import nl.ing.lovebird.errorhandling.ErrorDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class ClientMTLSDistinguishedNameControllerIT {
    private static final String ASSISTANCE_PORTAL_YTS = "assistance-portal-yts";
    private static final String DEV_PORTAL = "dev-portal";

    private static final String CERTIFICATE_CHAIN = """
            -----BEGIN CERTIFICATE-----
            MIII0jCCBrqgAwIBAgIMDtOUxizt/QBo1/7uMA0GCSqGSIb3DQEBCwUAMFExCzAJ
            BgNVBAYTAkJFMRkwFwYDVQQKExBHbG9iYWxTaWduIG52LXNhMScwJQYDVQQDEx5H
            bG9iYWxTaWduIFJTQSBFViBRV0FDIENBIDIwMTkwHhcNMjAwNjI2MTUzMzAwWhcN
            MjIwNjI3MTUzMzAwWjCB8jEdMBsGA1UEDwwUUHJpdmF0ZSBPcmdhbml6YXRpb24x
            ETAPBgNVBAUTCDEyMjI3ODkxMRMwEQYLKwYBBAGCNzwCAQMTAkdCMQswCQYDVQQG
            EwJHQjEPMA0GA1UECBMGbG9uZG9uMQ8wDQYDVQQHEwZsb25kb24xKTAnBgNVBAkT
            IFNlY29uZCBIb21lLCAxMjUtMTI3IE1hcmUgU3RyZWV0MQswCQYDVQQLEwJJVDEV
            MBMGA1UEChMMb2tlbyBsaW1pdGVkMRAwDgYDVQQDEwdva2VvLmNvMRkwFwYDVQRh
            ExBQU0RHQi1GQ0EtOTE2ODY2MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKC
            AQEAv8ujZfog5rlvCLVnoW0b7ihJ1SiQ7Cf6qvM/uccIju3UyjIhx8yYGvwKSgOi
            nyf+x52kBQxNfYVHBLMiMglcK4m8UbBYrO+MCajdM5+/2bsSPIsF/RKklFSXdDo9
            hOBtn3cz/Tld+SsV0rMub2gbEv3gFgxTXrC1xMTAhEEGdhS6OluIz8yWOH7kHDhe
            vmEAQQxRMFqx3zJVLFto3+htQwrRI5o7gQUwpjpkeXY/sbZFKBXejPlcA7HFyq+M
            U0Ea9hI3r9aW6GiycfQR0pJZ6NNv6HTvaYHjtsYm02VDoa9JoVreyw8oU8vkjjuI
            NJ0GN+YPJCXN5bdu2ZNHjue6SwIDAQABo4IEBjCCBAIwDgYDVR0PAQH/BAQDAgWg
            MIGLBggrBgEFBQcBAQR/MH0wQwYIKwYBBQUHMAKGN2h0dHA6Ly9zZWN1cmUuZ2xv
            YmFsc2lnbi5jb20vY2FjZXJ0L2dzcnNhZXZxd2FjMjAxOS5jcnQwNgYIKwYBBQUH
            MAGGKmh0dHA6Ly9vY3NwLmdsb2JhbHNpZ24uY29tL2dzcnNhZXZxd2FjMjAxOTCB
            pgYDVR0gBIGeMIGbMEEGCSsGAQQBoDIBATA0MDIGCCsGAQUFBwIBFiZodHRwczov
            L3d3dy5nbG9iYWxzaWduLmNvbS9yZXBvc2l0b3J5LzAHBgVngQwBATAJBgcEAIvs
            QAEEMEIGCisGAQQBoDIBAQIwNDAyBggrBgEFBQcCARYmaHR0cHM6Ly93d3cuZ2xv
            YmFsc2lnbi5jb20vcmVwb3NpdG9yeS8wCQYDVR0TBAIwADAgBgVngQwDAQQXMBUT
            A1BTRBMCR0IMCkZDQS05MTY4NjYwIgYDVR0RBBswGYIHb2tlby5jb4IObW9iaWxl
            Lm9rZW8uY28wgYYGCCsGAQUFBwEDBHoweDAIBgYEAI5GAQEwEwYGBACORgEGMAkG
            BwQAjkYBBgMwVwYGBACBmCcCME0wJjARBgcEAIGYJwEBDAZQU1BfQVMwEQYHBACB
            mCcBAwwGUFNQX0FJDBtGaW5hbmNpYWwgQ29uZHVjdCBBdXRob3JpdHkMBkdCLUZD
            QTAdBgNVHSUEFjAUBggrBgEFBQcDAQYIKwYBBQUHAwIwHwYDVR0jBBgwFoAUUo+q
            WrOrcu7dyfmRocm5BhLSBN0wHQYDVR0OBBYEFD1oL44IKu1r/x1pbZbsWy5NgFb+
            MIIBfgYKKwYBBAHWeQIEAgSCAW4EggFqAWgAdgBvU3asMfAxGdiZAKRRFf93FRwR
            2QLBACkGjbIImjfZEwAAAXLxQsNpAAAEAwBHMEUCIF55xnXOrpCgRsXmETUuPbOa
            MNFR5+hgc1A8KDx6d0iSAiEAvq0uCAH0AxWaFGg1PhtOcFzYaXz5Y7jaKGergmXz
            W7IAdgApeb7wnjk5IfBWc59jpXflvld9nGAK+PlNXSZcJV3HhAAAAXLxQsYaAAAE
            AwBHMEUCIQD7Rd7++HfuTqE11llocx2j2oi0MLLNl6Yx7SixSCIXwAIgHWlMvQ9r
            oZzrdc2cOPdCMtC4/80WARSU0LyHXnLymI4AdgBRo7D1/QF5nFZtuDd4jwykeswb
            J8v3nohCmg3+1IsF5QAAAXLxQsZIAAAEAwBHMEUCIBzlNriTbhEHbsMTjmxnuAM/
            lnODFdsrJuZwXgP4Nt9xAiEAtAw+VOuETEhKT0XXjeDKouMkpe1pjFRzGrV8lEf3
            fRAwDQYJKoZIhvcNAQELBQADggIBAGE0Ji39mIKIEI9Cr41nxQzlSB2qPsD4m9R+
            M/E59jIu8lDX0gbgtg+KFg/+ZndABPu8D1imo9aVuPdrqoV8nQzDeLFllQ1GGT3I
            rRhPv5CmNFKnETps/2zst5YT+VHz9qlCm1BO8ryYYx+VFiE9udIWXxetDxzMpSR8
            e/yBiByhZkFIEKB9maAXEsE3clxrV8EeNnj3nRVHisBUuVam9IzrRIDGV2PLWlJB
            LNqwgCd8GRHmEZWms0mTDlVZoNC0zgjiTaxv+YMJe5biA9OnQVeWl1oBYm/N0I+W
            HX3nHp9TEEfctGwwXw5l4w6rqZqW7PENHKei6Pwm+sX1HpEkEaYFrj8bmM0pMned
            MHDd7Zqo7L5aoSkywgcoF1ZyBkvTir09z9Fbek+ncR5ZaKfsTyBO+ASOmoGA6L2n
            LJaogVFMYmLH4dBnO1vowqlw7HUdp8rY1cf6pytdLR6i0aSqp9tfWv1M43rr9Osi
            5d9vVh1IQqh9VKVR4JG94PcYBNNZTN3zQ3cVU8vmA7O2K4e+ODptQC0pBlTab31w
            x7f8WOBO+Sh6GzTVmvamtqqCzDU9q+t9H5i0xiJF4eNQNfKFoQE2B1Rm4yJMaQGB
            76Ee9ZejBKEHHZfROf+qWpG3TrDwDkKfbuNdgcyoMZyZUTBe2ZOKlC7gZacyf8FN
            wHAZPA0B
            -----END CERTIFICATE-----
            -----BEGIN CERTIFICATE-----
            MIIFezCCBGOgAwIBAgIQdR4/Pels1yiyZmLFUjJYezANBgkqhkiG9w0BAQwFADBM
            MSAwHgYDVQQLExdHbG9iYWxTaWduIFJvb3QgQ0EgLSBSMzETMBEGA1UEChMKR2xv
            YmFsU2lnbjETMBEGA1UEAxMKR2xvYmFsU2lnbjAeFw0xOTA1MTUwMDAwMDBaFw0y
            ODA1MTUwMDAwMDBaMFExCzAJBgNVBAYTAkJFMRkwFwYDVQQKExBHbG9iYWxTaWdu
            IG52LXNhMScwJQYDVQQDEx5HbG9iYWxTaWduIFJTQSBFViBRV0FDIENBIDIwMTkw
            ggIiMA0GCSqGSIb3DQEBAQUAA4ICDwAwggIKAoICAQDERtVIA51JWfdcIKACzqJc
            FKtZ9l3yKtnILRI7rpVa6HkWsAfQlzrFMA+O7bCeiXN4WVN0Lm25VbKbsDtD/yuw
            4CNEWOljgas3QPIl8HqM7U2PVO6eal9gaaWL0ZBKzOi6yX3TWMerwah8AAfczXAb
            frz4RJ9RNniC3NlbeYfkiPh7HQ6Tdm1ouhUzFdO2hoOgOrsg9VVxCkJOSutm3pHC
            US+pSQcbz8Y+TluTAgSPY08qvPHJzfNfaD13LUXPmta8z74SGnd5b4yYd+1nNv1Q
            QPTiyyVvv6sKnUJc+RgchszudFW7BKifHDUDzgb2JWZm6rjPvWCfNXCdcTYp6dFM
            4qdx1DnooL/w7lPZ1o6/r03qNMdEoL8qRcw0NqnJwPu1fVwtNpCIvzlLMLhZiCFb
            mfK86fiBoVAQckYOJ2uj+Lk48FGU0dzjjaqK3011Q6xczxt7eNarZ+DMDENcgO8N
            ggu8mffq+GPxT7ycMSNcmvWhOPOy1hg3c/zYG6+cVdkVZvkWcFjGsozfmycNFLWn
            rSS0ikDrKeucA4MWhptIiha92dEaut4NlcyqxZjs7WQYq3+iNmf+WuMYGq/PMCY2
            bX4CSV2v39AjEtgy/PYhXOKreFQT1qsSQtvR975D9BVqMTC3dBpe9YgXnb3EL90X
            gz5BrJpMimF3hZKxXk88/QIDAQABo4IBUjCCAU4wDgYDVR0PAQH/BAQDAgEGMCcG
            A1UdJQQgMB4GCCsGAQUFBwMBBggrBgEFBQcDAgYIKwYBBQUHAwkwEgYDVR0TAQH/
            BAgwBgEB/wIBADAdBgNVHQ4EFgQUUo+qWrOrcu7dyfmRocm5BhLSBN0wHwYDVR0j
            BBgwFoAUj/BLf6guRSSuTVD6Y5qL3uLdG7wwPgYIKwYBBQUHAQEEMjAwMC4GCCsG
            AQUFBzABhiJodHRwOi8vb2NzcDIuZ2xvYmFsc2lnbi5jb20vcm9vdHIzMDYGA1Ud
            HwQvMC0wK6ApoCeGJWh0dHA6Ly9jcmwuZ2xvYmFsc2lnbi5jb20vcm9vdC1yMy5j
            cmwwRwYDVR0gBEAwPjA8BgRVHSAAMDQwMgYIKwYBBQUHAgEWJmh0dHBzOi8vd3d3
            Lmdsb2JhbHNpZ24uY29tL3JlcG9zaXRvcnkvMA0GCSqGSIb3DQEBDAUAA4IBAQCq
            RGVh8nPOW7WlILMsE7ZtvimOJRpEZ0kd/Xz2AuV/nj+rLIPi74iaqDbqguhHOey9
            lINqPcwKvRnCvDu55d9SHcQce4/Th6CdBWW6Gdia6EqmbclWKXvPldWwNNEmFGbU
            yGvbegtnN7oYP48Z9TUgxVmGq2BRTz9PqqxZ9HYHTg755QiiCQo/llMfMHedcGrh
            qDIMvz0zy1L75MGHjcLX0801YLWUN6A2hWERL+MxBlzgk4el9hi5JTJZe6MEepx5
            Y+8uTIO2QbRc7F7PQEU05/lAPVPZOzjRt632xNMaNCx8Fx7/PZWrCbAlcbHQMNEs
            Z1SvsHdUqryEOgyXBpms
            -----END CERTIFICATE-----
            -----BEGIN CERTIFICATE-----
            MIIDXzCCAkegAwIBAgILBAAAAAABIVhTCKIwDQYJKoZIhvcNAQELBQAwTDEgMB4G
            A1UECxMXR2xvYmFsU2lnbiBSb290IENBIC0gUjMxEzARBgNVBAoTCkdsb2JhbFNp
            Z24xEzARBgNVBAMTCkdsb2JhbFNpZ24wHhcNMDkwMzE4MTAwMDAwWhcNMjkwMzE4
            MTAwMDAwWjBMMSAwHgYDVQQLExdHbG9iYWxTaWduIFJvb3QgQ0EgLSBSMzETMBEG
            A1UEChMKR2xvYmFsU2lnbjETMBEGA1UEAxMKR2xvYmFsU2lnbjCCASIwDQYJKoZI
            hvcNAQEBBQADggEPADCCAQoCggEBAMwldpB5BngiFvXAg7aEyiie/QV2EcWtiHL8
            RgJDx7KKnQRfJMsuS+FggkbhUqsMgUdwbN1k0ev1LKMPgj0MK66X17YUhhB5uzsT
            gHeMCOFJ0mpiLx9e+pZo34knlTifBtc+ycsmWQ1z3rDI6SYOgxXG71uL0gRgykmm
            KPZpO/bLyCiR5Z2KYVc3rHQU3HTgOu5yLy6c+9C7v/U9AOEGM+iCK65TpjoWc4zd
            QQ4gOsC0p6Hpsk+QLjJg6VfLuQSSaGjlOCZgdbKfd/+RFO+uIEn8rUAVSNECMWEZ
            XriX7613t2Saer9fwRPvm2L7DWzgVGkWqQPabumDk3F2xmmFghcCAwEAAaNCMEAw
            DgYDVR0PAQH/BAQDAgEGMA8GA1UdEwEB/wQFMAMBAf8wHQYDVR0OBBYEFI/wS3+o
            LkUkrk1Q+mOai97i3Ru8MA0GCSqGSIb3DQEBCwUAA4IBAQBLQNvAUKr+yAzv95ZU
            RUm7lgAJQayzE4aGKAczymvmdLm6AC2upArT9fHxD4q/c2dKg8dEe3jgr25sbwMp
            jjM5RcOO5LlXbKr8EpbsU8Yt5CRsuZRj+9xTaGdWPoO4zzUhw8lo/s7awlOqzJCK
            6fBdRoyV3XpYKBovHd7NADdBj+1EbddTKJd+82cEHhXXipa0095MJ6RMG3NzdvQX
            mcIfeg7jLQitChws/zyrVQ4PkX4268NXSb7hLi18YIvDQVETI53O9zJrlAGomecs
            Mx86OyXShkDOOyyGeMlhLxS67ttVb9+E7gUJTb0o2HLO02JQZR7rkpeDMdmztcpH
            WD9f
            -----END CERTIFICATE-----
            """;
    private static final String CERTIFICATE_SUBJECT_DN = "BusinessCategory=Private Organization,SERIALNUMBER=12227891,1.3.6.1.4.1.311.60.2.1.3=GB,C=GB,ST=london,L=london,STREET=Second Home\\, 125-127 Mare Street,OU=IT,O=okeo limited,CN=okeo.co,organizationIdentifier=PSDGB-FCA-916866";
    private static final String CERTIFICATE_ISSUER_DN = "C=BE,O=GlobalSign nv-sa,CN=GlobalSign RSA EV QWAC CA 2019";

    private UUID clientGroupId;
    private UUID clientId;
    private ClientGroup clientGroup;
    private ClientToken clientToken;

    @Autowired
    private TestRestTemplate testRestTemplate;
    @Autowired
    private WireMockServer wireMockServer;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private Clock clock;

    @Autowired
    private TestClientTokens testClientTokens;
    @Autowired
    private ClientGroupRepository clientGroupRepository;
    @Autowired
    private ClientMTLSCertificateDNRepository clientMTLSCertificateDNRepository;

    @BeforeEach
    void setUp() {
        clientGroupId = UUID.randomUUID();
        clientId = UUID.randomUUID();
        clientGroup = new ClientGroup(clientGroupId, "clientGroupName");
        var client = new Client(
                clientId,
                clientGroupId,
                "client name",
                "NL",
                false,
                true,
                "10.71",
                null,
                false,
                true,
                false,
                true,
                true,
                false,
                true,
                true,
                false,
                false,
                false,
                false,
                1L,
                Collections.emptySet()
        );
        clientGroup.getClients().add(client);

        clientToken = testClientTokens.createClientToken(clientGroupId, clientId,
                claims -> claims.setClaim(ClientTokenConstants.EXTRA_CLAIM_ISSUED_FOR, ASSISTANCE_PORTAL_YTS));
    }

    @ParameterizedTest
    @ValueSource(strings = {ASSISTANCE_PORTAL_YTS, DEV_PORTAL})
    void create_withNonExistingDN_shouldAddDNInPendingAdditionState(String tokenIssuedFor) throws Exception {
        clientToken = testClientTokens.createClientToken(clientGroupId, clientId,
                claims -> claims.setClaim(ClientTokenConstants.EXTRA_CLAIM_ISSUED_FOR, tokenIssuedFor));
        clientGroupRepository.save(clientGroup);

        IssueResponseDTO issueResponseDTO = new IssueResponseDTO("1", "JIRA-123", "https://self/YT-123");
        wireMockServer.stubFor(
                WireMock.post("/jira/rest/api/2/issue/")
                        .withRequestBody(equalToJson("""
                                {
                                  "fields": {
                                    "project": {
                                      "key": "YT"
                                    },
                                    "summary": "[test] Request for updating the distinguished names allowlist",
                                    "description": "client id: %s\\nclient name: client name\\n\\nAdd the following distinguished names to the allowlist:\\n- BusinessCategory=Private Organization,SERIALNUMBER=12227891,1.3.6.1.4.1.311.60.2.1.3=GB,C=GB,ST=london,L=london,STREET=Second Home\\\\, 125-127 Mare Street,OU=IT,O=okeo limited,CN=okeo.co,organizationIdentifier=PSDGB-FCA-916866\\n  issued by:\\n  C=BE,O=GlobalSign nv-sa,CN=GlobalSign RSA EV QWAC CA 2019\\n\\n\\nRemove the following distinguished names from the allowlist:\\n<n/a>\\n",
                                    "issuetype": {
                                      "name": "Submit a request or incident"
                                    },
                                    "customfield_10002": [
                                      1
                                    ],
                                    "customfield_10010": "yt/bbda6e6e-255e-48c4-9d34-f8ba05374247"
                                  }
                                }
                                """.formatted(clientId), true, false))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody(objectMapper.writeValueAsBytes(issueResponseDTO)))
        );

        NewDistinguishedNameDTO newDistinguishedNameDTO = new NewDistinguishedNameDTO(CERTIFICATE_CHAIN);
        HttpEntity<NewDistinguishedNameDTO> request = new HttpEntity<>(newDistinguishedNameDTO, getHttpHeaders(clientToken));
        ResponseEntity<DistinguishedNameDTO> response = testRestTemplate.exchange("/internal/clients/{clientId}/client-mtls-dns", HttpMethod.POST, request, new ParameterizedTypeReference<>() {
        }, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isEqualTo(
                new DistinguishedNameDTO(
                        response.getBody().getId(),
                        clientId,
                        CERTIFICATE_SUBJECT_DN,
                        CERTIFICATE_ISSUER_DN,
                        CERTIFICATE_CHAIN,
                        Status.PENDING_ADDITION,
                        LocalDateTime.now(clock),
                        LocalDateTime.now(clock),
                        "JIRA-123"
                )
        );
        assertThat(clientMTLSCertificateDNRepository.findAllByClientId(clientId)).usingFieldByFieldElementComparator().containsExactlyInAnyOrder(
                new ClientMTLSCertificateDN(
                        response.getBody().getId(),
                        clientId,
                        CERTIFICATE_SUBJECT_DN,
                        CERTIFICATE_ISSUER_DN,
                        CERTIFICATE_CHAIN,
                        Status.PENDING_ADDITION,
                        LocalDateTime.now(clock),
                        LocalDateTime.now(clock),
                        "JIRA-123"
                )
        );
    }

    @Test
    void create_withRequestBodyThatIsNotAPEM_shouldReturnError() {
        NewDistinguishedNameDTO newDistinguishedNameDTO = new NewDistinguishedNameDTO("BAD input");
        HttpEntity<NewDistinguishedNameDTO> request = new HttpEntity<>(newDistinguishedNameDTO, getHttpHeaders(clientToken));
        ResponseEntity<ErrorDTO> response = testRestTemplate.exchange("/internal/clients/{clientId}/client-mtls-dns", HttpMethod.POST, request, new ParameterizedTypeReference<>() {
        }, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isEqualTo(
                new ErrorDTO(
                        "CLS1008",
                        "Method argument not valid (request body validation error). Offending field: certificateChain"
                )
        );
        assertThat(clientMTLSCertificateDNRepository.findAllByClientId(clientId)).isEmpty();
    }

    @Test
    void create_withRequestBodyThatIsAPublicKey_shouldReturnError() {
        NewDistinguishedNameDTO newDistinguishedNameDTO = new NewDistinguishedNameDTO("""
                -----BEGIN PUBLIC KEY-----
                MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAtRcgcAv26UwYNHYqQ93t
                aulbrTbqRV+sjQtDexVaZruV8l8QD61h8NYyH+nhiL3pB3+AoBncovg7+R5S/Btt
                9bnsxsVsUwCLP0szNWlzrbgQV4TyKrRFNPfzfycdnkt3wrZuRg+FPdWqsPHILdcF
                YsDBLtWUwId3gapdJU0k6/KVFTmeIpJRLu00yBJdDwz29JmOvnOWr5cxlgQzMzL/
                eJq667nRbBgoinJKLsZ7A2H3Shdd2stvSxE90B3T7yFmF7hqmliY30905lD0vQYa
                GtA+/QBtMjpOQupL+i8H5usu3Ob9HqStiFXEcxioL6yHRQmIajSZGcIxzCvQy0oj
                1EAbpGV1StIVZhxMYRjPwNBg/XyT+NFbcbqX+p6KS1WH/DefyeBO4XDJCEj/hB9/
                YKeIdPq3nCWBnpoP5C9eIE6ywlQ9FwCw+3hbfbAJUrmKJ1prV3QJHJf6GyFljezM
                bfoOwbAVUDsVi4eVu4xdaccdPt6gL47oWsvy603CuP3CsNstFLkky5CVMuzixFJL
                EWJXMpE9iCiT1CQUBxCIYplD5psY+REwv/YepGX3LQxvMhCCcnCH2zLrySm7cHcf
                +xC0kCJB5PlW+bHdFxJ6gkOkOtgxs19Osq4+YIEgaqFoy807SAvMJ1DqHYtyBNzn
                QbXUF7AHwjLI63DqZL3KHNcCAwEAAQ==
                -----END PUBLIC KEY-----
                """);
        HttpEntity<NewDistinguishedNameDTO> request = new HttpEntity<>(newDistinguishedNameDTO, getHttpHeaders(clientToken));
        ResponseEntity<ErrorDTO> response = testRestTemplate.exchange("/internal/clients/{clientId}/client-mtls-dns", HttpMethod.POST, request, new ParameterizedTypeReference<>() {
        }, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isEqualTo(
                new ErrorDTO(
                        "CLS1008",
                        "Method argument not valid (request body validation error). Offending field: certificateChain"
                )
        );
        assertThat(clientMTLSCertificateDNRepository.findAllByClientId(clientId)).isEmpty();
    }

    @Test
    void create_withRequestBodyTooLongCertificationPath_shouldReturnError() {
        StringBuilder sb = new StringBuilder();
        IntStream.range(0, 257).forEach(count -> sb.append("""
                -----BEGIN CERTIFICATE-----
                MIIDXzCCAkegAwIBAgILBAAAAAABIVhTCKIwDQYJKoZIhvcNAQELBQAwTDEgMB4G
                A1UECxMXR2xvYmFsU2lnbiBSb290IENBIC0gUjMxEzARBgNVBAoTCkdsb2JhbFNp
                Z24xEzARBgNVBAMTCkdsb2JhbFNpZ24wHhcNMDkwMzE4MTAwMDAwWhcNMjkwMzE4
                MTAwMDAwWjBMMSAwHgYDVQQLExdHbG9iYWxTaWduIFJvb3QgQ0EgLSBSMzETMBEG
                A1UEChMKR2xvYmFsU2lnbjETMBEGA1UEAxMKR2xvYmFsU2lnbjCCASIwDQYJKoZI
                hvcNAQEBBQADggEPADCCAQoCggEBAMwldpB5BngiFvXAg7aEyiie/QV2EcWtiHL8
                RgJDx7KKnQRfJMsuS+FggkbhUqsMgUdwbN1k0ev1LKMPgj0MK66X17YUhhB5uzsT
                gHeMCOFJ0mpiLx9e+pZo34knlTifBtc+ycsmWQ1z3rDI6SYOgxXG71uL0gRgykmm
                KPZpO/bLyCiR5Z2KYVc3rHQU3HTgOu5yLy6c+9C7v/U9AOEGM+iCK65TpjoWc4zd
                QQ4gOsC0p6Hpsk+QLjJg6VfLuQSSaGjlOCZgdbKfd/+RFO+uIEn8rUAVSNECMWEZ
                XriX7613t2Saer9fwRPvm2L7DWzgVGkWqQPabumDk3F2xmmFghcCAwEAAaNCMEAw
                DgYDVR0PAQH/BAQDAgEGMA8GA1UdEwEB/wQFMAMBAf8wHQYDVR0OBBYEFI/wS3+o
                LkUkrk1Q+mOai97i3Ru8MA0GCSqGSIb3DQEBCwUAA4IBAQBLQNvAUKr+yAzv95ZU
                RUm7lgAJQayzE4aGKAczymvmdLm6AC2upArT9fHxD4q/c2dKg8dEe3jgr25sbwMp
                jjM5RcOO5LlXbKr8EpbsU8Yt5CRsuZRj+9xTaGdWPoO4zzUhw8lo/s7awlOqzJCK
                6fBdRoyV3XpYKBovHd7NADdBj+1EbddTKJd+82cEHhXXipa0095MJ6RMG3NzdvQX
                mcIfeg7jLQitChws/zyrVQ4PkX4268NXSb7hLi18YIvDQVETI53O9zJrlAGomecs
                Mx86OyXShkDOOyyGeMlhLxS67ttVb9+E7gUJTb0o2HLO02JQZR7rkpeDMdmztcpH
                WD9f
                -----END CERTIFICATE-----
                """));

        NewDistinguishedNameDTO newDistinguishedNameDTO = new NewDistinguishedNameDTO(sb.toString());
        HttpEntity<NewDistinguishedNameDTO> request = new HttpEntity<>(newDistinguishedNameDTO, getHttpHeaders(clientToken));
        ResponseEntity<ErrorDTO> response = testRestTemplate.exchange("/internal/clients/{clientId}/client-mtls-dns", HttpMethod.POST, request, new ParameterizedTypeReference<>() {
        }, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isEqualTo(
                new ErrorDTO(
                        "CLS1008",
                        "Method argument not valid (request body validation error). Offending field: certificateChain"
                )
        );
        assertThat(clientMTLSCertificateDNRepository.findAllByClientId(clientId)).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(value = Status.class, names = {"ADDED", "PENDING_ADDITION"})
    void create_withExistingDNInAddedState_shouldNotDoAnyUpdates(Status status) {
        UUID dnId = UUID.randomUUID();
        clientGroupRepository.save(clientGroup);
        clientMTLSCertificateDNRepository.save(
                new ClientMTLSCertificateDN(
                        dnId,
                        clientId,
                        CERTIFICATE_SUBJECT_DN,
                        CERTIFICATE_ISSUER_DN,
                        CERTIFICATE_CHAIN,
                        status,
                        LocalDateTime.now(clock).minusDays(10),
                        LocalDateTime.now(clock).minusDays(5),
                        null
                )
        );

        NewDistinguishedNameDTO newDistinguishedNameDTO = new NewDistinguishedNameDTO(CERTIFICATE_CHAIN);
        HttpEntity<NewDistinguishedNameDTO> request = new HttpEntity<>(newDistinguishedNameDTO, getHttpHeaders(clientToken));
        ResponseEntity<DistinguishedNameDTO> response = testRestTemplate.exchange("/internal/clients/{clientId}/client-mtls-dns", HttpMethod.POST, request, new ParameterizedTypeReference<>() {
        }, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isEqualTo(
                new DistinguishedNameDTO(
                        dnId,
                        clientId,
                        CERTIFICATE_SUBJECT_DN,
                        CERTIFICATE_ISSUER_DN,
                        CERTIFICATE_CHAIN,
                        status,
                        LocalDateTime.now(clock).minusDays(10),
                        LocalDateTime.now(clock).minusDays(5),
                        null
                )
        );
        assertThat(clientMTLSCertificateDNRepository.findAllByClientId(clientId)).usingFieldByFieldElementComparator().containsExactlyInAnyOrder(
                new ClientMTLSCertificateDN(
                        dnId,
                        clientId,
                        CERTIFICATE_SUBJECT_DN,
                        CERTIFICATE_ISSUER_DN,
                        CERTIFICATE_CHAIN,
                        status,
                        LocalDateTime.now(clock).minusDays(10),
                        LocalDateTime.now(clock).minusDays(5),
                        null
                )
        );
    }

    @Test
    void create_withExistingDNInPendingRemovalState_shouldCancelTheRemovalAndUpdateJira() {
        UUID dnId = UUID.randomUUID();
        clientGroupRepository.save(clientGroup);
        clientMTLSCertificateDNRepository.save(
                new ClientMTLSCertificateDN(
                        dnId,
                        clientId,
                        CERTIFICATE_SUBJECT_DN,
                        CERTIFICATE_ISSUER_DN,
                        CERTIFICATE_CHAIN,
                        Status.PENDING_REMOVAL,
                        LocalDateTime.now(clock).minusDays(10),
                        LocalDateTime.now(clock).minusDays(5),
                        "JIRA-123"
                )
        );

        this.wireMockServer.stubFor(
                WireMock.post("/jira/rest/api/2/issue/JIRA-123/comment")
                        .withRequestBody(equalToJson("""
                                {
                                  "body": "The following distinguished names have been handled:\\n- BusinessCategory=Private Organization,SERIALNUMBER=12227891,1.3.6.1.4.1.311.60.2.1.3=GB,C=GB,ST=london,L=london,STREET=Second Home\\\\, 125-127 Mare Street,OU=IT,O=okeo limited,CN=okeo.co,organizationIdentifier=PSDGB-FCA-916866\\n  issued by:\\n  C=BE,O=GlobalSign nv-sa,CN=GlobalSign RSA EV QWAC CA 2019\\n\\n"
                                }
                                """, true, false))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody("comment"))
        );

        NewDistinguishedNameDTO newDistinguishedNameDTO = new NewDistinguishedNameDTO(CERTIFICATE_CHAIN);
        HttpEntity<NewDistinguishedNameDTO> request = new HttpEntity<>(newDistinguishedNameDTO, getHttpHeaders(clientToken));
        ResponseEntity<DistinguishedNameDTO> response = testRestTemplate.exchange("/internal/clients/{clientId}/client-mtls-dns", HttpMethod.POST, request, new ParameterizedTypeReference<>() {
        }, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isEqualTo(
                new DistinguishedNameDTO(
                        dnId,
                        clientId,
                        CERTIFICATE_SUBJECT_DN,
                        CERTIFICATE_ISSUER_DN,
                        CERTIFICATE_CHAIN,
                        Status.ADDED,
                        LocalDateTime.now(clock).minusDays(10),
                        LocalDateTime.now(clock),
                        null
                )
        );
        assertThat(clientMTLSCertificateDNRepository.findAllByClientId(clientId)).usingFieldByFieldElementComparator().containsExactlyInAnyOrder(
                new ClientMTLSCertificateDN(
                        dnId,
                        clientId,
                        CERTIFICATE_SUBJECT_DN,
                        CERTIFICATE_ISSUER_DN,
                        CERTIFICATE_CHAIN,
                        Status.ADDED,
                        LocalDateTime.now(clock).minusDays(10),
                        LocalDateTime.now(clock),
                        null
                )
        );
    }

    @Test
    void create_withExistingDNInRemovedState_shouldReAddTheDNInPendingAddition() throws Exception {
        UUID dnId = UUID.randomUUID();
        clientGroupRepository.save(clientGroup);
        clientMTLSCertificateDNRepository.save(
                new ClientMTLSCertificateDN(
                        dnId,
                        clientId,
                        CERTIFICATE_SUBJECT_DN,
                        CERTIFICATE_ISSUER_DN,
                        CERTIFICATE_CHAIN,
                        Status.REMOVED,
                        LocalDateTime.now(clock).minusDays(10),
                        LocalDateTime.now(clock).minusDays(5),
                        null
                )
        );

        IssueResponseDTO issueResponseDTO = new IssueResponseDTO("1", "JIRA-123", "https://self/JIRA-123");
        wireMockServer.stubFor(
                WireMock.post("/jira/rest/api/2/issue/")
                        .withRequestBody(equalToJson("""
                                {
                                  "fields": {
                                    "project": {
                                      "key": "YT"
                                    },
                                    "summary": "[test] Request for updating the distinguished names allowlist",
                                    "description": "client id: %s\\nclient name: client name\\n\\nAdd the following distinguished names to the allowlist:\\n- BusinessCategory=Private Organization,SERIALNUMBER=12227891,1.3.6.1.4.1.311.60.2.1.3=GB,C=GB,ST=london,L=london,STREET=Second Home\\\\, 125-127 Mare Street,OU=IT,O=okeo limited,CN=okeo.co,organizationIdentifier=PSDGB-FCA-916866\\n  issued by:\\n  C=BE,O=GlobalSign nv-sa,CN=GlobalSign RSA EV QWAC CA 2019\\n\\n\\nRemove the following distinguished names from the allowlist:\\n<n/a>\\n",
                                    "issuetype": {
                                      "name": "Submit a request or incident"
                                    },
                                    "customfield_10002": [
                                      1
                                    ],
                                    "customfield_10010": "yt/bbda6e6e-255e-48c4-9d34-f8ba05374247"
                                  }
                                }
                                """.formatted(clientId), true, false))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody(objectMapper.writeValueAsBytes(issueResponseDTO)))
        );

        NewDistinguishedNameDTO newDistinguishedNameDTO = new NewDistinguishedNameDTO(CERTIFICATE_CHAIN);
        HttpEntity<NewDistinguishedNameDTO> request = new HttpEntity<>(newDistinguishedNameDTO, getHttpHeaders(clientToken));
        ResponseEntity<DistinguishedNameDTO> response = testRestTemplate.exchange("/internal/clients/{clientId}/client-mtls-dns", HttpMethod.POST, request, new ParameterizedTypeReference<>() {
        }, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isEqualTo(
                new DistinguishedNameDTO(
                        dnId,
                        clientId,
                        CERTIFICATE_SUBJECT_DN,
                        CERTIFICATE_ISSUER_DN,
                        CERTIFICATE_CHAIN,
                        Status.PENDING_ADDITION,
                        LocalDateTime.now(clock).minusDays(10),
                        LocalDateTime.now(clock),
                        "JIRA-123"
                )
        );
        assertThat(clientMTLSCertificateDNRepository.findAllByClientId(clientId)).usingFieldByFieldElementComparator().containsExactlyInAnyOrder(
                new ClientMTLSCertificateDN(
                        dnId,
                        clientId,
                        CERTIFICATE_SUBJECT_DN,
                        CERTIFICATE_ISSUER_DN,
                        CERTIFICATE_CHAIN,
                        Status.PENDING_ADDITION,
                        LocalDateTime.now(clock).minusDays(10),
                        LocalDateTime.now(clock),
                        "JIRA-123"
                )
        );
    }

    @Test
    void create_withExistingDNInDeniedState_shouldReturnAnError() {
        UUID dnId = UUID.randomUUID();
        clientGroupRepository.save(clientGroup);
        clientMTLSCertificateDNRepository.save(
                new ClientMTLSCertificateDN(
                        dnId,
                        clientId,
                        CERTIFICATE_SUBJECT_DN,
                        CERTIFICATE_ISSUER_DN,
                        CERTIFICATE_CHAIN,
                        Status.DENIED,
                        LocalDateTime.now(clock).minusDays(10),
                        LocalDateTime.now(clock).minusDays(5),
                        null
                )
        );

        NewDistinguishedNameDTO newDistinguishedNameDTO = new NewDistinguishedNameDTO(CERTIFICATE_CHAIN);
        HttpEntity<NewDistinguishedNameDTO> request = new HttpEntity<>(newDistinguishedNameDTO, getHttpHeaders(clientToken));
        ResponseEntity<ErrorDTO> response = testRestTemplate.exchange("/internal/clients/{clientId}/client-mtls-dns", HttpMethod.POST, request, new ParameterizedTypeReference<>() {
        }, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isEqualTo(
                new ErrorDTO(
                        "CLS037",
                        "The DN in the supplied certificate has already been denied in the past."
                )
        );
        assertThat(clientMTLSCertificateDNRepository.findAllByClientId(clientId)).usingFieldByFieldElementComparator().containsExactlyInAnyOrder(
                new ClientMTLSCertificateDN(
                        dnId,
                        clientId,
                        CERTIFICATE_SUBJECT_DN,
                        CERTIFICATE_ISSUER_DN,
                        CERTIFICATE_CHAIN,
                        Status.DENIED,
                        LocalDateTime.now(clock).minusDays(10),
                        LocalDateTime.now(clock).minusDays(5),
                        null
                )
        );
    }

    @Test
    void create_withTooManyExistingItems_shouldRateLimit() {
        clientGroupRepository.save(clientGroup);

        IntStream.range(0, 10).forEach(count -> clientMTLSCertificateDNRepository.save(
                new ClientMTLSCertificateDN(
                        UUID.fromString("00000000-0000-0000-%04d-000000000000".formatted(count)),
                        clientId,
                        "subject_dn_" + count,
                        "issuer_dn_" + count,
                        CERTIFICATE_CHAIN,
                        Status.PENDING_ADDITION,
                        LocalDateTime.now(clock).minusMinutes(count),
                        LocalDateTime.now(clock).minusMinutes(count),
                        "jira-" + count
                )
        ));

        NewDistinguishedNameDTO newDistinguishedNameDTO = new NewDistinguishedNameDTO(CERTIFICATE_CHAIN);
        HttpEntity<NewDistinguishedNameDTO> request = new HttpEntity<>(newDistinguishedNameDTO, getHttpHeaders(clientToken));
        ResponseEntity<ErrorDTO> response = testRestTemplate.exchange("/internal/clients/{clientId}/client-mtls-dns", HttpMethod.POST, request, new ParameterizedTypeReference<>() {
        }, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isEqualTo(
                new ErrorDTO(
                        "CLS019",
                        "There are already too many pending tickets created."
                )
        );
        assertThat(clientMTLSCertificateDNRepository.findAllByClientId(clientId)).hasSize(10);
    }

    @Test
    void create_withOtherUserType_shouldReturnError() {
        clientToken = testClientTokens.createClientToken(clientGroupId, clientId,
                claims -> claims.setClaim(ClientTokenConstants.EXTRA_CLAIM_ISSUED_FOR, "other"));

        clientGroupRepository.save(clientGroup);

        NewDistinguishedNameDTO newDistinguishedNameDTO = new NewDistinguishedNameDTO(CERTIFICATE_CHAIN);
        HttpEntity<NewDistinguishedNameDTO> request = new HttpEntity<>(newDistinguishedNameDTO, getHttpHeaders(clientToken));
        ResponseEntity<ErrorDTO> response = testRestTemplate.exchange("/internal/clients/{clientId}/client-mtls-dns", HttpMethod.POST, request, new ParameterizedTypeReference<>() {
        }, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isEqualTo(
                new ErrorDTO(
                        "CLS9002",
                        "Token requester for client-token is unauthorized."
                )
        );
        assertThat(clientMTLSCertificateDNRepository.findAllByClientId(clientId)).isEmpty();
    }

    @Test
    void create_withDeletedClient_shouldReturnError() {
        clientToken = testClientTokens.createClientToken(clientGroupId, clientId, claims -> {
            claims.setClaim(ClientTokenConstants.EXTRA_CLAIM_ISSUED_FOR, ASSISTANCE_PORTAL_YTS);
            claims.setClaim(ClientTokenConstants.CLAIM_DELETED, true);
        });

        clientGroupRepository.save(clientGroup);

        NewDistinguishedNameDTO newDistinguishedNameDTO = new NewDistinguishedNameDTO(CERTIFICATE_CHAIN);
        HttpEntity<NewDistinguishedNameDTO> request = new HttpEntity<>(newDistinguishedNameDTO, getHttpHeaders(clientToken));
        ResponseEntity<ErrorDTO> response = testRestTemplate.exchange("/internal/clients/{clientId}/client-mtls-dns", HttpMethod.POST, request, new ParameterizedTypeReference<>() {
        }, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isEqualTo(
                new ErrorDTO(
                        "CLS9003",
                        "Client-token is unauthorized."
                )
        );
        assertThat(clientMTLSCertificateDNRepository.findAllByClientId(clientId)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {ASSISTANCE_PORTAL_YTS, DEV_PORTAL})
    void delete_withItemsInAllDifferentStates_shouldReturnOk(String tokenIssuedFor) {
        clientToken = testClientTokens.createClientToken(clientGroupId, clientId,
                claims -> claims.setClaim(ClientTokenConstants.EXTRA_CLAIM_ISSUED_FOR, tokenIssuedFor));
        clientGroupRepository.save(clientGroup);

        UUID dnId1 = UUID.randomUUID();
        UUID dnId2 = UUID.randomUUID();
        UUID dnId3 = UUID.randomUUID();
        UUID dnId4 = UUID.randomUUID();
        UUID dnId5 = UUID.randomUUID();

        clientMTLSCertificateDNRepository.save(
                new ClientMTLSCertificateDN(
                        dnId1,
                        clientId,
                        "subject_dn_PENDING_ADDITION",
                        "issuer_dn_PENDING_ADDITION",
                        CERTIFICATE_CHAIN,
                        Status.PENDING_ADDITION,
                        LocalDateTime.now(clock).minusMinutes(1),
                        LocalDateTime.now(clock).minusMinutes(1),
                        "jira-001"
                )
        );
        clientMTLSCertificateDNRepository.save(
                new ClientMTLSCertificateDN(
                        dnId2,
                        clientId,
                        "subject_dn_ADDED",
                        "issuer_dn_ADDED",
                        CERTIFICATE_CHAIN,
                        Status.ADDED,
                        LocalDateTime.now(clock).minusMinutes(1),
                        LocalDateTime.now(clock).minusMinutes(1),
                        null
                )
        );
        clientMTLSCertificateDNRepository.save(
                new ClientMTLSCertificateDN(
                        dnId3,
                        clientId,
                        "subject_dn_PENDING_REMOVAL",
                        "issuer_dn_PENDING_REMOVAL",
                        CERTIFICATE_CHAIN,
                        Status.PENDING_REMOVAL,
                        LocalDateTime.now(clock).minusMinutes(1),
                        LocalDateTime.now(clock).minusMinutes(1),
                        "jira-003"
                )
        );
        clientMTLSCertificateDNRepository.save(
                new ClientMTLSCertificateDN(
                        dnId4,
                        clientId,
                        "subject_dn_REMOVED",
                        "issuer_dn_REMOVED",
                        CERTIFICATE_CHAIN,
                        Status.REMOVED,
                        LocalDateTime.now(clock).minusMinutes(1),
                        LocalDateTime.now(clock).minusMinutes(1),
                        null
                )
        );
        clientMTLSCertificateDNRepository.save(
                new ClientMTLSCertificateDN(
                        dnId5,
                        clientId,
                        "subject_dn_DENIED",
                        "issuer_dn_DENIED",
                        CERTIFICATE_CHAIN,
                        Status.DENIED,
                        LocalDateTime.now(clock).minusMinutes(1),
                        LocalDateTime.now(clock).minusMinutes(1),
                        null
                )
        );

        // wiremocks
        // expect comment on "jira-001"
        this.wireMockServer.stubFor(
                WireMock.post("/jira/rest/api/2/issue/jira-001/comment")
                        .withRequestBody(equalToJson("""
                                {
                                  "body": "The following distinguished names have been handled:\\n- subject_dn_PENDING_ADDITION\\n  issued by:\\n  issuer_dn_PENDING_ADDITION\\n\\n"
                                }
                                """, true, false))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody("comment"))
        );
        // expect creation of ticket "jira-002"
        wireMockServer.stubFor(
                WireMock.post("/jira/rest/api/2/issue/")
                        .withRequestBody(equalToJson("""
                                {
                                  "fields": {
                                    "project": {
                                      "key": "YT"
                                    },
                                    "summary": "[test] Request for updating the distinguished names allowlist",
                                    "description": "client id: %s\\nclient name: client name\\n\\nAdd the following distinguished names to the allowlist:\\n<n/a>\\n\\nRemove the following distinguished names from the allowlist:\\n- subject_dn_ADDED\\n  issued by:\\n  issuer_dn_ADDED\\n\\n",
                                    "issuetype": {
                                      "name": "Submit a request or incident"
                                    },
                                    "customfield_10002": [
                                      1
                                    ],
                                    "customfield_10010": "yt/bbda6e6e-255e-48c4-9d34-f8ba05374247"
                                  }
                                }
                                """.formatted(clientId), true, false))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody("""
                                        {
                                          "id":"1",
                                          "key":"jira-002",
                                          "self":"https://self/jira-002"
                                        }
                                        """))
        );

        DistinguishedNameIdListDTO distinguishedNameIdListDTO = new DistinguishedNameIdListDTO(Set.of(dnId1, dnId2, dnId3, dnId4, dnId5));
        HttpEntity<DistinguishedNameIdListDTO> request = new HttpEntity<>(distinguishedNameIdListDTO, getHttpHeaders(clientToken));
        ResponseEntity<Set<DistinguishedNameDTO>> response = testRestTemplate.exchange("/internal/clients/{clientId}/client-mtls-dns/delete", HttpMethod.POST, request, new ParameterizedTypeReference<>() {
        }, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsExactlyInAnyOrder(
                new DistinguishedNameDTO(
                        dnId1,
                        clientId,
                        "subject_dn_PENDING_ADDITION",
                        "issuer_dn_PENDING_ADDITION",
                        CERTIFICATE_CHAIN,
                        Status.REMOVED,
                        LocalDateTime.now(clock).minusMinutes(1),
                        LocalDateTime.now(clock),
                        null
                ),
                new DistinguishedNameDTO(
                        dnId2,
                        clientId,
                        "subject_dn_ADDED",
                        "issuer_dn_ADDED",
                        CERTIFICATE_CHAIN,
                        Status.PENDING_REMOVAL,
                        LocalDateTime.now(clock).minusMinutes(1),
                        LocalDateTime.now(clock),
                        "jira-002"
                ),
                new DistinguishedNameDTO(
                        dnId3,
                        clientId,
                        "subject_dn_PENDING_REMOVAL",
                        "issuer_dn_PENDING_REMOVAL",
                        CERTIFICATE_CHAIN,
                        Status.PENDING_REMOVAL,
                        LocalDateTime.now(clock).minusMinutes(1),
                        LocalDateTime.now(clock).minusMinutes(1),
                        "jira-003"
                ),
                new DistinguishedNameDTO(
                        dnId4,
                        clientId,
                        "subject_dn_REMOVED",
                        "issuer_dn_REMOVED",
                        CERTIFICATE_CHAIN,
                        Status.REMOVED,
                        LocalDateTime.now(clock).minusMinutes(1),
                        LocalDateTime.now(clock).minusMinutes(1),
                        null
                ),
                new DistinguishedNameDTO(
                        dnId5,
                        clientId,
                        "subject_dn_DENIED",
                        "issuer_dn_DENIED",
                        CERTIFICATE_CHAIN,
                        Status.DENIED,
                        LocalDateTime.now(clock).minusMinutes(1),
                        LocalDateTime.now(clock).minusMinutes(1),
                        null
                )
        );
        assertThat(clientMTLSCertificateDNRepository.findAllByClientId(clientId)).usingFieldByFieldElementComparator().containsExactlyInAnyOrder(
                new ClientMTLSCertificateDN(
                        dnId1,
                        clientId,
                        "subject_dn_PENDING_ADDITION",
                        "issuer_dn_PENDING_ADDITION",
                        CERTIFICATE_CHAIN,
                        Status.REMOVED,
                        LocalDateTime.now(clock).minusMinutes(1),
                        LocalDateTime.now(clock),
                        null
                ),
                new ClientMTLSCertificateDN(
                        dnId2,
                        clientId,
                        "subject_dn_ADDED",
                        "issuer_dn_ADDED",
                        CERTIFICATE_CHAIN,
                        Status.PENDING_REMOVAL,
                        LocalDateTime.now(clock).minusMinutes(1),
                        LocalDateTime.now(clock),
                        "jira-002"
                ),
                new ClientMTLSCertificateDN(
                        dnId3,
                        clientId,
                        "subject_dn_PENDING_REMOVAL",
                        "issuer_dn_PENDING_REMOVAL",
                        CERTIFICATE_CHAIN,
                        Status.PENDING_REMOVAL,
                        LocalDateTime.now(clock).minusMinutes(1),
                        LocalDateTime.now(clock).minusMinutes(1),
                        "jira-003"
                ),
                new ClientMTLSCertificateDN(
                        dnId4,
                        clientId,
                        "subject_dn_REMOVED",
                        "issuer_dn_REMOVED",
                        CERTIFICATE_CHAIN,
                        Status.REMOVED,
                        LocalDateTime.now(clock).minusMinutes(1),
                        LocalDateTime.now(clock).minusMinutes(1),
                        null
                ),
                new ClientMTLSCertificateDN(
                        dnId5,
                        clientId,
                        "subject_dn_DENIED",
                        "issuer_dn_DENIED",
                        CERTIFICATE_CHAIN,
                        Status.DENIED,
                        LocalDateTime.now(clock).minusMinutes(1),
                        LocalDateTime.now(clock).minusMinutes(1),
                        null
                )
        );
    }

    @Test
    void delete_withTooManyExistingItems_shouldRateLimit() {
        clientGroupRepository.save(clientGroup);

        UUID dnId1 = UUID.randomUUID();

        clientMTLSCertificateDNRepository.save(
                new ClientMTLSCertificateDN(
                        dnId1,
                        clientId,
                        "subject_dn_ADDED",
                        "issuer_dn_ADDED",
                        CERTIFICATE_CHAIN,
                        Status.ADDED,
                        LocalDateTime.now(clock).minusMinutes(1),
                        LocalDateTime.now(clock).minusMinutes(1),
                        null
                )
        );

        IntStream.range(0, 10).forEach(count -> clientMTLSCertificateDNRepository.save(
                new ClientMTLSCertificateDN(
                        UUID.fromString("00000000-0000-0000-%04d-000000000000".formatted(count)),
                        clientId,
                        "subject_dn_" + count,
                        "issuer_dn_" + count,
                        CERTIFICATE_CHAIN,
                        Status.PENDING_ADDITION,
                        LocalDateTime.now(clock).minusMinutes(count),
                        LocalDateTime.now(clock).minusMinutes(count),
                        "jira-" + count
                )
        ));

        DistinguishedNameIdListDTO distinguishedNameIdListDTO = new DistinguishedNameIdListDTO(Set.of(dnId1));
        HttpEntity<DistinguishedNameIdListDTO> request = new HttpEntity<>(distinguishedNameIdListDTO, getHttpHeaders(clientToken));
        ResponseEntity<ErrorDTO> response = testRestTemplate.exchange("/internal/clients/{clientId}/client-mtls-dns/delete", HttpMethod.POST, request, new ParameterizedTypeReference<>() {
        }, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isEqualTo(
                new ErrorDTO(
                        "CLS019",
                        "There are already too many pending tickets created."
                )
        );
        assertThat(clientMTLSCertificateDNRepository.findAllByClientId(clientId)).hasSize(11).contains(
                new ClientMTLSCertificateDN(
                        dnId1,
                        clientId,
                        "subject_dn_ADDED",
                        "issuer_dn_ADDED",
                        CERTIFICATE_CHAIN,
                        Status.ADDED,
                        LocalDateTime.now(clock).minusMinutes(1),
                        LocalDateTime.now(clock).minusMinutes(1),
                        null
                )
        );
    }

    @Test
    void delete_withOtherUserType_shouldReturnError() {
        clientToken = testClientTokens.createClientToken(clientGroupId, clientId,
                claims -> claims.setClaim(ClientTokenConstants.EXTRA_CLAIM_ISSUED_FOR, "other"));

        clientGroupRepository.save(clientGroup);

        UUID dnId1 = UUID.randomUUID();

        clientMTLSCertificateDNRepository.save(
                new ClientMTLSCertificateDN(
                        dnId1,
                        clientId,
                        "subject_dn_PENDING_ADDITION",
                        "issuer_dn_PENDING_ADDITION",
                        CERTIFICATE_CHAIN,
                        Status.PENDING_ADDITION,
                        LocalDateTime.now(clock).minusMinutes(1),
                        LocalDateTime.now(clock).minusMinutes(1),
                        "jira-001"
                )
        );

        DistinguishedNameIdListDTO distinguishedNameIdListDTO = new DistinguishedNameIdListDTO(Set.of(dnId1));
        HttpEntity<DistinguishedNameIdListDTO> request = new HttpEntity<>(distinguishedNameIdListDTO, getHttpHeaders(clientToken));
        ResponseEntity<ErrorDTO> response = testRestTemplate.exchange("/internal/clients/{clientId}/client-mtls-dns/delete", HttpMethod.POST, request, new ParameterizedTypeReference<>() {
        }, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isEqualTo(
                new ErrorDTO(
                        "CLS9002",
                        "Token requester for client-token is unauthorized."
                )
        );
        assertThat(clientMTLSCertificateDNRepository.findAllByClientId(clientId)).hasSize(1).contains(
                new ClientMTLSCertificateDN(
                        dnId1,
                        clientId,
                        "subject_dn_PENDING_ADDITION",
                        "issuer_dn_PENDING_ADDITION",
                        CERTIFICATE_CHAIN,
                        Status.PENDING_ADDITION,
                        LocalDateTime.now(clock).minusMinutes(1),
                        LocalDateTime.now(clock).minusMinutes(1),
                        "jira-001"
                )
        );
    }

    @Test
    void delete_withDeletedClient_shouldReturnError() {
        clientToken = testClientTokens.createClientToken(clientGroupId, clientId, claims -> {
            claims.setClaim(ClientTokenConstants.EXTRA_CLAIM_ISSUED_FOR, ASSISTANCE_PORTAL_YTS);
            claims.setClaim(ClientTokenConstants.CLAIM_DELETED, true);
        });

        clientGroupRepository.save(clientGroup);

        UUID dnId1 = UUID.randomUUID();

        clientMTLSCertificateDNRepository.save(
                new ClientMTLSCertificateDN(
                        dnId1,
                        clientId,
                        "subject_dn_PENDING_ADDITION",
                        "issuer_dn_PENDING_ADDITION",
                        CERTIFICATE_CHAIN,
                        Status.PENDING_ADDITION,
                        LocalDateTime.now(clock).minusMinutes(1),
                        LocalDateTime.now(clock).minusMinutes(1),
                        "jira-001"
                )
        );

        DistinguishedNameIdListDTO distinguishedNameIdListDTO = new DistinguishedNameIdListDTO(Set.of(dnId1));
        HttpEntity<DistinguishedNameIdListDTO> request = new HttpEntity<>(distinguishedNameIdListDTO, getHttpHeaders(clientToken));
        ResponseEntity<ErrorDTO> response = testRestTemplate.exchange("/internal/clients/{clientId}/client-mtls-dns/delete", HttpMethod.POST, request, new ParameterizedTypeReference<>() {
        }, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isEqualTo(
                new ErrorDTO(
                        "CLS9003",
                        "Client-token is unauthorized."
                )
        );
        assertThat(clientMTLSCertificateDNRepository.findAllByClientId(clientId)).hasSize(1).contains(
                new ClientMTLSCertificateDN(
                        dnId1,
                        clientId,
                        "subject_dn_PENDING_ADDITION",
                        "issuer_dn_PENDING_ADDITION",
                        CERTIFICATE_CHAIN,
                        Status.PENDING_ADDITION,
                        LocalDateTime.now(clock).minusMinutes(1),
                        LocalDateTime.now(clock).minusMinutes(1),
                        "jira-001"
                )
        );
    }

    @Test
    void markApplied_asAPYUserWithItemsInAllDifferentStates_shouldReturnOk() {
        clientGroupRepository.save(clientGroup);

        UUID dnId1 = UUID.randomUUID();
        UUID dnId2 = UUID.randomUUID();
        UUID dnId3 = UUID.randomUUID();
        UUID dnId4 = UUID.randomUUID();
        UUID dnId5 = UUID.randomUUID();

        clientMTLSCertificateDNRepository.save(
                new ClientMTLSCertificateDN(
                        dnId1,
                        clientId,
                        "subject_dn_PENDING_ADDITION",
                        "issuer_dn_PENDING_ADDITION",
                        CERTIFICATE_CHAIN,
                        Status.PENDING_ADDITION,
                        LocalDateTime.now(clock).minusMinutes(1),
                        LocalDateTime.now(clock).minusMinutes(1),
                        "jira-001"
                )
        );
        clientMTLSCertificateDNRepository.save(
                new ClientMTLSCertificateDN(
                        dnId2,
                        clientId,
                        "subject_dn_ADDED",
                        "issuer_dn_ADDED",
                        CERTIFICATE_CHAIN,
                        Status.ADDED,
                        LocalDateTime.now(clock).minusMinutes(1),
                        LocalDateTime.now(clock).minusMinutes(1),
                        null
                )
        );
        clientMTLSCertificateDNRepository.save(
                new ClientMTLSCertificateDN(
                        dnId3,
                        clientId,
                        "subject_dn_PENDING_REMOVAL",
                        "issuer_dn_PENDING_REMOVAL",
                        CERTIFICATE_CHAIN,
                        Status.PENDING_REMOVAL,
                        LocalDateTime.now(clock).minusMinutes(1),
                        LocalDateTime.now(clock).minusMinutes(1),
                        "jira-003"
                )
        );
        clientMTLSCertificateDNRepository.save(
                new ClientMTLSCertificateDN(
                        dnId4,
                        clientId,
                        "subject_dn_REMOVED",
                        "issuer_dn_REMOVED",
                        CERTIFICATE_CHAIN,
                        Status.REMOVED,
                        LocalDateTime.now(clock).minusMinutes(1),
                        LocalDateTime.now(clock).minusMinutes(1),
                        null
                )
        );
        clientMTLSCertificateDNRepository.save(
                new ClientMTLSCertificateDN(
                        dnId5,
                        clientId,
                        "subject_dn_DENIED",
                        "issuer_dn_DENIED",
                        CERTIFICATE_CHAIN,
                        Status.DENIED,
                        LocalDateTime.now(clock).minusMinutes(1),
                        LocalDateTime.now(clock).minusMinutes(1),
                        null
                )
        );

        DistinguishedNameIdListDTO distinguishedNameIdListDTO = new DistinguishedNameIdListDTO(Set.of(dnId1, dnId2, dnId3, dnId4, dnId5));
        HttpEntity<DistinguishedNameIdListDTO> request = new HttpEntity<>(distinguishedNameIdListDTO, getHttpHeaders(clientToken));
        ResponseEntity<Set<DistinguishedNameDTO>> response = testRestTemplate.exchange("/internal/clients/{clientId}/client-mtls-dns/apply", HttpMethod.POST, request, new ParameterizedTypeReference<>() {
        }, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsExactlyInAnyOrder(
                new DistinguishedNameDTO(
                        dnId1,
                        clientId,
                        "subject_dn_PENDING_ADDITION",
                        "issuer_dn_PENDING_ADDITION",
                        CERTIFICATE_CHAIN,
                        Status.ADDED,
                        LocalDateTime.now(clock).minusMinutes(1),
                        LocalDateTime.now(clock),
                        null
                ),
                new DistinguishedNameDTO(
                        dnId2,
                        clientId,
                        "subject_dn_ADDED",
                        "issuer_dn_ADDED",
                        CERTIFICATE_CHAIN,
                        Status.ADDED,
                        LocalDateTime.now(clock).minusMinutes(1),
                        LocalDateTime.now(clock).minusMinutes(1),
                        null
                ),
                new DistinguishedNameDTO(
                        dnId3,
                        clientId,
                        "subject_dn_PENDING_REMOVAL",
                        "issuer_dn_PENDING_REMOVAL",
                        CERTIFICATE_CHAIN,
                        Status.REMOVED,
                        LocalDateTime.now(clock).minusMinutes(1),
                        LocalDateTime.now(clock),
                        null
                ),
                new DistinguishedNameDTO(
                        dnId4,
                        clientId,
                        "subject_dn_REMOVED",
                        "issuer_dn_REMOVED",
                        CERTIFICATE_CHAIN,
                        Status.REMOVED,
                        LocalDateTime.now(clock).minusMinutes(1),
                        LocalDateTime.now(clock).minusMinutes(1),
                        null
                ),
                new DistinguishedNameDTO(
                        dnId5,
                        clientId,
                        "subject_dn_DENIED",
                        "issuer_dn_DENIED",
                        CERTIFICATE_CHAIN,
                        Status.DENIED,
                        LocalDateTime.now(clock).minusMinutes(1),
                        LocalDateTime.now(clock).minusMinutes(1),
                        null
                )
        );
        assertThat(clientMTLSCertificateDNRepository.findAllByClientId(clientId)).usingFieldByFieldElementComparator().containsExactlyInAnyOrder(
                new ClientMTLSCertificateDN(
                        dnId1,
                        clientId,
                        "subject_dn_PENDING_ADDITION",
                        "issuer_dn_PENDING_ADDITION",
                        CERTIFICATE_CHAIN,
                        Status.ADDED,
                        LocalDateTime.now(clock).minusMinutes(1),
                        LocalDateTime.now(clock),
                        null
                ),
                new ClientMTLSCertificateDN(
                        dnId2,
                        clientId,
                        "subject_dn_ADDED",
                        "issuer_dn_ADDED",
                        CERTIFICATE_CHAIN,
                        Status.ADDED,
                        LocalDateTime.now(clock).minusMinutes(1),
                        LocalDateTime.now(clock).minusMinutes(1),
                        null
                ),
                new ClientMTLSCertificateDN(
                        dnId3,
                        clientId,
                        "subject_dn_PENDING_REMOVAL",
                        "issuer_dn_PENDING_REMOVAL",
                        CERTIFICATE_CHAIN,
                        Status.REMOVED,
                        LocalDateTime.now(clock).minusMinutes(1),
                        LocalDateTime.now(clock),
                        null
                ),
                new ClientMTLSCertificateDN(
                        dnId4,
                        clientId,
                        "subject_dn_REMOVED",
                        "issuer_dn_REMOVED",
                        CERTIFICATE_CHAIN,
                        Status.REMOVED,
                        LocalDateTime.now(clock).minusMinutes(1),
                        LocalDateTime.now(clock).minusMinutes(1),
                        null
                ),
                new ClientMTLSCertificateDN(
                        dnId5,
                        clientId,
                        "subject_dn_DENIED",
                        "issuer_dn_DENIED",
                        CERTIFICATE_CHAIN,
                        Status.DENIED,
                        LocalDateTime.now(clock).minusMinutes(1),
                        LocalDateTime.now(clock).minusMinutes(1),
                        null
                )
        );


    }

    @Test
    void markApplied_asDevPortalUser_shouldReturnError() {
        clientToken = testClientTokens.createClientToken(clientGroupId, clientId,
                claims -> claims.setClaim(ClientTokenConstants.EXTRA_CLAIM_ISSUED_FOR, DEV_PORTAL));
        clientGroupRepository.save(clientGroup);

        UUID dnId1 = UUID.randomUUID();

        clientMTLSCertificateDNRepository.save(
                new ClientMTLSCertificateDN(
                        dnId1,
                        clientId,
                        "subject_dn_PENDING_ADDITION",
                        "issuer_dn_PENDING_ADDITION",
                        CERTIFICATE_CHAIN,
                        Status.PENDING_ADDITION,
                        LocalDateTime.now(clock).minusMinutes(1),
                        LocalDateTime.now(clock).minusMinutes(1),
                        "jira-001"
                )
        );

        DistinguishedNameIdListDTO distinguishedNameIdListDTO = new DistinguishedNameIdListDTO(Set.of(dnId1));
        HttpEntity<DistinguishedNameIdListDTO> request = new HttpEntity<>(distinguishedNameIdListDTO, getHttpHeaders(clientToken));
        ResponseEntity<ErrorDTO> response = testRestTemplate.exchange("/internal/clients/{clientId}/client-mtls-dns/apply", HttpMethod.POST, request, new ParameterizedTypeReference<>() {
        }, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isEqualTo(
                new ErrorDTO(
                        "CLS9002",
                        "Token requester for client-token is unauthorized."
                )
        );
        assertThat(clientMTLSCertificateDNRepository.findAllByClientId(clientId)).usingFieldByFieldElementComparator().containsExactlyInAnyOrder(
                new ClientMTLSCertificateDN(
                        dnId1,
                        clientId,
                        "subject_dn_PENDING_ADDITION",
                        "issuer_dn_PENDING_ADDITION",
                        CERTIFICATE_CHAIN,
                        Status.PENDING_ADDITION,
                        LocalDateTime.now(clock).minusMinutes(1),
                        LocalDateTime.now(clock).minusMinutes(1),
                        "jira-001"
                )
        );
    }

    @Test
    void markApplied_withNullItems_shouldReturnError() {
        clientGroupRepository.save(clientGroup);

        UUID dnId1 = UUID.randomUUID();

        clientMTLSCertificateDNRepository.save(
                new ClientMTLSCertificateDN(
                        dnId1,
                        clientId,
                        "subject_dn_PENDING_ADDITION",
                        "issuer_dn_PENDING_ADDITION",
                        CERTIFICATE_CHAIN,
                        Status.PENDING_ADDITION,
                        LocalDateTime.now(clock).minusMinutes(1),
                        LocalDateTime.now(clock).minusMinutes(1),
                        "jira-001"
                )
        );

        DistinguishedNameIdListDTO distinguishedNameIdListDTO = new DistinguishedNameIdListDTO(null);
        HttpEntity<DistinguishedNameIdListDTO> request = new HttpEntity<>(distinguishedNameIdListDTO, getHttpHeaders(clientToken));
        ResponseEntity<ErrorDTO> response = testRestTemplate.exchange("/internal/clients/{clientId}/client-mtls-dns/apply", HttpMethod.POST, request, new ParameterizedTypeReference<>() {
        }, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isEqualTo(
                new ErrorDTO(
                        "CLS1008",
                        "Method argument not valid (request body validation error). Offending field: ids"
                )
        );
        assertThat(clientMTLSCertificateDNRepository.findAllByClientId(clientId)).usingFieldByFieldElementComparator().containsExactlyInAnyOrder(
                new ClientMTLSCertificateDN(
                        dnId1,
                        clientId,
                        "subject_dn_PENDING_ADDITION",
                        "issuer_dn_PENDING_ADDITION",
                        CERTIFICATE_CHAIN,
                        Status.PENDING_ADDITION,
                        LocalDateTime.now(clock).minusMinutes(1),
                        LocalDateTime.now(clock).minusMinutes(1),
                        "jira-001"
                )
        );
    }

    @Test
    void markApplied_withEmptyItems_shouldReturnError() {
        clientGroupRepository.save(clientGroup);

        UUID dnId1 = UUID.randomUUID();

        clientMTLSCertificateDNRepository.save(
                new ClientMTLSCertificateDN(
                        dnId1,
                        clientId,
                        "subject_dn_PENDING_ADDITION",
                        "issuer_dn_PENDING_ADDITION",
                        CERTIFICATE_CHAIN,
                        Status.PENDING_ADDITION,
                        LocalDateTime.now(clock).minusMinutes(1),
                        LocalDateTime.now(clock).minusMinutes(1),
                        "jira-001"
                )
        );

        DistinguishedNameIdListDTO distinguishedNameIdListDTO = new DistinguishedNameIdListDTO(Set.of());
        HttpEntity<DistinguishedNameIdListDTO> request = new HttpEntity<>(distinguishedNameIdListDTO, getHttpHeaders(clientToken));
        ResponseEntity<ErrorDTO> response = testRestTemplate.exchange("/internal/clients/{clientId}/client-mtls-dns/apply", HttpMethod.POST, request, new ParameterizedTypeReference<>() {
        }, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isEqualTo(
                new ErrorDTO(
                        "CLS1008",
                        "Method argument not valid (request body validation error). Offending field: ids"
                )
        );
        assertThat(clientMTLSCertificateDNRepository.findAllByClientId(clientId)).usingFieldByFieldElementComparator().containsExactlyInAnyOrder(
                new ClientMTLSCertificateDN(
                        dnId1,
                        clientId,
                        "subject_dn_PENDING_ADDITION",
                        "issuer_dn_PENDING_ADDITION",
                        CERTIFICATE_CHAIN,
                        Status.PENDING_ADDITION,
                        LocalDateTime.now(clock).minusMinutes(1),
                        LocalDateTime.now(clock).minusMinutes(1),
                        "jira-001"
                )
        );
    }

    @Test
    void markApplied_withOneNullItem_shouldReturnError() {
        clientGroupRepository.save(clientGroup);

        UUID dnId1 = UUID.randomUUID();

        clientMTLSCertificateDNRepository.save(
                new ClientMTLSCertificateDN(
                        dnId1,
                        clientId,
                        "subject_dn_PENDING_ADDITION",
                        "issuer_dn_PENDING_ADDITION",
                        CERTIFICATE_CHAIN,
                        Status.PENDING_ADDITION,
                        LocalDateTime.now(clock).minusMinutes(1),
                        LocalDateTime.now(clock).minusMinutes(1),
                        "jira-001"
                )
        );

        final Set<UUID> items = new HashSet<>();
        items.add(dnId1);
        items.add(null);
        DistinguishedNameIdListDTO distinguishedNameIdListDTO = new DistinguishedNameIdListDTO(items);
        HttpEntity<DistinguishedNameIdListDTO> request = new HttpEntity<>(distinguishedNameIdListDTO, getHttpHeaders(clientToken));
        ResponseEntity<ErrorDTO> response = testRestTemplate.exchange("/internal/clients/{clientId}/client-mtls-dns/apply", HttpMethod.POST, request, new ParameterizedTypeReference<>() {
        }, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isEqualTo(
                new ErrorDTO(
                        "CLS1008",
                        "Method argument not valid (request body validation error). Offending field: ids[]"
                )
        );
        assertThat(clientMTLSCertificateDNRepository.findAllByClientId(clientId)).usingFieldByFieldElementComparator().containsExactlyInAnyOrder(
                new ClientMTLSCertificateDN(
                        dnId1,
                        clientId,
                        "subject_dn_PENDING_ADDITION",
                        "issuer_dn_PENDING_ADDITION",
                        CERTIFICATE_CHAIN,
                        Status.PENDING_ADDITION,
                        LocalDateTime.now(clock).minusMinutes(1),
                        LocalDateTime.now(clock).minusMinutes(1),
                        "jira-001"
                )
        );
    }

    @Test
    void markDenied_asAPYUserWithItemsInAllDifferentStates_shouldReturnOk() {
        clientGroupRepository.save(clientGroup);

        UUID dnId1 = UUID.randomUUID();
        UUID dnId2 = UUID.randomUUID();
        UUID dnId3 = UUID.randomUUID();
        UUID dnId4 = UUID.randomUUID();
        UUID dnId5 = UUID.randomUUID();

        clientMTLSCertificateDNRepository.save(
                new ClientMTLSCertificateDN(
                        dnId1,
                        clientId,
                        "subject_dn_PENDING_ADDITION",
                        "issuer_dn_PENDING_ADDITION",
                        CERTIFICATE_CHAIN,
                        Status.PENDING_ADDITION,
                        LocalDateTime.now(clock).minusMinutes(1),
                        LocalDateTime.now(clock).minusMinutes(1),
                        "jira-001"
                )
        );
        clientMTLSCertificateDNRepository.save(
                new ClientMTLSCertificateDN(
                        dnId2,
                        clientId,
                        "subject_dn_ADDED",
                        "issuer_dn_ADDED",
                        CERTIFICATE_CHAIN,
                        Status.ADDED,
                        LocalDateTime.now(clock).minusMinutes(1),
                        LocalDateTime.now(clock).minusMinutes(1),
                        null
                )
        );
        clientMTLSCertificateDNRepository.save(
                new ClientMTLSCertificateDN(
                        dnId3,
                        clientId,
                        "subject_dn_PENDING_REMOVAL",
                        "issuer_dn_PENDING_REMOVAL",
                        CERTIFICATE_CHAIN,
                        Status.PENDING_REMOVAL,
                        LocalDateTime.now(clock).minusMinutes(1),
                        LocalDateTime.now(clock).minusMinutes(1),
                        "jira-003"
                )
        );
        clientMTLSCertificateDNRepository.save(
                new ClientMTLSCertificateDN(
                        dnId4,
                        clientId,
                        "subject_dn_REMOVED",
                        "issuer_dn_REMOVED",
                        CERTIFICATE_CHAIN,
                        Status.REMOVED,
                        LocalDateTime.now(clock).minusMinutes(1),
                        LocalDateTime.now(clock).minusMinutes(1),
                        null
                )
        );
        clientMTLSCertificateDNRepository.save(
                new ClientMTLSCertificateDN(
                        dnId5,
                        clientId,
                        "subject_dn_DENIED",
                        "issuer_dn_DENIED",
                        CERTIFICATE_CHAIN,
                        Status.DENIED,
                        LocalDateTime.now(clock).minusMinutes(1),
                        LocalDateTime.now(clock).minusMinutes(1),
                        null
                )
        );

        DistinguishedNameIdListDTO distinguishedNameIdListDTO = new DistinguishedNameIdListDTO(Set.of(dnId1, dnId2, dnId3, dnId4, dnId5));
        HttpEntity<DistinguishedNameIdListDTO> request = new HttpEntity<>(distinguishedNameIdListDTO, getHttpHeaders(clientToken));
        ResponseEntity<Set<DistinguishedNameDTO>> response = testRestTemplate.exchange("/internal/clients/{clientId}/client-mtls-dns/deny", HttpMethod.POST, request, new ParameterizedTypeReference<>() {
        }, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsExactlyInAnyOrder(
                new DistinguishedNameDTO(
                        dnId1,
                        clientId,
                        "subject_dn_PENDING_ADDITION",
                        "issuer_dn_PENDING_ADDITION",
                        CERTIFICATE_CHAIN,
                        Status.DENIED,
                        LocalDateTime.now(clock).minusMinutes(1),
                        LocalDateTime.now(clock),
                        null
                ),
                new DistinguishedNameDTO(
                        dnId2,
                        clientId,
                        "subject_dn_ADDED",
                        "issuer_dn_ADDED",
                        CERTIFICATE_CHAIN,
                        Status.ADDED,
                        LocalDateTime.now(clock).minusMinutes(1),
                        LocalDateTime.now(clock).minusMinutes(1),
                        null
                ),
                new DistinguishedNameDTO(
                        dnId3,
                        clientId,
                        "subject_dn_PENDING_REMOVAL",
                        "issuer_dn_PENDING_REMOVAL",
                        CERTIFICATE_CHAIN,
                        Status.PENDING_REMOVAL,
                        LocalDateTime.now(clock).minusMinutes(1),
                        LocalDateTime.now(clock).minusMinutes(1),
                        "jira-003"
                ),
                new DistinguishedNameDTO(
                        dnId4,
                        clientId,
                        "subject_dn_REMOVED",
                        "issuer_dn_REMOVED",
                        CERTIFICATE_CHAIN,
                        Status.REMOVED,
                        LocalDateTime.now(clock).minusMinutes(1),
                        LocalDateTime.now(clock).minusMinutes(1),
                        null
                ),
                new DistinguishedNameDTO(
                        dnId5,
                        clientId,
                        "subject_dn_DENIED",
                        "issuer_dn_DENIED",
                        CERTIFICATE_CHAIN,
                        Status.DENIED,
                        LocalDateTime.now(clock).minusMinutes(1),
                        LocalDateTime.now(clock).minusMinutes(1),
                        null
                )
        );
        assertThat(clientMTLSCertificateDNRepository.findAllByClientId(clientId)).usingFieldByFieldElementComparator().containsExactlyInAnyOrder(
                new ClientMTLSCertificateDN(
                        dnId1,
                        clientId,
                        "subject_dn_PENDING_ADDITION",
                        "issuer_dn_PENDING_ADDITION",
                        CERTIFICATE_CHAIN,
                        Status.DENIED,
                        LocalDateTime.now(clock).minusMinutes(1),
                        LocalDateTime.now(clock),
                        null
                ),
                new ClientMTLSCertificateDN(
                        dnId2,
                        clientId,
                        "subject_dn_ADDED",
                        "issuer_dn_ADDED",
                        CERTIFICATE_CHAIN,
                        Status.ADDED,
                        LocalDateTime.now(clock).minusMinutes(1),
                        LocalDateTime.now(clock).minusMinutes(1),
                        null
                ),
                new ClientMTLSCertificateDN(
                        dnId3,
                        clientId,
                        "subject_dn_PENDING_REMOVAL",
                        "issuer_dn_PENDING_REMOVAL",
                        CERTIFICATE_CHAIN,
                        Status.PENDING_REMOVAL,
                        LocalDateTime.now(clock).minusMinutes(1),
                        LocalDateTime.now(clock).minusMinutes(1),
                        "jira-003"
                ),
                new ClientMTLSCertificateDN(
                        dnId4,
                        clientId,
                        "subject_dn_REMOVED",
                        "issuer_dn_REMOVED",
                        CERTIFICATE_CHAIN,
                        Status.REMOVED,
                        LocalDateTime.now(clock).minusMinutes(1),
                        LocalDateTime.now(clock).minusMinutes(1),
                        null
                ),
                new ClientMTLSCertificateDN(
                        dnId5,
                        clientId,
                        "subject_dn_DENIED",
                        "issuer_dn_DENIED",
                        CERTIFICATE_CHAIN,
                        Status.DENIED,
                        LocalDateTime.now(clock).minusMinutes(1),
                        LocalDateTime.now(clock).minusMinutes(1),
                        null
                )
        );


    }

    @Test
    void markDenied_asDevPortalUser_shouldReturnError() {
        clientToken = testClientTokens.createClientToken(clientGroupId, clientId,
                claims -> claims.setClaim(ClientTokenConstants.EXTRA_CLAIM_ISSUED_FOR, DEV_PORTAL));
        clientGroupRepository.save(clientGroup);

        UUID dnId1 = UUID.randomUUID();

        clientMTLSCertificateDNRepository.save(
                new ClientMTLSCertificateDN(
                        dnId1,
                        clientId,
                        "subject_dn_PENDING_ADDITION",
                        "issuer_dn_PENDING_ADDITION",
                        CERTIFICATE_CHAIN,
                        Status.PENDING_ADDITION,
                        LocalDateTime.now(clock).minusMinutes(1),
                        LocalDateTime.now(clock).minusMinutes(1),
                        "jira-001"
                )
        );

        DistinguishedNameIdListDTO distinguishedNameIdListDTO = new DistinguishedNameIdListDTO(Set.of(dnId1));
        HttpEntity<DistinguishedNameIdListDTO> request = new HttpEntity<>(distinguishedNameIdListDTO, getHttpHeaders(clientToken));
        ResponseEntity<ErrorDTO> response = testRestTemplate.exchange("/internal/clients/{clientId}/client-mtls-dns/deny", HttpMethod.POST, request, new ParameterizedTypeReference<>() {
        }, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isEqualTo(
                new ErrorDTO(
                        "CLS9002",
                        "Token requester for client-token is unauthorized."
                )
        );
        assertThat(clientMTLSCertificateDNRepository.findAllByClientId(clientId)).usingFieldByFieldElementComparator().containsExactlyInAnyOrder(
                new ClientMTLSCertificateDN(
                        dnId1,
                        clientId,
                        "subject_dn_PENDING_ADDITION",
                        "issuer_dn_PENDING_ADDITION",
                        CERTIFICATE_CHAIN,
                        Status.PENDING_ADDITION,
                        LocalDateTime.now(clock).minusMinutes(1),
                        LocalDateTime.now(clock).minusMinutes(1),
                        "jira-001"
                )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {ASSISTANCE_PORTAL_YTS, DEV_PORTAL})
    void list_shouldReturnOk(String tokenIssuedFor) {
        clientToken = testClientTokens.createClientToken(clientGroupId, clientId,
                claims -> claims.setClaim(ClientTokenConstants.EXTRA_CLAIM_ISSUED_FOR, ASSISTANCE_PORTAL_YTS));
        clientGroupRepository.save(clientGroup);

        UUID dnId1 = UUID.randomUUID();
        UUID dnId2 = UUID.randomUUID();
        UUID dnId3 = UUID.randomUUID();
        UUID dnId4 = UUID.randomUUID();
        UUID dnId5 = UUID.randomUUID();

        clientMTLSCertificateDNRepository.save(
                new ClientMTLSCertificateDN(
                        dnId1,
                        clientId,
                        "subject_dn_PENDING_ADDITION",
                        "issuer_dn_PENDING_ADDITION",
                        CERTIFICATE_CHAIN,
                        Status.PENDING_ADDITION,
                        LocalDateTime.now(clock).minusMinutes(1),
                        LocalDateTime.now(clock).minusMinutes(1),
                        "jira-001"
                )
        );
        clientMTLSCertificateDNRepository.save(
                new ClientMTLSCertificateDN(
                        dnId2,
                        clientId,
                        "subject_dn_ADDED",
                        "issuer_dn_ADDED",
                        CERTIFICATE_CHAIN,
                        Status.ADDED,
                        LocalDateTime.now(clock).minusMinutes(1),
                        LocalDateTime.now(clock).minusMinutes(1),
                        null
                )
        );
        clientMTLSCertificateDNRepository.save(
                new ClientMTLSCertificateDN(
                        dnId3,
                        clientId,
                        "subject_dn_PENDING_REMOVAL",
                        "issuer_dn_PENDING_REMOVAL",
                        CERTIFICATE_CHAIN,
                        Status.PENDING_REMOVAL,
                        LocalDateTime.now(clock).minusMinutes(1),
                        LocalDateTime.now(clock).minusMinutes(1),
                        "jira-003"
                )
        );
        clientMTLSCertificateDNRepository.save(
                new ClientMTLSCertificateDN(
                        dnId4,
                        clientId,
                        "subject_dn_REMOVED",
                        "issuer_dn_REMOVED",
                        CERTIFICATE_CHAIN,
                        Status.REMOVED,
                        LocalDateTime.now(clock).minusMinutes(1),
                        LocalDateTime.now(clock).minusMinutes(1),
                        null
                )
        );
        clientMTLSCertificateDNRepository.save(
                new ClientMTLSCertificateDN(
                        dnId5,
                        clientId,
                        "subject_dn_DENIED",
                        "issuer_dn_DENIED",
                        CERTIFICATE_CHAIN,
                        Status.DENIED,
                        LocalDateTime.now(clock).minusMinutes(1),
                        LocalDateTime.now(clock).minusMinutes(1),
                        null
                )
        );

        HttpEntity<DistinguishedNameIdListDTO> request = new HttpEntity<>(getHttpHeaders(clientToken));
        ResponseEntity<Set<DistinguishedNameDTO>> response = testRestTemplate.exchange("/internal/clients/{clientId}/client-mtls-dns", HttpMethod.GET, request, new ParameterizedTypeReference<>() {
        }, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsExactlyInAnyOrder(
                new DistinguishedNameDTO(
                        dnId1,
                        clientId,
                        "subject_dn_PENDING_ADDITION",
                        "issuer_dn_PENDING_ADDITION",
                        CERTIFICATE_CHAIN,
                        Status.PENDING_ADDITION,
                        LocalDateTime.now(clock).minusMinutes(1),
                        LocalDateTime.now(clock).minusMinutes(1),
                        "jira-001"
                ),
                new DistinguishedNameDTO(
                        dnId2,
                        clientId,
                        "subject_dn_ADDED",
                        "issuer_dn_ADDED",
                        CERTIFICATE_CHAIN,
                        Status.ADDED,
                        LocalDateTime.now(clock).minusMinutes(1),
                        LocalDateTime.now(clock).minusMinutes(1),
                        null
                ),
                new DistinguishedNameDTO(
                        dnId3,
                        clientId,
                        "subject_dn_PENDING_REMOVAL",
                        "issuer_dn_PENDING_REMOVAL",
                        CERTIFICATE_CHAIN,
                        Status.PENDING_REMOVAL,
                        LocalDateTime.now(clock).minusMinutes(1),
                        LocalDateTime.now(clock).minusMinutes(1),
                        "jira-003"
                ),
                new DistinguishedNameDTO(
                        dnId4,
                        clientId,
                        "subject_dn_REMOVED",
                        "issuer_dn_REMOVED",
                        CERTIFICATE_CHAIN,
                        Status.REMOVED,
                        LocalDateTime.now(clock).minusMinutes(1),
                        LocalDateTime.now(clock).minusMinutes(1),
                        null
                ),
                new DistinguishedNameDTO(
                        dnId5,
                        clientId,
                        "subject_dn_DENIED",
                        "issuer_dn_DENIED",
                        CERTIFICATE_CHAIN,
                        Status.DENIED,
                        LocalDateTime.now(clock).minusMinutes(1),
                        LocalDateTime.now(clock).minusMinutes(1),
                        null
                )
        );
    }

    @Test
    void list_withOtherUserType_shouldReturnError() {
        clientToken = testClientTokens.createClientToken(clientGroupId, clientId,
                claims -> claims.setClaim(ClientTokenConstants.EXTRA_CLAIM_ISSUED_FOR, "other"));
        clientGroupRepository.save(clientGroup);

        UUID dnId1 = UUID.randomUUID();

        clientMTLSCertificateDNRepository.save(
                new ClientMTLSCertificateDN(
                        dnId1,
                        clientId,
                        "subject_dn_PENDING_ADDITION",
                        "issuer_dn_PENDING_ADDITION",
                        CERTIFICATE_CHAIN,
                        Status.PENDING_ADDITION,
                        LocalDateTime.now(clock).minusMinutes(1),
                        LocalDateTime.now(clock).minusMinutes(1),
                        "jira-001"
                )
        );

        HttpEntity<DistinguishedNameIdListDTO> request = new HttpEntity<>(getHttpHeaders(clientToken));
        ResponseEntity<ErrorDTO> response = testRestTemplate.exchange("/internal/clients/{clientId}/client-mtls-dns", HttpMethod.GET, request, new ParameterizedTypeReference<>() {
        }, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isEqualTo(
                new ErrorDTO(
                        "CLS9002",
                        "Token requester for client-token is unauthorized."
                )
        );
    }

    private HttpHeaders getHttpHeaders(ClientToken clientToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME, clientToken.getSerialized());
        return headers;
    }
}

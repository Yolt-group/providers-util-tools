package com.yolt.clients.client.mtlscertificates;

import com.yolt.clients.IntegrationTest;
import com.yolt.clients.clientgroup.ClientGroupRepository;
import com.yolt.clients.model.Client;
import com.yolt.clients.model.ClientGroup;
import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.clienttokens.constants.ClientTokenConstants;
import nl.ing.lovebird.clienttokens.test.TestClientTokens;
import nl.ing.lovebird.errorhandling.ErrorDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static com.yolt.clients.TestConfiguration.FIXED_CLOCK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@IntegrationTest
class ClientMTLSCertificatesControllerIT {
    private static final String ASSISTANCE_PORTAL_YTS = "assistance-portal-yts";
    private static final String DEV_PORTAL = "dev-portal";

    private static final LocalDateTime NOW = LocalDateTime.now(FIXED_CLOCK);

    @Autowired
    private TestRestTemplate testRestTemplate;
    @Autowired
    private TestClientTokens testClientTokens;
    @Autowired
    private ClientGroupRepository clientGroupRepository;
    @Autowired
    private ClientMTLSCertificateRepository clientMTLSCertificateRepository;

    private UUID clientId;
    private UUID clientGroupId;
    private ClientGroup clientGroup;
    private ClientToken clientToken;


    @BeforeEach
    void setup() {
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
                true,
                true,
                true,
                1L,
                Collections.emptySet()
        );
        clientGroup.getClients().add(client);

        clientToken = testClientTokens.createClientToken(clientGroupId, clientId,
                claims -> claims.setClaim(ClientTokenConstants.EXTRA_CLAIM_ISSUED_FOR, ASSISTANCE_PORTAL_YTS));
    }

    @ParameterizedTest
    @ValueSource(strings = {ASSISTANCE_PORTAL_YTS, DEV_PORTAL})
    void list(String issuedForClaim) {
        clientToken = testClientTokens.createClientToken(clientGroupId, clientId, claims -> {
            claims.setClaim(ClientTokenConstants.EXTRA_CLAIM_ISSUED_FOR, issuedForClaim);
        });

        clientGroupRepository.save(clientGroup);


        var cert0 = new ClientMTLSCertificate(clientId, "fp0", BigInteger.ONE, "subject", "issuer", NOW.minusDays(50), NOW.plusDays(50), NOW.minusDays(40), NOW.minusDays(38), "cert0", NOW.minusDays(38));
        clientMTLSCertificateRepository.save(cert0);

        var cert31 = new ClientMTLSCertificate(clientId, "fp31", BigInteger.ONE, "subject", "issuer", NOW, NOW.plusDays(100), null, null, "cert31", NOW);
        clientMTLSCertificateRepository.save(cert31);
        var cert32 = new ClientMTLSCertificate(clientId, "fp32", BigInteger.ONE, "subject", "issuer", NOW.minusDays(50), NOW.plusDays(50), null, null, "cert31", NOW.minusDays(50));
        clientMTLSCertificateRepository.save(cert32);

        List<ClientMTLSCertificate> expectedCertsP1 = new ArrayList<>(20);
        List<ClientMTLSCertificate> expectedCertsP2 = new ArrayList<>(20);

        expectedCertsP1.add(cert31);

        IntStream.rangeClosed(1, 30).forEach(i -> {
            ClientMTLSCertificate cert = new ClientMTLSCertificate(clientId, "fp" + i, BigInteger.ONE, "subject", "issuer", NOW.minusDays(50), NOW.plusDays(50), NOW.minusDays(40), NOW.minusDays(i), "cert" + i, NOW.minusDays(i));
            clientMTLSCertificateRepository.save(cert);
            if (i <= 19) {
                expectedCertsP1.add(cert);
            } else {
                expectedCertsP2.add(cert);
            }
        });
        expectedCertsP2.add(cert0);
        expectedCertsP2.add(cert32);

        HttpEntity<Void> request = new HttpEntity<>(null, getHttpHeaders(clientToken));
        ResponseEntity<List<ClientMTLSCertificate>> response = testRestTemplate.exchange("/internal/clients/{clientId}/client-mtls-certificates", HttpMethod.GET, request, new ParameterizedTypeReference<>() {
        }, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders()).containsEntry("X-Total-Count", List.of("33"));
        assertThat(response.getHeaders()).containsEntry("X-Pagination-Page", List.of("0"));
        assertThat(response.getHeaders()).containsEntry("X-Pagination-Pages", List.of("2"));
        assertThat(response.getHeaders()).containsEntry("X-Pagination-PageSize", List.of("20"));
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsExactlyElementsOf(expectedCertsP1);

        ResponseEntity<List<ClientMTLSCertificate>> responseP2 = testRestTemplate.exchange("/internal/clients/{clientId}/client-mtls-certificates?page=1", HttpMethod.GET, request, new ParameterizedTypeReference<>() {
        }, clientId);

        assertThat(responseP2.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseP2.getHeaders()).containsEntry("X-Total-Count", List.of("33"));
        assertThat(responseP2.getHeaders()).containsEntry("X-Pagination-Page", List.of("1"));
        assertThat(responseP2.getHeaders()).containsEntry("X-Pagination-Pages", List.of("2"));
        assertThat(responseP2.getHeaders()).containsEntry("X-Pagination-PageSize", List.of("20"));
        assertThat(responseP2.getBody()).isNotNull();
        assertThat(responseP2.getBody()).containsExactlyElementsOf(expectedCertsP2);

        ResponseEntity<List<ClientMTLSCertificate>> responseP3 = testRestTemplate.exchange("/internal/clients/{clientId}/client-mtls-certificates?page=2", HttpMethod.GET, request, new ParameterizedTypeReference<>() {
        }, clientId);

        assertThat(responseP3.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseP3.getHeaders()).containsEntry("X-Total-Count", List.of("33"));
        assertThat(responseP3.getHeaders()).containsEntry("X-Pagination-Page", List.of("2"));
        assertThat(responseP3.getHeaders()).containsEntry("X-Pagination-Pages", List.of("2"));
        assertThat(responseP3.getHeaders()).containsEntry("X-Pagination-PageSize", List.of("20"));
        assertThat(responseP3.getBody()).isNotNull();
        assertThat(responseP3.getBody()).isEmpty();
    }

    @Test
    void list_wrong_client_id() {
        clientGroupRepository.save(clientGroup);


        var cert0 = new ClientMTLSCertificate(clientId, "fp0", BigInteger.ONE, "subject", "issuer", NOW.minusDays(50), NOW.plusDays(50), NOW.minusDays(40), NOW.minusDays(38), "cert0", NOW.minusDays(38));
        clientMTLSCertificateRepository.save(cert0);

        HttpEntity<Void> request = new HttpEntity<>(null, getHttpHeaders(clientToken));
        ResponseEntity<ErrorDTO> response = testRestTemplate.exchange("/internal/clients/{clientId}/client-mtls-certificates", HttpMethod.GET, request, new ParameterizedTypeReference<>() {
        }, UUID.randomUUID());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isEqualTo(new ErrorDTO("CLS002", "The client id and/or client group id validation failed."));
    }

    @Test
    void list_wrong_issued_for_claim() {
        clientToken = testClientTokens.createClientToken(clientGroupId, clientId,
                claims -> claims.setClaim(ClientTokenConstants.EXTRA_CLAIM_ISSUED_FOR, "other"));
        clientGroupRepository.save(clientGroup);


        var cert0 = new ClientMTLSCertificate(clientId, "fp0", BigInteger.ONE, "subject", "issuer", NOW.minusDays(50), NOW.plusDays(50), NOW.minusDays(40), NOW.minusDays(38), "cert0", NOW.minusDays(38));
        clientMTLSCertificateRepository.save(cert0);

        HttpEntity<Void> request = new HttpEntity<>(null, getHttpHeaders(clientToken));
        ResponseEntity<ErrorDTO> response = testRestTemplate.exchange("/internal/clients/{clientId}/client-mtls-certificates", HttpMethod.GET, request, new ParameterizedTypeReference<>() {
        }, UUID.randomUUID());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isEqualTo(new ErrorDTO("CLS9002", "Token requester for client-token is unauthorized."));
    }

    @Test
    void addCertificate() {
        clientGroupRepository.save(clientGroup);

        var certificate = """
                -----BEGIN CERTIFICATE-----
                MIIEczCCA1ugAwIBAgIUe9Rb3xd8/+68OrC87iLMDwOvNLIwDQYJKoZIhvcNAQEL
                BQAwJTEjMCEGA1UEAxMaY2xpZW50LXByb3h5LnRlYW01LnlvbHQuaW8wHhcNMjEw
                ODA1MTQyMzIzWhcNMjIwODA1MTQyMzUzWjBuMRQwEgYDVQQKEwtZVFMgQ2xpZW50
                czEUMBIGA1UECxMLWVRTIENsaWVudHMxHDAaBgNVBAMME3l0cy1jbGllbnRzQHlv
                bHQuZXUxIjAgBgkqhkiG9w0BCQEME3l0cy1jbGllbnRzQHlvbHQuZXUwggIiMA0G
                CSqGSIb3DQEBAQUAA4ICDwAwggIKAoICAQCs9kI/w+/VlO51+NnuZuHmCos/PD0e
                /Lp9rfysqgECOogwZ2zsbl0fpOmppKVrmQfJyuowRXh5f8G37p2dVXY45LalerWE
                AAPuDW77Pz5u3sA2qSSq7zWy4KR1+xvErXQbkueYvb5KZxhKDkh+E3XryDYbwxWM
                DaBhMNoyNdDE8FwA5PhP+SMj+FvJRaY8oNfbBV9sj5x7QqLH6DyHMtV7i7kL40dE
                sX4qFG7mlGIFi8sG4+q8zJTtdJtecam9LebI22Vf0DA0PCpNdmnUb1jQHzY5b2Ua
                FqbUn5c63aJuPbwfrDvwOchNqTuQYSZPXHUA9VklkmMBwYdb4l39Cjlm7v6YipCx
                SNUZCeEj2l986u9kyZJLY+k6BNh5Py1qoD4kdjOBB3+E69T9KrWhjCHNJoLap6KV
                SHUD1W0oGDCObibcuESBb8juXSjAdqyasJnr1YE33GImAa46Oh1yLb0D6y/hj9T1
                tZscRW/eKDyl7p8c48R0DCHcYJZWch7slA/Jdga6/qFtt/NIppkW1mow6mWJ18in
                A+RR9dXQzaUWp5L0iybau0AbGD0IA1EvHg9CtwDdLzsyz12dQb/CCXh+C6fgPYrA
                mERdJPN/2uSXqO+3RY8htf8QkaqKtIbHHm0qQrZvEE+gG7yt0P2e6+bPABEqaQ6e
                3zY/sYZOsWhmNQIDAQABo1IwUDAOBgNVHQ8BAf8EBAMCA6gwHQYDVR0OBBYEFPHo
                ViuORw7eX/B/wHZXVG/mmiO1MB8GA1UdIwQYMBaAFPjwsFzyASjdoQXyU2lqyEDi
                Dg+2MA0GCSqGSIb3DQEBCwUAA4IBAQC4oZZtvaOx+EmdQF6/7M1FE6Nn80ZuBcy/
                IrmTRqZ1rm8cDyt7uV5sn+4yf/QDEf8UGxibdyHUWT6Tn2CHM/g9+FfeYBBWlb00
                yzYY8/bv+fYb5Qhe3HPqM/g65wy0nNtMUXSDV0yN+CiPwtjEJI/66uKvsyYepe3w
                7x0dUuvL8V123twOsQ6exg5Gnr4u0WCcnkRw1Dxews/z5L7gatUP7ZWR6PbofKS6
                ERbUITh+22JRXFeXqBaC1rur2+v32p+5EuT/DozX4PAXVnUfqPLONCoabj++MeLx
                PDhEYliyyLk/GCYLOm1MFj7HhchypFShTtvxJNwzXA0tn76TqJac
                -----END CERTIFICATE-----
                """;
        var fingerprint = "c7f082d6b120a676968b14481fad6d937945f3ea";
        var serial = new BigInteger("706941625925629646780921591789539939462392263858");
        var subjectDn = "O=YTS Clients,OU=YTS Clients,CN=yts-clients@yolt.eu,E=yts-clients@yolt.eu";
        var issuerDn = "CN=client-proxy.team5.yolt.io";
        var validStart = LocalDateTime.of(2021, 8, 5, 14, 23, 23);
        var validEnd = LocalDateTime.of(2022, 8, 5, 14, 23, 53);

        HttpEntity<NewClientMTLSCertificateDTO> request = new HttpEntity<>(new NewClientMTLSCertificateDTO(certificate), getHttpHeaders(clientToken));
        ResponseEntity<Void> response = testRestTemplate.exchange(
                "/internal/clients/{clientId}/client-mtls-certificates",
                HttpMethod.POST, request, new ParameterizedTypeReference<>() {
                },
                clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        await().untilAsserted(() -> assertThat(clientMTLSCertificateRepository.findByClientIdAndFingerprint(clientId, fingerprint))
                .contains(
                        new ClientMTLSCertificate(
                                clientId,
                                fingerprint,
                                serial,
                                subjectDn,
                                issuerDn,
                                validStart,
                                validEnd,
                                null,
                                null,
                                certificate,
                                validStart
                        )
                )
        );
    }

    @Test
    void addCertificate_certificate_known() {
        clientGroupRepository.save(clientGroup);

        var certificate = """
                -----BEGIN CERTIFICATE-----
                MIIEczCCA1ugAwIBAgIUe9Rb3xd8/+68OrC87iLMDwOvNLIwDQYJKoZIhvcNAQEL
                BQAwJTEjMCEGA1UEAxMaY2xpZW50LXByb3h5LnRlYW01LnlvbHQuaW8wHhcNMjEw
                ODA1MTQyMzIzWhcNMjIwODA1MTQyMzUzWjBuMRQwEgYDVQQKEwtZVFMgQ2xpZW50
                czEUMBIGA1UECxMLWVRTIENsaWVudHMxHDAaBgNVBAMME3l0cy1jbGllbnRzQHlv
                bHQuZXUxIjAgBgkqhkiG9w0BCQEME3l0cy1jbGllbnRzQHlvbHQuZXUwggIiMA0G
                CSqGSIb3DQEBAQUAA4ICDwAwggIKAoICAQCs9kI/w+/VlO51+NnuZuHmCos/PD0e
                /Lp9rfysqgECOogwZ2zsbl0fpOmppKVrmQfJyuowRXh5f8G37p2dVXY45LalerWE
                AAPuDW77Pz5u3sA2qSSq7zWy4KR1+xvErXQbkueYvb5KZxhKDkh+E3XryDYbwxWM
                DaBhMNoyNdDE8FwA5PhP+SMj+FvJRaY8oNfbBV9sj5x7QqLH6DyHMtV7i7kL40dE
                sX4qFG7mlGIFi8sG4+q8zJTtdJtecam9LebI22Vf0DA0PCpNdmnUb1jQHzY5b2Ua
                FqbUn5c63aJuPbwfrDvwOchNqTuQYSZPXHUA9VklkmMBwYdb4l39Cjlm7v6YipCx
                SNUZCeEj2l986u9kyZJLY+k6BNh5Py1qoD4kdjOBB3+E69T9KrWhjCHNJoLap6KV
                SHUD1W0oGDCObibcuESBb8juXSjAdqyasJnr1YE33GImAa46Oh1yLb0D6y/hj9T1
                tZscRW/eKDyl7p8c48R0DCHcYJZWch7slA/Jdga6/qFtt/NIppkW1mow6mWJ18in
                A+RR9dXQzaUWp5L0iybau0AbGD0IA1EvHg9CtwDdLzsyz12dQb/CCXh+C6fgPYrA
                mERdJPN/2uSXqO+3RY8htf8QkaqKtIbHHm0qQrZvEE+gG7yt0P2e6+bPABEqaQ6e
                3zY/sYZOsWhmNQIDAQABo1IwUDAOBgNVHQ8BAf8EBAMCA6gwHQYDVR0OBBYEFPHo
                ViuORw7eX/B/wHZXVG/mmiO1MB8GA1UdIwQYMBaAFPjwsFzyASjdoQXyU2lqyEDi
                Dg+2MA0GCSqGSIb3DQEBCwUAA4IBAQC4oZZtvaOx+EmdQF6/7M1FE6Nn80ZuBcy/
                IrmTRqZ1rm8cDyt7uV5sn+4yf/QDEf8UGxibdyHUWT6Tn2CHM/g9+FfeYBBWlb00
                yzYY8/bv+fYb5Qhe3HPqM/g65wy0nNtMUXSDV0yN+CiPwtjEJI/66uKvsyYepe3w
                7x0dUuvL8V123twOsQ6exg5Gnr4u0WCcnkRw1Dxews/z5L7gatUP7ZWR6PbofKS6
                ERbUITh+22JRXFeXqBaC1rur2+v32p+5EuT/DozX4PAXVnUfqPLONCoabj++MeLx
                PDhEYliyyLk/GCYLOm1MFj7HhchypFShTtvxJNwzXA0tn76TqJac
                -----END CERTIFICATE-----
                """;
        var fingerprint = "c7f082d6b120a676968b14481fad6d937945f3ea";
        var serial = new BigInteger("706941625925629646780921591789539939462392263858");
        var subjectDn = "O=YTS Clients,OU=YTS Clients,CN=yts-clients@yolt.eu,E=yts-clients@yolt.eu";
        var issuerDn = "CN=client-proxy.team5.yolt.io";
        var validStart = LocalDateTime.of(2021, 8, 5, 14, 23, 23);
        var validEnd = LocalDateTime.of(2022, 8, 5, 14, 23, 53);

        clientMTLSCertificateRepository.save(new ClientMTLSCertificate(
                clientId,
                fingerprint,
                serial,
                subjectDn,
                issuerDn,
                validStart,
                validEnd,
                null,
                null,
                certificate,
                validStart
        ));

        HttpEntity<NewClientMTLSCertificateDTO> request = new HttpEntity<>(new NewClientMTLSCertificateDTO(certificate), getHttpHeaders(clientToken));
        ResponseEntity<ErrorDTO> response = testRestTemplate.exchange(
                "/internal/clients/{clientId}/client-mtls-certificates",
                HttpMethod.POST, request, new ParameterizedTypeReference<>() {
                },
                clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isEqualTo(new ErrorDTO("CLS032", "The provided client certificate is already stored in the system."));
    }

    @ParameterizedTest
    @ValueSource(strings = {"wrong"})
    @NullAndEmptySource
    void addCertificate_certificate_badly_formatted(String certificate) {
        HttpEntity<NewClientMTLSCertificateDTO> request = new HttpEntity<>(new NewClientMTLSCertificateDTO(certificate), getHttpHeaders(clientToken));
        ResponseEntity<ErrorDTO> response = testRestTemplate.exchange(
                "/internal/clients/{clientId}/client-mtls-certificates",
                HttpMethod.POST, request, new ParameterizedTypeReference<>() {
                },
                clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo(new ErrorDTO("CLS1008", "Method argument not valid (request body validation error). Offending field: certificateChain"));
    }

    @Test
    void addCertificate_certificate_not_provided() {
        HttpEntity<NewClientMTLSCertificateDTO> request = new HttpEntity<>(new NewClientMTLSCertificateDTO(null), getHttpHeaders(clientToken));
        ResponseEntity<ErrorDTO> response = testRestTemplate.exchange(
                "/internal/clients/{clientId}/client-mtls-certificates",
                HttpMethod.POST, request, new ParameterizedTypeReference<>() {
                },
                clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo(new ErrorDTO("CLS1008", "Method argument not valid (request body validation error). Offending field: certificateChain"));
    }

    private HttpHeaders getHttpHeaders(ClientToken clientToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME, clientToken.getSerialized());
        return headers;
    }
}

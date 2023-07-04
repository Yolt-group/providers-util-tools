package com.yolt.clients.client.redirecturls;

import com.yolt.clients.IntegrationTest;
import com.yolt.clients.client.ClientsRepository;
import com.yolt.clients.client.redirecturls.dto.RedirectURLLicensedDTO;
import com.yolt.clients.client.redirecturls.repository.RedirectURL;
import com.yolt.clients.client.redirecturls.repository.RedirectURLRepository;
import com.yolt.clients.clientgroup.ClientGroupRepository;
import com.yolt.clients.model.Client;
import com.yolt.clients.model.ClientGroup;
import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.clienttokens.constants.ClientTokenConstants;
import nl.ing.lovebird.clienttokens.test.TestClientTokens;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class RedirectURLLicensedControllerIT {

    public static final String DEV_PORTAL = "dev-portal";

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private TestClientTokens testClientTokens;

    @Autowired
    private ClientGroupRepository clientGroupRepository;

    @Autowired
    private RedirectURLRepository redirectURLRepository;

    @Autowired
    private ClientsRepository clientsRepository;

    private UUID clientId;
    private UUID redirectURLId;
    private ClientGroup clientGroup;
    private Client client;
    private RedirectURL redirectURL;
    private ClientToken clientToken;

    @BeforeEach
    void setup() {
        clientId = UUID.randomUUID();
        var clientGroupId = UUID.randomUUID();
        redirectURLId = UUID.randomUUID();
        clientGroup = new ClientGroup(clientGroupId, "clientGroupRedirectURL");
        client = new Client(
                clientId,
                clientGroupId,
                "client Redirect URL",
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
        redirectURL = new RedirectURL(clientId, redirectURLId, "https://junit.test");

        clientToken = testClientTokens.createClientToken(clientGroupId, clientId,
                claims -> claims.setClaim(ClientTokenConstants.EXTRA_CLAIM_ISSUED_FOR, DEV_PORTAL));
    }

    @Test
    void testGetAllRedirectURLsLicensed() {
        clientGroupRepository.save(clientGroup);
        clientsRepository.save(client);
        redirectURLRepository.save(redirectURL);

        HttpEntity<Void> request = new HttpEntity<>(null, getHttpHeaders(clientToken));
        ResponseEntity<List<RedirectURLLicensedDTO>> response = testRestTemplate.exchange(
                "/internal/clients/" + clientId + "/redirect-urls-licensed",
                HttpMethod.GET, request, new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody()).contains(new RedirectURLLicensedDTO(redirectURLId, "https://junit.test"));
    }

    private HttpHeaders getHttpHeaders(ClientToken clientToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME, clientToken.getSerialized());
        return headers;
    }
}

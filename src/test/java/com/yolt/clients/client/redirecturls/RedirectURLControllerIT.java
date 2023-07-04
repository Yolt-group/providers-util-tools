package com.yolt.clients.client.redirecturls;

import com.yolt.clients.IntegrationTest;
import com.yolt.clients.client.ClientsRepository;
import com.yolt.clients.client.redirecturls.dto.RedirectURLChangelogDTO;
import com.yolt.clients.client.redirecturls.dto.RedirectURLDTO;
import com.yolt.clients.client.redirecturls.repository.RedirectURL;
import com.yolt.clients.client.redirecturls.repository.RedirectURLRepository;
import com.yolt.clients.clientgroup.ClientGroupRepository;
import com.yolt.clients.model.Client;
import com.yolt.clients.model.ClientGroup;
import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.clienttokens.constants.ClientTokenConstants;
import nl.ing.lovebird.clienttokens.test.TestClientTokens;
import nl.ing.lovebird.errorhandling.ErrorDTO;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static java.util.stream.IntStream.range;
import static nl.ing.lovebird.clienttokens.constants.ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME;
import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class RedirectURLControllerIT {

    private static final String DEV_PORTAL = "dev-portal";

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
        clientId = randomUUID();
        var clientGroupId = randomUUID();
        redirectURLId = randomUUID();
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
    void testGetAllRedirectURLs() {
        clientGroupRepository.save(clientGroup);
        clientsRepository.save(client);
        redirectURLRepository.save(redirectURL);

        HttpEntity<Void> request = new HttpEntity<>(null, getHttpHeaders());
        ResponseEntity<List<RedirectURLDTO>> response = testRestTemplate.exchange(
                "/internal/clients/{clientId}/redirect-urls", HttpMethod.GET,
                request, new ParameterizedTypeReference<>() {}, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains(new RedirectURLDTO(redirectURLId, "https://junit.test"));
    }

    @Test
    void testGetRedirectURLById() {
        clientGroupRepository.save(clientGroup);
        clientsRepository.save(client);
        redirectURLRepository.save(redirectURL);

        var request = new HttpEntity<>(null, getHttpHeaders());
        ResponseEntity<RedirectURLDTO> response = testRestTemplate.exchange(
                "/internal/clients/{clientId}/redirect-urls/{redirectURLId}",
                HttpMethod.GET,
                request,
                RedirectURLDTO.class,
                clientId,
                redirectURLId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(new RedirectURLDTO(redirectURLId, "https://junit.test"));
    }

    @Test
    void testCreateRedirectURL() {
        clientGroupRepository.save(clientGroup);
        clientsRepository.save(client);

        HttpEntity<RedirectURLDTO> request = new HttpEntity<>(
                new RedirectURLDTO(null, "https://newurl.org"),
                getHttpHeaders());

        ResponseEntity<RedirectURLDTO> response = testRestTemplate.exchange(
                "/internal/clients/{clientId}/redirect-urls", HttpMethod.POST, request,
                new ParameterizedTypeReference<>() {}, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getRedirectURLId()).isNotNull();
        assertThat(response.getBody().getRedirectURL()).isEqualTo("https://newurl.org");
    }

    @Test
    void testCreateRedirectURLWithId() {
        clientGroupRepository.save(clientGroup);
        clientsRepository.save(client);

        HttpEntity<RedirectURLDTO> request = new HttpEntity<>(
                new RedirectURLDTO(redirectURLId, "https://newurl.org"),
                getHttpHeaders());

        ResponseEntity<RedirectURLDTO> response = testRestTemplate.exchange(
                "/internal/clients/{clientId}/redirect-urls", HttpMethod.POST,
                request, new ParameterizedTypeReference<>() {}, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(new RedirectURLDTO(redirectURLId, "https://newurl.org"));
    }

    @Test
    void givenExistingRedirectURLId_whenCreate_then400IsReturned() {
        clientGroupRepository.save(clientGroup);
        clientsRepository.save(client);
        redirectURLRepository.save(redirectURL);

        HttpEntity<RedirectURLDTO> request = new HttpEntity<>(
                new RedirectURLDTO(redirectURLId, "https://newurl.test"),
                getHttpHeaders());

        ResponseEntity<ErrorDTO> response = testRestTemplate.exchange(
                "/internal/clients/{clientId}/redirect-urls", HttpMethod.POST,
                request, new ParameterizedTypeReference<>() {}, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo(new ErrorDTO("CLS034", "The redirect URL already exists."));
    }

    @Test
    void givenExistingRedirectURL_whenCreate_then400IsReturned() {
        clientGroupRepository.save(clientGroup);
        clientsRepository.save(client);
        redirectURLRepository.save(redirectURL);

        HttpEntity<RedirectURLDTO> request = new HttpEntity<>(
                new RedirectURLDTO(randomUUID(), "https://junit.test"),
                getHttpHeaders());

        ResponseEntity<ErrorDTO> response = testRestTemplate.exchange(
                "/internal/clients/{clientId}/redirect-urls", HttpMethod.POST,
                request, new ParameterizedTypeReference<>() {}, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo(new ErrorDTO("CLS034", "The redirect URL already exists."));
    }

    @Test
    void givenExistingAllowedRedirectURLsWithoutSandboxFlag_whenCreate_then200IsReturned() {
        clientGroupRepository.save(clientGroup);
        clientsRepository.save(client);

        range(0, 5)
                .mapToObj(i -> new RedirectURL(clientId, randomUUID(), "https://junit.test" + i))
                .forEach(redirectURLRepository::save);

        HttpEntity<RedirectURLDTO> request = new HttpEntity<>(
                new RedirectURLDTO(redirectURLId, "https://junit.test"),
                getHttpHeaders());

        ResponseEntity<RedirectURLDTO> response = testRestTemplate.exchange(
                "/internal/clients/{clientId}/redirect-urls", HttpMethod.POST,
                request, new ParameterizedTypeReference<>() {}, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(new RedirectURLDTO(redirectURLId, "https://junit.test"));
    }

    @Test
    void givenNonLowercaseRedirectURLButSimilarToExistingOne_whenCreateAddRequest_shouldReturn400() {
        clientGroupRepository.save(clientGroup);
        clientsRepository.save(client);
        redirectURLRepository.save(new RedirectURL(clientId, redirectURLId, "https://some-url.com"));

        HttpEntity<RedirectURLDTO> request = new HttpEntity<>(
                new RedirectURLDTO(UUID.randomUUID(), "https://Some-URL.com"),
                getHttpHeaders());

        ResponseEntity<ErrorDTO> response = testRestTemplate.exchange(
                "/internal/clients/{clientId}/redirect-urls", HttpMethod.POST,
                request, new ParameterizedTypeReference<>() {}, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo(new ErrorDTO("CLS034", "The redirect URL already exists."));
    }

    @Test
    void testUpdateRedirectURL() {
        clientGroupRepository.save(clientGroup);
        clientsRepository.save(client);
        redirectURLRepository.save(redirectURL);

        HttpEntity<RedirectURLDTO> request = new HttpEntity<>(
                new RedirectURLDTO(redirectURLId, "https://updated.test"),
                getHttpHeaders());

        ResponseEntity<RedirectURLDTO> response = testRestTemplate.exchange(
                "/internal/clients/{clientId}/redirect-urls/{redirectURLId}", HttpMethod.PUT,
                request, new ParameterizedTypeReference<>() {}, clientId, redirectURLId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(new RedirectURLDTO(redirectURLId, "https://updated.test"));
    }

    @Test
    void givenNoRedirectURL_whenUpdate_then404IsReturned() {
        clientGroupRepository.save(clientGroup);
        clientsRepository.save(client);

        HttpEntity<RedirectURLDTO> request = new HttpEntity<>(
                new RedirectURLDTO(redirectURLId, "https://updated.test"),
                getHttpHeaders());

        ResponseEntity<ErrorDTO> response = testRestTemplate.exchange(
                "/internal/clients/{clientId}/redirect-urls/{redirectURLId}", HttpMethod.PUT,
                request, new ParameterizedTypeReference<>() {}, clientId, redirectURLId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isEqualTo(new ErrorDTO("CLS033", "The redirect URL not found."));
    }

    @Test
    void givenAlreadyExistingRedirectURL_whenUpdate_then400IsReturned() {
        clientGroupRepository.save(clientGroup);
        clientsRepository.save(client);
        redirectURLRepository.save(redirectURL);

        HttpEntity<RedirectURLDTO> request = new HttpEntity<>(
                new RedirectURLDTO(redirectURL.getRedirectURLId(), redirectURL.getRedirectURL()),
                getHttpHeaders());

        ResponseEntity<ErrorDTO> response = testRestTemplate.exchange(
                "/internal/clients/{clientId}/redirect-urls/{redirectURLId}", HttpMethod.PUT,
                request, new ParameterizedTypeReference<>() {}, clientId, redirectURLId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo(new ErrorDTO("CLS034", "The redirect URL already exists."));
    }

    @Test
    void givenNonLowercaseRedirectURLButSimilarToExistingOne_whenUpdate_then400IsReturned() {
        clientGroupRepository.save(clientGroup);
        clientsRepository.save(client);
        redirectURLRepository.save(new RedirectURL(clientId, redirectURLId, "https://some-url.com/Custom-Page"));

        HttpEntity<RedirectURLDTO> request = new HttpEntity<>(
                new RedirectURLDTO(redirectURL.getRedirectURLId(),"https://Some-URL.com/Custom-Page"),
                getHttpHeaders());

        ResponseEntity<ErrorDTO> response = testRestTemplate.exchange(
                "/internal/clients/{clientId}/redirect-urls/{redirectURLId}", HttpMethod.PUT,
                request, new ParameterizedTypeReference<>() {}, clientId, redirectURLId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo(new ErrorDTO("CLS034", "The redirect URL already exists."));
    }

    @Test
    void testDeleteRedirectURL() {
        clientGroupRepository.save(clientGroup);
        clientsRepository.save(client);
        redirectURLRepository.save(redirectURL);

        var request = new HttpEntity<>(null, getHttpHeaders());
        ResponseEntity<RedirectURLDTO> response = testRestTemplate.exchange(
                "/internal/clients/{clientId}/redirect-urls/{redirectURLId}", HttpMethod.DELETE,
                request, new ParameterizedTypeReference<>() {}, clientId, redirectURLId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).isEqualTo(new RedirectURLDTO(redirectURLId, "https://junit.test"));
    }

    @Test
    void givenNoRedirectURL_whenDelete_then404IsReturned() {
        clientGroupRepository.save(clientGroup);
        clientsRepository.save(client);

        var request = new HttpEntity<>(null, getHttpHeaders());
        ResponseEntity<ErrorDTO> response = testRestTemplate.exchange(
                "/internal/clients/{clientId}/redirect-urls/{redirectURLId}", HttpMethod.DELETE,
                request, new ParameterizedTypeReference<>() {}, clientId, redirectURLId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isEqualTo(new ErrorDTO("CLS033", "The redirect URL not found."));
    }


    /* User input verification */

    @Test
    void givenTooLongRedirectURL_whenCreate_shouldFail() {
        var urlOver2000Chars = "https://" + StringUtils.repeat("url", 700) + ".org";
        var requestDTO = new RedirectURLDTO(redirectURLId, urlOver2000Chars);
        var request = new HttpEntity<>(requestDTO, getHttpHeaders());
        ResponseEntity<RedirectURLChangelogDTO> response = testRestTemplate.exchange(
                "/internal/clients/{clientId}/redirect-urls", HttpMethod.POST, request,
                new ParameterizedTypeReference<>() {}, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void givenNullableRedirectURL_whenCreate_shouldFail() {
        var requestDTO = new RedirectURLDTO(redirectURLId, null);
        var request = new HttpEntity<>(requestDTO, getHttpHeaders());
        ResponseEntity<RedirectURLChangelogDTO> response = testRestTemplate.exchange(
                "/internal/clients/{clientId}/redirect-urls", HttpMethod.POST, request,
                new ParameterizedTypeReference<>() {}, clientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private HttpHeaders getHttpHeaders() {
        return getHttpHeaders(Map.of());
    }

    private HttpHeaders getHttpHeaders(Map<String, String> toAdd) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(CLIENT_TOKEN_HEADER_NAME, clientToken.getSerialized());
        toAdd.forEach(headers::add);
        return headers;
    }
}

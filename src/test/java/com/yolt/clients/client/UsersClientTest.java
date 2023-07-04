package com.yolt.clients.client;

import com.yolt.clients.client.dto.CountDTO;
import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.clienttokens.constants.ClientTokenConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsersClientTest {
    private UsersClient usersClient;
    @Mock
    private RestTemplate usersRestTemplate;

    @BeforeEach
    void setup() {
        RestTemplateBuilder restTemplateBuilder = mock(RestTemplateBuilder.class);
        when(restTemplateBuilder.rootUri("usersURI")).thenReturn(restTemplateBuilder);
        when(restTemplateBuilder.build()).thenReturn(usersRestTemplate);
        usersClient = new UsersClient(restTemplateBuilder, "usersURI");
    }

    @Test
    void getCount() {
        var clientId = UUID.randomUUID();
        var serializedClientToken = "CT_" + clientId;
        var clientToken = mock(ClientToken.class);
        when(clientToken.getSerialized()).thenReturn(serializedClientToken);
        when(clientToken.getClientIdClaim()).thenReturn(clientId);

        var httpHeaders = new HttpHeaders();
        httpHeaders.add(ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME, serializedClientToken);

        var builder = UriComponentsBuilder.fromPath("/users")
                .queryParam("count-by", "CLIENT")
                .queryParam("client-id", clientToken.getClientIdClaim());

        when(usersRestTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                new HttpEntity<>(httpHeaders),
                CountDTO.class
        )).thenReturn(new ResponseEntity<>(new CountDTO(25L), HttpStatus.OK));

        var result = usersClient.getCount(clientToken);

        assertThat(result).isEqualTo(25L);
    }
}
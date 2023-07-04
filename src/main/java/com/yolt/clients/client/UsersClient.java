package com.yolt.clients.client;

import com.yolt.clients.client.dto.CountDTO;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.clienttokens.constants.ClientTokenConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
public class UsersClient {

    private final RestTemplate usersRestTemplate;

    public UsersClient(RestTemplateBuilder restTemplateBuilder,
                       @Value("${service.users.url}") String usersUrl) {
        this.usersRestTemplate = restTemplateBuilder.rootUri(usersUrl).build();
    }

    public long getCount(ClientToken clientToken) {
        var httpHeaders = new HttpHeaders();
        httpHeaders.add(ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME, clientToken.getSerialized());
        var builder = UriComponentsBuilder.fromPath("/users")
                .queryParam("count-by", "CLIENT")
                .queryParam("client-id", clientToken.getClientIdClaim());

        var usersResponse = usersRestTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                new HttpEntity<>(httpHeaders),
                CountDTO.class
        ).getBody();
        return usersResponse.getCount();
    }

}

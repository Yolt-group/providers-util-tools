package com.yolt.clients.admins.portalusersservice;

import nl.ing.lovebird.clienttokens.ClientGroupToken;
import nl.ing.lovebird.clienttokens.ClientToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static nl.ing.lovebird.clienttokens.constants.ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME;

@Service
public class DevPortalUserService {

    private final RestTemplate restTemplate;

    public DevPortalUserService(
            @Value("${service.dev-portal.url}") String devPortalUrl,
            RestTemplateBuilder restTemplateBuilder
    ) {
        this.restTemplate = restTemplateBuilder.rootUri(devPortalUrl).build();
    }

    public boolean isAdminForClientGroup(ClientGroupToken clientGroupToken, String email) {
        return getClientGroupAdmins(clientGroupToken).stream().anyMatch(portalUser -> email.equals(portalUser.getEmail()));
    }

    public boolean isAdminForClientGroup(UUID clientGroupId, UUID portalUserId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<Void> request = new HttpEntity<>(null, headers);
            ResponseEntity<IsAdminDTO> responseEntity = restTemplate.exchange(
                    "/internal-api/clientgroups/{clientGroupId}/is-admin/{portalUserId}",
                    HttpMethod.GET,
                    request,
                    new ParameterizedTypeReference<>() {},
                    clientGroupId,
                    portalUserId
            );
            return Optional.ofNullable(responseEntity.getBody()).map(IsAdminDTO::isAdmin).orElseThrow(() -> new PortalUserIsGroupAdminException(clientGroupId, portalUserId));
        } catch (HttpStatusCodeException e) {
            throw new PortalUserIsGroupAdminException(clientGroupId, portalUserId, e);
        }
    }

    public boolean isAdminForClient(ClientToken clientToken, String email) {
        return getClientAdmins(clientToken).stream().anyMatch(portalUser -> email.equals(portalUser.getEmail()));
    }

    public boolean isAdminForClient(UUID clientId, UUID portalUserId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<Void> request = new HttpEntity<>(null, headers);
            ResponseEntity<IsAdminDTO> responseEntity = restTemplate.exchange(
                    "/internal-api/clients/{clientId}/is-admin/{portalUserId}",
                    HttpMethod.GET,
                    request,
                    new ParameterizedTypeReference<>() {},
                    clientId,
                    portalUserId
            );
            return Optional.ofNullable(responseEntity.getBody()).map(IsAdminDTO::isAdmin).orElseThrow(() -> new PortalUserIsAdminException(clientId, portalUserId));
        } catch (HttpStatusCodeException e) {
            throw new PortalUserIsAdminException(clientId, portalUserId, e);
        }
    }

    public Set<PortalUser> getClientGroupAdmins(ClientGroupToken clientGroupToken) {
        try {
            HttpHeaders headers = getHeaders(clientGroupToken.getSerialized());
            HttpEntity<Void> request = new HttpEntity<>(null, headers);
            ResponseEntity<Set<PortalUser>> responseEntity = restTemplate.exchange(
                    "/internal-api/clientgroups/details/{clientGroupId}",
                    HttpMethod.GET,
                    request,
                    new ParameterizedTypeReference<>() {},
                    clientGroupToken.getClientGroupIdClaim()
            );
            return Optional.ofNullable(responseEntity.getBody()).orElseThrow(() -> new PortalUsersFetchException(clientGroupToken));
        } catch (HttpStatusCodeException e) {
            throw new PortalUsersFetchException(clientGroupToken, e);
        }
    }

    public Set<PortalUser> getClientAdmins(ClientToken clientToken) {
        try {
            HttpHeaders headers = getHeaders(clientToken.getSerialized());
            HttpEntity<Void> request = new HttpEntity<>(null, headers);
            ResponseEntity<Set<PortalUser>> responseEntity = restTemplate.exchange(
                    "/internal-api/client/details/{clientId}",
                    HttpMethod.GET,
                    request,
                    new ParameterizedTypeReference<>() {},
                    clientToken.getClientIdClaim()
            );
            return Optional.ofNullable(responseEntity.getBody()).orElseThrow(() -> new PortalUsersFetchException(clientToken));
        } catch (HttpStatusCodeException e) {
            throw new PortalUsersFetchException(clientToken, e);
        }
    }

    public void removeAuthorization(ClientGroupToken clientGroupToken, UUID portalUserId) {
        try {
            HttpHeaders headers = getHeaders(clientGroupToken.getSerialized());
            HttpEntity<Void> request = new HttpEntity<>(null, headers);
            restTemplate.exchange(
                    "/internal-api/clientgroups/{clientGroupId}/admins/{portalUserId}",
                    HttpMethod.DELETE,
                    request,
                    new ParameterizedTypeReference<>() {},
                    clientGroupToken.getClientGroupIdClaim(),
                    portalUserId
            );
        } catch (HttpStatusCodeException e) {
            throw new RemoveAuthorizationException(clientGroupToken, portalUserId, e);
        }
    }

    public void removeAuthorization(ClientToken clientToken, UUID portalUserId) {
        try {
            HttpHeaders headers = getHeaders(clientToken.getSerialized());
            HttpEntity<Void> request = new HttpEntity<>(null, headers);
            restTemplate.exchange(
                    "/internal-api/clients/{clientId}/admins/{portalUserId}",
                    HttpMethod.DELETE,
                    request,
                    new ParameterizedTypeReference<>() {},
                    clientToken.getClientIdClaim(),
                    portalUserId
            );
        } catch (HttpStatusCodeException e) {
            throw new RemoveAuthorizationException(clientToken, portalUserId, e);
        }
    }

    private HttpHeaders getHeaders(String serializedClientToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(CLIENT_TOKEN_HEADER_NAME, serializedClientToken);

        return headers;
    }
}

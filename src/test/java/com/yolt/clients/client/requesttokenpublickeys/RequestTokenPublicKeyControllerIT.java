package com.yolt.clients.client.requesttokenpublickeys;

import com.yolt.clients.IntegrationTest;
import com.yolt.clients.RequestTokenPublicKeyEventKafkaConsumer;
import com.yolt.clients.TestConfiguration;
import com.yolt.clients.client.requesttokenpublickeys.dto.AddRequestTokenPublicKeyDTO;
import com.yolt.clients.client.requesttokenpublickeys.dto.RequestTokenPublicKeyDTO;
import com.yolt.clients.client.requesttokenpublickeys.events.RequestTokenPublicKeyEvent;
import com.yolt.clients.client.requesttokenpublickeys.model.RequestTokenPublicKey;
import com.yolt.clients.clientgroup.ClientGroupRepository;
import com.yolt.clients.model.Client;
import com.yolt.clients.model.ClientGroup;
import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.clienttokens.constants.ClientTokenConstants;
import nl.ing.lovebird.clienttokens.test.TestClientTokens;
import nl.ing.lovebird.errorhandling.ErrorDTO;
import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@IntegrationTest
class RequestTokenPublicKeyControllerIT {

    private static final String REQUEST_TOKEN_PUBLIC_KEY_PEM = """
            -----BEGIN PUBLIC KEY-----
            MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAwOMSOOK2qzbQDhiZQdoT
            vhi36UWw+Hv7eGKqrKAt2GcU8oiNLpIflWp7vWflfUuuMR959MU6d5m3Z6H3IWOe
            A20n+XKhagtHe8biJNNHuZhg5cewHFJVo1YO4xNrBXdCpQuc3eo58MIgoeuImcXk
            1wx22toMNUHwOvVyW26IFV9GB3HFl5GqeuBvdzvC+U0ImFqfzoLsD5Z0vI0UW/sK
            7WRLXTvaSH7jtApDmL6Q4g+JbvFgvBKbouHbCCN5qbZe1Xh/iJ8VjoTO1VT7UUKL
            +mePuPdQRn216LhLNKMBkR9j4WLyvKf0HaLQUlg+QjATfehP3/M87xCUAi70r8Wy
            RQIDAQAB
            -----END PUBLIC KEY-----""";

    @Autowired
    private RequestTokenPublicKeyRepository requestTokenPublicKeyRepository;

    @Autowired
    private ClientGroupRepository clientGroupRepository;

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private TestClientTokens testClientTokens;

    private UUID clientId;
    private ClientToken clientToken;

    @Autowired
    RequestTokenPublicKeyEventKafkaConsumer consumer;

    @BeforeEach
    void setup() {
        var clientGroupId = UUID.randomUUID();
        clientId = UUID.randomUUID();
        var clientGroup = new ClientGroup(clientGroupId, "clientGroupName");
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
        clientGroupRepository.save(clientGroup);

        clientToken = testClientTokens.createClientToken(clientGroupId, clientId);
    }

    @Test
    void list() {
        var kid1 = "verification key 1";
        var kid2 = "verification key 2";
        var requestTokenPublicKey1 = new RequestTokenPublicKey(clientId, kid1, REQUEST_TOKEN_PUBLIC_KEY_PEM, LocalDateTime.now(TestConfiguration.FIXED_CLOCK));
        var requestTokenPublicKey2 = new RequestTokenPublicKey(clientId, kid2, REQUEST_TOKEN_PUBLIC_KEY_PEM, LocalDateTime.now(TestConfiguration.FIXED_CLOCK), LocalDateTime.now(TestConfiguration.FIXED_CLOCK));
        requestTokenPublicKeyRepository.save(requestTokenPublicKey1);
        requestTokenPublicKeyRepository.save(requestTokenPublicKey2);

        var requestEntity = new HttpEntity<>(getHttpHeaders());
        var responseEntity = testRestTemplate.exchange("/internal/clients/{clientId}/request-token-public-keys", HttpMethod.GET, requestEntity, new ParameterizedTypeReference<List<RequestTokenPublicKeyDTO>>() {
        }, clientId);

        RequestTokenPublicKeyDTO expectedKey1 = new RequestTokenPublicKeyDTO(clientId, kid1, REQUEST_TOKEN_PUBLIC_KEY_PEM, LocalDateTime.of(2020, 4, 13, 0, 0), null);
        RequestTokenPublicKeyDTO expectedKey2 = new RequestTokenPublicKeyDTO(clientId, kid2, REQUEST_TOKEN_PUBLIC_KEY_PEM, LocalDateTime.of(2020, 4, 13, 0, 0), LocalDateTime.of(2020, 4, 13, 0, 0));

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody()).containsExactly(expectedKey1, expectedKey2);
    }

    @Test
    void getKey() {
        var kid = "auto-generated verification key";
        var requestTokenPublicKey = new RequestTokenPublicKey(clientId, kid, REQUEST_TOKEN_PUBLIC_KEY_PEM, LocalDateTime.now(TestConfiguration.FIXED_CLOCK));
        requestTokenPublicKeyRepository.save(requestTokenPublicKey);

        var requestEntity = new HttpEntity<>(getHttpHeaders());
        var responseEntity = testRestTemplate.exchange("/internal/clients/{clientId}/request-token-public-keys/{kid}", HttpMethod.GET, requestEntity, new ParameterizedTypeReference<RequestTokenPublicKeyDTO>() {
        }, clientId, kid);

        RequestTokenPublicKeyDTO expectedKey = new RequestTokenPublicKeyDTO(clientId, kid, REQUEST_TOKEN_PUBLIC_KEY_PEM, LocalDateTime.of(2020, 4, 13, 0, 0), null);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody()).isEqualTo(expectedKey);
    }

    @Test
    void getKeyWhichIsNotPresent() {
        var kid = "auto-generated verification key";

        var requestEntity = new HttpEntity<>(getHttpHeaders());
        var responseEntity = testRestTemplate.exchange("/internal/clients/{clientId}/request-token-public-keys/{kid}", HttpMethod.GET, requestEntity, new ParameterizedTypeReference<ErrorDTO>() {
        }, clientId, kid);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(responseEntity.getBody()).isEqualTo(new ErrorDTO("CLS011", "request token public key not found"));
    }

    @Test
    void save() {
        var kid = "auto-generated verification key";
        var addRequestTokenPublicKeyDTO = new AddRequestTokenPublicKeyDTO(new String(Base64.encodeBase64(REQUEST_TOKEN_PUBLIC_KEY_PEM.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));

        var requestEntity = new HttpEntity<>(addRequestTokenPublicKeyDTO, getHttpHeaders());
        var responseEntity = testRestTemplate.exchange("/internal/clients/{clientId}/request-token-public-keys/{kid}", HttpMethod.POST, requestEntity, new ParameterizedTypeReference<RequestTokenPublicKeyDTO>() {
        }, clientId, kid);

        var expectedKey = new RequestTokenPublicKey(clientId, kid, REQUEST_TOKEN_PUBLIC_KEY_PEM, LocalDateTime.of(2020, 4, 13, 0, 0));
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

        Optional<RequestTokenPublicKey> actualKey = requestTokenPublicKeyRepository.findByClientIdAndKeyId(clientId, kid);
        assertThat(actualKey).contains(expectedKey);

        await().untilAsserted(() -> assertThat(consumer.getEventQueue()).contains(new RequestTokenPublicKeyEventKafkaConsumer.Record(
                new RequestTokenPublicKeyEvent(RequestTokenPublicKeyEvent.Action.ADD, clientId, kid, REQUEST_TOKEN_PUBLIC_KEY_PEM, LocalDateTime.of(2020, 4, 13, 0, 0)),
                clientToken
        )));
    }

    @Test
    void saveKeyAlreadyStored() {
        var kid = "auto-generated verification key";
        var requestTokenPublicKey = new RequestTokenPublicKey(clientId, kid, REQUEST_TOKEN_PUBLIC_KEY_PEM, LocalDateTime.now(TestConfiguration.FIXED_CLOCK));
        requestTokenPublicKeyRepository.save(requestTokenPublicKey);

        var addRequestTokenPublicKeyDTO = new AddRequestTokenPublicKeyDTO(new String(Base64.encodeBase64(REQUEST_TOKEN_PUBLIC_KEY_PEM.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
        var requestEntity = new HttpEntity<>(addRequestTokenPublicKeyDTO, getHttpHeaders());

        var responseEntity = testRestTemplate.exchange("/internal/clients/{clientId}/request-token-public-keys/{kid}", HttpMethod.POST, requestEntity, new ParameterizedTypeReference<ErrorDTO>() {
        }, clientId, kid);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(responseEntity.getBody()).isEqualTo(new ErrorDTO("CLS010", "request token public key is already stored"));
    }

    @Test
    void deleteKey() {
        var kid = "auto-generated verification key";
        var requestTokenPublicKey = new RequestTokenPublicKey(clientId, kid, REQUEST_TOKEN_PUBLIC_KEY_PEM, LocalDateTime.now(TestConfiguration.FIXED_CLOCK));
        requestTokenPublicKeyRepository.save(requestTokenPublicKey);

        var requestEntity = new HttpEntity<>(getHttpHeaders());
        var responseEntity = testRestTemplate.exchange("/internal/clients/{clientId}/request-token-public-keys/{kid}", HttpMethod.DELETE, requestEntity, new ParameterizedTypeReference<>() {
        }, clientId, kid);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        Optional<RequestTokenPublicKey> actualKey = requestTokenPublicKeyRepository.findByClientIdAndKeyId(clientId, kid);
        assertThat(actualKey).isEmpty();

        await().untilAsserted(() -> assertThat(consumer.getEventQueue()).contains(new RequestTokenPublicKeyEventKafkaConsumer.Record(
                new RequestTokenPublicKeyEvent(RequestTokenPublicKeyEvent.Action.DELETE, clientId, kid, REQUEST_TOKEN_PUBLIC_KEY_PEM, LocalDateTime.of(2020, 4, 13, 0, 0)),
                clientToken
        )));
    }

    @Test
    void deleteKeyWhichIsNotPresent() {
        var kid = "auto-generated verification key";

        var requestEntity = new HttpEntity<>(getHttpHeaders());
        var responseEntity = testRestTemplate.exchange("/internal/clients/{clientId}/request-token-public-keys/{kid}", HttpMethod.DELETE, requestEntity, new ParameterizedTypeReference<ErrorDTO>() {
        }, clientId, kid);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(responseEntity.getBody()).isEqualTo(new ErrorDTO("CLS011", "request token public key not found"));
    }

    @Test
    void updateKey() {
        var kid = "auto-generated verification key";
        var requestTokenPublicKey = new RequestTokenPublicKey(clientId, kid, "other", LocalDateTime.now(TestConfiguration.FIXED_CLOCK).minusDays(10));
        requestTokenPublicKeyRepository.save(requestTokenPublicKey);

        var addRequestTokenPublicKeyDTO = new AddRequestTokenPublicKeyDTO(new String(Base64.encodeBase64(REQUEST_TOKEN_PUBLIC_KEY_PEM.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
        var requestEntity = new HttpEntity<>(addRequestTokenPublicKeyDTO, getHttpHeaders());

        var responseEntity = testRestTemplate.exchange("/internal/clients/{clientId}/request-token-public-keys/{kid}", HttpMethod.PUT, requestEntity, new ParameterizedTypeReference<RequestTokenPublicKeyDTO>() {
        }, clientId, kid);

        var expectedKey = new RequestTokenPublicKey(clientId, kid, REQUEST_TOKEN_PUBLIC_KEY_PEM, LocalDateTime.of(2020, 4, 3, 0, 0), LocalDateTime.of(2020, 4, 13, 0, 0));
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

        var actualKey = requestTokenPublicKeyRepository.findByClientIdAndKeyId(clientId, kid);
        assertThat(actualKey).contains(expectedKey);

        await().untilAsserted(() -> assertThat(consumer.getEventQueue()).contains(new RequestTokenPublicKeyEventKafkaConsumer.Record(
                new RequestTokenPublicKeyEvent(RequestTokenPublicKeyEvent.Action.UPDATE, clientId, kid, REQUEST_TOKEN_PUBLIC_KEY_PEM, LocalDateTime.of(2020, 4, 13, 0, 0)),
                clientToken
        )));
    }

    @Test
    void updateKeyWhichIsNotPresent() {
        var kid = "auto-generated verification key";

        var addRequestTokenPublicKeyDTO = new AddRequestTokenPublicKeyDTO(new String(Base64.encodeBase64(REQUEST_TOKEN_PUBLIC_KEY_PEM.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
        var requestEntity = new HttpEntity<>(addRequestTokenPublicKeyDTO, getHttpHeaders());

        var responseEntity = testRestTemplate.exchange("/internal/clients/{clientId}/request-token-public-keys/{kid}", HttpMethod.PUT, requestEntity, new ParameterizedTypeReference<ErrorDTO>() {
        }, clientId, kid);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(responseEntity.getBody()).isEqualTo(new ErrorDTO("CLS011", "request token public key not found"));
    }

    private HttpHeaders getHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME, clientToken.getSerialized());
        return headers;
    }
}

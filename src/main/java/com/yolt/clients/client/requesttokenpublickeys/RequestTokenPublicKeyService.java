package com.yolt.clients.client.requesttokenpublickeys;

import com.yolt.clients.client.requesttokenpublickeys.dto.RequestTokenPublicKeyDTO;
import com.yolt.clients.client.requesttokenpublickeys.events.RequestTokenPublicKeyEvent;
import com.yolt.clients.client.requesttokenpublickeys.exception.RequestTokenPublicKeyAlreadyStoredException;
import com.yolt.clients.client.requesttokenpublickeys.exception.RequestTokenPublicKeyNotFoundException;
import com.yolt.clients.client.requesttokenpublickeys.model.RequestTokenPublicKey;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.clienttokens.ClientToken;
import org.bouncycastle.util.encoders.Base64;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RequestTokenPublicKeyService {
    private final RequestTokenPublicKeyRepository requestTokenPublicKeyRepository;
    private final RequestTokenPublicKeyProducer requestTokenPublicKeyProducer;
    private final Clock clock;

    RequestTokenPublicKeyService(RequestTokenPublicKeyRepository requestTokenPublicKeyRepository,
                                 RequestTokenPublicKeyProducer requestTokenPublicKeyProducer,
                                 Clock clock) {
        this.requestTokenPublicKeyRepository = requestTokenPublicKeyRepository;
        this.requestTokenPublicKeyProducer = requestTokenPublicKeyProducer;
        this.clock = clock;
    }

    public List<RequestTokenPublicKeyDTO> getRequestTokenPublicKeys(UUID clientId) {
        return requestTokenPublicKeyRepository.findAllByClientId(clientId).stream()
                .map(this::mapFrom)
                .collect(Collectors.toList());
    }

    public RequestTokenPublicKeyDTO getRequestTokenPublicKey(UUID clientId, String keyId) {
        return requestTokenPublicKeyRepository.findByClientIdAndKeyId(clientId, keyId)
                .map(this::mapFrom)
                .orElseThrow(() -> new RequestTokenPublicKeyNotFoundException(clientId, keyId));
    }

    public boolean hasRequestTokenPublicKeysConfigured(ClientToken clientToken) {
        return requestTokenPublicKeyRepository.existsByClientId(clientToken.getClientIdClaim());
    }

    void createRequestTokenPublicKey(ClientToken clientToken, UUID clientId, String keyId, String requestTokenPublicKey) {
        Optional<RequestTokenPublicKey> requestTokenPublicKeyOptional = requestTokenPublicKeyRepository.findByClientIdAndKeyId(clientId, keyId);
        if (requestTokenPublicKeyOptional.isPresent()) {
            throw new RequestTokenPublicKeyAlreadyStoredException(clientId, keyId);
        }
        var decodedKey = new String(Base64.decode(requestTokenPublicKey), StandardCharsets.UTF_8);
        var newRequestTokenPublicKey = new RequestTokenPublicKey(clientId, keyId, decodedKey, LocalDateTime.now(clock));

        requestTokenPublicKeyRepository.save(newRequestTokenPublicKey);
        requestTokenPublicKeyProducer.sendMessage(clientToken, RequestTokenPublicKeyEvent.addRequestTokenPublicKey(mapFrom(newRequestTokenPublicKey)));
    }

    void updateRequestTokenPublicKey(ClientToken clientToken, UUID clientId, String keyId, String encodedRequestTokenPublicKey) {
        var requestTokenPublicKey = requestTokenPublicKeyRepository.findByClientIdAndKeyId(clientId, keyId)
                .orElseThrow(() -> new RequestTokenPublicKeyNotFoundException(clientId, keyId));

        var decodedRequestTokenPublicKey = new String(Base64.decode(encodedRequestTokenPublicKey), StandardCharsets.UTF_8);
        requestTokenPublicKey.setRequestTokenPublicKey(decodedRequestTokenPublicKey);
        requestTokenPublicKey.setUpdated(LocalDateTime.now(clock));

        requestTokenPublicKeyRepository.save(requestTokenPublicKey);
        requestTokenPublicKeyProducer.sendMessage(clientToken, RequestTokenPublicKeyEvent.updateRequestTokenPublicKey(mapFrom(requestTokenPublicKey)));
    }

    public void deleteRequestTokenPublicKey(ClientToken clientToken, String keyId) {
        var clientId = clientToken.getClientIdClaim();
        var requestTokenPublicKey = requestTokenPublicKeyRepository.findByClientIdAndKeyId(clientId, keyId).orElseThrow(() -> new RequestTokenPublicKeyNotFoundException(clientId, keyId));
        requestTokenPublicKeyRepository.delete(requestTokenPublicKey);
        requestTokenPublicKeyProducer.sendMessage(clientToken, RequestTokenPublicKeyEvent.deleteRequestTokenPublicKey(mapFrom(requestTokenPublicKey)));
    }

    private RequestTokenPublicKeyDTO mapFrom(RequestTokenPublicKey requestTokenPublicKey) {
        return new RequestTokenPublicKeyDTO(requestTokenPublicKey.getClientId(),
                requestTokenPublicKey.getKeyId(),
                requestTokenPublicKey.getRequestTokenPublicKey(),
                requestTokenPublicKey.getCreated(),
                requestTokenPublicKey.getUpdated());
    }
}

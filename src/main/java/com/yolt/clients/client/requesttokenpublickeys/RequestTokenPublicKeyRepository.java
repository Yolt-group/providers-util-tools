package com.yolt.clients.client.requesttokenpublickeys;

import com.yolt.clients.client.requesttokenpublickeys.model.RequestTokenPublicKey;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RequestTokenPublicKeyRepository extends CrudRepository<RequestTokenPublicKey, RequestTokenPublicKey.RequestTokenPublicKeyId> {

    List<RequestTokenPublicKey> findAll();

    Optional<RequestTokenPublicKey> findByClientIdAndKeyId(UUID clientId, String keyId);

    List<RequestTokenPublicKey> findAllByClientId(UUID clientId);

    boolean existsByClientId(UUID clientId);

}

package com.yolt.clients.client;

import com.yolt.clients.model.Client;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClientsRepository extends CrudRepository<Client, UUID> {

    Optional<Client> findClientByClientGroupIdAndClientId(UUID clientGroupId, UUID clientId);

    Optional<Client> findClientByClientGroupIdAndClientIdAndDeletedIsFalse(UUID clientGroupId, UUID clientId);

    Optional<Client> findById(UUID clientId);

    Optional<Client> findByClientIdAndDeletedIsFalse(UUID clientId);

    List<Client> findAll();

    List<Client> findAllByDeleted(boolean deleted);
}

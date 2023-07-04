package com.yolt.clients.client.creditoraccounts;

import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CreditorAccountRepository extends CrudRepository<CreditorAccount, UUID> {

    List<CreditorAccount> getCreditorAccountsByClientId(UUID clientId);

    Optional<CreditorAccount> getByIdAndClientId(UUID id, UUID clientId);
}

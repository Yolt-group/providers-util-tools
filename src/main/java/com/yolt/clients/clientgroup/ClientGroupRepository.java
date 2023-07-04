package com.yolt.clients.clientgroup;

import com.yolt.clients.model.ClientGroup;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.UUID;

public interface ClientGroupRepository extends CrudRepository<ClientGroup, UUID> {
    @Override
    List<ClientGroup> findAll();
}

package com.yolt.clients.clientsitemetadata;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.UUID;

public interface ClientSiteMetadataRepository extends CrudRepository<ClientSiteMetadata, ClientSiteMetadata.ClientSiteMetadataId> {

    @Query(value = "select distinct csmd FROM ClientSiteMetadata csmd left join fetch csmd.tags where csmd.id.clientId = ?1")
    List<ClientSiteMetadata> findAllByIdClientId(UUID clientId);

    @Query(value = "select distinct csmd FROM ClientSiteMetadata csmd left join fetch csmd.tags")
    List<ClientSiteMetadata> findAll();
}
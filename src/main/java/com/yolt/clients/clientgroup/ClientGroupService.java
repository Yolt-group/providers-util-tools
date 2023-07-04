package com.yolt.clients.clientgroup;

import com.yolt.clients.client.ClientEventProducer;
import com.yolt.clients.client.ClientsRepository;
import com.yolt.clients.client.dto.ClientDTO;
import com.yolt.clients.client.dto.NewClientDTO;
import com.yolt.clients.clientgroup.admins.models.ClientGroupAdminInvitationCode;
import com.yolt.clients.clientgroup.dto.AdminInviteDTO;
import com.yolt.clients.clientgroup.dto.ClientGroupDTO;
import com.yolt.clients.clientgroup.dto.ClientGroupDetailsDTO;
import com.yolt.clients.clientgroup.dto.DomainDTO;
import com.yolt.clients.clientgroup.dto.NewClientGroupDTO;
import com.yolt.clients.clientgroup.dto.UpdateClientGroupDTO;
import com.yolt.clients.events.ClientEvent;
import com.yolt.clients.events.ClientGroupEvent;
import com.yolt.clients.exceptions.ClientAlreadyExistsException;
import com.yolt.clients.exceptions.ClientGroupAlreadyExistsException;
import com.yolt.clients.exceptions.ClientGroupNotFoundException;
import com.yolt.clients.exceptions.DomainNotFoundException;
import com.yolt.clients.model.Client;
import com.yolt.clients.model.ClientGroup;
import com.yolt.clients.model.EmailDomain;
import lombok.RequiredArgsConstructor;
import nl.ing.lovebird.clienttokens.ClientGroupToken;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClientGroupService {

    private final ClientGroupRepository clientGroupRepository;
    private final ClientsRepository clientsRepository;
    private final ClientGroupEventProducer clientGroupEventProducer;
    private final ClientEventProducer clientEventProducer;

    public List<ClientDTO> getClients(ClientGroupToken clientGroupToken) {
        return clientGroupRepository.findById(clientGroupToken.getClientGroupIdClaim())
                .map(ClientGroup::getClients)
                .map(clients -> clients.stream().map(ClientDTO::fromClient).collect(Collectors.toList()))
                .orElseThrow(() -> new ClientGroupNotFoundException(clientGroupToken.getClientGroupIdClaim()));
    }

    public ClientDTO addClient(ClientGroupToken clientGroupToken, NewClientDTO newClientDTO) {
        UUID newClientId;
        if (newClientDTO.getClientId() != null) {
            newClientId = newClientDTO.getClientId();
            clientsRepository.findById(newClientId).ifPresent(client -> {
                throw new ClientAlreadyExistsException(newClientId);
            });
        } else {
            newClientId = UUID.randomUUID();
        }

        var newClient = new Client(
                newClientId,
                clientGroupToken.getClientGroupIdClaim(),
                newClientDTO.getName(),
                newClientDTO.getKycCountryCode(),
                newClientDTO.isKycPrivateIndividuals(),
                newClientDTO.isKycEntities(),
                newClientDTO.getSbiCode(),
                newClientDTO.getGracePeriodInDays(),
                newClientDTO.isDataEnrichmentMerchantRecognition(),
                newClientDTO.isDataEnrichmentCategorization(),
                newClientDTO.isDataEnrichmentCycleDetection(),
                newClientDTO.isDataEnrichmentLabels(),
                newClientDTO.isCam(),
                newClientDTO.isPsd2Licensed(),
                newClientDTO.isAis(),
                newClientDTO.isPis(),
                false,
                newClientDTO.isConsentStarter(),
                newClientDTO.isOneOffAis(),
                newClientDTO.isRiskInsights(),
                newClientDTO.getJiraId(),
                Collections.emptySet()
        );
        var storedClient = clientGroupRepository.findById(clientGroupToken.getClientGroupIdClaim())
                .map(clientGroup -> clientsRepository.save(newClient))
                .orElseThrow(() -> new ClientGroupNotFoundException(clientGroupToken.getClientGroupIdClaim()));
        clientEventProducer.sendMessage(clientGroupToken, ClientEvent.addClientEvent(storedClient));
        return ClientDTO.fromClient(storedClient);
    }

    ClientGroupDTO updateClientGroup(ClientGroupToken clientGroupToken, UUID clientGroupId, UpdateClientGroupDTO updateClientGroupDTO) {
        var clientGroup = clientGroupRepository.findById(clientGroupId).orElseThrow(() -> new ClientGroupNotFoundException(clientGroupId));
        clientGroup.setName(updateClientGroupDTO.getName());
        var storedClientGroup = clientGroupRepository.save(clientGroup);
        clientGroupEventProducer.sendMessage(clientGroupToken, new ClientGroupEvent(ClientGroupEvent.Action.UPDATE, storedClientGroup.getId(), storedClientGroup.getName()));
        return new ClientGroupDTO(storedClientGroup.getId(), storedClientGroup.getName());
    }

    ClientGroupDTO addClientGroup(NewClientGroupDTO newClientGroupDTO) {
        UUID newClientGroupId;
        if (newClientGroupDTO.getClientGroupId() != null) {
            newClientGroupId = newClientGroupDTO.getClientGroupId();
            clientGroupRepository.findById(newClientGroupId).ifPresent(clientGroup -> {
                throw new ClientGroupAlreadyExistsException(newClientGroupId);
            });
        } else {
            newClientGroupId = UUID.randomUUID();
        }

        var clientGroup = clientGroupRepository.save(new ClientGroup(newClientGroupId, newClientGroupDTO.getName(), Collections.emptySet(), Collections.emptySet(), Collections.emptySet()));
        clientGroupEventProducer.sendMessage(new ClientGroupEvent(ClientGroupEvent.Action.ADD, clientGroup.getId(), clientGroup.getName()));
        return new ClientGroupDTO(clientGroup.getId(), clientGroup.getName());
    }

    List<ClientGroupDTO> getClientGroups() {
        return clientGroupRepository.findAll().stream().map(clientGroup -> new ClientGroupDTO(clientGroup.getId(), clientGroup.getName())).collect(Collectors.toList());
    }

    ClientGroupDetailsDTO getClientGroupDetails(UUID clientGroupId) {
        return clientGroupRepository.findById(clientGroupId).map(clientGroup -> {
            List<ClientDTO> clients = clientGroup.getClients().stream().map(ClientDTO::fromClient).collect(Collectors.toList());
            List<DomainDTO> domains = clientGroup.getEmailDomains().stream().map(domain -> new DomainDTO(domain.getDomain())).collect(Collectors.toList());
            List<AdminInviteDTO> clientGroupAdminInvites = clientGroup.getClientGroupAdminInvitations().stream()
                    .map(clientGroupAdminInvitation -> clientGroupAdminInvitation.getCodes().stream()
                            .max(Comparator.comparing(ClientGroupAdminInvitationCode::getGeneratedAt))
                            .map(lastCode -> new AdminInviteDTO(clientGroupAdminInvitation.getEmail(), clientGroupAdminInvitation.getName(), lastCode.getGeneratedAt(), lastCode.getUsedAt()))
                            .orElse(null))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            return new ClientGroupDetailsDTO(clientGroup.getId(), clientGroup.getName(), clients, domains, clientGroupAdminInvites);
        }).orElseThrow(() -> new ClientGroupNotFoundException(clientGroupId));
    }

    public void addDomain(UUID clientGroupId, DomainDTO domainDTO) {
        clientGroupRepository.findById(clientGroupId)
                .map(clientGroup -> {
                    clientGroup.getEmailDomains().add(new EmailDomain(clientGroupId, domainDTO.getDomain()));
                    return clientGroup;
                })
                .map(clientGroupRepository::save)
                .orElseThrow(() -> new ClientGroupNotFoundException(clientGroupId));
    }

    public void deleteDomain(UUID clientGroupId, DomainDTO domainDTO) {
        clientGroupRepository.findById(clientGroupId)
                .map(clientGroup -> {
                    boolean domainExists = clientGroup.getEmailDomains().stream().anyMatch(domain -> domain.getDomain().equals(domainDTO.getDomain()));
                    if (!domainExists) throw new DomainNotFoundException(domainDTO.getDomain(), clientGroupId);

                    clientGroup.setEmailDomains(
                            clientGroup.getEmailDomains().stream().filter(domain -> !domain.getDomain().equals(domainDTO.getDomain())).collect(Collectors.toSet())
                    );
                    return clientGroup;
                })
                .map(clientGroupRepository::save)
                .orElseThrow(() -> new ClientGroupNotFoundException(clientGroupId));
    }

    public void syncClientGroups() {
        clientGroupRepository.findAll().forEach(group -> {
            clientGroupEventProducer.sendMessage(new ClientGroupEvent(ClientGroupEvent.Action.SYNC, group.getId(), group.getName()));
        });
    }
}

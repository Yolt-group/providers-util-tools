package com.yolt.clients.client;

import com.yolt.clients.client.creditoraccounts.CreditorAccountDTO;
import com.yolt.clients.client.creditoraccounts.CreditorAccountService;
import com.yolt.clients.client.dto.ClientDTO;
import com.yolt.clients.client.dto.UpdateClientDTO;
import com.yolt.clients.events.ClientEvent;
import com.yolt.clients.exceptions.ClientNotFoundException;
import com.yolt.clients.model.Client;
import lombok.AllArgsConstructor;
import nl.ing.lovebird.clienttokens.ClientToken;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ClientService {

    private final ClientsRepository clientsRepository;
    private final ClientEventProducer clientEventProducer;
    private final CreditorAccountService creditorAccountService;

    public ClientDTO getClient(ClientToken clientToken) {
        return clientsRepository.findClientByClientGroupIdAndClientId(clientToken.getClientGroupIdClaim(), clientToken.getClientIdClaim())
                .map(ClientDTO::fromClient)
                .orElseThrow(() -> new ClientNotFoundException(clientToken));
    }

    public ClientDTO updateClient(ClientToken clientToken, UpdateClientDTO updateClientDTO) {
        return clientsRepository.findClientByClientGroupIdAndClientIdAndDeletedIsFalse(clientToken.getClientGroupIdClaim(), clientToken.getClientIdClaim())
                .map(client -> updateClient(clientToken, client, updateClientDTO))
                .map(ClientDTO::fromClient)
                .orElseThrow(() -> new ClientNotFoundException(clientToken));
    }

    private Client updateClient(ClientToken clientToken, Client original, UpdateClientDTO updateClientDTO) {
        original.setName(updateClientDTO.getName());
        original.setKycPrivateIndividuals(updateClientDTO.isKycPrivateIndividuals());
        original.setKycEntities(updateClientDTO.isKycEntities());
        original.setKycCountryCode(updateClientDTO.getKycCountryCode());
        original.setSbiCode(updateClientDTO.getSbiCode());
        original.setGracePeriodInDays(updateClientDTO.getGracePeriodInDays());
        original.setDataEnrichmentMerchantRecognition(updateClientDTO.isDataEnrichmentMerchantRecognition());
        original.setDataEnrichmentCategorization(updateClientDTO.isDataEnrichmentCategorization());
        original.setDataEnrichmentCycleDetection(updateClientDTO.isDataEnrichmentCycleDetection());
        original.setDataEnrichmentLabels(updateClientDTO.isDataEnrichmentLabels());
        original.setJiraId(updateClientDTO.getJiraId());
        original.setCam(updateClientDTO.isCam());
        original.setAis(updateClientDTO.isAis());
        original.setPis(updateClientDTO.isPis());
        original.setConsentStarter(updateClientDTO.isConsentStarter());
        original.setOneOffAis(updateClientDTO.isOneOffAis());
        original.setRiskInsights(updateClientDTO.isRiskInsights());
        var storedClient = clientsRepository.save(original);

        List<CreditorAccountDTO> creditorAccounts = creditorAccountService.getCreditorAccounts(clientToken.getClientIdClaim());

        clientEventProducer.sendMessage(clientToken, ClientEvent.updateClientEvent(storedClient, creditorAccounts));
        return storedClient;
    }

    public List<ClientDTO> getClients(boolean deleted) {
        return clientsRepository
                .findAllByDeleted(deleted)
                .stream()
                .map(ClientDTO::fromClient)
                .collect(Collectors.toList());
    }

    public void markClientDeleted(ClientToken clientToken) {
        var client = clientsRepository.findClientByClientGroupIdAndClientId(clientToken.getClientGroupIdClaim(), clientToken.getClientIdClaim())
                .orElseThrow(() -> new ClientNotFoundException(clientToken));
        client.setDeleted(true);
        clientsRepository.save(client);
    }

    public void syncClients() {
        clientsRepository.findAll().forEach(client -> {
            List<CreditorAccountDTO> creditorAccounts = creditorAccountService.getCreditorAccounts(client.getClientId());
            clientEventProducer.sendMessage(ClientEvent.syncClientEvent(client, creditorAccounts));
        });
    }
}

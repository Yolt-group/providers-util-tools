package com.yolt.clients.client.creditoraccounts;

import com.yolt.clients.client.ClientEventProducer;
import com.yolt.clients.client.ClientsRepository;
import com.yolt.clients.events.ClientEvent;
import com.yolt.clients.events.sema.CreditorAccountSemaEvent;
import com.yolt.clients.exceptions.ClientNotFoundException;
import com.yolt.clients.model.Client;
import lombok.RequiredArgsConstructor;
import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.logging.SemaEventLogger;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CreditorAccountService {

    private final CreditorAccountRepository creditorAccountRepository;
    private final ClientsRepository clientsRepository;
    private final ClientEventProducer clientEventProducer;

    public List<CreditorAccountDTO> getCreditorAccounts(UUID clientId) {
        List<CreditorAccount> creditorAccountsByClientId = creditorAccountRepository.getCreditorAccountsByClientId(clientId);

        return creditorAccountsByClientId.stream()
                .map(creditorAccount ->
                        new CreditorAccountDTO(
                                creditorAccount.getId(),
                                creditorAccount.getAccountHolderName(),
                                creditorAccount.getAccountNumber(),
                                creditorAccount.getAccountIdentifierScheme(),
                                creditorAccount.getSecondaryIdentification()
                        )
                )
                .collect(Collectors.toList());
    }

    public CreditorAccountDTO addCreditorAccount(ClientToken clientToken, CreateCreditorAccountDTO creditorAccountDTO) {
        UUID creditorAccountId = creditorAccountDTO.getCreditorAccountId() != null ?
                creditorAccountDTO.getCreditorAccountId() : UUID.randomUUID();

        CreditorAccount newCreditorAccount = new CreditorAccount(
                creditorAccountId,
                clientToken.getClientIdClaim(),
                creditorAccountDTO.getAccountHolderName(),
                creditorAccountDTO.getAccountNumber(),
                creditorAccountDTO.getAccountIdentifierScheme(),
                creditorAccountDTO.getSecondaryIdentification()
        );

        creditorAccountRepository.save(newCreditorAccount);

        Client client = clientsRepository.findClientByClientGroupIdAndClientId(clientToken.getClientGroupIdClaim(), clientToken.getClientIdClaim())
                .orElseThrow(() -> new ClientNotFoundException(clientToken));
        List<CreditorAccountDTO> creditorAccounts = getCreditorAccounts(clientToken.getClientIdClaim());

        clientEventProducer.sendMessage(clientToken, ClientEvent.updateClientEvent(client, creditorAccounts));

        SemaEventLogger.log(CreditorAccountSemaEvent.builder()
                .message("Creditor account was added for client")
                .clientId(client.getClientId())
                .accountHolderName(creditorAccountDTO.getAccountHolderName())
                .accountNumber(creditorAccountDTO.getAccountNumber())
                .accountIdentifierScheme(creditorAccountDTO.getAccountIdentifierScheme())
                .secondaryIdentification(creditorAccountDTO.getSecondaryIdentification())
                .build());

        return new CreditorAccountDTO(
                newCreditorAccount.getId(),
                newCreditorAccount.getAccountHolderName(),
                newCreditorAccount.getAccountNumber(),
                newCreditorAccount.getAccountIdentifierScheme(),
                newCreditorAccount.getSecondaryIdentification()
        );
    }

    public void removeCreditorAccount(ClientToken clientToken, UUID creditorAccountId) {
        CreditorAccount creditorAccount = creditorAccountRepository.getByIdAndClientId(creditorAccountId, clientToken.getClientIdClaim())
                .orElseThrow(() -> new ClientNotFoundException(clientToken));

        creditorAccountRepository.delete(creditorAccount);

        Client client = clientsRepository.findClientByClientGroupIdAndClientId(clientToken.getClientGroupIdClaim(), clientToken.getClientIdClaim())
                .orElseThrow(() -> new ClientNotFoundException(clientToken));
        List<CreditorAccountDTO> creditorAccounts = getCreditorAccounts(clientToken.getClientIdClaim());

        clientEventProducer.sendMessage(clientToken, ClientEvent.updateClientEvent(client, creditorAccounts));

        SemaEventLogger.log(CreditorAccountSemaEvent.builder()
                .message("Creditor account was removed from client")
                .clientId(client.getClientId())
                .accountHolderName(creditorAccount.getAccountHolderName())
                .accountNumber(creditorAccount.getAccountNumber())
                .accountIdentifierScheme(creditorAccount.getAccountIdentifierScheme())
                .secondaryIdentification(creditorAccount.getSecondaryIdentification())
                .build());
    }
}

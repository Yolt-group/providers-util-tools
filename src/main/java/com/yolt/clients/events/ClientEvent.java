package com.yolt.clients.events;

import com.yolt.clients.client.creditoraccounts.CreditorAccountDTO;
import com.yolt.clients.model.Client;
import lombok.Value;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

@Value
public class ClientEvent {
    @NotNull
    Action action;
    @NotNull
    UUID clientId;
    @NotNull
    UUID clientGroupId;
    @NotNull
    String name;
    @NotNull
    String countryCode;
    Integer gracePeriodInDays;
    boolean cam;
    boolean psd2Licensed;
    String sbiCode;
    @NotNull
    ClientUsersKyc clientUsersKyc;
    @NotNull
    DataEnrichment dataEnrichment;
    boolean ais;
    boolean pis;
    boolean consentStarter;
    boolean oneOffAis;
    boolean riskInsights;
    List<CreditorAccountDTO> creditorAccounts;

    @Value
    public static class ClientUsersKyc {
        boolean privateIndividuals;
        boolean entities;
    }

    @Value
    public static class DataEnrichment {
        boolean merchantRecognition;
        boolean categorization;
        boolean cycleDetection;
        boolean labels;
    }

    public static ClientEvent addClientEvent(Client client) {
        return new ClientEvent(
                Action.ADD,
                client.getClientId(),
                client.getClientGroupId(),
                client.getName(),
                client.getKycCountryCode(),
                client.getGracePeriodInDays(),
                client.isCam(),
                client.isPsd2Licensed(),
                client.getSbiCode(),
                new ClientEvent.ClientUsersKyc(client.isKycPrivateIndividuals(), client.isKycEntities()),
                new ClientEvent.DataEnrichment(
                        client.isDataEnrichmentMerchantRecognition(),
                        client.isDataEnrichmentCategorization(),
                        client.isDataEnrichmentCycleDetection(),
                        client.isDataEnrichmentLabels()
                ),
                client.isAis(),
                client.isPis(),
                client.isConsentStarter(),
                client.isOneOffAis(),
                client.isRiskInsights(),
                null
        );
    }

    public static ClientEvent updateClientEvent(Client client, List<CreditorAccountDTO> creditorAccounts) {
        return new ClientEvent(
                Action.UPDATE,
                client.getClientId(),
                client.getClientGroupId(),
                client.getName(),
                client.getKycCountryCode(),
                client.getGracePeriodInDays(),
                client.isCam(),
                client.isPsd2Licensed(),
                client.getSbiCode(),
                new ClientEvent.ClientUsersKyc(client.isKycPrivateIndividuals(), client.isKycEntities()),
                new ClientEvent.DataEnrichment(
                        client.isDataEnrichmentMerchantRecognition(),
                        client.isDataEnrichmentCategorization(),
                        client.isDataEnrichmentCycleDetection(),
                        client.isDataEnrichmentLabels()
                ),
                client.isAis(),
                client.isPis(),
                client.isConsentStarter(),
                client.isOneOffAis(),
                client.isRiskInsights(),
                creditorAccounts
        );
    }

    public static ClientEvent syncClientEvent(Client client, List<CreditorAccountDTO> creditorAccounts) {
        return new ClientEvent(
                Action.SYNC,
                client.getClientId(),
                client.getClientGroupId(),
                client.getName(),
                client.getKycCountryCode(),
                client.getGracePeriodInDays(),
                client.isCam(),
                client.isPsd2Licensed(),
                client.getSbiCode(),
                new ClientEvent.ClientUsersKyc(client.isKycPrivateIndividuals(), client.isKycEntities()),
                new ClientEvent.DataEnrichment(
                        client.isDataEnrichmentMerchantRecognition(),
                        client.isDataEnrichmentCategorization(),
                        client.isDataEnrichmentCycleDetection(),
                        client.isDataEnrichmentLabels()
                ),
                client.isAis(),
                client.isPis(),
                client.isConsentStarter(),
                client.isOneOffAis(),
                client.isRiskInsights(),
                creditorAccounts
        );
    }

    public enum Action {
        ADD, SYNC, UPDATE
    }
}

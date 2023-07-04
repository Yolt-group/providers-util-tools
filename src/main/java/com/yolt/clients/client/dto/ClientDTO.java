package com.yolt.clients.client.dto;

import com.yolt.clients.client.admins.models.ClientAdminInvitationCode;
import com.yolt.clients.clientgroup.dto.AdminInviteDTO;
import com.yolt.clients.model.Client;
import lombok.Value;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Value
public class ClientDTO {
    UUID clientId;
    UUID clientGroupId;
    String name;
    String kycCountryCode;
    boolean kycPrivateIndividuals;
    boolean kycEntities;
    String sbiCode;
    Integer gracePeriodInDays;
    boolean dataEnrichmentMerchantRecognition;
    boolean dataEnrichmentCategorization;
    boolean dataEnrichmentCycleDetection;
    boolean dataEnrichmentLabels;
    boolean cam;
    boolean psd2Licensed;
    boolean ais;
    boolean pis;
    boolean deleted;
    boolean consentStarter;
    boolean oneOffAis;
    boolean riskInsights;
    Long jiraId;

    /**
     * @deprecated see YCL-2517
     */
    @Deprecated(forRemoval = true)
    List<AdminInviteDTO> clientAdminInvites;

    public static ClientDTO fromClient(Client client) {

        List<AdminInviteDTO> adminInvites = client.getClientAdminInvitations().stream()
                .map(invite -> invite.getCodes()
                        .stream()
                        .max(Comparator.comparing(ClientAdminInvitationCode::getGeneratedAt))
                        .map(lastCode -> new AdminInviteDTO(
                                invite.getEmail(),
                                invite.getName(),
                                lastCode.getGeneratedAt(),
                                lastCode.getUsedAt()
                        )).orElse(null)
                )
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return new ClientDTO(
                client.getClientId(),
                client.getClientGroupId(),
                client.getName(),
                client.getKycCountryCode(),
                client.isKycPrivateIndividuals(),
                client.isKycEntities(),
                client.getSbiCode(),
                client.getGracePeriodInDays(),
                client.isDataEnrichmentMerchantRecognition(),
                client.isDataEnrichmentCategorization(),
                client.isDataEnrichmentMerchantRecognition(),
                client.isDataEnrichmentLabels(),
                client.isCam(),
                client.isPsd2Licensed(),
                client.isAis(),
                client.isPis(),
                client.isDeleted(),
                client.isConsentStarter(),
                client.isOneOffAis(),
                client.isRiskInsights(),
                client.getJiraId(),
                adminInvites
        );
    }
}

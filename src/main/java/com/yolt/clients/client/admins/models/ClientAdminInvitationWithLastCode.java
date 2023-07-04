package com.yolt.clients.client.admins.models;

import java.time.LocalDateTime;
import java.util.UUID;

public interface ClientAdminInvitationWithLastCode {
    default UUID getId() {
        return getIdText() == null ? null : UUID.fromString(getIdText());
    }

    String getIdText();

    default UUID getClientId() {
        return getClientIdText() == null ? null : UUID.fromString(getClientIdText());
    }

    String getClientIdText();

    String getEmail();

    String getName();

    LocalDateTime getGeneratedAt();

    LocalDateTime getExpiresAt();

    LocalDateTime getUsedAt();

    default UUID getUsedBy() {
        return getUsedByText() == null ? null : UUID.fromString(getUsedByText());
    }

    String getUsedByText();

    int getNumberOfCodes();
}

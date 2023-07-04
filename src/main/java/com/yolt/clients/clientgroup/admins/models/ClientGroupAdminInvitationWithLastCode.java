package com.yolt.clients.clientgroup.admins.models;

import java.time.LocalDateTime;
import java.util.UUID;

public interface ClientGroupAdminInvitationWithLastCode {
    default UUID getId() {
        return getIdText() == null ? null : UUID.fromString(getIdText());
    }

    String getIdText();

    default UUID getClientGroupId() {
        return getClientGroupIdText() == null ? null : UUID.fromString(getClientGroupIdText());
    }

    String getClientGroupIdText();

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

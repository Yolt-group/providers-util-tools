package com.yolt.clients.admins.dto;

import lombok.Value;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.UUID;

@Value
public class InvitationCodeDTO {
    @NotNull
    @Size(min = 28, max = 32)
    String code;

    @NotNull
    UUID portalUserId;
}

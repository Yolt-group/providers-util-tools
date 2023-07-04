package com.yolt.clients.clientgroup.dto;

import lombok.Value;

import javax.validation.constraints.Email;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

@Value
public class AdminInviteDTO {

    @Email
    @Size(max = 200)
    String email;

    @Size(max = 200)
    String name;

    LocalDateTime invitedAt;
    LocalDateTime used;
}

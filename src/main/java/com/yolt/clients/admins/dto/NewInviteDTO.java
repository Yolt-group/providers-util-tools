package com.yolt.clients.admins.dto;

import lombok.Value;

import javax.validation.constraints.Email;
import javax.validation.constraints.Size;

@Value
public class NewInviteDTO {
    @Email
    @Size(max = 200)
    String email;

    @Size(max = 200)
    String name;
}

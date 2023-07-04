package com.yolt.clients.clientgroup.dto;

import lombok.Value;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Value
public class UpdateClientGroupDTO {
    @NotNull
    @Size(min = 1, max = 80)
    String name;
}

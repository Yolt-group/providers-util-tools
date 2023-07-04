package com.yolt.clients.client.creditoraccounts;

import lombok.Value;

import javax.validation.constraints.NotNull;
import java.util.UUID;

@Value
public class RemoveCreditorAccountDTO {

    @NotNull
    UUID id;
}

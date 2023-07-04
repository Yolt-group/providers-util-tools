package com.yolt.clients.client.creditoraccounts;

import lombok.Value;

import javax.validation.constraints.NotNull;
import java.util.UUID;

@Value
public class CreditorAccountDTO {

    @NotNull
    UUID id;

    @NotNull
    String accountHolderName;

    @NotNull
    String accountNumber;

    @NotNull
    AccountIdentifierSchemeEnum accountIdentifierScheme;

    String secondaryIdentification;
}

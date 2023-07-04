package com.yolt.clients.client.creditoraccounts;

import lombok.Value;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.UUID;

@Value
public class CreateCreditorAccountDTO {

    UUID creditorAccountId;

    @NotNull
    @Size(min = 1, max = 70)
    String accountHolderName;

    @NotNull
    @Size(min = 1, max = 34)
    String accountNumber;

    @NotNull
    AccountIdentifierSchemeEnum accountIdentifierScheme;

    String secondaryIdentification;
}

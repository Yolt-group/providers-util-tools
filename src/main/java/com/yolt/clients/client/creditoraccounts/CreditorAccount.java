package com.yolt.clients.client.creditoraccounts;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "creditor_account")
@Builder
@AllArgsConstructor
@Data
@NoArgsConstructor
public class CreditorAccount {

    @Id
    private UUID id;

    @Column(name = "client_id")
    private UUID clientId;

    private String accountHolderName;

    private String accountNumber;

    @Enumerated(EnumType.STRING)
    AccountIdentifierSchemeEnum accountIdentifierScheme;

    private String secondaryIdentification;
}
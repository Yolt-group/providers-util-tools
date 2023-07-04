package com.yolt.clients.client.requesttokenpublickeys.dto;

import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

@Value
public class RequestTokenPublicKeyDTO {

    UUID clientId;
    String keyId;
    String requestTokenPublicKey;
    LocalDateTime created;
    LocalDateTime updated;
}

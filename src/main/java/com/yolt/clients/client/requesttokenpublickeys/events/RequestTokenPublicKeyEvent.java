package com.yolt.clients.client.requesttokenpublickeys.events;

import com.yolt.clients.client.requesttokenpublickeys.dto.RequestTokenPublicKeyDTO;
import lombok.Value;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;

@Value
public class RequestTokenPublicKeyEvent {
    @NotNull
    RequestTokenPublicKeyEvent.Action action;

    @NotNull
    UUID clientId;

    @NotNull
    String keyId;

    @NotNull
    String requestTokenPublicKey;

    @NotNull
    LocalDateTime created;

    public enum Action {
        ADD, DELETE, UPDATE
    }


    public static RequestTokenPublicKeyEvent updateRequestTokenPublicKey(RequestTokenPublicKeyDTO requestTokenPublicKey) {
        return new RequestTokenPublicKeyEvent(Action.UPDATE,
                requestTokenPublicKey.getClientId(),
                requestTokenPublicKey.getKeyId(),
                requestTokenPublicKey.getRequestTokenPublicKey(),
                requestTokenPublicKey.getUpdated()
        );
    }

    public static RequestTokenPublicKeyEvent addRequestTokenPublicKey(RequestTokenPublicKeyDTO requestTokenPublicKey) {
        return new RequestTokenPublicKeyEvent(Action.ADD,
                requestTokenPublicKey.getClientId(),
                requestTokenPublicKey.getKeyId(),
                requestTokenPublicKey.getRequestTokenPublicKey(),
                requestTokenPublicKey.getCreated()
        );
    }

    public static RequestTokenPublicKeyEvent deleteRequestTokenPublicKey(RequestTokenPublicKeyDTO requestTokenPublicKey) {
        return new RequestTokenPublicKeyEvent(Action.DELETE,
                requestTokenPublicKey.getClientId(),
                requestTokenPublicKey.getKeyId(),
                requestTokenPublicKey.getRequestTokenPublicKey(),
                requestTokenPublicKey.getCreated()
        );
    }
}

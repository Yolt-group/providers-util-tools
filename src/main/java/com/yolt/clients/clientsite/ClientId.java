package com.yolt.clients.clientsite;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public final class ClientId {
    private final UUID value;

    public UUID unwrap() {
        return value;
    }

    @Override
    public String toString() {
        return value.toString();
    }
}

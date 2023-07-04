package com.yolt.clients.events;

import lombok.Value;

import javax.validation.constraints.NotNull;
import java.util.UUID;

@Value
public class ClientGroupEvent {
    @NotNull
    Action action;
    @NotNull
    UUID clientGroupId;
    @NotNull
    String name;

    public enum Action {
        ADD, UPDATE, SYNC
    }
}

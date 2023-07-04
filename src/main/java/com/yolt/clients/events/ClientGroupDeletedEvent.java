package com.yolt.clients.events;

import lombok.Value;

import javax.validation.constraints.NotNull;
import java.util.UUID;

@Value
public class ClientGroupDeletedEvent {
    @NotNull
    UUID clientGroupId;
}

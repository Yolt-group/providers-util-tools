package com.yolt.clients.client.outboundallowlist.dto;

import lombok.Value;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Set;
import java.util.UUID;

@Value
public class AllowedOutboundHostIdListDTO {
    @NotNull
    @NotEmpty
    Set<@NotNull UUID> ids;
}

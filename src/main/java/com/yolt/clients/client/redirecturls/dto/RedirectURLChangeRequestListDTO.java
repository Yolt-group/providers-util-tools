package com.yolt.clients.client.redirecturls.dto;

import lombok.Value;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Set;
import java.util.UUID;

@Value
public class RedirectURLChangeRequestListDTO {
    @NotNull
    @NotEmpty
    Set<@NotNull UUID> ids;
}

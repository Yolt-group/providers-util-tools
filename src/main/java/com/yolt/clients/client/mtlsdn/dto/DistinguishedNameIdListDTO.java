package com.yolt.clients.client.mtlsdn.dto;

import lombok.Value;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Set;
import java.util.UUID;

@Value
public class DistinguishedNameIdListDTO {
    @NotNull
    @NotEmpty
    Set<@NotNull UUID> ids;
}

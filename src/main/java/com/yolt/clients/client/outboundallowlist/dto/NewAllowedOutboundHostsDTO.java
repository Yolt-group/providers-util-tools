package com.yolt.clients.client.outboundallowlist.dto;

import lombok.Value;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Set;

@Value
public class NewAllowedOutboundHostsDTO {
    @NotNull
    @NotEmpty
    @Size(max = 10)
    Set<@NotNull @Hostname @Size(max = 1024) String> hosts;
}

package com.yolt.clients.client.ipallowlist.dto;

import lombok.Value;
import nl.ing.lovebird.validation.cidr.CIDR;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Set;

@Value
public class NewAllowedIPsDTO {
    @NotNull
    @NotEmpty
    @Size(max = 10)
    Set<@NotNull @CIDR(minNetMask = 24) String> cidrs;
}

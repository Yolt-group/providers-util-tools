package com.yolt.clients.clientgroup.dto;

import lombok.Value;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Value
public class DomainDTO {
    @Size(max = 200)
    @Pattern(regexp = "/^([0-9a-z-]+\\.?)+$/i")
    String domain;
}

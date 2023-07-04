package com.yolt.clients.clientgroup.certificatemanagement.dto;

import lombok.Value;

import javax.validation.constraints.Size;

@Value
public class SimpleDistinguishedNameElement {
    @Size(min = 1, max = 256)
    String type;

    @Size(min = 1, max = 256)
    String value;
}

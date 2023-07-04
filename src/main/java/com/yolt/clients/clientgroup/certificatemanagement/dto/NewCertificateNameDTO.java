package com.yolt.clients.clientgroup.certificatemanagement.dto;

import lombok.Value;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Value
public class NewCertificateNameDTO {
    @NotBlank
    @Size(min = 1, max = 128)
    @Pattern(regexp = "^[\\w\\- ]+$")
    String name;
}

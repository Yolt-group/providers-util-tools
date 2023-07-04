package com.yolt.clients.client.redirecturls.dto;

import lombok.Value;

import java.util.UUID;

@Value
public class RedirectURLLicensedDTO {
    UUID redirectURLId;
    String redirectURL;
}

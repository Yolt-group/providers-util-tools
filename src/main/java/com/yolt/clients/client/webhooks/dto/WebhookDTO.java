package com.yolt.clients.client.webhooks.dto;

import lombok.Data;
import org.hibernate.validator.constraints.URL;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class WebhookDTO {

    @Size(max = 2000, message = "URL cannot exceed 2000 chars in order to avoid issues for different combinations of client and server software")
    @URL(protocol = "https")
    @NotNull
    private final String url;

    private final boolean enabled;
}

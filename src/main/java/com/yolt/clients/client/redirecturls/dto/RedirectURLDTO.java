package com.yolt.clients.client.redirecturls.dto;

import lombok.Data;
import org.hibernate.validator.constraints.URL;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.UUID;

@Data
public class RedirectURLDTO {

    private final UUID redirectURLId;

    @Size(max = 2000, message = "URL cannot exceed 2000 chars in order to avoid issues for different combinations of client and server software")
    @URL
    @NotNull
    private final String redirectURL;
}

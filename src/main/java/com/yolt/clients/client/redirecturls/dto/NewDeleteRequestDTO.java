package com.yolt.clients.client.redirecturls.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.UUID;

@Data
@AllArgsConstructor
public class NewDeleteRequestDTO {

    @NotNull
    private final UUID redirectURLId;

    @Size(max = 500)
    private final String comment;
}

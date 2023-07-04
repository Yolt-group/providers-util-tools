package com.yolt.clients.client.redirecturls;

import com.yolt.clients.client.redirecturls.dto.RedirectURLLicensedDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/internal/clients/{clientId}/redirect-urls-licensed")
public class RedirectURLLicensedController {

    private final RedirectURLLicensedService service;

    @GetMapping(produces = APPLICATION_JSON_VALUE)
    public List<RedirectURLLicensedDTO> list(@PathVariable UUID clientId) {
        return service.findAll(clientId);
    }
}

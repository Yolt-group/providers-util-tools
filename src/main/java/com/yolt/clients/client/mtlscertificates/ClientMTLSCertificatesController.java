package com.yolt.clients.client.mtlscertificates;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.clienttokens.annotations.NonDeletedClient;
import nl.ing.lovebird.clienttokens.annotations.VerifiedClientToken;
import nl.ing.lovebird.clienttokens.verification.ClientIdVerificationService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/clients/{clientId}/client-mtls-certificates")
public class ClientMTLSCertificatesController {
    private static final String ASSISTANCE_PORTAL_YTS = "assistance-portal-yts";
    private static final String DEV_PORTAL = "dev-portal";

    private final ClientIdVerificationService clientIdVerificationService;
    private final ClientMTLSCertificateService clientMTLSCertificateService;

    /**
     * Get the known client mtls certificates.
     *
     * @param clientToken the client token of the client
     * @param clientId    the client id of the client
     * @param pageable    the pagnation object, default size = 20, sorted by last-seen desc.
     * @return List of known client mtls certificates
     */
    @GetMapping(produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ClientMTLSCertificate>> list(
            @VerifiedClientToken(restrictedTo = {ASSISTANCE_PORTAL_YTS, DEV_PORTAL}) final ClientToken clientToken,
            @PathVariable UUID clientId,
            @PageableDefault(sort = {"sortDate"}, value = 20, direction = Sort.Direction.DESC) Pageable pageable
    ) {
        clientIdVerificationService.verify(clientToken, clientId);

        var certificatesPage = clientMTLSCertificateService.findAllByClientId(clientId, pageable);

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("X-Total-Count", String.valueOf(certificatesPage.getTotalElements()));
        responseHeaders.set("X-Pagination-Page", String.valueOf(certificatesPage.getNumber()));
        responseHeaders.set("X-Pagination-Pages", String.valueOf(certificatesPage.getTotalPages()));
        responseHeaders.set("X-Pagination-PageSize", String.valueOf(certificatesPage.getSize()));

        return ResponseEntity.ok()
                .headers(responseHeaders)
                .body(certificatesPage.getContent());
    }

    @NonDeletedClient
    @PostMapping(consumes = APPLICATION_JSON_VALUE)
    public void addCertificate(
            @VerifiedClientToken final ClientToken clientToken,
            @PathVariable UUID clientId,
            @RequestBody @Valid @NotNull NewClientMTLSCertificateDTO newClientMTLSCertificateDTO
    ) {
        clientIdVerificationService.verify(clientToken, clientId);
        clientMTLSCertificateService.addCertificate(clientId, newClientMTLSCertificateDTO);
    }
}

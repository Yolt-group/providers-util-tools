package com.yolt.clients.clientsitemetadata;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ClientSiteSyncController {

    private final SynchronizeWithSiteManagementService synchronizeWithSiteManagementService;

    @PostMapping(value = "/batch/client-sites-sync", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> synchronizeFromSiteManagement(@RequestParam(defaultValue = "true") boolean dryrun) {
        synchronizeWithSiteManagementService.synchronizeWithSiteManagement(dryrun);
        return ResponseEntity.accepted().build();
    }

}

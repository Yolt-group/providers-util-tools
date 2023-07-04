package com.yolt.clients.authmeans;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
class OnboardedProvidersController {

    private final SynchronizeWithProvidersService synchronizeWithProvidersService;

    @PostMapping("/synchronize-onboarded-providers")
    public void synchronize(@RequestParam(name = "dryrun", defaultValue = "true") boolean dryrun) {
        log.info("Will synchronize onboarded providers, dryrun={}", dryrun);
        synchronizeWithProvidersService.synchronizeWithProviders(dryrun);
    }

}

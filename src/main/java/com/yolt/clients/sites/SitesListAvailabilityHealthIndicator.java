package com.yolt.clients.sites;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class SitesListAvailabilityHealthIndicator extends AbstractHealthIndicator {

    private final SitesProvider sitesProvider;

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        if (sitesProvider.isRunning()) {
            builder.up();
        } else {
            log.warn("SitesListAvailabilityHealthIndicator = down.  Failed to retrieve the sites list from the providers service, cannot start without it.");
            builder.down();
        }
    }

}

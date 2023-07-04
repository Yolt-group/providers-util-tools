package com.yolt.clients.sites;

import com.yolt.clients.clientsite.ClientSitesUpdateProducer;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class SitesProvider implements SmartLifecycle {

    private final SitesMapper sitesMapper = new SitesMapper();
    private final ProviderRestClient providerRestClient;
    private final Clock clock;
    private final MeterRegistry meterRegistry;
    private final ClientSitesUpdateProducer clientSitesUpdateProducer;

    private List<Site> sites = List.of();
    private Map<Site.SiteId, Site> sitesById = Map.of();
    private Map<String, List<Site>> sitesByProvider = Map.of();

    volatile boolean loaded = false;
    private Instant lastLoaded = Instant.EPOCH;

    /**
     * By setting the lifecycle phase of this bean to a negative integer, we ensure that this service will be initialized
     * before all other services.
     */
    @Override
    public int getPhase() {
        return -1;
    }

    @Override
    public void start() {
        registerMetric();
        update();
    }

    @Override
    public boolean isRunning() {
        return loaded;
    }

    @Override
    public void stop() {
        loaded = false;
    }

    /**
     * Retrieve details about sites every seven minutes
     */
    // second, minute, hour, day of month, month, day(s) of week
    @Scheduled(cron = "0 0/7 * * * *")
    public void retrieveSitesPeriodically() {
        update();
    }

    /**
     * TODO make this non-public
     */
    public void update() {
        try {
            Set<UUID> previousSiteIds = sites.stream().map(Site::getId).map(Site.SiteId::unwrap).collect(Collectors.toSet());
            var providersSites = providerRestClient.getProvidersSites();

            // This is a really nasty assumption, but there should be AIS details if there are PIS details..
            // because the AIS details contains, for example, the bankname.
            Map<UUID, List<ProvidersSites.RegisteredPisSite>> pisDetailsBySiteId = providersSites.getPisSiteDetails().stream()
                    .collect(Collectors.groupingBy(ProvidersSites.RegisteredPisSite::getId));

            this.sitesById = providersSites.getAisSiteDetails().stream()
                    .map(entry ->
                        Map.entry(new Site.SiteId(entry.getId()), toSite(entry, pisDetailsBySiteId.getOrDefault(entry.getId(), null)))
                    )
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            this.sites = List.copyOf(this.sitesById.values());
            this.sitesByProvider = this.sites.stream().collect(Collectors.groupingBy(Site::getProvider));
            this.loaded = true;
            this.lastLoaded = Instant.now(clock);
            log.info("loaded {} sites", sitesById.size());
            Set<UUID> newSiteIds = sites.stream().map(Site::getId).map(Site.SiteId::unwrap).collect(Collectors.toSet());
            if (!previousSiteIds.isEmpty() && !newSiteIds.equals(previousSiteIds)) {
                clientSitesUpdateProducer.sendMessage();
            }
        } catch (RuntimeException e) {
            log.warn("Failed to call providers/sites-details", e);
        }
    }

    private Site toSite(ProvidersSites.RegisteredSite registeredSite, List<ProvidersSites.RegisteredPisSite> pisDetails) {
        return sitesMapper.mapToSite(registeredSite, pisDetails);
    }

    public List<Site> allSites() {
        return sites;
    }

    public List<Site> findByProvider(@NonNull String provider) {
        return sitesByProvider.getOrDefault(provider, List.of());
    }

    public Optional<Site> findById(@NonNull Site.SiteId id) {
        return Optional.ofNullable(sitesById.get(id));
    }

    public Site findByIdOrThrow(@NonNull Site.SiteId id) {
        return findById(id).orElseThrow(() -> new SiteNotFoundException("Site with id " + id + " does not exist."));
    }


    /**
     * Site details are retrieved every seven minutes.  During normal operation this gauge value should always be at
     * most 7 minutes.
     */
    private void registerMetric() {
        Gauge.builder("site_details_age_minutes", () -> {
            if (lastLoaded == Instant.EPOCH) {
                return 0;
            }
            return Math.abs((int) ChronoUnit.MINUTES.between(Instant.now(clock), lastLoaded));
        })
                .description("age of the sites list in minutes")
                .register(meterRegistry);
    }

}

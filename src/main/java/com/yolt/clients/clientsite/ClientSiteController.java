package com.yolt.clients.clientsite;

import com.yolt.clients.clientsite.dto.ClientSiteDTO;
import com.yolt.clients.clientsite.dto.ProviderClientSitesDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.clienttokens.annotations.VerifiedClientToken;
import nl.ing.lovebird.errorhandling.ErrorDTO;
import nl.ing.lovebird.springdoc.annotations.ExternalApi;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequiredArgsConstructor
@Validated
@Tag(name = "sites")
public class ClientSiteController {

    private final ClientSiteService clientSiteService;
    private final ClientSiteDTOMapper clientSiteDTOMapper;

    @ExternalApi
    @Operation(summary = "Retrieve sites", description = "Get all available sites, optionally filtered by tags and/or redirectUrlId.", responses = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successful"
            )
    })
    @GetMapping(value = "/v2/sites", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ClientSiteDTO>> listEnabledSites(@Parameter(hidden = true) @VerifiedClientToken final ClientToken clientToken,
                                                                @Valid @Parameter(description = "An optional list of tags to use for filtering. Only sites containing ALL tags will be retrieved.")
                                                                @RequestParam(name = "tag", required = false, defaultValue = "") @Size(max = 256) final List<@Pattern(regexp = "[A-Za-z0-9_]*") @Size(min = 1, max = 20) String> tags,
                                                                @Parameter(description = "An optional redirectUrlId to use for filtering")
                                                                @RequestParam(required = false) final UUID redirectUrlId) {
        List<ClientSite> clientSiteDetailList = clientSiteService.listEnabledClientSites(clientToken, redirectUrlId, tags);
        final List<ClientSiteDTO> siteGroupsDTO = clientSiteDTOMapper.mapClientSiteDTO(clientSiteDetailList);
        return ResponseEntity.ok(siteGroupsDTO);
    }

    @ExternalApi
    @Operation(summary = "Retrieve site by id", responses = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successful"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Not Found",
                    content = { @Content(schema = @Schema(implementation = ErrorDTO.class)) }
            )
    })
    @GetMapping(value = "/v2/sites/{siteId}", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<ClientSiteDTO> getEnabledSite(@Parameter(hidden = true) @VerifiedClientToken final ClientToken clientToken,
                                                        @PathVariable final UUID siteId) {
        ClientSite clientSite = clientSiteService.getEnabledClientSite(clientToken, siteId);
        final ClientSiteDTO clientSiteDTO = clientSiteDTOMapper.mapClientSiteDTO(clientSite);
        return ResponseEntity.ok(clientSiteDTO);
    }

    @Operation(summary = "Enables a site for a client.", responses = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successful"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "The site provider is not enabled. Enable the provider by providing authentication means before continuing.",
                    content = { @Content(schema = @Schema(implementation = ErrorDTO.class)) }
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "SiteEntity ID not found",
                    content = { @Content(schema = @Schema(implementation = ErrorDTO.class)) }
            )
    })
    @PostMapping(value = "/client-sites/{siteId}", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> enableSite(@PathVariable final UUID siteId,
                                           @Parameter(hidden = true) @VerifiedClientToken final ClientToken clientToken) {
        clientSiteService.enableSite(clientToken, siteId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Disables a site for a client.", responses = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successful"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "The client site is not enabled.",
                    content = { @Content(schema = @Schema(implementation = ErrorDTO.class)) }
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "SiteEntity ID not found",
                    content = { @Content(schema = @Schema(implementation = ErrorDTO.class)) }
            )
    })
    @DeleteMapping(value = "/client-sites/{siteId}", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> disableSite(@PathVariable final UUID siteId,
                                            @Parameter(hidden = true) @VerifiedClientToken final ClientToken clientToken) {
        clientSiteService.disableSite(clientToken, siteId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Sets available flag of a site for a client.", responses = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successful"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "The site provider or site itself is not enabled. Enable provider and site by providing authentication means before continuing.",
                    content = { @Content(schema = @Schema(implementation = ErrorDTO.class)) }
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "SiteEntity ID not found",
                    content = { @Content(schema = @Schema(implementation = ErrorDTO.class)) }
            )
    })
    @PostMapping(value = "/client-sites/{siteId}/available/{isAvailable}", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> setAvailableForSite(@PathVariable final UUID siteId,
                                                    @PathVariable final boolean isAvailable,
                                                    @Parameter(hidden = true) @VerifiedClientToken final ClientToken clientToken) {
        if (isAvailable) {
            clientSiteService.markSiteAvailable(clientToken, siteId);
        } else {
            clientSiteService.markSiteUnavailable(clientToken, siteId);
        }

        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Sets experimental version of a site for a client.", responses = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successful"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "The site provider or site itself is not enabled. Enable provider and site by providing authentication means before continuing.",
                    content = { @Content(schema = @Schema(implementation = ErrorDTO.class)) }
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "SiteEntity ID not found",
                    content = { @Content(schema = @Schema(implementation = ErrorDTO.class)) }
            )
    })
    @PostMapping(value = "/client-sites/{siteId}/experimental/{useExperimentalVersion}", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> setExperimentalVersionForSite(@PathVariable final UUID siteId,
                                                              @PathVariable final boolean useExperimentalVersion,
                                                              @Parameter(hidden = true) @VerifiedClientToken final ClientToken clientToken) {
        clientSiteService.setExperimentalVersionForSite(clientToken, siteId, useExperimentalVersion);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Set the tags for a site.", responses = {
            @ApiResponse(
                    responseCode = "202",
                    description = "Successfully saved site tags"
            )
    })
    @PutMapping(value = "/client-sites/{siteId}/tags", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> tagSite(@PathVariable final UUID siteId,
                                        @Parameter(hidden = true) @VerifiedClientToken final ClientToken clientToken,
                                        @Valid @RequestBody @Size(max = 256) List<@Pattern(regexp = "[A-Za-z0-9_]*") @Size(min = 1, max = 20) String> tags) {
        clientSiteService.tagSite(clientToken, siteId, tags);
        return ResponseEntity.accepted().build();
    }

    /**
     * Only called from YAP. ClientSites are grouped by provider. For that provider the 'registered authentication means' are also
     * given. In YAP it is shown whether (and for which scope) you have the authentication-means.
     * <p>
     * Thus, this is actually not just GET 'client-sites'.. but i'll not change it now because that breaks backwardscompatibility.
     */
    @Operation(summary = "Lists client sites for a client.", responses = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successful"
            )
    })
    @GetMapping(value = "/client-sites", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ProviderClientSitesDTO>> listEnabledSites(@Parameter(hidden = true) @VerifiedClientToken final ClientToken clientToken,
                                                                         @RequestParam(value = "only-available", defaultValue = "false") boolean onlyAvailable) {
        List<ProviderClientSitesDTO> enabledSiteDetailList = clientSiteService.listAllClientSitesForInternalUsage(clientToken, onlyAvailable);
        return ResponseEntity.ok(enabledSiteDetailList);
    }

    /**
     * Internal endpoint that exposes the sites for each client.
     */
    @Operation(summary = "Lists client sites for a client.", responses = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successful"
            )
    })
    @GetMapping(value = "/internal/v2/sites-per-client", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<UUID, List<ClientSiteDTO>>> listEnabledSitesPerClient() {
        Map<UUID, List<ClientSite>> clientSitesPerClient = clientSiteService.listEnabledSitesPerClient();
        Map<UUID, List<ClientSiteDTO>> clientSiteDTOPerClient = clientSitesPerClient.entrySet().stream()
                .map(e -> Map.entry(e.getKey(), clientSiteDTOMapper.mapClientSiteDTO(e.getValue())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return ResponseEntity.ok(clientSiteDTOPerClient);
    }

}

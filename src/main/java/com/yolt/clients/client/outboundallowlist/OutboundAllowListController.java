package com.yolt.clients.client.outboundallowlist;

import com.yolt.clients.client.outboundallowlist.dto.AllowedOutboundHostDTO;
import com.yolt.clients.client.outboundallowlist.dto.AllowedOutboundHostIdListDTO;
import com.yolt.clients.client.outboundallowlist.dto.NewAllowedOutboundHostsDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.clienttokens.annotations.NonDeletedClient;
import nl.ing.lovebird.clienttokens.annotations.VerifiedClientToken;
import nl.ing.lovebird.clienttokens.verification.ClientIdVerificationService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/internal/clients/{clientId}/outbound-allow-list")
public class OutboundAllowListController {
    private static final String ASSISTANCE_PORTAL_YTS = "assistance-portal-yts";
    private static final String DEV_PORTAL = "dev-portal";

    private final OutboundAllowListService outboundAllowListService;
    private final ClientIdVerificationService clientIdVerificationService;

    /**
     * Get all allowed outbound hosts for a given client.
     *
     * @param clientToken the client token of the client
     * @param clientId    the client id of the client
     * @return list with the status of all known outbound hosts linked to the client.
     */
    @GetMapping(produces = APPLICATION_JSON_VALUE)
    public Set<AllowedOutboundHostDTO> list(
            @VerifiedClientToken(restrictedTo = {ASSISTANCE_PORTAL_YTS, DEV_PORTAL}) final ClientToken clientToken,
            @PathVariable UUID clientId
    ) {
        clientIdVerificationService.verify(clientToken, clientId);
        var list = outboundAllowListService.findAll(clientId);
        log.info("called by {} for {} retrieved {}", clientToken.getSubject(), clientId, String.join(", ",
                list.stream().map( x -> x.toString()).collect(Collectors.toList()) ));
        return list;
    }

    /**
     * Create or re-add allowed outbound hosts.
     *
     * @param clientToken      the client token of the client
     * @param clientId         the id of the client
     * @param newAllowedOutboundHostsDTO the list of outbound hosts to add
     * @return list with the (updated) internal state of each provided host.
     */
    @NonDeletedClient
    @PostMapping(produces = APPLICATION_JSON_VALUE)
    public Set<AllowedOutboundHostDTO> create(
            @VerifiedClientToken(restrictedTo = {ASSISTANCE_PORTAL_YTS, DEV_PORTAL}) final ClientToken clientToken,
            @PathVariable final UUID clientId,
            @Valid @RequestBody final NewAllowedOutboundHostsDTO newAllowedOutboundHostsDTO
    ) {
        clientIdVerificationService.verify(clientToken, clientId);
        return outboundAllowListService.create(clientToken, newAllowedOutboundHostsDTO);
    }

    /**
     * Mark changed as applied (only for APY).
     *
     * @param clientToken        the client token for the client for which we want to mark the change applied.
     * @param clientId           the client id of the client for which we want to mark it as applied.
     * @param itemsToMarkApplied the set of items to mark as applied.
     * @return list with the (updated) internal state of each provided host that exists for this client.
     */
    @PostMapping(value = "apply", produces = APPLICATION_JSON_VALUE)
    public Set<AllowedOutboundHostDTO> markApplied(
            @VerifiedClientToken(restrictedTo = {ASSISTANCE_PORTAL_YTS}) final ClientToken clientToken,
            @PathVariable final UUID clientId,
            @Valid @RequestBody final AllowedOutboundHostIdListDTO itemsToMarkApplied
    ) {
        clientIdVerificationService.verify(clientToken, clientId);
        return outboundAllowListService.markApplied(clientToken, itemsToMarkApplied);
    }

    /**
     * Mark ip blocks for deletion.
     *
     * @param clientToken   the client token for the client for which we want to mark the change applied.
     * @param clientId      the client id of the client for which we want to mark it as applied.
     * @param itemsToRemove the set of items to mark for deletion.
     * @return list with the (updated) internal state of each provided block that exists for this client.
     */
    @NonDeletedClient
    @PostMapping(value = "delete", produces = APPLICATION_JSON_VALUE)
    public Set<AllowedOutboundHostDTO> delete(
            @VerifiedClientToken(restrictedTo = {ASSISTANCE_PORTAL_YTS, DEV_PORTAL}) final ClientToken clientToken,
            @PathVariable final UUID clientId,
            @Valid @RequestBody final AllowedOutboundHostIdListDTO itemsToRemove
    ) {
        clientIdVerificationService.verify(clientToken, clientId);
        return outboundAllowListService.deleteLimited(clientToken, itemsToRemove);
    }

    /**
     * Mark pending additions as denied (only for APY).
     *
     * @param clientToken       the client token for the client for which we want to mark the change applied.
     * @param clientId          the client id of the client for which we want to mark it as applied.
     * @param itemsToMarkDenied the set of items to mark as rejected (should be in pending addition).
     * @return list with the (updated) internal state of each provided block that exists for this client.
     */
    @PostMapping(value = "deny", produces = APPLICATION_JSON_VALUE)
    public Set<AllowedOutboundHostDTO> markDenied(
            @VerifiedClientToken(restrictedTo = {ASSISTANCE_PORTAL_YTS}) final ClientToken clientToken,
            @PathVariable final UUID clientId,
            @Valid @RequestBody final AllowedOutboundHostIdListDTO itemsToMarkDenied
    ) {
        clientIdVerificationService.verify(clientToken, clientId);
        return outboundAllowListService.markDenied(clientToken, itemsToMarkDenied);
    }
}

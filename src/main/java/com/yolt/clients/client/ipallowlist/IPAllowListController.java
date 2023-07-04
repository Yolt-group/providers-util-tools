package com.yolt.clients.client.ipallowlist;

import com.yolt.clients.client.ipallowlist.dto.AllowedIPDTO;
import com.yolt.clients.client.ipallowlist.dto.AllowedIPIdListDTO;
import com.yolt.clients.client.ipallowlist.dto.NewAllowedIPsDTO;
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

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/internal/clients/{clientId}/ip-allow-list")
public class IPAllowListController {

    private static final String ASSISTANCE_PORTAL_YTS = "assistance-portal-yts";
    private static final String DEV_PORTAL = "dev-portal";

    private final IPAllowListService ipAllowListService;
    private final ClientIdVerificationService clientIdVerificationService;

    /**
     * Get all allowed ip-blocks for a given client.
     *
     * @param clientToken the client token of the client
     * @param clientId    the client id of the client
     * @return list with the status of all known ip blocks linked to the client.
     */
    @GetMapping(produces = APPLICATION_JSON_VALUE)
    public Set<AllowedIPDTO> list(
            @VerifiedClientToken(restrictedTo = {ASSISTANCE_PORTAL_YTS, DEV_PORTAL}) final ClientToken clientToken,
            @PathVariable UUID clientId
    ) {
        clientIdVerificationService.verify(clientToken, clientId);
        return ipAllowListService.findAll(clientId);
    }

    /**
     * Create or re-add allowed ip blocks.
     *
     * @param clientToken      the client token of the client
     * @param clientId         the id of the client
     * @param newAllowedIPsDTO the list of cidr blocks to add
     * @return list with the (updated) internal state of each provided block.
     */
    @NonDeletedClient
    @PostMapping(produces = APPLICATION_JSON_VALUE)
    public Set<AllowedIPDTO> create(
            @VerifiedClientToken(restrictedTo = {ASSISTANCE_PORTAL_YTS, DEV_PORTAL}) final ClientToken clientToken,
            @PathVariable final UUID clientId,
            @Valid @RequestBody final NewAllowedIPsDTO newAllowedIPsDTO
    ) {
        clientIdVerificationService.verify(clientToken, clientId);
        return ipAllowListService.create(clientToken, newAllowedIPsDTO);
    }

    /**
     * Mark changed as applied (only for APY).
     *
     * @param clientToken        the client token for the client for which we want to mark the change applied.
     * @param clientId           the client id of the client for which we want to mark it as applied.
     * @param itemsToMarkApplied the set of items to mark as applied.
     * @return list with the (updated) internal state of each provided block that exists for this client.
     */
    @PostMapping(value = "apply", produces = APPLICATION_JSON_VALUE)
    public Set<AllowedIPDTO> markApplied(
            @VerifiedClientToken(restrictedTo = {ASSISTANCE_PORTAL_YTS}) final ClientToken clientToken,
            @PathVariable final UUID clientId,
            @Valid @RequestBody final AllowedIPIdListDTO itemsToMarkApplied
    ) {
        clientIdVerificationService.verify(clientToken, clientId);
        return ipAllowListService.markApplied(clientToken, itemsToMarkApplied);
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
    public Set<AllowedIPDTO> delete(
            @VerifiedClientToken(restrictedTo = {ASSISTANCE_PORTAL_YTS, DEV_PORTAL}) final ClientToken clientToken,
            @PathVariable final UUID clientId,
            @Valid @RequestBody final AllowedIPIdListDTO itemsToRemove
    ) {
        clientIdVerificationService.verify(clientToken, clientId);
        return ipAllowListService.deleteLimited(clientToken, itemsToRemove);
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
    public Set<AllowedIPDTO> markDenied(
            @VerifiedClientToken(restrictedTo = {ASSISTANCE_PORTAL_YTS}) final ClientToken clientToken,
            @PathVariable final UUID clientId,
            @Valid @RequestBody final AllowedIPIdListDTO itemsToMarkDenied
    ) {
        clientIdVerificationService.verify(clientToken, clientId);
        return ipAllowListService.markDenied(clientToken, itemsToMarkDenied);
    }
}

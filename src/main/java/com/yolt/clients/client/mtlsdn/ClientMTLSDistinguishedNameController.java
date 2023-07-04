package com.yolt.clients.client.mtlsdn;

import com.yolt.clients.client.mtlsdn.dto.DistinguishedNameDTO;
import com.yolt.clients.client.mtlsdn.dto.DistinguishedNameIdListDTO;
import com.yolt.clients.client.mtlsdn.dto.NewDistinguishedNameDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.clienttokens.annotations.NonDeletedClient;
import nl.ing.lovebird.clienttokens.annotations.VerifiedClientToken;
import nl.ing.lovebird.clienttokens.verification.ClientIdVerificationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Set;
import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/clients/{clientId}/client-mtls-dns")
public class ClientMTLSDistinguishedNameController {

    private static final String ASSISTANCE_PORTAL_YTS = "assistance-portal-yts";
    private static final String DEV_PORTAL = "dev-portal";

    private final DistinguishedNameService distinguishedNameService;
    private final ClientIdVerificationService clientIdVerificationService;

    /**
     * Get all allowed Distinguished Names for a given client.
     *
     * @param clientToken the client token of the client
     * @param clientId    the client id of the client
     * @return list with the status of all known Distinguished Names linked to the client.
     */
    @GetMapping(produces = APPLICATION_JSON_VALUE)
    public Set<DistinguishedNameDTO> list(
            @VerifiedClientToken(restrictedTo = {ASSISTANCE_PORTAL_YTS, DEV_PORTAL}) final ClientToken clientToken,
            @PathVariable UUID clientId
    ) {
        clientIdVerificationService.verify(clientToken, clientId);
        return distinguishedNameService.findAll(clientId);
    }

    /**
     * Create or re-add a distinguished name.
     *
     * @param clientToken             the client token of the client
     * @param clientId                the id of the client
     * @param newDistinguishedNameDTO the Distinguished Name to add
     * @return list with the (updated) internal state of each provided DN.
     */
    @NonDeletedClient
    @PostMapping(produces = APPLICATION_JSON_VALUE)
    public DistinguishedNameDTO create(
            @VerifiedClientToken(restrictedTo = {ASSISTANCE_PORTAL_YTS, DEV_PORTAL}) final ClientToken clientToken,
            @PathVariable final UUID clientId,
            @Valid @RequestBody final NewDistinguishedNameDTO newDistinguishedNameDTO
    ) {
        clientIdVerificationService.verify(clientToken, clientId);
        return distinguishedNameService.create(clientToken, newDistinguishedNameDTO);
    }

    /**
     * Mark distinguished names for deletion.
     *
     * @param clientToken   the client token for the client for which we want to mark the change applied.
     * @param clientId      the client id of the client for which we want to mark it as applied.
     * @param itemsToRemove the set of items to mark for deletion.
     * @return list with the (updated) internal state of each provided DNs that exists for this client.
     */
    @NonDeletedClient
    @PostMapping(value = "delete", produces = APPLICATION_JSON_VALUE)
    public Set<DistinguishedNameDTO> delete(
            @VerifiedClientToken(restrictedTo = {ASSISTANCE_PORTAL_YTS, DEV_PORTAL}) final ClientToken clientToken,
            @PathVariable final UUID clientId,
            @Valid @RequestBody final DistinguishedNameIdListDTO itemsToRemove
    ) {
        clientIdVerificationService.verify(clientToken, clientId);
        return distinguishedNameService.deleteLimited(clientToken, itemsToRemove);
    }

    /**
     * Mark changed as applied (only for APY).
     *
     * @param clientToken        the client token for the client for which we want to mark the change applied.
     * @param clientId           the client id of the client for which we want to mark it as applied.
     * @param itemsToMarkApplied the set of items to mark as applied.
     * @return list with the (updated) internal state of each provided DNs that exists for this client.
     */
    @PostMapping(value = "apply", produces = APPLICATION_JSON_VALUE)
    public Set<DistinguishedNameDTO> markApplied(
            @VerifiedClientToken(restrictedTo = {ASSISTANCE_PORTAL_YTS}) final ClientToken clientToken,
            @PathVariable final UUID clientId,
            @Valid @RequestBody final DistinguishedNameIdListDTO itemsToMarkApplied
    ) {
        clientIdVerificationService.verify(clientToken, clientId);
        return distinguishedNameService.markApplied(clientToken, itemsToMarkApplied);
    }

    /**
     * Mark pending additions as denied (only for APY).
     *
     * @param clientToken       the client token for the client for which we want to mark the change applied.
     * @param clientId          the client id of the client for which we want to mark it as applied.
     * @param itemsToMarkDenied the set of items to mark as rejected (should be in pending addition).
     * @return list with the (updated) internal state of each provided DNs that exists for this client.
     */
    @PostMapping(value = "deny", produces = APPLICATION_JSON_VALUE)
    public Set<DistinguishedNameDTO> markDenied(
            @VerifiedClientToken(restrictedTo = {ASSISTANCE_PORTAL_YTS}) final ClientToken clientToken,
            @PathVariable final UUID clientId,
            @Valid @RequestBody final DistinguishedNameIdListDTO itemsToMarkDenied
    ) {
        clientIdVerificationService.verify(clientToken, clientId);
        return distinguishedNameService.markDenied(clientToken, itemsToMarkDenied);
    }

}

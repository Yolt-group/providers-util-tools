package com.yolt.clients.clientgroup;

import com.yolt.clients.client.dto.ClientDTO;
import com.yolt.clients.client.dto.NewClientDTO;
import com.yolt.clients.client.validators.ValidKYCOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.clienttokens.ClientGroupToken;
import nl.ing.lovebird.clienttokens.annotations.VerifiedClientToken;
import nl.ing.lovebird.clienttokens.verification.ClientGroupIdVerificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/internal/client-groups/{clientGroupId}/clients")
public class ClientGroupClientsController {

    private static final String ASSISTANCE_PORTAL_YTS = "assistance-portal-yts";
    private static final String DEV_PORTAL = "dev-portal";

    private final ClientGroupService clientGroupService;

    private final ClientGroupIdVerificationService clientGroupIdVerificationService;

    @GetMapping(produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ClientDTO>> getClients(@PathVariable UUID clientGroupId,
                                                      @VerifiedClientToken(restrictedTo = ASSISTANCE_PORTAL_YTS) ClientGroupToken clientGroupToken) {
        clientGroupIdVerificationService.verify(clientGroupToken, clientGroupId);
        return new ResponseEntity<>(clientGroupService.getClients(clientGroupToken), HttpStatus.OK);
    }

    @PostMapping(consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<ClientDTO> addClient(@PathVariable UUID clientGroupId,
                                               @RequestBody @Valid @ValidKYCOptions NewClientDTO newClientDTO,
                                               @VerifiedClientToken(restrictedTo = {ASSISTANCE_PORTAL_YTS, DEV_PORTAL}) ClientGroupToken clientGroupToken) {
        clientGroupIdVerificationService.verify(clientGroupToken, clientGroupId);
        return new ResponseEntity<>(clientGroupService.addClient(clientGroupToken, newClientDTO), HttpStatus.CREATED);
    }
}

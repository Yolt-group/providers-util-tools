package com.yolt.clients.admins;

import com.yolt.clients.admins.dto.ConnectionDTO;
import com.yolt.clients.admins.dto.InvitationCodeDTO;
import com.yolt.clients.exceptions.InvitationCodeNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/internal/connect")
public class ConnectController {

    private final AdminInvitationService adminInvitationService;

    @PostMapping(produces = APPLICATION_JSON_VALUE)
    public ConnectionDTO connect(@RequestBody InvitationCodeDTO code) {
        return adminInvitationService.connect(code)
                .orElseThrow(() -> new InvitationCodeNotFoundException(code.getCode()));
    }
}

package com.yolt.clients.clientgroup.certificatemanagement.other;

import com.yolt.clients.clientgroup.certificatemanagement.CertificateService;
import com.yolt.clients.clientgroup.certificatemanagement.dto.CertificateDTO;
import com.yolt.clients.clientgroup.certificatemanagement.dto.CertificateType;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.clienttokens.ClientGroupToken;
import nl.ing.lovebird.clienttokens.annotations.VerifiedClientToken;
import nl.ing.lovebird.clienttokens.verification.ClientGroupIdVerificationService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/internal/client-groups/{clientGroupId}/other-certificates")
@Api(tags = "certificates")
public class OtherCertificatesController {
    private final ClientGroupIdVerificationService clientGroupIdVerificationService;
    private final CertificateService certificateService;

    @ApiOperation(value = "get all certificates for a client group", response = CertificateDTO.class, responseContainer = "List")
    @GetMapping
    public List<CertificateDTO> getAllCertificates(
            @PathVariable UUID clientGroupId,
            @VerifiedClientToken ClientGroupToken clientGroupToken
    ) {
        clientGroupIdVerificationService.verify(clientGroupToken, clientGroupId);
        return certificateService.getCertificatesByCertificateType(clientGroupToken.getClientGroupIdClaim(), CertificateType.OTHER);
    }

    @ApiOperation(value = "get a certificate", response = CertificateDTO.class)
    @GetMapping("{certificateId}")
    public CertificateDTO getCertificate(
            @PathVariable UUID clientGroupId,
            @PathVariable String certificateId,
            @VerifiedClientToken ClientGroupToken clientGroupToken
    ) {
        clientGroupIdVerificationService.verify(clientGroupToken, clientGroupId);
        return certificateService.getCertificateByIdAndType(clientGroupToken, CertificateType.OTHER, certificateId);
    }
}

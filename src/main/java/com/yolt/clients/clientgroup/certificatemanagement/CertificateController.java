package com.yolt.clients.clientgroup.certificatemanagement;

import com.yolt.clients.clientgroup.certificatemanagement.dto.CertificateDTO;
import com.yolt.clients.clientgroup.certificatemanagement.dto.CertificateUsageType;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.clienttokens.ClientGroupToken;
import nl.ing.lovebird.clienttokens.annotations.VerifiedClientToken;
import nl.ing.lovebird.clienttokens.verification.ClientGroupIdVerificationService;
import nl.ing.lovebird.providerdomain.ServiceType;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/internal/client-groups/{clientGroupId}/certificates")
@Api(tags = "certificates")
public class CertificateController {
    private final ClientGroupIdVerificationService clientGroupIdVerificationService;
    private final CertificateService certificateService;

    @ApiOperation(value = "Sends back a list of keypairs which are compatible with the requested information", response = CertificateDTO.class, responseContainer = "List")
    @GetMapping()
    public List<CertificateDTO> findCompatibleCertificates(
            @VerifiedClientToken ClientGroupToken clientGroupToken,
            @PathVariable UUID clientGroupId,
            @RequestParam("providerKey") @Pattern(regexp = "^[A-Z0-9_]+$") String providerKey,
            @RequestParam("serviceTypes") @NotEmpty Set<ServiceType> serviceTypes,
            @RequestParam("usageType") @NotNull CertificateUsageType usageType) {
        clientGroupIdVerificationService.verify(clientGroupToken, clientGroupId);
        return certificateService.findCompatibleCertificates(clientGroupToken, providerKey, serviceTypes, usageType);
    }

    @ApiOperation(value = "Deletes a given certificate")
    @DeleteMapping("{certificateId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCertificate(@VerifiedClientToken ClientGroupToken clientGroupToken,
                                  @PathVariable UUID clientGroupId,
                                  @PathVariable String certificateId) {
        clientGroupIdVerificationService.verify(clientGroupToken, clientGroupId);
        certificateService.deleteCertificate(clientGroupToken, certificateId);
    }


}

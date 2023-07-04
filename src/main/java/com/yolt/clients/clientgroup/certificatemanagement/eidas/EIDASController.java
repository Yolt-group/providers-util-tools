package com.yolt.clients.clientgroup.certificatemanagement.eidas;

import com.yolt.clients.clientgroup.certificatemanagement.KeyUtil;
import com.yolt.clients.clientgroup.certificatemanagement.dto.*;
import com.yolt.clients.clientgroup.certificatemanagement.dto.serviceinfo.ServiceInfo;
import com.yolt.clients.clientgroup.certificatemanagement.exceptions.CertificateValidationException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.clienttokens.ClientGroupToken;
import nl.ing.lovebird.clienttokens.annotations.VerifiedClientToken;
import nl.ing.lovebird.clienttokens.verification.ClientGroupIdVerificationService;
import nl.ing.lovebird.providerdomain.ServiceType;
import org.bouncycastle.util.encoders.Base64;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/internal/client-groups/{clientGroupId}/eidas-certificates")
@Api(tags = "certificates")
public class EIDASController {
    private final ClientGroupIdVerificationService clientGroupIdVerificationService;
    private final EIDASService eidasService;

    @ApiOperation(value = "Return the service map for eIDAS services. This is used to render the form in the client management portal for creating eIDAS keys.", response = Map.class)
    @GetMapping("services")
    public Map<ServiceType, ServiceInfo> getServices(@PathVariable UUID clientGroupId,
                                                     @VerifiedClientToken ClientGroupToken clientGroupToken) {
        clientGroupIdVerificationService.verify(clientGroupToken, clientGroupId);
        return EIDASHelper.getServiceInfoMap();
    }

    @ApiOperation(value = "creates a certificate signing request", response = CertificateDTO.class)
    @PostMapping
    public CertificateDTO createCertificateSigningRequest(
            @PathVariable UUID clientGroupId,
            @Valid @RequestBody EIDASCertificateSigningRequestDTO inputDTO,
            @VerifiedClientToken ClientGroupToken clientGroupToken
    ) {
        clientGroupIdVerificationService.verify(clientGroupToken, clientGroupId);
        return eidasService.createCertificateSigningRequest(clientGroupToken, inputDTO);
    }

    @ApiOperation(value = "get all certificates for a client group", response = CertificateDTO.class, responseContainer = "List")
    @GetMapping
    public List<CertificateDTO> getAllCertificates(
            @PathVariable UUID clientGroupId,
            @VerifiedClientToken ClientGroupToken clientGroupToken
    ) {
        clientGroupIdVerificationService.verify(clientGroupToken, clientGroupId);
        return eidasService.getCertificates(clientGroupToken);
    }

    @ApiOperation(value = "get a certificate", response = CertificateDTO.class)
    @GetMapping("{certificateId}")
    public CertificateDTO getCertificate(
            @PathVariable UUID clientGroupId,
            @PathVariable String certificateId,
            @VerifiedClientToken ClientGroupToken clientGroupToken
    ) {
        clientGroupIdVerificationService.verify(clientGroupToken, clientGroupId);
        return eidasService.getCertificate(clientGroupToken, certificateId);
    }

    @ApiOperation(value = "update the certificate name", response = CertificateDTO.class)
    @PutMapping("{certificateId}/name")
    public CertificateDTO updateCertificateName(
            @PathVariable UUID clientGroupId,
            @PathVariable String certificateId,
            @Valid @RequestBody NewCertificateNameDTO inputDTO,
            @VerifiedClientToken ClientGroupToken clientGroupToken
    ) {
        clientGroupIdVerificationService.verify(clientGroupToken, clientGroupId);
        return eidasService.updateName(clientGroupToken, certificateId, inputDTO);
    }

    @ApiOperation(value = "validate a certificate chain against the stored keys", response = CertificateDTO.class)
    @PostMapping("{certificateId}/validate-certificate-chain")
    public ValidatedCertificateChainDTO validateCertificateChain(
            @PathVariable UUID clientGroupId,
            @PathVariable String certificateId,
            @Valid @RequestBody CertificateChainDTO inputDTO,
            @VerifiedClientToken(restrictedTo = "dev-portal") ClientGroupToken clientGroupToken
    ) {
        clientGroupIdVerificationService.verify(clientGroupToken, clientGroupId);
        String certificate = new String(Base64.decode(inputDTO.getCertificateChain()));
        List<X509Certificate> certificates = KeyUtil.parseCertificateChain(certificate);
        return eidasService.verifyCertificateChain(clientGroupToken, certificateId, certificates);
    }

    @ApiOperation(value = "store signed certificate chain", response = CertificateDTO.class)
    @PutMapping("{certificateId}/certificate-chain")
    public List<CertificateInfoDTO> updateCertificateChain(
            @PathVariable UUID clientGroupId,
            @PathVariable String certificateId,
            @Valid @RequestBody CertificateChainDTO inputDTO,
            @VerifiedClientToken(restrictedTo = "dev-portal") ClientGroupToken clientGroupToken
    ) throws CertificateValidationException {
        clientGroupIdVerificationService.verify(clientGroupToken, clientGroupId);
        String certificate = new String(Base64.decode(inputDTO.getCertificateChain()));
        List<X509Certificate> certificates = KeyUtil.parseCertificateChain(certificate);
        return eidasService.updateCertificateChain(clientGroupToken, certificateId, certificates);
    }

    @ApiOperation(value = "Sign the certificate using yolt bank", response = CertificateDTO.class)
    @PostMapping("{certificateId}/sign")
    public CertificateChainDTO signCertificate(
            @PathVariable UUID clientGroupId,
            @PathVariable String certificateId,
            @VerifiedClientToken(restrictedTo = "dev-portal") ClientGroupToken clientGroupToken
    ) {
        clientGroupIdVerificationService.verify(clientGroupToken, clientGroupId);
        return new CertificateChainDTO(eidasService.sign(clientGroupToken, certificateId));
    }
}

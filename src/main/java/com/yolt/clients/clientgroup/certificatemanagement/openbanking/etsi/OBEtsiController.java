package com.yolt.clients.clientgroup.certificatemanagement.openbanking.etsi;

import com.yolt.clients.clientgroup.certificatemanagement.KeyUtil;
import com.yolt.clients.clientgroup.certificatemanagement.dto.*;
import com.yolt.clients.clientgroup.certificatemanagement.exceptions.CertificateValidationException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.clienttokens.ClientGroupToken;
import nl.ing.lovebird.clienttokens.annotations.VerifiedClientToken;
import nl.ing.lovebird.clienttokens.verification.ClientGroupIdVerificationService;
import org.bouncycastle.util.encoders.Base64;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/internal/client-groups/{clientGroupId}/open-banking-etsi-certificates")
@Api(tags = "certificates")
public class OBEtsiController {
    private final ClientGroupIdVerificationService clientGroupIdVerificationService;
    private final OBEtsiService obEtsiService;

    @ApiOperation(value = "creates a certificate signing request", response = CertificateDTO.class)
    @PostMapping
    public CertificateDTO createCertificateSigningRequest(
            @PathVariable UUID clientGroupId,
            @Valid @RequestBody OBEtsiCertificateSigningRequestDTO inputDTO,
            @VerifiedClientToken(restrictedTo = "dev-portal") ClientGroupToken clientGroupToken
    ) {
        clientGroupIdVerificationService.verify(clientGroupToken, clientGroupId);
        return obEtsiService.createCertificateSigningRequest(clientGroupToken, inputDTO);
    }

    @ApiOperation(value = "get all certificates for a client group", response = CertificateDTO.class, responseContainer = "List")
    @GetMapping
    public List<CertificateDTO> getAllCertificates(
            @PathVariable UUID clientGroupId,
            @VerifiedClientToken ClientGroupToken clientGroupToken
    ) {
        clientGroupIdVerificationService.verify(clientGroupToken, clientGroupId);
        return obEtsiService.getCertificates(clientGroupToken);
    }

    @ApiOperation(value = "get a certificate", response = CertificateDTO.class)
    @GetMapping("{certificateId}")
    public CertificateDTO getCertificate(
            @PathVariable UUID clientGroupId,
            @PathVariable String certificateId,
            @VerifiedClientToken ClientGroupToken clientGroupToken
    ) {
        clientGroupIdVerificationService.verify(clientGroupToken, clientGroupId);
        return obEtsiService.getCertificate(clientGroupToken, certificateId);
    }

    @ApiOperation(value = "update the certificate name", response = CertificateDTO.class)
    @PutMapping("{certificateId}/name")
    public CertificateDTO updateCertificateName(
            @PathVariable UUID clientGroupId,
            @PathVariable String certificateId,
            @Valid @RequestBody NewCertificateNameDTO inputDTO,
            @VerifiedClientToken(restrictedTo = "dev-portal") ClientGroupToken clientGroupToken
    ) {
        clientGroupIdVerificationService.verify(clientGroupToken, clientGroupId);
        return obEtsiService.updateName(clientGroupToken, certificateId, inputDTO);
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
        return obEtsiService.verifyCertificateChain(clientGroupToken, certificateId, certificates);
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
        return obEtsiService.updateCertificateChain(clientGroupToken, certificateId, certificates);
    }
}

package com.yolt.clients.clientgroup.certificatemanagement.eidas;

import com.yolt.clients.clientgroup.certificatemanagement.CertificateService;
import com.yolt.clients.clientgroup.certificatemanagement.aws.S3StorageClient;
import com.yolt.clients.clientgroup.certificatemanagement.dto.*;
import com.yolt.clients.clientgroup.certificatemanagement.exceptions.CertificateAlreadySignedException;
import com.yolt.clients.clientgroup.certificatemanagement.exceptions.CertificateValidationException;
import com.yolt.clients.clientgroup.certificatemanagement.yoltbank.YoltbankService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.clienttokens.ClientGroupToken;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EIDASService {
    private static final CertificateType CERTIFICATE_TYPE = CertificateType.EIDAS;
    private final EIDASValidationService eidasValidationService;
    private final CertificateService certificateService;
    private final S3StorageClient s3StorageClient;
    private final JWKSService jwksService;
    private final YoltbankService yoltbankService;

    public CertificateDTO createCertificateSigningRequest(ClientGroupToken clientGroupToken, CertificateSigningRequestDTO certificateSigningRequestDTO) {
        return certificateService.createCertificateSigningRequest(clientGroupToken, certificateSigningRequestDTO, CERTIFICATE_TYPE);
    }

    public List<CertificateDTO> getCertificates(ClientGroupToken clientGroupToken) {
        return certificateService.getCertificatesByCertificateType(clientGroupToken.getClientGroupIdClaim(), CERTIFICATE_TYPE);
    }

    public CertificateDTO getCertificate(ClientGroupToken clientGroupToken, String certificateId) {
        return certificateService.getCertificateByIdAndType(clientGroupToken, CERTIFICATE_TYPE, certificateId);
    }

    public CertificateDTO updateName(ClientGroupToken clientGroupToken, String certificateId, NewCertificateNameDTO newCertificateNameDTO) {
        return certificateService.updateCertificateNameForId(clientGroupToken, CERTIFICATE_TYPE, certificateId, newCertificateNameDTO);
    }

    public ValidatedCertificateChainDTO verifyCertificateChain(ClientGroupToken clientGroupToken, String certificateId, List<X509Certificate> certificateChain) {
        X509Certificate leafCertificate = certificateChain.get(0);
        try {
            eidasValidationService.validateCertificateChain(certificateChain);
            CertificateInfoDTO certificateInfoDTO = certificateService.validateCertificate(clientGroupToken, CERTIFICATE_TYPE, certificateId, leafCertificate);
            return new ValidatedCertificateChainDTO(true, null, certificateInfoDTO);
        } catch (CertificateValidationException e) {
            log.info("Certificate validation failed, possibly due to a technical error.", e);
            return new ValidatedCertificateChainDTO(false, e.getMessage(), null);
        }
    }

    public List<CertificateInfoDTO> updateCertificateChain(ClientGroupToken clientGroupToken, String certificateId, List<X509Certificate> certificateChain) throws CertificateValidationException {
        eidasValidationService.validateCertificateChain(certificateChain);
        List<CertificateInfoDTO> certificateChainInfo = certificateService.updateCertificateChainForId(clientGroupToken, CERTIFICATE_TYPE, certificateId, certificateChain);
        s3StorageClient.storeCertificate(certificateChain);
        jwksService.createJWKSJsonOnS3(clientGroupToken);
        return certificateChainInfo;
    }

    public String sign(ClientGroupToken clientGroupToken, String certificateId) {
        String csrPEM = Optional.of(certificateService.getCertificateByIdAndType(clientGroupToken, CERTIFICATE_TYPE, certificateId))
                .filter(cert -> ObjectUtils.isEmpty(cert.getSignedCertificateChain()))
                .map(CertificateDTO::getCertificateSigningRequest)
                .orElseThrow(CertificateAlreadySignedException::new);
        return yoltbankService.signCSR(csrPEM);
    }
}

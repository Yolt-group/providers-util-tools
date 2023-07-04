package com.yolt.clients.clientgroup.certificatemanagement.openbanking.etsi;

import com.yolt.clients.clientgroup.certificatemanagement.CertificateService;
import com.yolt.clients.clientgroup.certificatemanagement.dto.*;
import com.yolt.clients.clientgroup.certificatemanagement.exceptions.CertificateValidationException;
import com.yolt.clients.clientgroup.certificatemanagement.openbanking.OpenBankingValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.clienttokens.ClientGroupToken;
import org.springframework.stereotype.Service;

import java.security.cert.X509Certificate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OBEtsiService {
    private static final CertificateType CERTIFICATE_TYPE = CertificateType.OB_ETSI;
    private final CertificateService certificateService;
    private final OpenBankingValidationService validationService;

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
            validationService.validateEtsiCertificateChain(certificateChain);
            CertificateInfoDTO certificateInfoDTO = certificateService.validateCertificate(clientGroupToken, CERTIFICATE_TYPE, certificateId, leafCertificate);
            return new ValidatedCertificateChainDTO(true, null, certificateInfoDTO);
        } catch (CertificateValidationException e) {
            log.info("Certificate validation failed, possibly due to a technical error.", e);
            return new ValidatedCertificateChainDTO(false, e.getMessage(), null);
        }
    }

    public List<CertificateInfoDTO> updateCertificateChain(ClientGroupToken clientGroupToken, String certificateId, List<X509Certificate> certificateChain) throws CertificateValidationException {
        validationService.validateEtsiCertificateChain(certificateChain);
        return certificateService.updateCertificateChainForId(clientGroupToken, CERTIFICATE_TYPE, certificateId, certificateChain);
    }

}

package com.yolt.clients.clientgroup.certificatemanagement;

import com.yolt.clients.clientgroup.certificatemanagement.crypto.CryptoService;
import com.yolt.clients.clientgroup.certificatemanagement.dto.*;
import com.yolt.clients.clientgroup.certificatemanagement.dto.serviceinfo.KeyMaterialRequirements;
import com.yolt.clients.clientgroup.certificatemanagement.exceptions.*;
import com.yolt.clients.clientgroup.certificatemanagement.providers.ProvidersService;
import com.yolt.clients.clientgroup.certificatemanagement.providers.dto.ProviderInfo;
import com.yolt.clients.clientgroup.certificatemanagement.providers.exceptions.NoProviderInfoFoundException;
import com.yolt.clients.clientgroup.certificatemanagement.repository.Certificate;
import com.yolt.clients.clientgroup.certificatemanagement.repository.CertificateRepository;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.clienttokens.ClientGroupToken;
import nl.ing.lovebird.providerdomain.ServiceType;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringWriter;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CertificateService {

    private final CertificateRepository certificateRepository;
    private final CryptoService cryptoService;
    private final CertificateValidationService certificateValidationService;
    private final ProvidersService providersService;
    private final int certificatesLimit;

    public CertificateService(CertificateRepository certificateRepository,
                              CryptoService cryptoService,
                              CertificateValidationService certificateValidationService,
                              ProvidersService providersService,
                              @Value("${clients.certificates.limit}") int certificatesLimit) {
        this.certificateRepository = certificateRepository;
        this.cryptoService = cryptoService;
        this.certificateValidationService = certificateValidationService;
        this.providersService = providersService;
        this.certificatesLimit = certificatesLimit;
    }

    public CertificateDTO createCertificateSigningRequest(ClientGroupToken clientGroupToken, CertificateSigningRequestDTO inputDTO, CertificateType certificateType) {
        validateNameAvailable(clientGroupToken, inputDTO.getName());
        if (certificateRepository.countByClientGroupId(clientGroupToken.getClientGroupIdClaim()) >= certificatesLimit) {
            throw new TooManyCertificatesException(clientGroupToken.getClientGroupIdClaim());
        }

        String generatedKeyId = cryptoService.createPrivateKey(clientGroupToken, inputDTO.getKeyAlgorithm(), inputDTO.getUsageType());
        String certificateSigningRequest = cryptoService.createCSR(
                clientGroupToken,
                generatedKeyId,
                inputDTO.getUsageType(),
                inputDTO.getSignatureAlgorithm(),
                inputDTO.getSubjectDNs(),
                qcStatementsNeeded(certificateType),
                inputDTO.getServiceTypes(),
                inputDTO.getSubjectAlternativeNames()
        );

        Certificate certificate = certificateRepository.save(createCertificateEntity(
                certificateType,
                generatedKeyId,
                clientGroupToken,
                inputDTO,
                certificateSigningRequest
        ));

        return toDTO(certificate);
    }

    public List<CertificateDTO> getCertificatesByCertificateType(UUID clientGroupId, CertificateType certificateType) {
        return certificateRepository.findCertificatesByClientGroupIdAndCertificateType(clientGroupId, certificateType).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public CertificateDTO getCertificateByIdAndType(ClientGroupToken clientGroupToken, CertificateType certificateType, String certificateId) {
        return certificateRepository.findCertificateByClientGroupIdAndCertificateTypeAndKid(
                clientGroupToken.getClientGroupIdClaim(),
                certificateType,
                certificateId
        ).map(this::toDTO).orElseThrow(() -> new CertificateNotFoundException(
                String.format("Certificate not found for client-group: %s, certificateType: %s and kid: %s", clientGroupToken.getClientGroupIdClaim(), certificateType, certificateId)
        ));
    }

    public CertificateDTO updateCertificateNameForId(ClientGroupToken clientGroupToken, CertificateType certificateType, String certificateId, NewCertificateNameDTO newCertificateNameDTO) {
        Certificate certificate = getCertificate(clientGroupToken, certificateType, certificateId);

        if (certificate.getName().equals(newCertificateNameDTO.getName())) {
            return toDTO(certificate);
        }

        validateNameAvailable(clientGroupToken, newCertificateNameDTO.getName());

        certificate.setName(newCertificateNameDTO.getName());
        certificate = certificateRepository.save(certificate);

        return toDTO(certificate);
    }

    public CertificateInfoDTO validateCertificate(ClientGroupToken clientGroupToken, CertificateType certificateType, String certificateId, X509Certificate leafCertificate) throws CertificateValidationException {
        Certificate certificate = getCertificate(clientGroupToken, certificateType, certificateId);
        certificateValidationService.validateValidity(leafCertificate);
        certificateValidationService.validateCertificateWithPrivateKey(clientGroupToken, certificate.getCertificateUsageType(), certificateId, leafCertificate);
        return new CertificateInfoDTO(certificateId, leafCertificate.getSerialNumber(), leafCertificate.getSubjectDN().getName(), leafCertificate.getIssuerDN().getName(),
                leafCertificate.getNotBefore(), leafCertificate.getNotAfter(), getPublicKeyAlgorithm(leafCertificate.getPublicKey()), leafCertificate.getSigAlgName());
    }

    public List<CertificateInfoDTO> updateCertificateChainForId(ClientGroupToken clientGroupToken, CertificateType certificateType, String certificateId, List<X509Certificate> certificateChain) throws CertificateValidationException {
        validateCertificate(clientGroupToken, certificateType, certificateId, certificateChain.get(0));

        Certificate certificate = getCertificate(clientGroupToken, certificateType, certificateId);
        if (certificate.getSignedCertificateChain() != null) {
            throw new CertificateAlreadySignedException();
        }

        certificate.setSignedCertificateChain(writeCertificateChain(certificateChain));
        certificateRepository.save(certificate);

        return certificateChain.stream()
                .map(cert -> new CertificateInfoDTO(certificateId, cert.getSerialNumber(), cert.getSubjectDN().getName(), cert.getIssuerDN().getName(),
                        cert.getNotBefore(), cert.getNotAfter(), getPublicKeyAlgorithm(cert.getPublicKey()), cert.getSigAlgName()))
                .collect(Collectors.toList());
    }

    private void validateNameAvailable(ClientGroupToken clientGroupToken, String name) {
        if (certificateRepository.existsByClientGroupIdAndName(clientGroupToken.getClientGroupIdClaim(), name)) {
            throw new NameIsAlreadyUsedException();
        }
    }

    private boolean qcStatementsNeeded(CertificateType certificateType) {
        return switch (certificateType) {
            case EIDAS, OB_ETSI -> true;
            case OB_LEGACY, OTHER -> false;
        };
    }

    private Certificate createCertificateEntity(CertificateType certificateType, String generatedKeyId, ClientGroupToken clientGroupToken, CertificateSigningRequestDTO inputDTO, String certificateSigningRequest) {
        return new Certificate(
                certificateType,
                generatedKeyId,
                clientGroupToken.getClientGroupIdClaim(),
                inputDTO.getName(),
                inputDTO.getServiceTypes(),
                inputDTO.getUsageType(),
                inputDTO.getKeyAlgorithm(),
                inputDTO.getSignatureAlgorithm(),
                inputDTO.getSubjectAlternativeNames(),
                certificateSigningRequest,
                null
        );
    }

    private CertificateDTO toDTO(Certificate certificate) {
        return new CertificateDTO(
                certificate.getName(),
                certificate.getCertificateType(),
                certificate.getKid(),
                certificate.getCertificateUsageType(),
                certificate.getServiceTypes(),
                certificate.getKeyAlgorithm(),
                certificate.getSignatureAlgorithm(),
                certificate.getCertificateSigningRequest(),
                certificate.getSignedCertificateChain()
        );
    }

    private Certificate getCertificate(ClientGroupToken clientGroupToken, CertificateType certificateType, String certificateId) {
        return certificateRepository.findCertificateByClientGroupIdAndCertificateTypeAndKid(
                clientGroupToken.getClientGroupIdClaim(),
                certificateType,
                certificateId
        ).orElseThrow(() -> new CertificateNotFoundException(
                String.format("Certificate not found for client-group: %s, certificateType: %s and kid: %s", clientGroupToken.getClientGroupIdClaim(), certificateType, certificateId)
        ));
    }

    private String writeCertificateChain(List<X509Certificate> certificates) {
        StringWriter writer = new StringWriter();
        try (JcaPEMWriter pemWriter = new JcaPEMWriter(writer)) {
            for (X509Certificate x509Certificate : certificates) {
                pemWriter.writeObject(x509Certificate);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Error occurred during PEM writing", e);
        }
        return writer.toString();
    }

    private String getPublicKeyAlgorithm(PublicKey publicKey) {
        if (publicKey instanceof RSAPublicKey) {
            return publicKey.getAlgorithm() + ((RSAPublicKey) publicKey).getModulus().bitLength();
        } else if (publicKey instanceof DSAPublicKey) {
            return publicKey.getAlgorithm() + ((DSAPublicKey) publicKey).getY().bitLength();
        } else {
            return publicKey.getAlgorithm();
        }
    }

    public List<CertificateDTO> findCompatibleCertificates(ClientGroupToken clientGroupToken, String providerKey, Set<ServiceType> serviceTypes, CertificateUsageType usageType) {
        List<Certificate> certificates = certificateRepository.findCertificatesByClientGroupIdAndCertificateUsageType(clientGroupToken.getClientGroupIdClaim(), usageType);
        ProviderInfo providersInfo = providersService.getProviderInfo(providerKey);

        List<KeyMaterialRequirements> keyRequirements;
        try {
            keyRequirements = providersInfo.getKeyRequirements(serviceTypes, usageType);
        } catch (NoProviderInfoFoundException e) {
            log.info("Key requirements not found for the search query, providerKey: {}, serviceTypes: {}, usageType: {}", providerKey, serviceTypes, usageType, e);
            return List.of();
        }

        return certificates.stream()
                .filter(certificate -> isCompatibleWithKeyRequirements(certificate, keyRequirements))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private boolean isCompatibleWithKeyRequirements(Certificate certificate, List<KeyMaterialRequirements> keyRequirements) {
        return keyRequirements.stream().allMatch(keyMaterialRequirements ->
                keyMaterialRequirements.getKeyAlgorithms().contains(certificate.getKeyAlgorithm()) &&
                        keyMaterialRequirements.getSignatureAlgorithms().contains(certificate.getSignatureAlgorithm())
        );
    }

    public void deleteCertificate(ClientGroupToken clientGroupToken, String certificateId) {
        certificateRepository.findCertificateByClientGroupIdAndKid(clientGroupToken.getClientGroupIdClaim(), certificateId)
                .ifPresentOrElse(certificate -> {
                    cryptoService.deletePrivateKey(clientGroupToken, certificateId);
                    certificateRepository.delete(certificate);
                }, () -> {
                    throw new CertificateNotFoundException(
                            "Certificate not found for client-group: %s and certificateId: %s".formatted(clientGroupToken.getClientGroupIdClaim(), certificateId)
                    );
                });
    }
}

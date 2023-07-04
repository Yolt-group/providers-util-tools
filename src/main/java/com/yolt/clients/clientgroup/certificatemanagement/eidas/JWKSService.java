package com.yolt.clients.clientgroup.certificatemanagement.eidas;

import com.yolt.clients.clientgroup.certificatemanagement.KeyUtil;
import com.yolt.clients.clientgroup.certificatemanagement.aws.S3StorageClient;
import com.yolt.clients.clientgroup.certificatemanagement.dto.CertificateType;
import com.yolt.clients.clientgroup.certificatemanagement.dto.CertificateUsageType;
import com.yolt.clients.clientgroup.certificatemanagement.exceptions.CertificateValidationException;
import com.yolt.clients.clientgroup.certificatemanagement.repository.Certificate;
import com.yolt.clients.clientgroup.certificatemanagement.repository.CertificateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.clienttokens.ClientGroupToken;
import org.apache.commons.lang3.StringUtils;
import org.jose4j.jwk.JsonWebKeySet;
import org.jose4j.jwk.RsaJsonWebKey;
import org.springframework.stereotype.Service;

import java.security.cert.CertPathValidatorException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class JWKSService {
    private final CertificateRepository certificateRepository;
    private final S3StorageClient s3StorageClient;
    private final EIDASValidationService eidasValidationService;

    public void createJWKSJsonOnS3(final ClientGroupToken clientToken) {
        JsonWebKeySet jwks = fetchJWKSJson(clientToken);
        s3StorageClient.storeJWKS(clientToken, jwks);
    }

    private JsonWebKeySet fetchJWKSJson(final ClientGroupToken clientGroupToken) {
        List<Certificate> certificates = certificateRepository.findCertificatesByClientGroupIdAndCertificateType(clientGroupToken.getClientGroupIdClaim(), CertificateType.EIDAS);
        List<RsaJsonWebKey> rsaKeys = certificates
                .stream()
                .filter(signedCertificate -> StringUtils.isNotBlank(signedCertificate.getSignedCertificateChain()))
                .map(this::convertToRSAKey)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return new JsonWebKeySet(rsaKeys);
    }

    private RsaJsonWebKey convertToRSAKey(Certificate certificate) {
        try {
            List<X509Certificate> certificateChain = KeyUtil.parseCertificateChain(certificate.getSignedCertificateChain());
            if (!isValidCertPath(certificate, certificateChain)) {
                return null;
            }
            X509Certificate x509Certificate = certificateChain.get(0);
            RSAPublicKey rsaPublicKey = (RSAPublicKey) x509Certificate.getPublicKey();
            RsaJsonWebKey rsaJsonWebKey = new RsaJsonWebKey(rsaPublicKey);
            rsaJsonWebKey.setUse(mapKeyPairTypeToKeyUse(certificate.getCertificateUsageType()));
            rsaJsonWebKey.setAlgorithm("RS256");
            rsaJsonWebKey.setKeyId(certificate.getKid());
            rsaJsonWebKey.setCertificateChain(certificateChain);
            return rsaJsonWebKey;
        } catch (RuntimeException e) {
            log.warn("Problems parsing certificate, certificate={} in clientGroupId={}", certificate.getKid(), certificate.getClientGroupId(), e); //NOSHERIFF
        }
        return null;
    }

    private boolean isValidCertPath(Certificate certificate, List<X509Certificate> certificates) {
        try {
            eidasValidationService.validateCertificateChain(certificates);
            return true;
        } catch (CertificateValidationException e) {
            if (e.getCause() instanceof CertPathValidatorException) {
                log.info("Certificate does not have a valid chain, certificate={} in clientGroupId={}", certificate.getKid(), certificate.getClientGroupId(), e); //NOSHERIFF
            } else {
                log.warn("Certificate is incorrect, certificate={} in clientGroupId={}", certificate.getKid(), certificate.getClientGroupId(), e); //NOSHERIFF
            }
            return false;
        }
    }

    private String mapKeyPairTypeToKeyUse(CertificateUsageType certificateUsageType) {
        return switch (certificateUsageType) {
            case SIGNING -> "sig";
            case TRANSPORT -> "enc";
        };
    }
}

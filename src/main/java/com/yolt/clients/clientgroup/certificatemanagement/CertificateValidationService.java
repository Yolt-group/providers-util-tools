package com.yolt.clients.clientgroup.certificatemanagement;

import com.yolt.clients.clientgroup.certificatemanagement.crypto.CryptoService;
import com.yolt.clients.clientgroup.certificatemanagement.crypto.SignatureAlgorithm;
import com.yolt.clients.clientgroup.certificatemanagement.dto.CertificateUsageType;
import com.yolt.clients.clientgroup.certificatemanagement.exceptions.CertificateValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.clienttokens.ClientGroupToken;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.util.encoders.Base64;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.*;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.security.cert.CertPathValidatorException.BasicReason.UNSPECIFIED;

@Service
@RequiredArgsConstructor
@Slf4j
public class CertificateValidationService {
    private static final byte[] SECRET = "We walked in darkness, kept hittin' the walls".getBytes(StandardCharsets.UTF_8);
    private final CryptoService cryptoService;
    private final Clock clock;

    public void validateValidity(X509Certificate leafCertificate) throws CertificateValidationException {
        try {
            leafCertificate.checkValidity(Date.from(Instant.now(clock)));
        } catch (CertificateExpiredException | CertificateNotYetValidException e) {
            throw new CertificateValidationException("Certificate is not valid", e);
        }
    }

    public void validateCertificateWithPrivateKey(ClientGroupToken clientGroupToken, CertificateUsageType usageType, String certificateId, X509Certificate leafCertificate) throws CertificateValidationException {
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.SHA256_WITH_RSA;
        String encodedSignature = cryptoService.sign(clientGroupToken, usageType, certificateId, signatureAlgorithm, SECRET);
        try {
            Signature signature = Signature.getInstance(signatureAlgorithm.getAlgorithm(), "BC");
            signature.initVerify(leafCertificate.getPublicKey());
            signature.update(SECRET);
            if (!signature.verify(Base64.decode(encodedSignature))) {
                throw new CertificateValidationException("Verification of certificate failed.");
            }
        } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchProviderException | SignatureException e) {
            throw new CertificateValidationException("Unable to verify certificate", e);
        }
    }

    public void hasQCStatementsExtension(X509Certificate x509Certificate) throws CertificateValidationException {
        byte[] extensionValue = x509Certificate.getExtensionValue(Extension.qCStatements.getId());
        if (extensionValue == null) {
            throw new CertificateValidationException("Expected the qcStatements extension to be in the certificate");
        }
    }

    public void validateCertPath(X509Certificate[] trustedCAs, List<X509Certificate> certificateChain) throws CertificateValidationException {
        try {
            CertPath certPath = CertificateFactory.getInstance("X.509", "BC").generateCertPath(certificateChain);
            Set<TrustAnchor> trust = new HashSet<>();
            for (X509Certificate trustedCA : trustedCAs) {
                trust.add(new TrustAnchor(trustedCA, null));
            }
            CertPathValidator certPathValidator = CertPathValidator.getInstance("PKIX", "BC");
            PKIXParameters param = new PKIXParameters(trust);
            param.setRevocationEnabled(false);
            param.setDate(Date.from(Instant.now(clock)));

            certPathValidator.validate(certPath, param);
        } catch (CertPathValidatorException e) {
            if (e.getReason().equals(UNSPECIFIED)
                    && e.getMessage().equals("Trust anchor for certification path not found.")
                    && isRootCertificate(certificateChain.get(certificateChain.size() - 1))) {
                log.warn("Root certificate from chain is not in truststore", e);
                throw new CertificateValidationException("Certificate chain is not trusted", e);
            } else {
                throw new CertificateValidationException("Certificate chain is incorrect", e);
            }
        } catch (InvalidAlgorithmParameterException | NoSuchProviderException | NoSuchAlgorithmException | CertificateException e) {
            throw new IllegalStateException("Error occurred during certificate path validation", e);
        }
    }

    private boolean isRootCertificate(X509Certificate certificate) {
        try {
            certificate.verify(certificate.getPublicKey());
            return true;
        } catch (SignatureException e) {
            return false;
        } catch (CertificateException | NoSuchAlgorithmException | InvalidKeyException | NoSuchProviderException e) {
            throw new IllegalStateException("Error occurred during root certificate verification", e);
        }
    }
}

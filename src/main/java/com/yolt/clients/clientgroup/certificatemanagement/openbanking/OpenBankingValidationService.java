package com.yolt.clients.clientgroup.certificatemanagement.openbanking;

import com.yolt.clients.clientgroup.certificatemanagement.CertificateValidationService;
import com.yolt.clients.clientgroup.certificatemanagement.exceptions.CertificateValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import javax.security.auth.x500.X500Principal;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OpenBankingValidationService {
    private static final char[] TRUSTSTORE_WITH_OPENBANKING_PASS = "changeit".toCharArray();
    private static final String ALLOWED_C_VALUE = "GB";
    private static final String ALLOWED_O_VALUE = "OpenBanking";

    private final X509Certificate[] trustedCAs;
    private final CertificateValidationService certificateValidationService;

    public OpenBankingValidationService(
            @Value("${yolt.openbanking.validation.openbanking-ca-trustore:classpath:truststore-with-openbanking.jks}") String trustStoreLocation,
            CertificateValidationService certificateValidationService
    ) {
        this.certificateValidationService = certificateValidationService;
        this.trustedCAs = getTrustedCAs(trustStoreLocation);
    }

    public void validateLegacyCertificateChain(List<X509Certificate> certificateChain) throws CertificateValidationException {
        certificateValidationService.validateCertPath(trustedCAs, certificateChain);
        validateOBLegacySubject(certificateChain.get(0));
    }

    public void validateEtsiCertificateChain(List<X509Certificate> certificateChain) throws CertificateValidationException {
        certificateValidationService.validateCertPath(trustedCAs, certificateChain);
        certificateValidationService.hasQCStatementsExtension(certificateChain.get(0));
    }

    private static X509Certificate[] getTrustedCAs(String trustStoreLocation) {
        try {
            Resource truststoreWithOpenBanking = new DefaultResourceLoader().getResource(trustStoreLocation);
            KeyStore trustStore = KeyStore.getInstance(truststoreWithOpenBanking.getFile(), TRUSTSTORE_WITH_OPENBANKING_PASS);
            log.info("Setup OpenBanking validation service with truststore containing only Open Banking CA");

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStore);

            return Arrays.stream(trustManagerFactory.getTrustManagers())
                    .filter(X509TrustManager.class::isInstance)
                    .findAny()
                    .map(trustManager -> ((X509TrustManager) trustManager).getAcceptedIssuers())
                    .orElseThrow(() -> new IllegalStateException("Could not find any X509TrustManagers in the trustManagerFactory for algorithm " + TrustManagerFactory.getDefaultAlgorithm()));
        } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException e) {
            throw new IllegalStateException("Cannot resolve JVM truststore", e);
        }
    }

    private void validateOBLegacySubject(X509Certificate leafCertificate) throws CertificateValidationException {
        X500Principal subject = leafCertificate.getSubjectX500Principal();
        LdapName ln = getLdapName(subject);
        Map<String, Object> relativeDistinguishedNames = ln.getRdns().stream().collect(Collectors.toMap(Rdn::getType, Rdn::getValue));
        if (!ALLOWED_C_VALUE.equals(relativeDistinguishedNames.get("C"))) {
            throw new CertificateValidationException(String.format("Expected C to be %s for certificate", ALLOWED_C_VALUE));
        }
        if (!ALLOWED_O_VALUE.equals(relativeDistinguishedNames.get("O"))) {
            throw new CertificateValidationException(String.format("Expected O to be %s for certificate", ALLOWED_O_VALUE));
        }
    }

    private LdapName getLdapName(X500Principal subject) throws CertificateValidationException {
        try {
            return new LdapName(subject.getName());
        } catch (InvalidNameException e) {
            throw new CertificateValidationException("Unable to parse distinguished name when validating ob legacy certificate", e);
        }
    }
}

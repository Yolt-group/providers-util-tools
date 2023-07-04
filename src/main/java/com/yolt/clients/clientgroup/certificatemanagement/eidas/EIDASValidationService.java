package com.yolt.clients.clientgroup.certificatemanagement.eidas;

import com.yolt.clients.clientgroup.certificatemanagement.CertificateValidationService;
import com.yolt.clients.clientgroup.certificatemanagement.exceptions.CertificateValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class EIDASValidationService {
    private static final char[] TRUSTSTORE_WITH_YOLTBANK_PASS = "changeit".toCharArray();

    private final CertificateValidationService certificateValidationService;
    private final X509Certificate[] trustedCAs;

    public EIDASValidationService(
            @Value("${yolt.eidas.validation.yoltbank-ca-allowed:false}") boolean isYoltbankCAAllowed,
            @Value("${yolt.eidas.validation.yoltbank-ca-trustore:classpath:truststore-with-yoltbank.jks}") String trustStoreLocation,
            CertificateValidationService certificateValidationService
    ) {
        this.certificateValidationService = certificateValidationService;
        trustedCAs = getTrustedCAs(isYoltbankCAAllowed, trustStoreLocation);
    }

    private static X509Certificate[] getTrustedCAs(boolean isYoltbankCAAllowed, String trustStoreLocation) {
        try {
            KeyStore trustStore;
            if (isYoltbankCAAllowed) {
                Resource truststoreWithYoltbank = new DefaultResourceLoader().getResource(trustStoreLocation);
                trustStore = KeyStore.getInstance(truststoreWithYoltbank.getFile(), TRUSTSTORE_WITH_YOLTBANK_PASS);
                log.info("Setup EIDAS validation service with truststore containing only yoltbank's CA");
            } else {
                trustStore = null;
                log.info("Setup EIDAS validation service with JVM truststore");
            }
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

    public void validateCertificateChain(List<X509Certificate> certificateChain) throws CertificateValidationException {
        certificateValidationService.validateCertPath(trustedCAs, certificateChain);
        certificateValidationService.hasQCStatementsExtension(certificateChain.get(0));
    }
}

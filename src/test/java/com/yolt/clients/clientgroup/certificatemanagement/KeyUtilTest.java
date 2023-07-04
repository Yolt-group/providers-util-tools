package com.yolt.clients.clientgroup.certificatemanagement;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import java.security.cert.X509Certificate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KeyUtilTest {
    @Test
    void parseX509CertificateCorrectly() throws Exception {
        List<X509Certificate> certs = KeyUtil.parseCertificateChain(
                new String(this.getClass().getResourceAsStream("valid-sandbox-chain.pem").readAllBytes())
        );

        assertThat(certs.size()).isEqualTo(1);
        assertThat(certs.get(0).getIssuerDN().getName()).isEqualTo("CN=TppSandboxCertificate, O=ABC, ST=Some-State, C=NL");
        assertThat(certs.get(0).getSubjectDN().getName()).isEqualTo("CN=TppSandboxCertificate, O=ABC, ST=Some-State, C=NL");
        assertThat(certs.get(0).getNotBefore()).withDateFormat("yyyy-MM-dd'T'HH:mm:ssX").isEqualTo("2020-02-19T17:19:04+01:00");
        assertThat(certs.get(0).getNotAfter()).withDateFormat("yyyy-MM-dd'T'HH:mm:ssX").isEqualTo("2030-02-16T17:19:04+01:00");
        assertThat(certs.get(0).getSigAlgName()).isEqualTo("SHA256withRSA");
    }

    @Test
    void parseX509CertificateIncorrectly() throws Exception {
        String pem = new String(this.getClass().getResourceAsStream("certificate-signing-request.pem").readAllBytes());
        assertThrows(IllegalArgumentException.class, () -> KeyUtil.parseCertificateChain(pem));
    }

    @Test
    void writeToPem() throws Exception {
        String pem = new String(this.getClass().getResourceAsStream("valid-sandbox-chain.pem").readAllBytes());
        List<X509Certificate> certs = KeyUtil.parseCertificateChain(pem);

        String result = KeyUtil.writeToPem("CERTIFICATE", certs.get(0).getEncoded());

        assertThat(StringUtils.chomp(result)).isEqualToNormalizingNewlines(StringUtils.chomp(pem));
    }

    @Test
    void writeToPemChain() throws Exception {
        String pem = new String(this.getClass().getResourceAsStream("valid-yoltbank-chain.pem").readAllBytes());
        List<X509Certificate> certs = KeyUtil.parseCertificateChain(pem);

        String result = KeyUtil.writeToPemChain(certs);

        assertThat(StringUtils.chomp(result)).isEqualToNormalizingNewlines(StringUtils.chomp(pem));
    }
}
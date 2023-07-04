package com.yolt.clients.clientgroup.certificatemanagement;

import lombok.experimental.UtilityClass;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.LinkedList;
import java.util.List;

@UtilityClass
public class KeyUtil {
    public static List<X509Certificate> parseCertificateChain(String pemCertificateChain) {
        List<X509Certificate> certificateChain = new LinkedList<>();
        try (PEMParser pemParser = new PEMParser(new StringReader(pemCertificateChain))) {
            Object pemObject;
            while ((pemObject = pemParser.readObject()) != null) {
                if (!(pemObject instanceof X509CertificateHolder)) {
                    throw new IllegalArgumentException("Expected only certificate types");
                }
                X509CertificateHolder certificateHolder = (X509CertificateHolder) pemObject;
                X509Certificate certificate = new JcaX509CertificateConverter().getCertificate(certificateHolder);
                certificateChain.add(certificate);
            }
            return certificateChain;
        } catch (IOException | CertificateException e) {
            throw new IllegalStateException("Error occurred during PEM parsing", e);
        }
    }

    public static String writeToPem(String type, byte[] content) {
        PemObject pkPemObject = new PemObject(type, content);

        try (StringWriter output = new StringWriter(); PemWriter pemWriter = new PemWriter(output)) {
            pemWriter.writeObject(pkPemObject);
            pemWriter.flush();

            return String.valueOf(output.getBuffer());
        } catch (IOException e) {
            throw new IllegalStateException("Writing to PEM type failed, for type: " + type, e);
        }
    }

    public static String writeToPemChain(List<X509Certificate> certificates) {
        try (StringWriter output = new StringWriter(); PemWriter pemWriter = new PemWriter(output)) {
            for (Certificate certificate : certificates) {
                pemWriter.writeObject(new PemObject("CERTIFICATE", certificate.getEncoded()));
            }
            pemWriter.flush();

            return String.valueOf(output.getBuffer());
        } catch (Exception e) {
            throw new IllegalStateException("Writing to PEM chain failed", e);
        }
    }
}
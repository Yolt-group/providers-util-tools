package com.yolt.clients.clientgroup.certificatemanagement.aws;

import com.yolt.clients.clientgroup.certificatemanagement.KeyUtil;
import com.yolt.clients.clientgroup.certificatemanagement.exceptions.CertificateValidationException;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.clienttokens.AbstractClientToken;
import org.bouncycastle.util.Fingerprint;
import org.jose4j.jwk.JsonWebKeySet;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import javax.validation.constraints.NotNull;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.List;

@Service
@Slf4j
public class S3StorageClient {
    private final S3Client s3Client;
    private final S3Properties s3Properties;

    public S3StorageClient(@NotNull S3Properties s3Properties, @NotNull S3Client s3Client) {
        this.s3Properties = s3Properties;
        this.s3Client = s3Client;
    }

    public void storeCertificate(List<X509Certificate> x509Certificates) throws CertificateValidationException {
        try {
            X509Certificate leaf = x509Certificates.get(0);
            String leafPEM = KeyUtil.writeToPem("CERTIFICATE", leaf.getEncoded());
            Fingerprint fingerprint = new Fingerprint(leaf.getEncoded(), true);
            upload(leafPEM.getBytes(StandardCharsets.UTF_8), fingerprint + ".pem");
            upload(leaf.getEncoded(), fingerprint + ".der");

            if (x509Certificates.size() > 1) {
                String chainPEM = KeyUtil.writeToPemChain(x509Certificates);
                upload(chainPEM.getBytes(StandardCharsets.UTF_8), fingerprint + "-chain.pem");
            }

            log.info("Successfully saved certificate chain in S3");

        } catch (CertificateEncodingException e) {
            throw new CertificateValidationException("Certificate is not valid", e);
        }
    }

    public void storeJWKS(AbstractClientToken clientGroupIdClaim, JsonWebKeySet jwksJson) {
        String key = "jwks/" + clientGroupIdClaim.getClientGroupIdClaim() + "/keys";
        byte[] bytes = jwksJson.toJson().getBytes(StandardCharsets.UTF_8);
        upload(bytes, key, MediaType.APPLICATION_JSON_VALUE);
    }

    private void upload(byte[] object, String key) {
        upload(object, key, null);
    }

    private void upload(byte[] object, String key, String contentType) {
        PutObjectRequest.Builder builder = PutObjectRequest.builder()
                .bucket(s3Properties.getBucket());
        if (StringUtils.hasText(contentType)) {
            builder.contentType(contentType);
        }
        PutObjectRequest putObjectRequest = builder
                .key(key).build();
        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(object));
        log.info("Stored object in bucket {} with key {}", s3Properties.getBucket(), key); //NOSHERIFF
    }
}

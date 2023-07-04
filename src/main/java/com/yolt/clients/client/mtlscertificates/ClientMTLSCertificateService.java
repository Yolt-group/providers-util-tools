package com.yolt.clients.client.mtlscertificates;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.openssl.PEMParser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.StringReader;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClientMTLSCertificateService {

    private final ClientMTLSCertificateRepository clientMTLSCertificateRepository;

    public boolean hasClientMTLSCertificate(UUID clientId) {
        return clientMTLSCertificateRepository.existsByClientId(clientId);
    }

    public Page<ClientMTLSCertificate> findAllByClientId(UUID clientId, Pageable pageable) {
        return clientMTLSCertificateRepository.findAllByClientId(clientId, pageable);
    }

    public void addCertificate(
            UUID clientId,
            NewClientMTLSCertificateDTO newClientMTLSCertificateDTO
    ) {
        X509CertificateHolder pemObject = getClientCertificate(newClientMTLSCertificateDTO);

        String fingerprint = getFingerprint(pemObject);

        if (clientMTLSCertificateRepository.existsByClientIdAndFingerprint(clientId, fingerprint)) {
            throw new MTLSCertificateExistsException(clientId, fingerprint);
        }

        clientMTLSCertificateRepository.save(new ClientMTLSCertificate(
                clientId,
                fingerprint,
                pemObject.getSerialNumber(),
                StringUtils.substring(pemObject.getSubject().toString(), 0, 1024),
                StringUtils.substring(pemObject.getIssuer().toString(), 0, 1024),
                pemObject.getNotBefore().toInstant().atZone(ZoneOffset.UTC).toLocalDateTime(),
                pemObject.getNotAfter().toInstant().atZone(ZoneOffset.UTC).toLocalDateTime(),
                null,
                null,
                newClientMTLSCertificateDTO.getCertificateChain(),
                pemObject.getNotBefore().toInstant().atZone(ZoneOffset.UTC).toLocalDateTime()
        ));
    }

    @SneakyThrows
    private String getFingerprint(X509CertificateHolder pemObject) {
        byte[] derEncodedCert = pemObject.getEncoded();
        SHA1Digest digest = new SHA1Digest();
        digest.update(derEncodedCert, 0, derEncodedCert.length);
        byte[] sha1Hash = new byte[20];
        digest.doFinal(sha1Hash, 0);
        return new String(Hex.encodeHex(sha1Hash));
    }

    @SneakyThrows
    private X509CertificateHolder getClientCertificate(NewClientMTLSCertificateDTO newClientMTLSCertificateDTO) {
        PEMParser pemParser = new PEMParser(new StringReader(newClientMTLSCertificateDTO.getCertificateChain()));
        return (X509CertificateHolder) pemParser.readObject();
    }
}

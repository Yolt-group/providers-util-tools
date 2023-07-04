package com.yolt.clients.clientgroup.certificatemanagement.providers;

import com.yolt.clients.clientgroup.certificatemanagement.providers.dto.ProviderAuthenticationMeansBatch.AuthenticationMeans;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

import java.util.Set;
import java.util.UUID;

import static org.bouncycastle.util.encoders.Base64.toBase64String;

@UtilityClass
public class YoltProvider {
    public static String YOLT_PROVIDER = "YOLT_PROVIDER";
    public static UUID YOLT_TEST_BANK_SITE_ID = UUID.fromString("33aca8b9-281a-4259-8492-1b37706af6db");

    static Set<AuthenticationMeans> makeAISAuthenticationMeansForYoltProvider(
            @NonNull String transportPrivateKid,
            @NonNull String transportCert,
            @NonNull String signingPrivateKid,
            @NonNull String signingCert
    ) {
        return Set.of(
                // The client-id is not used by YOLTPROVIDER, but it's required anyway.  Same for client-secret.  Go figure.
                new AuthenticationMeans("client-id", toBase64String(new UUID(0, 0).toString().getBytes())),
                new AuthenticationMeans("client-secret", toBase64String(new UUID(0, 0).toString().getBytes())),

                // transport key/cert
                new AuthenticationMeans("client-transport-private-key-id", toBase64String(transportPrivateKid.getBytes())),
                new AuthenticationMeans("client-transport-certificate", toBase64String(transportCert.getBytes())),

                // signing key/cert
                new AuthenticationMeans("client-signing-private-key-id", toBase64String(signingPrivateKid.getBytes())),
                new AuthenticationMeans("client-signing-certificate", toBase64String(signingCert.getBytes()))
        );
    }

    static Set<AuthenticationMeans> makePISAuthenticationMeansForYoltProvider(
            @NonNull String signingPrivateKeyId,
            @NonNull String publicKeyId
    ) {
        return Set.of(
                // The client-id is not used by YOLTPROVIDER, but it's required anyway. Go figure.
                new AuthenticationMeans("client-id", toBase64String(new UUID(0, 0).toString().getBytes())),

                // signing key/cert
                new AuthenticationMeans("client-signing-private-keyid", toBase64String(signingPrivateKeyId.getBytes())),
                new AuthenticationMeans("client-public-keyid", toBase64String(publicKeyId.getBytes()))
        );
    }
}

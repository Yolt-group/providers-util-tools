package com.yolt.clients.config;

import com.yolt.service.starter.YoltAWSAutoConfiguration;
import com.yolt.service.starter.vault.YoltVaultCredentialsReader;
import lombok.Generated;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.http.YoltProxySelector;
import org.apache.http.impl.conn.SystemDefaultRoutePlanner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;

import java.beans.ConstructorProperties;
import java.nio.file.Path;
import java.util.Properties;

@Slf4j
@Configuration
public class AWSEmailConfiguration {

    @Bean
    @ConditionalOnProperty(value = "clients.amazon-ses.enabled", havingValue = "true")
    public SesClient amazonSimpleEmailService(@Value("${isp.proxy.host}") final String ispProxyHost,
                                              @Value("${isp.proxy.port}") final Integer ispProxyPort,
                                              @Value("${yolt.vault.aws.email:/vault/secrets/aws}") String vaultCredentialsFile) {

        SdkHttpClient httpClient = ApacheHttpClient.builder()
                .httpRoutePlanner(new SystemDefaultRoutePlanner(new YoltProxySelector(ispProxyHost, ispProxyPort)))
                .build();
        log.info("Configuring {} with credentials from: {}", YoltAWSAutoConfiguration.AWSCredentialsFromVaultProvider.class.getSimpleName(), vaultCredentialsFile);
        AwsCredentialsProvider awsCredentialsProvider = new AWSEmailConfiguration.AWSCredentialsFromVaultProvider(vaultCredentialsFile);

        return SesClient.builder()
                .region(Region.EU_CENTRAL_1)
                .credentialsProvider(awsCredentialsProvider)
                .httpClient(httpClient)
                .build();
    }

    public static class AWSCredentialsFromVaultProvider implements AwsCredentialsProvider {
        private final Path vaultCredentialsFile;

        public AwsCredentials resolveCredentials() {
            Properties credentials = YoltVaultCredentialsReader.readCredentials(this.vaultCredentialsFile);
            String accessKey = credentials.getProperty("aws_access_key_id");
            String secretKey = credentials.getProperty("aws_secret_access_key");
            String sessionToken = credentials.getProperty("aws_session_token");
            return AwsSessionCredentials.create(accessKey, secretKey, sessionToken);
        }

        @ConstructorProperties({"vaultCredentialsFile"})
        @Generated
        public AWSCredentialsFromVaultProvider(final String vaultCredentialsFile) {
            this.vaultCredentialsFile = Path.of(vaultCredentialsFile);
        }
    }
}

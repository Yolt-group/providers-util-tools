package com.yolt.clients.clientgroup.certificatemanagement.aws;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class S3Configuration {

    @Bean
    public S3Client s3Client(
            S3Properties s3Properties,
            AwsCredentialsProvider awsCredentialsFromVaultProvider
    ) {
        S3Client s3Client = S3Client.builder()
                .region(Region.of(s3Properties.getRegion()))
                .credentialsProvider(awsCredentialsFromVaultProvider).build();
        // Throws an exception if it can't find the bucket. The service will fail to boot.
        s3Client.headBucket(HeadBucketRequest.builder().bucket(s3Properties.getBucket()).build());
        return s3Client;
    }

    @Bean
    public S3StorageClient certificateStorageClient(
            S3Properties s3Properties,
            S3Client s3Client
    ) {
        return new S3StorageClient(s3Properties, s3Client);
    }
}

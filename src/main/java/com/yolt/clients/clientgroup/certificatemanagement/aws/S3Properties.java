package com.yolt.clients.clientgroup.certificatemanagement.aws;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;

@Validated
@Data
@Configuration
@ConfigurationProperties("s3properties")
public class S3Properties {
    private @NotNull String bucket;
    private @NotNull String region;
}

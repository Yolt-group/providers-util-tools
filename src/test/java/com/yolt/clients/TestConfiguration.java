package com.yolt.clients;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SendEmailResponse;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

@Configuration
@ComponentScan("nl.ing.lovebird.testsupport")
@Slf4j
public class TestConfiguration {

    private final static LocalDate LOCAL_DATE = LocalDate.of(2020, 4, 13);
    public static final Clock FIXED_CLOCK = Clock.fixed(LOCAL_DATE.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);

    @Primary
    @Bean
    public Clock fixedClock() {
        return FIXED_CLOCK;
    }

    @Primary
    @Bean
    public SesClient amazonSimpleEmailServiceLocal() {
        log.warn("Using local configuration for email service");
        return new SesClient() {

            @Override
            public SendEmailResponse sendEmail(SendEmailRequest sendEmailRequest) {
                log.info("Email request send - " + sendEmailRequest.toString());
                return SendEmailResponse.builder()
                        .messageId(UUID.randomUUID().toString())
                        .build();
            }

            @Override
            public String serviceName() {
                return "Local";
            }

            @Override
            public void close() {
                //no-op
            }
        };
    }
}

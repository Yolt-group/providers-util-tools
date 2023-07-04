package com.yolt.clients.config;

import org.springframework.boot.task.TaskExecutorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.Clock;

@Configuration
public class ApplicationConfiguration {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean(name = "syncClientsExecutor")
    public ThreadPoolTaskExecutor threadPoolTaskExecutor(TaskExecutorBuilder builder) {
        return builder.build();
    }
}

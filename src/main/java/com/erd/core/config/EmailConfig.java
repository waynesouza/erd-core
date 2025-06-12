package com.erd.core.config;

import feign.Request;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.openfeign.FeignClientsConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;

import java.time.Duration;

@Configuration
@EnableAsync
@EnableRetry
@Import(FeignClientsConfiguration.class)
public class EmailConfig {

    @Value("${emailService.timeout:5000}")
    private int timeout;

    @Bean
    public Request.Options feignRequestOptions() {
        return new Request.Options(
            Duration.ofMillis(timeout),  // connectTimeout
            Duration.ofMillis(timeout),  // readTimeout
            true                         // followRedirects
        );
    }

}

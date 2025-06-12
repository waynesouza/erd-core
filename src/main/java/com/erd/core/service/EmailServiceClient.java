package com.erd.core.service;

import com.erd.core.client.EmailServiceFeignClient;
import com.erd.core.dto.request.EmailRequestDTO;
import com.erd.core.dto.response.EmailResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import feign.FeignException;

import java.util.concurrent.CompletableFuture;

@Service
public class EmailServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceClient.class);

    private final EmailServiceFeignClient emailServiceFeignClient;

    public EmailServiceClient(EmailServiceFeignClient emailServiceFeignClient) {
        this.emailServiceFeignClient = emailServiceFeignClient;
    }

    @Async
    @Retryable(
        value = {FeignException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public CompletableFuture<Void> sendEmailAsync(String to, String subject, String template) {
        try {
            logger.info("Sending email to: {} via FeignClient", to);
            
            EmailRequestDTO request = new EmailRequestDTO(to, subject, template);
            
            EmailResponseDTO response = emailServiceFeignClient.sendEmail(request);
            
            logger.info("Email sent successfully to: {} with ID: {}", to, response != null ? response.getId() : "unknown");
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            logger.error("Failed to send email to: {}", to, e);
            return CompletableFuture.failedFuture(e);
        }
    }

}

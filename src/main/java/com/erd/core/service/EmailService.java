package com.erd.core.service;

import com.erd.core.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Service responsible for all email operations
 */
@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final EmailServiceClient emailServiceClient;
    private final EmailTemplateService emailTemplateService;
    private final String appUrl;

    public EmailService(EmailServiceClient emailServiceClient, 
                       EmailTemplateService emailTemplateService,
                       @Value("${app.url:http://localhost:4200}") String appUrl) {
        this.emailServiceClient = emailServiceClient;
        this.emailTemplateService = emailTemplateService;
        this.appUrl = appUrl;
    }

    /**
     * Send welcome email to new user
     */
    public void sendWelcomeEmail(User user) {
        try {
            logger.info("Sending welcome email to user: {}", user.getEmail());
            
            Map<String, String> variables = Map.of(
                "username", user.getFirstName() + " " + user.getLastName(),
                "email", user.getEmail(),
                "appUrl", appUrl
            );
            
            String template = emailTemplateService.getTemplate("welcome", variables);
            
            emailServiceClient.sendEmailAsync(
                user.getEmail(),
                "Welcome to ERD Collaborative Modeler!",
                template
            );
        } catch (Exception e) {
            logger.error("Failed to send welcome email to user: {}", user.getEmail(), e);
            // Don't throw exception to avoid failing user creation if email fails
        }
    }

    /**
     * Send password reset email
     */
    public void sendPasswordResetEmail(String email, String username, String resetToken) {
        try {
            logger.info("Sending password reset email to user: {}", email);
            
            String resetLink = appUrl + "/reset-password?token=" + resetToken;
            Map<String, String> variables = Map.of(
                "username", username,
                "resetLink", resetLink,
                "appUrl", appUrl
            );
            
            String template = emailTemplateService.getTemplate("password-reset", variables);
            
            emailServiceClient.sendEmailAsync(
                email,
                "Password Reset Request - ERD Collaborative Modeler",
                template
            );
        } catch (Exception e) {
            logger.error("Failed to send password reset email to user: {}", email, e);
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }

    /**
     * Send generic notification email
     */
    public void sendNotificationEmail(String email, String title, String message, String actionText, String actionUrl) {
        try {
            logger.info("Sending notification email to user: {}", email);
            
            Map<String, String> variables = Map.of(
                "title", title,
                "message", message,
                "actionText", actionText != null ? actionText : "",
                "actionUrl", actionUrl != null ? actionUrl : "#",
                "appUrl", appUrl
            );
            
            String template = emailTemplateService.getTemplate("notification", variables);
            
            emailServiceClient.sendEmailAsync(
                email,
                title + " - ERD Collaborative Modeler",
                template
            );
        } catch (Exception e) {
            logger.error("Failed to send notification email to user: {}", email, e);
            // Don't throw exception as this is usually not critical
        }
    }

    /**
     * Send custom email using any template
     */
    public void sendCustomEmail(String to, String subject, String templateName, Map<String, String> variables) {
        try {
            logger.info("Sending custom email using template '{}' to user: {}", templateName, to);
            
            String template = emailTemplateService.getTemplate(templateName, variables);
            emailServiceClient.sendEmailAsync(to, subject, template);
            
        } catch (Exception e) {
            logger.error("Failed to send custom email to user: {}", to, e);
            throw new RuntimeException("Failed to send custom email", e);
        }
    }

}

package com.erd.core.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class EmailTemplateService {

    private static final Logger logger = LoggerFactory.getLogger(EmailTemplateService.class);
    private static final String TEMPLATE_BASE_PATH = "templates/email/";

    /**
     * Generic method to load and process email templates
     * 
     * @param templateName Name of the HTML template file (without extension)
     * @param variables Map of placeholder variables to replace in template
     * @return Processed HTML template with variables replaced
     */
    public String getTemplate(String templateName, Map<String, String> variables) {
        try {
            String template = loadTemplateFromFile(templateName);
            return processTemplateVariables(template, variables);
        } catch (Exception e) {
            logger.error("Error loading template: {}", templateName, e);
            return generateFallbackTemplate(templateName, variables);
        }
    }

    private String loadTemplateFromFile(String templateName) throws IOException {
        String templatePath = TEMPLATE_BASE_PATH + ensureHtmlExtension(templateName);
        logger.debug("Loading email template: {}", templatePath);
        
        ClassPathResource resource = new ClassPathResource(templatePath);
        if (!resource.exists()) {
            throw new IOException("Template not found: " + templatePath);
        }
        
        try (InputStream inputStream = resource.getInputStream()) {
            String template = StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
            logger.debug("Successfully loaded template: {} (length: {})", templateName, template.length());
            return template;
        }
    }

    private String processTemplateVariables(String template, Map<String, String> variables) {
        if (variables == null || variables.isEmpty()) {
            return template;
        }

        String processedTemplate = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() != null ? entry.getValue() : "";
            processedTemplate = processedTemplate.replace(placeholder, value);
        }
        return processedTemplate;
    }

    private String ensureHtmlExtension(String templateName) {
        return templateName.endsWith(".html") ? templateName : templateName + ".html";
    }

    private String generateFallbackTemplate(String templateName, Map<String, String> variables) {
        logger.warn("Using fallback template for: {}", templateName);
        
        String title = variables.getOrDefault("title", "ERD Collaborative Modeler");
        String message = variables.getOrDefault("message", "Thank you for using our service!");
        
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <title>%s</title>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px; }
                    .container { background: white; border: 1px solid #ddd; border-radius: 10px; padding: 30px; }
                    .header { background: #667eea; color: white; padding: 20px; text-align: center; border-radius: 10px 10px 0 0; margin: -30px -30px 20px -30px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>%s</h1>
                    </div>
                    <p>%s</p>
                    <p>Best regards,<br><strong>ERD Collaborative Modeler Team</strong></p>
                </div>
            </body>
            </html>
            """.formatted(title, title, message);
    }

}

package com.erd.core.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;

import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

@Service
public class MailService {

    private static final Logger logger = LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    public MailService(JavaMailSender mailSender, SpringTemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    public void sendEmail(String to, Map<String, String> variables, String templateName) {
        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, UTF_8.name());
            helper.setFrom("mailtrap@demomailtrap.com");
            helper.setTo(to);
            helper.setSubject("Welcome to our platform!");
            helper.setText(getTemplate(variables, templateName), true);

            logger.info("Sending email to: {}", to);
            mailSender.send(message);
        } catch (Exception e) {
            logger.error("Error sending email", e);
        }
    }

    private String getTemplate(Map<String, String> variables, String templateName) {
        var context = new Context();
        context.setVariable("firstName", variables.get("firstName"));
        context.setVariable("lastName", variables.get("lastName"));

        return templateEngine.process(templateName, context);
    }

}

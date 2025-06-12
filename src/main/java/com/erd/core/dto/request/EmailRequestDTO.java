package com.erd.core.dto.request;

public class EmailRequestDTO {

    private String to;
    private String subject;
    private String template;

    public EmailRequestDTO() {
    }

    public EmailRequestDTO(String to, String subject, String template) {
        this.to = to;
        this.subject = subject;
        this.template = template;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

}

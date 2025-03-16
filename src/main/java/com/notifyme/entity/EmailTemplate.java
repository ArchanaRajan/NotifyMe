package com.notifyme.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "email_template")
public class EmailTemplate {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Template name is required")
    @Column(name = "template_name", nullable = false, unique = true)
    private String templateName;

    @NotBlank(message = "Subject template is required")
    @Column(name = "subject_template", nullable = false, columnDefinition = "TEXT")
    private String subjectTemplate;

    @NotBlank(message = "Body template is required")
    @Column(name = "body_template", nullable = false, columnDefinition = "TEXT")
    private String bodyTemplate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public String processTemplate(String template, Object... args) {
        String result = template;
        for (int i = 0; i < args.length; i += 2) {
            if (i + 1 < args.length) {
                String placeholder = String.valueOf(args[i]);
                String value = String.valueOf(args[i + 1]);
                result = result.replace(placeholder, value);
            }
        }
        return result;
    }

    public String getProcessedSubject(Object... args) {
        return processTemplate(subjectTemplate, args);
    }

    public String getProcessedBody(Object... args) {
        return processTemplate(bodyTemplate, args);
    }
} 
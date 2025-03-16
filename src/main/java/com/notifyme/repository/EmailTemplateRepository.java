package com.notifyme.repository;

import com.notifyme.entity.EmailTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, Long> {
    
    Optional<EmailTemplate> findByTemplateName(String templateName);
    
    boolean existsByTemplateName(String templateName);
} 
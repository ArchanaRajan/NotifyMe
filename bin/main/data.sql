-- Sample email templates
INSERT INTO email_template (template_name, subject_template, body_template, created_at, updated_at)
VALUES ('MOVIE_RELEASE', 
        'Movie Alert: ${movieName} is now available!',
        'Dear Movie Fan,\n\nGreat news! The movie "${movieName}" is now available for booking in ${location} on ${releaseDate}.\n\nDon''t miss out - book your tickets now!\n\nBest regards,\nNotifyMe Team',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP)
ON CONFLICT (template_name) DO NOTHING; 
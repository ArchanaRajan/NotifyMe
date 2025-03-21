# NotifyMe - Movie Ticket Availability Notifier

A Spring Boot application that notifies users when movie tickets become available for their preferred movies at specific locations.

## Features

- Register for movie ticket availability notifications
- Email notifications when tickets become available
- Rate limiting for email notifications
- Asynchronous processing
- Web scraping using jsoup for real-time ticket availability from multiple platforms:
  - BookMyShow (Implemented)
  - Paytm (Planned)
  - INOX (Planned)
  - PVR (Planned)
  - TicketNew (Planned)
  - Amazon (Planned)

## Prerequisites

- Java 17 or higher
- PostgreSQL
- Gradle
- Gmail account (for sending notifications)

## Configuration

1. Database Configuration:
   - Create a PostgreSQL database named 'notifyme'
   - Update database credentials in application.properties

2. Email Configuration:
   - Set up Gmail App Password
   - Update email credentials in application.properties

3. Web Scraping Configuration:
   - Scraping logs are stored in `logs/scraping/` directory
   - Each platform has its own log file
   - Logs are rotated daily
   - Error logs are separated from regular logs

## Building and Running

1. Clone the repository:
```bash
git clone https://github.com/yourusername/NotifyMe.git
cd NotifyMe
```

2. Build the project:
```bash
./gradlew build
```

3. Run the application:
```bash
./gradlew bootRun
```

## API Endpoints

### Register Notification
```http
POST /api/notifications/register
Content-Type: application/json

{
    "email": "user@example.com",
    "movieName": "Movie Name",
    "location": "City",
    "startDate": "2024-03-20",
    "endDate": "2024-03-25"
}
```

### Get Notifications
```http
GET /api/notifications?email=user@example.com
```

### Simulate Movie Release (Test Endpoint)
```http
POST /api/test/simulate-release
Content-Type: application/json

{
    "movieName": "Movie Name",
    "location": "City",
    "releaseDate": "2024-03-21"
}
```

## Logging

The application uses a comprehensive logging strategy:

1. Application Logs:
   - Location: `logs/application.log`
   - Contains general application logs
   - Rotated daily

2. Scraping Logs:
   - Location: `logs/scraping/`
   - Separate files for each platform
   - Format: JSON for easy parsing
   - Contains:
     - Scraping attempts
     - Success/failure status
     - Scraped data
     - Error details
     - Performance metrics

3. Error Logs:
   - Location: `logs/error/`
   - Contains detailed error information
   - Stack traces
   - Error context

## Contributing

1. Fork the repository
2. Create your feature branch
3. Commit your changes
4. Push to the branch
5. Create a new Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## MVP Overview
NotifyMe MVP provides a simple and efficient email notification service for movie releases. Users can register their movie preferences with location and date range, and receive timely email notifications when tickets become available.

## Technical Stack
- Java 17
- Spring Boot 3
- Gradle
- PostgreSQL
- Spring Data JPA
- Spring Mail
- Lombok
- Spring Web

## Features

### Core Functionality
- Movie notification registration
- Email notification service with retry mechanism
- Scheduled release checker
- Location-based filtering
- Date range preferences

### Advanced Features
- Rate limiting for Gmail SMTP (500 emails/day)
- Async email processing
- Email templating
- Notification status tracking
- Bulk notification queuing

### Database Schema

**NotificationRequest**
```sql
CREATE TABLE notification_request (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    movie_name VARCHAR(255) NOT NULL,
    location VARCHAR(255) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_notification_status ON notification_request(status);
CREATE INDEX idx_notification_dates ON notification_request(start_date, end_date);
```

**EmailTemplate**
```sql
CREATE TABLE email_template (
    id BIGSERIAL PRIMARY KEY,
    template_name VARCHAR(100) NOT NULL UNIQUE,
    subject_template TEXT NOT NULL,
    body_template TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

### API Endpoints

1. **Notification Management**
   - POST /api/v1/notifications/register
     ```json
     {
       "email": "user@example.com",
       "movieName": "Movie Title",
       "location": "City Name",
       "startDate": "2024-03-21",
       "endDate": "2024-04-21"
     }
     ```
   - GET /api/v1/notifications/{email}
   - PUT /api/v1/notifications/{id}/cancel

## Project Structure
```
notifyme/
├── src/
│   ├── main/
│   │   ├── java/com/notifyme/
│   │   │   ├── controller/
│   │   │   │   └── NotificationController.java
│   │   │   ├── dto/
│   │   │   │   ├── NotificationRequestDTO.java
│   │   │   │   └── NotificationResponseDTO.java
│   │   │   ├── entity/
│   │   │   │   ├── NotificationRequest.java
│   │   │   │   └── EmailTemplate.java
│   │   │   ├── repository/
│   │   │   │   ├── NotificationRepository.java
│   │   │   │   └── EmailTemplateRepository.java
│   │   │   ├── service/
│   │   │   │   ├── NotificationService.java
│   │   │   │   ├── EmailService.java
│   │   │   │   └── MovieReleaseService.java
│   │   │   ├── config/
│   │   │   │   ├── AsyncConfig.java
│   │   │   │   └── EmailConfig.java
│   │   │   ├── scheduler/
│   │   │   │   └── ReleaseCheckScheduler.java
│   │   │   └── util/
│   │   │       ├── EmailTemplateUtil.java
│   │   │       └── RateLimiter.java
│   │   └── resources/
│   │       ├── application.properties
│   │       └── templates/
│   │           ├── notification.html
│   │           └── reminder.html
│   └── test/
└── build.gradle
```

## Email Configuration

```properties
# Email Configuration (Gmail SMTP)
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=${GMAIL_USERNAME}
spring.mail.password=${GMAIL_APP_PASSWORD}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# Email Rate Limiting
email.rate.limit.per.day=500
email.rate.limit.per.hour=50

# Async Email Configuration
async.core-pool-size=2
async.max-pool-size=5
async.queue-capacity=500
```

## Implementation Phases

1. **Phase 1: Core Setup**
   - Project structure setup
   - Database configuration
   - Entity creation
   - Basic repository implementation

2. **Phase 2: Email Service**
   - Gmail SMTP configuration
   - Email template system
   - Rate limiting implementation
   - Retry mechanism
   - Async processing setup

3. **Phase 3: Notification System**
   - Registration endpoint
   - Movie release checker
   - Status tracking
   - Email queue management

## Getting Started

1. Clone the repository
2. Configure Gmail SMTP credentials in application.properties
3. Run the application using: `./gradlew bootRun`

## Security Considerations
- Email credentials stored as environment variables
- Rate limiting to prevent abuse
- Input validation for all endpoints
- SQL injection prevention through JPA
- XSS protection in email templates

## Performance Optimization
- Async email processing
- Database indexing
- Connection pooling
- Batch notification processing
- Rate limiting implementation

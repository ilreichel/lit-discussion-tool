# Literature Discussion Tool

An educational discussion platform for humanities-based literature and scientific textbooks. Supports classroom-based reading analysis using quotations and themes/tags.

## Features

- **Home Page**: Dashboard showing enrolled classes with login
- **Discussion Page**: Book and chapter-organized discussion posts with quote detection
- **Visualizations Page**: Interactive force-directed graph of theme/tag relationships
- **Themes Page**: Theme-filtered discussion posts

## Tech Stack

- **Backend**: Java 17, Spring Boot 3.2, JPA/Hibernate
- **Frontend**: HTML, CSS, JavaScript (D3.js for visualizations)
- **Database**: H2 (embedded, PostgreSQL-compatible schema)

## Quick Start

### Prerequisites

- Java 17+
- Maven 3.9+

### Run the Application

```bash
mvn spring-boot:run
```

The application will be available at **http://localhost:8080**

### Demo Access

The application launches with a preconfigured demo:
- **Username**: `student1`
- **Class**: Intro to English Lit
- **Book**: Brave New World by Aldous Huxley

Click "Login" with the pre-filled username to enter the experience.

## Project Structure

```
src/main/java/com/litdiscussion/
├── model/          # JPA entity classes
├── repository/     # Spring Data repositories
├── service/        # Business logic services
├── controller/     # REST API controllers
└── config/         # Data initialization

src/main/resources/
├── static/
│   ├── css/        # Stylesheets
│   └── js/         # Frontend JavaScript
│       └── pages/  # Page-specific modules
└── application.yml # Application configuration
```

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/login | Authenticate user |
| GET | /api/classes?username= | Get user's classes |
| GET | /api/classes/{id}/books | Get books for a class |
| GET | /api/books/{id}/posts | Get discussion posts |
| POST | /api/posts | Create discussion post |
| POST | /api/detect-quotes | Detect quotes in text |
| GET | /api/themes | List all themes |
| GET | /api/themes/{name}/posts | Get posts by theme |
| GET | /api/visualization | Get visualization graph data |
| GET | /api/books | List all books |

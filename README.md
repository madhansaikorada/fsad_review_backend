# SmartCity Backend

This is the backend for the SmartCity project, built with Spring Boot and MySQL.

## Prerequisites

- Java 17
- Maven
- MySQL

## Setup

1. Create a MySQL database named `smartcitydb`.
2. Update `src/main/resources/application.properties` with your MySQL credentials.
3. Run `mvn clean install` to build the project.
4. Run `mvn spring-boot:run` to start the server.

The server will run on http://localhost:8080.

## API Endpoints

- GET /api/users - Get all users
- POST /api/users - Create a user
- GET /api/services - Get all services
- POST /api/services - Create a service
- GET /api/feedback - Get all feedback
- POST /api/feedback - Create feedback
- GET /api/reports - Get all reports
- POST /api/reports - Create a report
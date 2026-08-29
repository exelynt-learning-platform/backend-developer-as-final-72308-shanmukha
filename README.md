# Resource Booking System

A production-ready RESTful Resource Booking System built with Spring Boot 3, JWT Authentication, and PostgreSQL.

## Project Overview

This backend assignment implements a secure Resource Booking System where:
- **ADMIN** users have full CRUD access to Resources and Reservations
- **USER** users can view Resources, create Reservations, and manage only their own Reservations

## Architecture

```
com.assignment.booking
├── config          # Security, Swagger, App configuration
├── controller      # REST API controllers
├── dto             # Request and Response DTOs
│   ├── request
│   └── response
├── entity          # JPA entities
├── enums           # Enumerations
├── exception       # Custom exceptions and global handler
├── mapper          # Entity-DTO mapping
├── repository      # Spring Data JPA repositories
├── security        # JWT provider, filter, UserDetailsService
├── service         # Business logic services
├── specification   # JPA Specifications for filtering
└── util            # Utility classes
```

## Tech Stack

- Java 17+
- Spring Boot 3.2.5
- Spring Security
- JWT Authentication (jjwt 0.12.5)
- Spring Data JPA / Hibernate
- PostgreSQL
- Maven
- Springdoc OpenAPI 2.5.0
- JUnit 5 + Mockito
- Lombok

## Setup Instructions

### Prerequisites

- Java 17 or higher
- Maven 3.8+
- PostgreSQL 14+
- IDE (IntelliJ IDEA recommended)

### 1. Clone the Repository

```bash
git clone https://github.com/exelynt-learning-platform/backend-developer-as-final-72308-shanmukha.git
cd backend-developer-as-final-72308-shanmukha
git checkout backend-developer-assignment-deadline-30th-sep-2026-64051-2721
```

### 2. Database Configuration

Create a PostgreSQL database:

```sql
CREATE DATABASE booking_db;
```

### 3. Environment Variables (Optional)

Create `application-local.yml` or set environment variables:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/booking_db
    username: postgres
    password: postgres
```

### 4. JWT Configuration

In `application.yml`:

```yaml
jwt:
  secret: Ym9va2luZy1zeXN0ZW0tc2VjcmV0LWtleS1mb3Itand0LXRva2VuLWdlbmVyYXRpb24tMjAyNA==
  expiration-ms: 86400000
```

### 5. Run the Application

```bash
mvn spring-boot:run
```

Or:

```bash
mvn clean install
java -jar target/resource-booking-system-1.0.0.jar
```

The application starts on port **8080** with context path `/api`.

## API Documentation Access

### Swagger UI

Open browser: http://localhost:8080/api/swagger-ui.html

### OpenAPI Docs

OpenAPI JSON: http://localhost:8080/api/api-docs

## API Endpoints

### Authentication

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | /api/auth/login | Login user | No |
| POST | /api/auth/register | Register user | No |

### Resources

| Method | Endpoint | Description | Role |
|--------|----------|-------------|------|
| GET | /api/resources | Get all resources | Public |
| GET | /api/resources/{id} | Get resource by ID | Public |
| POST | /api/resources | Create resource | ADMIN |
| PUT | /api/resources/{id} | Update resource | ADMIN |
| DELETE | /api/resources/{id} | Delete resource | ADMIN |

### Reservations

| Method | Endpoint | Description | Role |
|--------|----------|-------------|------|
| GET | /api/reservations | Get reservations | Authenticated |
| GET | /api/reservations/{id} | Get reservation by ID | Authenticated |
| POST | /api/reservations | Create reservation | Authenticated |
| PUT | /api/reservations/{id} | Update reservation | Authenticated |
| DELETE | /api/reservations/{id} | Delete reservation | Authenticated |

### Reservation Query Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| status | Enum | PENDING, CONFIRMED, CANCELLED |
| minPrice | BigDecimal | Minimum price filter |
| maxPrice | BigDecimal | Maximum price filter |
| page | int | Page number (default: 0) |
| size | int | Page size (default: 10) |
| sort | String | Sort field and direction (default: createdAt,desc) |

## Seed User Credentials

### ADMIN

- **Username:** `admin`
- **Password:** `Admin@123`

### USER

- **Username:** `user`
- **Password:** `User@123`

## Running the Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=AuthServiceTest

# Run integration tests
mvn test -Dtest=AuthControllerIntegrationTest
```

## Sample API Calls

### 1. Login as Admin

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "Admin@123"}'
```

### 2. Create a Resource (Admin)

```bash
curl -X POST http://localhost:8080/api/resources \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "name": "New Conference Room",
    "description": "A spacious conference room",
    "type": "ROOM",
    "pricePerUnit": 75.00,
    "location": "Building C",
    "capacity": "15 people"
  }'
```

### 3. Create a Reservation

```bash
curl -X POST http://localhost:8080/api/reservations \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "resourceId": 1,
    "startTime": "2026-09-15T10:00:00",
    "endTime": "2026-09-15T12:00:00",
    "price": 100.00
  }'
```

### 4. Filter Reservations

```bash
curl -X GET "http://localhost:8080/api/reservations?status=PENDING&minPrice=50&maxPrice=200&page=0&size=10&sort=price,asc" \
  -H "Authorization: Bearer <token>"
```

## Database Schema

### Tables

- **users** - User accounts
- **roles** - Role definitions (ROLE_USER, ROLE_ADMIN)
- **user_roles** - Many-to-many user-role mapping
- **resources** - Bookable resources
- **reservations** - Booking records

### Relationships

```
User (1) ---- (*) Reservation
Resource (1) ---- (*) Reservation
Role (1) ---- (*) User (Many-to-Many)
```

## Error Handling

All errors return consistent JSON format:

```json
{
  "success": false,
  "message": "Error description",
  "error": "Error type",
  "status": 404,
  "path": "/api/resources/999",
  "timestamp": 1724956800000
}
```

## Ownership Rules

- **USER** can only view/update/delete their own reservations
- **ADMIN** can access all reservations
- Unauthorized access returns HTTP 403 Forbidden
- User identity is always extracted from JWT token (never from request payload)

## Validation

Bean Validation annotations used:
- `@NotBlank` - Required string fields
- `@NotNull` - Required fields
- `@DecimalMin` - Price must be > 0
- `@Future` - Start time must be in the future
- Custom validation for time ranges (start < end)

## License

This project is created as a backend developer assignment submission.

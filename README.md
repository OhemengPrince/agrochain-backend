# AgroChain Backend API

## Overview
Spring Boot REST API for AgroChain — a mobile agricultural marketplace and traceability platform for Ghana connecting farmers, equipment owners, produce buyers and general users.

## Tech Stack
- Java 21 + Spring Boot 3.5
- Spring Security + JWT Authentication
- PostgreSQL Database
- Spring Data JPA + Hibernate
- JavaMail (OTP Email)
- Maven

## Prerequisites
- Java JDK 17 or higher
- PostgreSQL 14 or higher
- Maven 3.8+

## Setup & Run
1. Clone the repo
2. Create PostgreSQL database: `CREATE DATABASE agrochain_db`
3. Update `application.properties` with your credentials
4. Run: `mvn spring-boot:run`
5. API available at `http://localhost:8080`

## API Endpoints

### Authentication
- `POST /auth/register`
- `POST /auth/verify-otp`
- `POST /auth/login`
- `POST /auth/forgot-password`
- `POST /auth/reset-password`

### Users
- `GET /users/me`
- `PUT /users/me`

### Equipment (Sprint 2)
- `GET /equipment`
- `POST /equipment`
- `PUT /equipment/:id`
- `DELETE /equipment/:id`

### Bookings (Sprint 2)
- `POST /bookings`
- `GET /bookings/mine`
- `GET /bookings/incoming`

### Produce Traceability (Sprint 2)
- `POST /produce/batches`
- `GET /produce/batches/mine`
- `GET /produce/scan`

### Marketplace (Sprint 3)
- `GET /marketplace/listings`
- `POST /marketplace/listings`

### Chat (Sprint 3)
- WebSocket: `ws://server/ws`

### Payments (Sprint 3)
- `POST /payments/initiate`
- `POST /payments/verify`

## Team
- Prince Ohemeng — Frontend + Backend Lead
- Teammate 2 — Backend Sprint 2
- Teammate 3 — Backend Sprint 3
- Teammate 4 — Backend Sprint 3

## Environment Variables
- `spring.datasource.password` — PostgreSQL password
- `spring.mail.username` — Gmail address
- `spring.mail.password` — Gmail App Password
- `jwt.secret` — JWT signing secret

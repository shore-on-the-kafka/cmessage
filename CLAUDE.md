# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a messaging application built with a Spring Boot backend (Kotlin) and Flutter frontend, featuring LINE OAuth authentication and support for both direct messages and group chats.

## Development Commands

### Backend (Spring Boot with Kotlin)
- **Build**: `./gradlew build`
- **Run**: `./gradlew bootRun`
- **Test**: `./gradlew test`
- **Run single test**: `./gradlew test --tests "ClassName.methodName"`
- **Run integration tests**: `./gradlew test --tests "*IntegrationTest"`

### Frontend (Flutter)
- **Install dependencies**: `cd frontend && flutter pub get`
- **Run**: `cd frontend && flutter run`
- **Build**: `cd frontend && flutter build apk` (Android) or `cd frontend && flutter build ios` (iOS)
- **Test**: `cd frontend && flutter test`

### API Testing
- **Generate test token**: `./test-scripts/auth-test-token.sh <userId> <name>`
- **Send message**: `./test-scripts/send-user-message.sh <token> <receiverId> <content>`

## Architecture

### Backend Architecture
The backend follows Clean Architecture/Hexagonal Architecture principles:

- **Domain Layer**: Pure business entities (`User`, `Message`, `Group`)
- **Repository Layer**: Data access interfaces
- **Service Layer**: Business logic (`MessageService`, `AuthService`)
- **API Layer**: REST controllers with `/api/v1/` prefix
- **Infrastructure Layer**: In-memory repository implementations

### Key Technologies
- **Backend**: Spring Boot 3.4.2, Kotlin 2.1.20, Spring WebFlux (reactive), Spring Security, JWT
- **Frontend**: Flutter 3.8.1, Dart
- **Authentication**: LINE OAuth 2.0
- **Database**: Currently in-memory implementations
- **Testing**: JUnit 5, Mockito, Spring REST Docs, Fixture Monkey

### Domain Model
- **Message**: Central entity supporting both direct messages (`receiverId`) and group messages (`groupId`)
- **User**: Simple entity with `id` and `name`
- **Group**: Contains `id`, `name`, and `members` list
- All IDs are value objects wrapping `String` for type safety

## Code Conventions

### Backend
- Use suspend functions for async operations (Kotlin coroutines)
- Repository interfaces in `repository` package, implementations in `infra`
- Domain models are pure data classes with no framework dependencies
- Use sealed classes for API responses
- Follow Spring Boot naming conventions

### Frontend
- Use StatefulWidget for components with state
- Services handle API communication and token management
- Secure storage for authentication tokens
- Material Design components

## Configuration

### Backend Configuration
- Main config: `src/main/resources/application.yaml`
- OAuth config requires LINE client ID and secret
- JWT configuration in `SecurityConfig`
- Base URL configurable via `app.base-url`

### Frontend Configuration
- Dependencies in `pubspec.yaml`
- Platform-specific configs in `android/` and `ios/` directories

## Testing

### Backend Testing
- Unit tests for repositories and services
- Integration tests for full API flows
- Test utilities in `testutil` package
- Spring REST Docs for API documentation generation

### Test Data
- Use Fixture Monkey for test data generation
- Mock external services (LINE OAuth) in tests
- Integration tests use real Spring context

## API Structure

### Authentication
- `GET /api/v1/auth/login/line` - Initiate LINE OAuth
- `GET /api/v1/auth/login/oauth2/line` - OAuth callback
- `GET /api/v1/auth/test/token` - Generate test token (development)

### Messages
- `POST /api/v1/messages` - Send direct message
- `POST /api/v1/messages/group` - Send group message
- `GET /api/v1/messages/users/{userId}` - Get user's messages

### Groups
- `POST /api/v1/groups` - Create group
- `POST /api/v1/groups/{groupId}/members` - Add members

## Development Notes

- Backend runs on port 8080 by default
- Frontend connects to localhost:8080 in development
- All API endpoints require JWT authorization except auth endpoints
- Use `Bearer <token>` header for authentication
- Message service combines direct and group messages for user queries
- Groups are simple collections of users without advanced permissions
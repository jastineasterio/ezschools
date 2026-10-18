# EduManage Platform

Production-grade Multi-School SaaS School Management Platform.

## Technology Stack

### Backend
- Java 17
- Spring Boot 3.4.5
- Spring Security
- Spring Data JPA / Hibernate
- PostgreSQL
- Flyway
- JWT (jjwt)
- OpenAPI (springdoc)
- Lombok
- MapStruct

### Frontend
- React 18
- TypeScript
- Vite
- Tailwind CSS
- shadcn/ui-style components
- React Router v6
- TanStack Query
- React Hook Form + Zod
- Axios
- Recharts
- Lucide React

## Project Structure

```
multi-school-saas/
├── backend/
│   ├── pom.xml
│   ├── .env.example
│   └── src/
│       ├── main/
│       │   ├── java/com/edumanage/
│       │   │   ├── config/
│       │   │   ├── security/
│       │   │   ├── common/
│       │   │   ├── exception/
│       │   │   ├── auth/
│       │   │   ├── tenant/
│       │   │   ├── users/
│       │   │   ├── roles/
│       │   │   ├── permissions/
│       │   │   ├── packages/
│       │   │   ├── subscriptions/
│       │   │   ├── schools/
│       │   │   ├── branding/
│       │   │   ├── audit/
│       │   │   └── platform/
│       │   └── resources/
│       │       ├── application.yml
│       │       ├── application-dev.yml
│       │       └── db/migration/
│       └── test/
├── frontend/
│   ├── package.json
│   ├── .env.example
│   ├── vite.config.ts
│   ├── tailwind.config.ts
│   └── src/
│       ├── main.tsx
│       ├── App.tsx
│       ├── index.css
│       ├── types/
│       ├── services/
│       ├── store/
│       ├── layouts/
│       ├── components/
│       ├── pages/
│       ├── hooks/
│       └── lib/
└── README.md
```

## Prerequisites

- Java 17+
- Node.js 18+
- PostgreSQL 14+
- Maven 3.8+

## Setup

### 1. Database Setup

```bash
# Create PostgreSQL database
psql -U postgres -c "CREATE DATABASE edumanage;"
psql -U postgres -c "CREATE USER edumanage WITH PASSWORD 'edumanage';"
psql -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE edumanage TO edumanage;"
```

### 2. Backend Setup

```bash
cd backend

# Copy environment file
cp .env.example .env

# Run Flyway migrations and start Spring Boot
mvn spring-boot:run
```

### 3. Frontend Setup

```bash
cd frontend

# Install dependencies
npm install

# Start development server
npm run dev
```

## Default Credentials

| Role | Email | Password |
|------|-------|----------|
| Super Admin | admin@edumanage.com | Admin@123 |

## Architecture

### Multi-Tenancy
- Shared database with `school_id` isolation.
- Current tenant derived from authenticated user's `school_id`.
- Super Admin has no school restriction.

### RBAC
- Users → Roles → Permissions
- Granular permission system (e.g., `schools.view`, `packages.manage`).
- Method-level authorization with `@PreAuthorize`.

### Packages
- Data-driven package system with configurable roles, features, and limits.
- Default packages: Economy, Classic, Premium.
- Custom packages supported.

### Branding
- Each school has customizable theme (primary, secondary, accent, background, text colors, logo).
- Frontend applies theme dynamically via CSS custom properties.

## API Documentation

Once backend is running, visit:
- Swagger UI: http://localhost:8080/api/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/api/v3/api-docs

## Development

### Backend
```bash
cd backend
mvn clean install
mvn spring-boot:run
```

### Frontend
```bash
cd frontend
npm run dev      # Development
npm run build    # Production build
npm run lint     # Lint
```

## Testing

```bash
cd backend
mvn test

cd frontend
npm run build
```

## Security

- Passwords hashed with BCrypt.
- JWT-based stateless authentication.
- Short-lived access tokens + refresh tokens.
- Tenant isolation enforced at service/repository layer.
- No secrets in frontend or source code.
- Audit logging for sensitive actions.

## License

Proprietary

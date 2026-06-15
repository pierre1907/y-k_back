# Y&K Platform — Backend API

API REST multi-tenant pour la plateforme Y&K, développée avec Spring Boot 4 / Java 21 et PostgreSQL.

## Stack technique

| Brique | Choix |
|--------|-------|
| Framework | Spring Boot 4.0 |
| Langage | Java 21 |
| Base de données | PostgreSQL 16+ |
| ORM | Spring Data JPA / Hibernate |
| Migrations | Flyway |
| Sécurité | Spring Security + JWT (jjwt 0.12) |
| Documentation | SpringDoc OpenAPI 3 (Swagger UI) |
| Build | Maven 3.9 |

## Architecture multi-tenant

Le modèle de données distingue deux niveaux d'utilisateurs :
- **tenant_users** — administrateurs d'un tenant (opérateur de la plateforme)
- **merchant_users** — utilisateurs d'un e-commerçant rattaché à un tenant

Chaque requête authentifiée passe par un filtre JWT qui extrait et valide `tenant_id` / `merchant_id` pour scopper automatiquement les accès aux données.

## Démarrage rapide

### Prérequis
- Java 21+
- Maven 3.9+
- PostgreSQL 16+

### Configuration

```bash
cp .env.example .env
# Remplir les valeurs dans .env
```

Créer la base de données :
```sql
CREATE DATABASE yk_db;
```

### Lancer en développement

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

L'API est disponible sur `http://localhost:8080/api`
Swagger UI : `http://localhost:8080/api/swagger-ui.html`

### Tests

```bash
./mvnw test
```

### Build production

```bash
./mvnw clean package -DskipTests
java -jar target/yk-back-0.0.1-SNAPSHOT.jar
```

## Structure du projet

```
src/
├── main/
│   ├── java/com/yk/back/
│   │   ├── config/        # SecurityConfig, OpenApiConfig, CorsConfig
│   │   ├── controller/    # REST controllers (@RestController)
│   │   ├── dto/           # Request / Response DTOs
│   │   ├── entity/        # Entités JPA
│   │   ├── exception/     # Exceptions métier + GlobalExceptionHandler
│   │   ├── filter/        # JwtAuthenticationFilter, TenantContextFilter
│   │   ├── repository/    # Interfaces JPA repositories
│   │   ├── service/       # Logique métier
│   │   └── util/          # JwtUtil, etc.
│   └── resources/
│       ├── db/migration/  # Scripts Flyway (V1__, V2__, ...)
│       ├── application.yml
│       └── application-dev.yml
└── test/
```

## Branches

| Branche | Usage |
|---------|-------|
| `main` | Production — protégée, merge via PR uniquement |
| `develop` | Intégration continue |
| `deploy-test` | Staging / tests de déploiement |
| `feature/*` | Nouvelles fonctionnalités |
| `fix/*` | Corrections de bugs |

## Licence

MIT — voir [LICENSE](LICENSE)

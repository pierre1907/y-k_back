# CLAUDE.md — y-k_back

## Contexte

Backend API REST de la plateforme Y&K. Architecture **multi-tenant** : chaque requête est scopée par `tenant_id` et optionnellement par `merchant_id`. Spring Boot 4 / Java 21 / PostgreSQL.

## Commandes essentielles

```bash
# Dev
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Tests
./mvnw test

# Build
./mvnw clean package -DskipTests

# Vérifier la compilation
./mvnw compile
```

## Architecture

### Modèle multi-tenant
- `tenants` → opérateurs de la plateforme
- `merchants` → e-commerçants rattachés à un tenant
- `tenant_users` → admins d'un tenant (rôles : `TENANT_ADMIN`, `TENANT_USER`)
- `merchant_users` → users d'un merchant (rôles : `MERCHANT_ADMIN`, `MERCHANT_USER`)

### Filtrage des données
Toujours filtrer par `tenant_id` dans les repositories. Ne jamais retourner de données cross-tenant.

### JWT
Le token contient : `userId`, `tenantId`, `merchantId` (nullable), `role`.
Le filtre `JwtAuthenticationFilter` valide le token et injecte ces claims dans le `SecurityContext`.

## Conventions de code

- DTOs séparés pour Request et Response (jamais exposer les entités JPA directement)
- `@Valid` sur tous les DTOs en entrée controller
- Exceptions métier dans `exception/` — remontées par `GlobalExceptionHandler`
- Migrations Flyway séquentielles : `V{n}__{description}.sql`
- Pas de logique métier dans les controllers — tout passe par les services

## Migrations DB

Ajouter un fichier `V{n+1}__description.sql` dans `src/main/resources/db/migration/`.
Ne jamais modifier un fichier de migration existant déjà appliqué.

## Variables d'environnement

Voir `.env.example`. Les valeurs sensibles (JWT_SECRET, DB_PASSWORD) ne doivent jamais être committées.

## Branches

- `main` : protégée — merge via PR depuis `develop` uniquement
- `develop` : branche d'intégration principale
- `deploy-test` : staging
- `feature/*` / `fix/*` : branches de travail

# Keycloak production deployment

This stack replaces the disposable `start-dev`/H2 setup with:

- Keycloak 26.4 running `start --optimized`;
- PostgreSQL with a persistent Docker volume;
- HTTPS termination and automatic certificates through Caddy;
- readiness health checks and restart policies;
- realm import support for migration from the current H2 container.

## Prerequisites

1. Create a public DNS record such as `auth.example.com` pointing to the deployment host.
2. Allow inbound TCP ports 80 and 443. Caddy uses them to obtain and renew TLS certificates.
3. Install Docker Compose v2.
4. Keep the current `keycloak` container until the new deployment has passed all checks.

## 1. Export the current DEV/H2 realm

The current container has no mounted volume, so deleting it before export permanently loses its realm and users.

From PowerShell:

```powershell
Set-Location infrastructure/keycloak
./Export-KeycloakRealm.ps1 -ContainerName keycloak -Realm healthcare
```

The script stops the old container briefly, copies a consistent H2 snapshot, starts the old container again, and performs the export against the copy. It never removes the old container.

Copy the exported JSON file(s) from `backups/<timestamp>/export` into `import/`. Both directories are ignored by Git because realm exports may contain users and password hashes.

## 2. Configure production secrets

```powershell
Copy-Item .env.example .env
```

Edit `.env` and set:

- the real public hostname;
- a unique PostgreSQL password of at least 32 random characters;
- a unique bootstrap administrator password of at least 32 random characters.

Do not reuse the existing development administrator password. The `.env` file is ignored by Git. In Kubernetes or another orchestrator, inject the same values through its secret manager instead.

## 3. Start the production stack

```powershell
docker compose build --pull
docker compose up -d
docker compose ps
```

Keycloak is not published over plain HTTP. Only Caddy publishes ports 80/443; traffic between Caddy and Keycloak stays on the private Docker network.

## 4. Point applications to the HTTPS issuer

Set these environment variables when starting the services:

```text
KEYCLOAK_ISSUER_URI=https://auth.example.com/realms/healthcare
KEYCLOAK_SERVER_URL=https://auth.example.com
```

For the frontend production build:

```text
VITE_KEYCLOAK_URL=https://auth.example.com
VITE_KEYCLOAK_REALM=healthcare
VITE_KEYCLOAK_CLIENT_ID=healthcare-app
```

Update the Keycloak client's valid redirect URIs and web origins to the production frontend HTTPS URL. If Google login is enabled, update the Google OAuth redirect URI to:

```text
https://auth.example.com/realms/healthcare/broker/google/endpoint
```

## 5. Verify before cutover

1. Open `https://<hostname>/realms/healthcare/.well-known/openid-configuration`.
2. Confirm the returned issuer is the same HTTPS URL configured in the services.
3. Log in as one PATIENT, DOCTOR and ADMIN account.
4. Confirm realm roles are present in `realm_access.roles`.
5. Restart the stack and confirm the same users still exist:

```powershell
docker compose restart
```

6. Test creating a fresh database from the exported realm before treating the migration as complete.

Only after these checks pass should the old H2 container be archived or removed.

## Database backup

Realm export is useful for migration, but PostgreSQL backup is the normal production backup:

```powershell
docker compose exec -T keycloak-db pg_dump -U keycloak -d keycloak -Fc -f /tmp/keycloak.dump
docker compose cp keycloak-db:/tmp/keycloak.dump ./backups/keycloak.dump
```

Store backups outside the deployment host and periodically test restore into a separate database.

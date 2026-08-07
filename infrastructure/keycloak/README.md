# Keycloak production deployment

## Realm config in the repo (`realm-config/healthcare-realm.json`)

A committed, reproducible snapshot of the `healthcare` realm — roles, clients,
identity providers, authentication flows, and the **`MediBook`** brand
(`displayName` / `displayNameHtml`, so the login page reads "Sign in to MediBook").

It is exported **without users** and with **secrets scrubbed** to `${env.*}`
placeholders, so it is safe to commit (unlike `import/` and `backups/`, which are
gitignored because they carry users and password hashes). Placeholders present:

| Placeholder                        | Where                          | Set via                          |
| ---------------------------------- | ------------------------------ | -------------------------------- |
| `${env.GOOGLE_CLIENT_ID}`          | Google identity provider       | `.env` → `GOOGLE_CLIENT_ID`      |
| `${env.GOOGLE_CLIENT_SECRET}`      | Google identity provider       | `.env` → `GOOGLE_CLIENT_SECRET`  |
| `${env.KEYCLOAK_ADMIN_CLIENT_SECRET}` | `healthcare-admin-cli` client | `.env` → `KEYCLOAK_ADMIN_CLIENT_SECRET` |

To import into a **fresh** realm (Keycloak imports only when the realm does not
already exist): copy the file into `import/` and start the stack, then set the
three secrets in **Admin Console** (Identity Providers → google, and Clients →
healthcare-admin-cli → Credentials). Regenerate any secret that was ever real.

```powershell
Copy-Item realm-config/healthcare-realm.json import/healthcare-realm.json
docker compose --profile cloudflare up -d   # command already has --import-realm
```

It also embeds the **service-account users** (`service-account-*`) with their role
mappings — the `healthcare-admin-cli` service account keeps `ADMIN` (for internal
service-to-service calls like `/patients/batch`) and the `realm-management` roles
`manage-users` / `view-users` (doctor-service creates Keycloak accounts when adding
a doctor). Human users and password hashes are dropped.

> Earlier this export was taken with `--users skip`, which silently dropped the
> service-account users too — so a fresh import lost those roles and internal calls
> started returning 403. Always export **with** users and scrub instead (below).

To refresh this snapshot after changing the realm in the Admin Console:

```powershell
# 1) Export WITH users (the script already passes --users realm_file)
./Export-KeycloakRealm.ps1 -ContainerName keycloak -Realm healthcare
# 2) Scrub: keep only service-account users, drop humans, scrub secrets, set brand
python scrub-realm-export.py backups/<timestamp>/export/healthcare-realm.json realm-config/healthcare-realm.json
```

`scrub-realm-export.py` keeps service-account users, removes real users + password
hashes, replaces client secrets with `${env.*}` placeholders, and sets the MediBook
brand + `resetPasswordAllowed`. Review the diff before committing.

---

This stack replaces the disposable `start-dev`/H2 setup with:

- Keycloak 26.4 running `start --optimized`;
- PostgreSQL with a persistent Docker volume;
- Cloudflare Tunnel for public ingress and Caddy for forwarding-header normalization;
- readiness health checks and restart policies;
- realm import support for migration from the current H2 container.

## Prerequisites

1. Add the domain to Cloudflare and create a remotely-managed Tunnel for `auth.example.com`.
2. Do not open inbound ports 80/443; the connector creates an outbound-only connection.
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
- the `CLOUDFLARE_KEYCLOAK_TUNNEL_TOKEN` secret.

Do not reuse the existing development administrator password. The `.env` file is ignored by Git. In Kubernetes or another orchestrator, inject the same values through its secret manager instead.

## 3. Start the production stack

```powershell
docker compose --profile cloudflare build --pull
docker compose --profile cloudflare up -d
docker compose ps
```

Configure the Tunnel public hostname to route to `http://keycloak-tls:8080`. Caddy's recovery port
binds only to `127.0.0.1:8088`; no public origin port is required. Public TLS terminates at Cloudflare,
and Caddy overwrites forwarding headers before Keycloak consumes them.

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

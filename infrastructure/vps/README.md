# Deploy tren VPS (IP public + Caddy HTTPS)

Chay ca stack tren 1 VPS (vd Contabo 8GB Singapore, Ubuntu 24.04). VPS co IP
public nen **khong dung Cloudflare Tunnel** - Caddy tu xin chung chi Let's
Encrypt va route:

- `https://auth.<domain>`  -> Keycloak
- `https://api.<domain>`   -> api-gateway
- Frontend van o **Vercel** (chi can set lai 2 bien env tro ve 2 URL tren).

DB dung **Supabase** (external). Ca 3 stack (keycloak / backend / edge-caddy)
noi chung 1 network ngoai `web`.

```
Internet ──443──> Caddy (edge) ──> keycloak:8080
                                └─> api-gateway:8090 ──> 6 microservices ──> Supabase
```

## 0. Chuan bi

1. VPS Ubuntu 24.04, da cai **Docker + Docker Compose plugin**:
   ```bash
   curl -fsSL https://get.docker.com | sh
   ```
2. Mo tuong lua cong **22, 80, 443**:
   ```bash
   ufw allow 22 && ufw allow 80 && ufw allow 443 && ufw enable
   ```
3. **DNS**: tao 2 ban ghi **A** tro ve IP VPS:
   - `auth.<domain>` -> `<IP VPS>`
   - `api.<domain>`  -> `<IP VPS>`
   (Cho DNS lan truyen vai phut truoc khi len Caddy, de Let's Encrypt xac thuc duoc.)
4. Clone repo:
   ```bash
   git clone https://github.com/Pledat02/healthcare.git && cd healthcare
   ```

## 1. Network chung (chay 1 lan)

```bash
docker network create web
```

## 2. Keycloak

1. Nap realm import (co MediBook + resetPasswordAllowed, khong users, secret da scrub):
   ```bash
   cp infrastructure/keycloak/realm-config/healthcare-realm.json infrastructure/keycloak/import/
   ```
2. Tao `infrastructure/keycloak/.env` tu `.env.example`, dat:
   - `KEYCLOAK_HOSTNAME=auth.<domain>`
   - `KEYCLOAK_DB_PASSWORD`, `KEYCLOAK_BOOTSTRAP_ADMIN_USERNAME/PASSWORD` (chuoi dai, ngau nhien)
   - `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `KEYCLOAK_ADMIN_CLIENT_SECRET`
3. Len Keycloak (khong profile cloudflare):
   ```bash
   docker compose -f infrastructure/keycloak/compose.yaml \
     -f infrastructure/vps/compose.keycloak.override.yaml \
     --env-file infrastructure/keycloak/.env up -d --build
   # (tuy chon) tat Caddy noi bo khong dung:
   docker compose -f infrastructure/keycloak/compose.yaml stop keycloak-tls
   ```

## 3. Backend (6 service + gateway + Redis + Kafka)

1. Tao `backend/.env` tu `backend/.env.example`, dat:
   - Supabase: `*_DB_URL / *_DB_USER / *_DB_PASSWORD` cho tung service
   - `KEYCLOAK_ISSUER_URI=https://auth.<domain>/realms/healthcare`
   - `KEYCLOAK_SERVER_URL=https://auth.<domain>`
   - `FRONTEND_URL=https://healthcare-rosy-eta.vercel.app`  (CORS)
   - Mail SMTP (medibook00@gmail.com + App Password), Kafka/Redis giu mac dinh (DNS noi bo)
2. Len backend:
   ```bash
   docker compose -f backend/compose.yaml \
     -f infrastructure/vps/compose.backend.override.yaml \
     --env-file backend/.env up -d --build
   ```

## 4. Edge Caddy (HTTPS)

1. Tao `infrastructure/vps/.env` tu `.env.example`: `DOMAIN`, `ACME_EMAIL`.
2. Len Caddy:
   ```bash
   docker compose -f infrastructure/vps/compose.caddy.yaml \
     --env-file infrastructure/vps/.env up -d
   docker compose -f infrastructure/vps/compose.caddy.yaml logs -f caddy   # xem xin chung chi
   ```
3. Kiem tra:
   ```bash
   curl -I https://auth.<domain>/realms/healthcare/.well-known/openid-configuration
   curl -I https://api.<domain>/actuator/health   # hoac 1 endpoint public cua gateway
   ```

## 5. Keycloak sau import (Admin Console)

Vao `https://auth.<domain>/admin`:
- **Identity Providers -> google**: dat lai Client ID/Secret (placeholder `${env.*}`).
- **Clients -> healthcare-admin-cli -> Credentials**: dat lai secret.
- **Clients -> healthcare-app**: them Valid redirect URIs + Web origins:
  - `https://healthcare-rosy-eta.vercel.app/*`
- **Realm settings -> Email**: nhap lai SMTP (khong nam trong realm export).
- Google OAuth Console: them redirect URI
  `https://auth.<domain>/realms/healthcare/broker/google/endpoint`.

## 6. Wire Vercel

Vercel -> Project -> Settings -> Environment Variables:
- `VITE_API_URL=https://api.<domain>/api`
- `VITE_KEYCLOAK_URL=https://auth.<domain>`

Redeploy. Xong: app chay that dau-cuoi.

## Van hanh

```bash
docker compose -f backend/compose.yaml -f infrastructure/vps/compose.backend.override.yaml ps
docker compose -f backend/compose.yaml -f infrastructure/vps/compose.backend.override.yaml logs -f api-gateway
# RAM chat -> ep Xmx tung service qua JAVA_TOOL_OPTIONS=-Xmx256m trong backend/.env
```

# Triển khai Cloudflare

Project dùng hai Cloudflare Tunnel riêng:

- `api.<domain>` → `http://api-gateway:8090`
- `auth.<domain>` → `http://keycloak-tls:8080`

Tunnel chỉ tạo kết nối đi ra ngoài. API Gateway và Caddy chỉ publish port trên `127.0.0.1`, vì vậy
origin IP không còn là đường vào public. Baseline này không bật CAPTCHA hoặc Turnstile.

## 1. Tạo hai Tunnel

Trong Cloudflare Dashboard, vào **Networking → Tunnels**:

1. Tạo remotely-managed tunnel `healthcare-api`.
2. Tạo Public hostname `api.<domain>` với service `http://api-gateway:8090`.
3. Copy riêng tunnel token vào `backend/.env`:

   ```dotenv
   SECURITY_CLOUDFLARE_ENABLED=true
   SECURITY_CLOUDFLARE_REQUIRED=true
   SECURITY_CLOUDFLARE_TRUSTED_PROXY_CIDRS=172.29.250.2/32
   CLOUDFLARE_TUNNEL_TOKEN=<secret>
   ```

4. Tạo tunnel thứ hai `healthcare-keycloak`.
5. Tạo Public hostname `auth.<domain>` với service `http://keycloak-tls:8080`.
6. Copy token thứ hai vào `infrastructure/keycloak/.env`:

   ```dotenv
   KEYCLOAK_HOSTNAME=auth.<domain>
   CLOUDFLARE_KEYCLOAK_TUNNEL_TOKEN=<secret>
   ```

Tunnel token có quyền chạy connector. Không commit token, không truyền token trên command line và
rotate ngay khi nghi ngờ bị lộ.

## 2. Khởi động

Backend:

```powershell
docker compose -f backend/compose.yaml --env-file backend/.env --profile cloudflare up -d --build
docker compose -f backend/compose.yaml --env-file backend/.env --profile cloudflare logs cloudflared --tail 100
```

Keycloak:

```powershell
docker compose -f infrastructure/keycloak/compose.yaml --env-file infrastructure/keycloak/.env --profile cloudflare up -d --build
docker compose -f infrastructure/keycloak/compose.yaml --env-file infrastructure/keycloak/.env --profile cloudflare logs keycloak-cloudflared --tail 100
```

Public hostname phải dùng đúng service name ở trên, không dùng `localhost` vì connector chạy trong
container.

## 3. Cloudflare security baseline, không CAPTCHA

Trong **Security → WAF**:

1. Bật Cloudflare Managed Ruleset có trong plan; ban đầu để sensitivity mặc định.
2. Tạo rule block `.env`, `.git`, path traversal, `wp-admin` và `phpmyadmin` trên API hostname.
3. Trên API hostname, chỉ cho `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS`; block method còn lại.
4. Chặn public `/admin/` và `/realms/master/` trên auth hostname. Quản trị Keycloak qua private access
   hoặc Cloudflare Access, không mở Admin Console cho Internet.
5. Không bật interactive challenge, Turnstile hay CAPTCHA trong baseline. Dùng action `Block` cho rule
   chắc chắn và `Log` khi đang tuning rule có nguy cơ false-positive.

Trong **Security → WAF → Rate limiting rules**:

- API hostname: bắt đầu ở `300 request / 1 phút / IP`, block 60 giây.
- Keycloak login/token endpoints: bắt đầu ở `60 POST / 1 phút / IP`, block 5 phút.
- Theo dõi Security Analytics ít nhất 7 ngày và tăng ngưỡng nếu bác sĩ/bệnh viện dùng chung NAT.
  Period, counting key và số rule khả dụng phụ thuộc Cloudflare plan.

Gateway vẫn giữ Redis rate limit và IP block để bảo vệ khi rule Cloudflare bị cấu hình sai. Cloudflare
xử lý lưu lượng lớn ở edge; Gateway xử lý authorization và hành vi theo nghiệp vụ.

## 4. TLS, cache và header

- Bật **Always Use HTTPS** và TLS tối thiểu 1.2 ở edge.
- Không tạo Cache Everything rule cho `/api/**`, Keycloak, response chứa dữ liệu bệnh nhân hoặc response
  đã authenticated.
- Gateway chỉ tin `CF-Connecting-IP` nếu TCP peer là `172.29.250.2`, IP tĩnh của connector trên Docker
  edge network. `X-Forwarded-For` không được dùng trong Cloudflare mode.
- Caddy ghi đè `X-Forwarded-*` trước khi Keycloak xử lý, tránh client chèn IP/protocol giả.
- Gateway lưu `CF-Ray` trong `security_events.cloudflare_ray_id` để đối chiếu với Cloudflare Security Events.

Nếu đổi topology hoặc subnet, cập nhật cả `compose.yaml` và
`SECURITY_CLOUDFLARE_TRUSTED_PROXY_CIDRS`. Không đặt trusted range thành `0.0.0.0/0` hay `::/0`.

## 5. Kiểm chứng

1. `https://api.<domain>/actuator/health` trả về healthy qua Tunnel.
2. API có JWT hoạt động bình thường và CORS chỉ cho frontend domain.
3. Security event có client IP thật và có `cloudflareRayId`.
4. Port `8090`, `80`, `443` không truy cập được từ máy khác bằng origin IP.
5. Header `CF-Connecting-IP` giả gửi tới origin không được Gateway tin.
6. Dừng connector API: request nghiệp vụ bị `403 ORIGIN_BYPASS`; health check vẫn dùng được trên loopback.
7. Test WAF/rate-limit bằng IP test trước khi bật rule cho toàn bộ traffic.

Cloudflare có thể thấy URL và metadata request. Không đưa patient ID, chẩn đoán hoặc PHI vào hostname,
path hay query string; tiếp tục dùng TLS và audit log đã sanitize.


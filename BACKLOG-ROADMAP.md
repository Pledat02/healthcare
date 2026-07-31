# Lộ trình xử lý backlog Healthcare

**Thời gian dự kiến:** 29/07/2026 – 11/09/2026  
**Giả định nguồn lực:** 2 Backend Developer và 1 Frontend/DevOps. Nếu chỉ có 1 người thực hiện, dự kiến cần khoảng 10–12 tuần.

## 1. Mục tiêu

Đưa hệ thống từ trạng thái phù hợp cho phát triển/demo sang trạng thái có thể triển khai production, ưu tiên theo thứ tự:

1. Đóng các lỗ hổng bảo mật và nguy cơ mất dữ liệu.
2. Loại bỏ lỗi gửi email trùng khi scale nhiều instance.
3. Tăng khả năng chịu lỗi của luồng gọi liên service.
4. Hoàn thiện phân trang, batch API và xử lý múi giờ.
5. Bổ sung khả năng quan sát, đóng gói và CI/CD.
6. Chuẩn hóa cấu trúc Maven, cấu hình và code.

## 2. Mức độ ưu tiên

### P0 – Chặn triển khai production

- Vi phạm BR-06 tại các API truy cập dữ liệu theo ID, đặc biệt `patient/batch`.
- Keycloak đang chạy DEV mode với H2, không TLS và không lưu trữ bền vững.
- Reminder có thể gửi trùng khi notification-service chạy nhiều instance.

### P1 – Chặn mở rộng hệ thống

- REST đồng bộ chưa có timeout, circuit breaker và retry có kiểm soát.
- Appointment chưa phân trang đầy đủ; batch ID có thể vượt giới hạn URL.
- Chưa tách cấu hình dev/staging/prod và còn hardcode `localhost`.
- Slot picker có nguy cơ xử lý sai khi người dùng ở múi giờ khác phòng khám.
- Chưa có observability tối thiểu để điều tra lỗi liên service.

### P2 – Chất lượng và khả năng bảo trì

- Chưa có Maven parent/aggregator.
- Dockerfile, Docker Compose và CI/CD chưa đầy đủ.
- Còn dead code và cách truy vấn/cache chưa nhất quán.
- Entity Patient chưa khai báo `@Table` rõ ràng.

## 3. Lịch triển khai

| Thời gian | Trọng tâm | Công việc chính | Điều kiện hoàn thành |
|---|---|---|---|
| **29–31/07** | Vá bảo mật BR-06 | Chặn `patient/batch` lấy ID tùy ý; giới hạn tối đa 100 ID; bổ sung test PATIENT/DOCTOR/ADMIN; rà lại `getOne`, `update`, `delete` | User A không xem/sửa được dữ liệu user B; vi phạm trả `403`; batch quá giới hạn trả `400` |
| **03–07/08** | Nền tảng build và môi trường | Tạo Maven parent/aggregator; tách profile `dev`, `staging`, `prod`; chuyển URL và secret sang biến môi trường; thêm CI tối thiểu để build và chạy unit test | Một lệnh build được toàn bộ service; production config không còn `localhost` hoặc secret trong repo |
| **10–14/08** | Keycloak production | Chạy Keycloak production mode; dùng PostgreSQL; TLS tại reverse proxy/ingress; export/import realm; backup/restore; health check | Restart không mất realm/user; không còn H2; đăng nhập qua HTTPS; thử restore thành công |
| **17–21/08** | Reminder không gửi trùng | Thêm ShedLock vào DB của notification-service; unique constraint cho reminder; cơ chế claim `PENDING → PROCESSING`; xử lý job bị treo | Chạy 3 instance vẫn chỉ gửi 1 email cho một reminder; restart giữa job không làm mất reminder |
| **24–28/08** | Resilience liên service | Thiết lập connect/read timeout; circuit breaker; retry có kiểm soát; fallback; chuẩn hóa lỗi `503`; test khi service phụ thuộc ngừng hoạt động | Request không bị treo; circuit mở đúng; luồng đặt lịch có kết quả xác định khi một service chết |
| **31/08–04/09** | Phân trang và timezone | Phân trang DB cho `/appointments`; sửa trang admin; xử lý các màn hình đang tải toàn bộ appointment; đổi batch sang POST body hoặc chia chunk; chuẩn hóa timezone phòng khám | Không còn tải toàn bảng; page size tối đa 100; user ở múi giờ khác vẫn chọn đúng slot |
| **07–11/09** | Observability và release gate | Prometheus metrics; structured log có `traceId`; distributed tracing; dashboard/alert; Dockerfile cho 6 service; Compose staging; hoàn thiện CI/CD; dọn dead code | Theo dõi được request xuyên service; staging dựng lại được từ đầu; pipeline build/test/image/deploy thành công |

## 4. Kế hoạch chi tiết theo backlog

### BL-02 – Keycloak production mode + PostgreSQL + TLS

**Công việc**

- [x] Tạo PostgreSQL database/schema riêng cho Keycloak.
- [x] Chuyển lệnh khởi động từ DEV mode sang production mode.
- [x] Đưa hostname, database URL, username và password ra biến môi trường/secret.
- [x] Bật HTTPS tại ingress hoặc reverse proxy.
- [x] Cấu hình proxy headers và hostname cho Keycloak.
- [ ] Export realm hiện tại và kiểm thử import sang môi trường mới.
- [ ] Thiết lập backup định kỳ và diễn tập restore.
- [x] Xóa H2 và các giá trị DEV khỏi production profile.

**Definition of Done**

- Restart hoặc thay container không làm mất user, role, client hay realm.
- Token issuer dùng HTTPS và các service xác thực JWT thành công.
- Production log không còn cảnh báo cấu hình DEV.
- Có tài liệu backup/restore đã được kiểm thử.

### BL-03 – Resilience cho REST liên service

**Công việc**

- [ ] Thống kê toàn bộ lời gọi giữa appointment, doctor, patient, notification và medical-record.
- [ ] Đặt connect timeout và read timeout cho mọi REST client.
- [ ] Tạo circuit breaker riêng cho từng downstream service.
- [ ] Chỉ retry GET hoặc thao tác có idempotency key.
- [ ] Không tự động retry tạo lịch, tạo user Keycloak hoặc gửi email.
- [ ] Giới hạn số retry và bổ sung backoff.
- [ ] Map lỗi downstream sang mã lỗi nghiệp vụ và HTTP `503` phù hợp.
- [ ] Thêm test lỗi mạng, timeout, HTTP 5xx và circuit open.

**Definition of Done**

- Một service chết không làm request treo vô thời hạn.
- Circuit breaker mở/đóng đúng theo cấu hình.
- Không phát sinh appointment hoặc notification trùng do retry.
- Có metrics cho timeout, retry, circuit breaker và lỗi downstream.

### BL-04 – Reminder không gửi trùng khi scale

**Công việc**

- [ ] Thêm ShedLock và bảng khóa trong database của notification-service.
- [ ] Đặt `lockAtMostFor` và `lockAtLeastFor` phù hợp với chu kỳ cron.
- [ ] Thêm unique constraint cho cặp dữ liệu định danh reminder, ví dụ `(appointment_id, notification_type)`.
- [ ] Claim row theo cách atomic trước khi gửi: `PENDING → PROCESSING`.
- [ ] Ghi nhận `SENT`, `FAILED`, số lần thử và thời điểm thử tiếp theo.
- [ ] Có cơ chế đưa row `PROCESSING` bị treo trở lại hàng đợi.
- [ ] Dùng idempotency key với nhà cung cấp email nếu được hỗ trợ.
- [ ] Viết integration test chạy đồng thời ba scheduler.

**Definition of Done**

- Ba instance cùng chạy nhưng chỉ một instance thực thi mỗi lượt quét.
- Một appointment chỉ có một reminder hợp lệ.
- Restart/crash giữa job không làm mất reminder.
- Trường hợp gửi thành công nhưng cập nhật DB thất bại được theo dõi và có chiến lược xử lý.

### BL-06 – Phân trang, batch API và BR-06

**Công việc**

- [ ] Đóng quyền truy cập tùy ý tại `patient/batch`.
- [ ] Xác định API nội bộ dùng service account hoặc để appointment-service trả dữ liệu đã kiểm tra quyền.
- [ ] Bổ sung ownership check cho các API lấy/sửa dữ liệu theo ID.
- [ ] Áp giới hạn tối đa 100 ID mỗi batch.
- [ ] Chuyển batch lớn sang `POST` với JSON body hoặc chia request thành chunk.
- [ ] Thêm server-side pagination, sort và filter cho `/appointments`.
- [ ] Cập nhật `AllAppointmentsPage` dùng pagination từ server.
- [ ] Cập nhật `ManagePatientsPage` để không tải toàn bộ appointment chỉ nhằm tính thống kê.
- [ ] Chuyển phần thống kê tổng hợp sang endpoint/query phía backend.
- [ ] Cân nhắc chuyển phân trang doctor từ lọc toàn cache sang phân trang tại repository.

**Definition of Done**

- PATIENT không thể lấy hồ sơ của PATIENT khác bằng ID hoặc batch.
- DOCTOR chỉ xem được dữ liệu cần thiết theo quan hệ điều trị/lịch hẹn.
- ADMIN vẫn tra cứu được theo đúng quyền.
- Không endpoint danh sách nào trả toàn bộ dữ liệu không giới hạn.
- Page size có mặc định hợp lý và giới hạn tối đa 100.

### BL-07 – Observability

**Công việc**

- [x] Bật Actuator health, readiness và liveness phù hợp. *(management.endpoint.health.probes.enabled; api-gateway thêm actuator)*
- [x] Thêm Micrometer và Prometheus metrics. *(micrometer-registry-prometheus, /actuator/prometheus, tag application)*
- [x] Chuẩn hóa JSON log. *(prod: logging.structured.format.console=ecs — Boot 4.1 native)*
- [x] Truyền `traceId`/`spanId` xuyên gateway và các service. *(bridge-otel + log pattern [app,traceId,spanId])*
- [x] Bổ sung distributed tracing bằng OpenTelemetry. *(micrometer-tracing-bridge-otel + OTel SDK; exporter OTLP là bước infra sau)*
- [ ] Thiết lập Prometheus + Grafana và hệ thống log/tracing phù hợp. *(hạ tầng — cần compose Prometheus/Grafana/collector)*
- [ ] Tạo dashboard cho latency, throughput, error rate và saturation. *(hạ tầng — sau khi có Grafana)*
- [ ] Tạo alert cho service down, tỷ lệ lỗi cao, circuit open và backlog notification tăng. *(hạ tầng)*
- [ ] Giới hạn quyền truy cập các endpoint quản trị/metrics. *(code Java: permit /actuator/health trong 6 SecurityConfig, /prometheus hạn chế ADMIN — hướng dẫn)*

**Definition of Done**

- Có thể tìm toàn bộ log của một request bằng `traceId`.
- Dashboard thể hiện được bốn golden signals.
- Có cảnh báo khi service chết hoặc tỷ lệ lỗi vượt ngưỡng.
- Có thể xác định service gây lỗi trong luồng đi qua nhiều service.

### BL-08 – Docker, môi trường và CI/CD

**Công việc**

- [x] Tạo Dockerfile multi-stage cho 6 service. *(1 Dockerfile chung, ARG SERVICE — đã build-verify)*
- [x] Thêm health check và chạy container bằng non-root user. *(healthcheck TCP; user `app` uid 999)*
- [x] Tạo Compose cho local/staging gồm gateway, service và các dependency. *(backend/compose.yaml + Redis + Kafka)*
- [x] Đưa URL, database credential, Kafka, Redis và Keycloak config ra environment/secret. *(.env.example, secret gitignored)*
- [x] Tạo pipeline build, unit test và integration test. *(build+unit test; integration test chưa)*
- [x] Build image có tag bất biến theo commit SHA. *(ci.yml job docker-build, matrix 6 service)*
- [x] Thêm dependency scan, image scan và secret scan. *(Trivy image + gitleaks; dependency scan chưa)*
- [ ] Deploy tự động lên staging; production cần bước phê duyệt. *(cần registry + hạ tầng staging)*
- [ ] Thiết lập rollback về image version trước. *(có tag SHA để rollback; quy trình deploy chưa)*
- [ ] Tách `application-staging`. *(dev + prod đã có; staging chưa tách riêng)*

**Definition of Done**

- Một máy sạch có thể dựng toàn bộ staging theo tài liệu.
- CI chạy được toàn bộ test và tạo image cho mọi service.
- Không có production secret trong Git hoặc image.
- Có quy trình deploy và rollback đã thử nghiệm.

### BL-09 – Cấu trúc và chất lượng code

**Công việc**

- [ ] Tạo Maven parent/aggregator ở thư mục backend.
- [ ] Gom version Spring Boot, Java, plugin và dependency dùng chung.
- [ ] Thống nhất cách build/test toàn bộ service.
- [ ] Xóa hoặc khôi phục có chủ đích phần phân quyền đang bị comment trong DoctorService.
- [ ] Thống nhất chiến lược batch giữa doctor và patient.
- [ ] Thêm `@Table(name = "patients")` cho Patient và migration tương ứng nếu cần.
- [ ] Dọn import thừa, dead code và comment lỗi thời.
- [x] Bổ sung Architecture Decision Record cho cache, batch và service-to-service auth. *(docs/adr/0001–0003)*

**Definition of Done**

- Không còn dependency version bị khai báo lặp không cần thiết.
- Build/test toàn bộ backend bằng một lệnh.
- Không còn security code bị comment mà không có quyết định rõ ràng.
- Entity/table mapping nhất quán giữa các service.

### BL-09B – Timezone cho slot picker

**Công việc**

- [ ] Chọn timezone chuẩn của phòng khám, mặc định `Asia/Ho_Chi_Minh`.
- [ ] Backend lưu thời điểm dưới dạng UTC/`Instant`.
- [ ] API trả kèm timezone hoặc quy ước timezone rõ ràng.
- [ ] Frontend tạo slot theo timezone phòng khám, không theo timezone của trình duyệt.
- [ ] Không dùng `toISOString().slice(0, 10)` cho ngày nghiệp vụ nếu việc chuyển UTC có thể đổi ngày.
- [ ] Viết test cho UTC, `Asia/Ho_Chi_Minh`, múi giờ Mỹ và thời điểm gần nửa đêm.

**Definition of Done**

- Cùng một lịch khám hiển thị và gửi lên backend đúng bất kể timezone của trình duyệt.
- Không lệch ngày hoặc lệch slot khi người dùng ở nước ngoài.
- Email, lịch admin, lịch bác sĩ và lịch bệnh nhân hiển thị nhất quán.

## 5. Quy tắc triển khai

1. Không đưa tính năng mới vào production trước khi hoàn thành toàn bộ P0.
2. Mỗi backlog phải có unit test và integration test tương ứng.
3. Thay đổi database phải đi qua migration có version, không dùng Hibernate tự sửa schema trong production.
4. Mọi REST client bắt buộc có timeout trước khi thêm retry.
5. Retry chỉ được bật sau khi xác định rõ thao tác có idempotent hay không.
6. Endpoint nội bộ phải có service authentication; không dựa vào việc URL khó đoán.
7. Mỗi sprint phải deploy lên staging và chạy smoke test.

## 6. Release gate trước production

- [ ] Keycloak production mode + PostgreSQL + TLS.
- [ ] Đã thử backup và restore realm/user.
- [ ] Không còn lỗ BR-06 trong test phân quyền.
- [ ] Ba notification instance không gửi reminder trùng.
- [ ] Tất cả REST client có connect/read timeout.
- [ ] Circuit breaker và retry có metrics, test và giới hạn rõ ràng.
- [ ] Appointment sử dụng server-side pagination.
- [ ] Batch API có giới hạn kích thước và kiểm tra quyền.
- [ ] Production không chứa `localhost`, H2 hoặc secret mặc định.
- [ ] Docker image được tạo từ CI và staging deploy thành công.
- [ ] Có health check, metrics, log `traceId`, tracing và alert cơ bản.
- [ ] Test timezone vượt qua trên nhiều múi giờ.
- [ ] Có kế hoạch rollback và đã diễn tập ít nhất một lần.

## 7. Ước lượng tổng

| Nhóm công việc | Ước lượng |
|---|---:|
| BR-06 và batch API | 3–5 person-days |
| Keycloak production | 5–7 person-days |
| Reminder locking/idempotency | 3–5 person-days |
| Resilience | 5–7 person-days |
| Pagination và timezone | 7–10 person-days |
| Observability | 5–7 person-days |
| Docker, môi trường và CI/CD | 7–10 person-days |
| Maven/code cleanup | 4–6 person-days |
| **Tổng** | **39–57 person-days** |

Các ước lượng đã bao gồm phát triển và test kỹ thuật, nhưng chưa bao gồm thời gian chờ cấp hạ tầng, domain, chứng chỉ TLS hoặc quyền truy cập môi trường production.

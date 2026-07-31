# ADR-0003: Xác thực service-to-service bằng Keycloak service account

- **Trạng thái:** Accepted
- **Ngày:** 2026-07-31
- **Liên quan:** BR-06, ADR-0002, BL-02 (Keycloak prod)

## Bối cảnh

`appointment-service` gọi `patient-service`/`doctor-service` để làm giàu dữ liệu. Các endpoint
nội bộ này trả PII (tên, điện thoại). Nếu chỉ dựa vào "URL khó đoán" hoặc để mở, bất kỳ ai
trong mạng cũng gọi được → vi phạm BR-06.

## Quyết định

- Endpoint nội bộ (ví dụ `patient/batch`, `doctor` getByIds) yêu cầu **JWT của service account**
  Keycloak, không nhận token người dùng cuối tùy ý cho mục đích này.
- `appointment-service` lấy token qua **client credentials** (service account
  `healthcare-admin-cli`) bằng `InternalTokenClient`, gắn `Authorization: Bearer` khi gọi nội bộ.
- Service account được cấp role phù hợp (ví dụ ADMIN) để qua được kiểm tra phân quyền ở
  service đích; secret giữ trong biến môi trường/secret manager, không commit.
- Endpoint nội bộ vẫn kiểm tra role trong token (không tin "gọi từ nội bộ là an toàn").

## Hệ quả

- Truy cập PII nội bộ luôn có danh tính (service account) và bị phân quyền, không dựa vào ẩn URL.
- Cần quản lý vòng đời secret của service account (rotate) — thuộc BL-02.
- Thêm một lần lấy token (có thể cache token đến khi hết hạn) trước khi gọi nội bộ.

## Phương án đã cân nhắc

- **mTLS giữa service:** mạnh hơn nhưng nặng vận hành cho quy mô hiện tại; có thể thêm ở lớp mesh/ingress sau.
- **Shared secret header tĩnh:** đơn giản nhưng khó rotate, dễ lộ, không có danh tính rõ ràng → loại.
- **Truyền thẳng token người dùng:** không đủ quyền cho thao tác tổng hợp và dễ rò rỉ phạm vi → loại.

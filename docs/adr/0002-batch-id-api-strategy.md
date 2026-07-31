# ADR-0002: Chiến lược batch API lấy theo danh sách ID

- **Trạng thái:** Accepted
- **Ngày:** 2026-07-31
- **Liên quan:** BR-06 (owner-only), BL-06

## Bối cảnh

`appointment-service` cần "làm giàu" dữ liệu lịch hẹn bằng tên/điện thoại bệnh nhân và
thông tin bác sĩ → phải lấy nhiều bản ghi theo danh sách ID trong một request (tránh N+1
cross-service). Trước đây hai service làm hai kiểu khác nhau:

- `doctor` batch: lọc từ cache.
- `patient` batch: query thẳng DB.

Không nhất quán, khó suy luận về hiệu năng và bảo mật.

## Quyết định

1. **Thống nhất truy vấn:** cả hai dùng `repository.findAllById(ids)` (một query IN), bỏ kiểu
   lọc-từ-cache thủ công.
2. **Giới hạn kích thước:** tối đa **100 ID/batch**; vượt → `400 TOO_MANY_IDS`. Chống lạm dụng
   và tránh vượt giới hạn độ dài URL.
3. **Chia chunk phía gọi:** client (frontend/service) tự chia ≤100 ID mỗi lần gọi.
4. **Batch là API nội bộ, phân quyền chặt (BR-06):**
   - `patient/batch` chỉ dành cho **service account / ADMIN**, không cho PATIENT tùy ý truyền ID.
   - `appointment-service` gọi bằng internal token (xem [ADR-0003](0003-service-to-service-auth.md))
     và chỉ trả về đúng dữ liệu mà người gọi có quyền theo quan hệ điều trị/lịch hẹn.

## Hệ quả

- PATIENT không thể lấy hồ sơ PATIENT khác qua batch (đóng lỗ BR-06).
- Một query IN thay vì N request → giảm tải rõ rệt.
- Payload batch có trần cứng, dễ dự đoán.

## Phương án đã cân nhắc

- **GET với ID trên query string:** dễ vượt giới hạn URL khi nhiều ID → chuyển sang trần 100 + cân nhắc POST body cho batch lớn.
- **Giữ lọc-từ-cache cho doctor:** logic phân tán, cache miss vẫn phải chạm DB → thống nhất `findAllById` đơn giản hơn.

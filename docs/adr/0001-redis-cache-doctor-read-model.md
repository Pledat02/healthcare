# ADR-0001: Redis cache cho read-model doctor-service

- **Trạng thái:** Accepted
- **Ngày:** 2026-07-31
- **Liên quan:** BL-06 (batch/pagination), BL-07 (observability)

## Bối cảnh

`doctor-service` phục vụ danh sách/chi tiết bác sĩ cho luồng đặt lịch — đọc rất nhiều,
ghi rất ít (bác sĩ hầu như không đổi trong ngày). Với giả định tải cao (kịch bản ~1 triệu
người đọc), mỗi request đi thẳng Postgres (Supabase free, tối đa 15 connection/project,
HikariCP bị cap 4–5) sẽ cạn connection và tăng latency.

## Quyết định

- Cache read-model `DoctorResponse` trong **Redis** bằng `@Cacheable` (đọc) và
  `@CacheEvict` (khi tạo/sửa/xóa bác sĩ) để cache luôn nhất quán sau ghi.
- **Serialization: JDK serialization** (`DoctorResponse implements Serializable`), KHÔNG
  dùng JSON generic-typing.

## Lý do chọn JDK serialization thay vì JSON

Bản JSON với default typing gặp bug round-trip: lần đọc thứ 2 trả 0 record do immutable
list + kiểu bị mất khi deserialize. JDK serialization khôi phục đúng object graph, đơn giản,
đủ dùng cho read-model nội bộ. Đánh đổi: giá trị cache gắn với class version (đổi field của
`DoctorResponse` phải flush cache) và không đọc được cache bằng công cụ ngoài JVM.

## Hệ quả

- Đọc bác sĩ không còn chạm DB khi cache hit → giảm tải connection Supabase.
- Sau mỗi thao tác ghi phải evict đúng key, nếu không sẽ trả dữ liệu cũ.
- Khi đổi cấu trúc `DoctorResponse`: cần flush key Redis liên quan lúc deploy.
- Healthcheck/metrics cache (hit ratio) sẽ bổ sung ở BL-07.

## Phương án đã cân nhắc

- **Không cache, chỉ tăng connection pool:** bị chặn bởi trần 15 connection của Supabase free.
- **Caffeine (in-process):** nhanh nhưng không chia sẻ giữa nhiều instance → tỉ lệ hit thấp khi scale ngang.
- **Redis + JSON:** gặp bug typing như trên; có thể quay lại nếu cần đọc cache cross-language.

# ERD — Hệ thống Đặt lịch khám bệnh (Microservice)

**Phiên bản:** 1.0 | **Ngày:** 21/07/2026

> **Nguyên tắc microservice:** mỗi service có **1 database riêng**. Không có khóa ngoại (FK) nối cứng giữa 2 database khác nhau.
> - `——` (đường liền): **FK thật**, chỉ tồn tại **trong cùng 1 database**.
> - `··`  (đường đứt): **tham chiếu mềm** — chỉ lưu con số ID của service khác, lấy dữ liệu bằng **gọi API / nghe event**, KHÔNG phải FK.

---

## 1. Bản đồ Database ⇄ Service

| Service | Database | Bảng |
|---------|----------|------|
| patient-service | `patient_db` | `patients` |
| doctor-service | `doctor_db` | `doctors` |
| appointment-service | `appointment_db` | `appointments` |
| medical-record-service | `medical_record_db` | `medical_records`, `prescription_items` |
| notification-service | `notification_db` | `notifications` |

---

## 2. ERD tổng thể (Mermaid)

```mermaid
erDiagram
    %% ===== patient-service / patient_db =====
    PATIENTS {
        bigint      id PK
        uuid        keycloak_id UK "ID người dùng bên Keycloak"
        varchar     full_name
        date        date_of_birth
        varchar     gender
        varchar     phone
        varchar     email
        varchar     address
        timestamp   created_at
        timestamp   updated_at
    }

    %% ===== doctor-service / doctor_db =====
    DOCTORS {
        bigint      id PK
        uuid        keycloak_id UK
        varchar     full_name
        varchar     specialization "Chuyên khoa"
        time        work_start_time "Giờ bắt đầu làm"
        time        work_end_time  "Giờ kết thúc làm"
        varchar     phone
        varchar     email
        timestamp   created_at
        timestamp   updated_at
    }

    %% ===== appointment-service / appointment_db =====
    APPOINTMENTS {
        bigint      id PK
        bigint      patient_id "soft ref -> patients.id"
        bigint      doctor_id  "soft ref -> doctors.id"
        timestamp   appointment_time
        int         duration_minutes "mặc định 30"
        varchar     reason
        varchar     status "PENDING/CONFIRMED/CANCELLED/COMPLETED"
        timestamp   created_at
        timestamp   updated_at
    }

    %% ===== medical-record-service / medical_record_db =====
    MEDICAL_RECORDS {
        bigint      id PK
        bigint      appointment_id UK "soft ref -> appointments.id"
        bigint      patient_id "soft ref -> patients.id"
        bigint      doctor_id  "soft ref -> doctors.id"
        text        diagnosis "Chẩn đoán"
        text        notes
        timestamp   created_at
    }
    PRESCRIPTION_ITEMS {
        bigint      id PK
        bigint      medical_record_id FK "FK thật, cùng DB"
        varchar     medicine_name
        varchar     dosage "Liều dùng"
        int         quantity
        varchar     instruction "Cách dùng"
    }

    %% ===== notification-service / notification_db =====
    NOTIFICATIONS {
        bigint      id PK
        bigint      appointment_id "soft ref -> appointments.id"
        varchar     recipient_email
        varchar     type "CONFIRMATION/REMINDER"
        varchar     status "PENDING/SENT/FAILED"
        timestamp   scheduled_at "thời điểm cần gửi (nhắc 24h)"
        timestamp   appointment_time "bản sao từ event"
        varchar     doctor_name "bản sao từ event"
        timestamp   sent_at
    }

    %% ===== Quan hệ =====
    %% FK thật (cùng DB) = đường liền
    MEDICAL_RECORDS ||--o{ PRESCRIPTION_ITEMS : "1 hồ sơ - N đơn thuốc (FK thật)"

    %% Tham chiếu mềm giữa các service = đường đứt (..)
    PATIENTS     ||..o{ APPOINTMENTS     : "1 bệnh nhân - N lịch (soft ref, API)"
    DOCTORS      ||..o{ APPOINTMENTS     : "1 bác sĩ - N lịch (soft ref, API)"
    APPOINTMENTS ||..|| MEDICAL_RECORDS  : "1 lịch (đã khám) - 1 hồ sơ (soft ref)"
    APPOINTMENTS ||..o{ NOTIFICATIONS    : "1 lịch - N thông báo (soft ref, event)"
```

---

## 3. Vì sao lại là "tham chiếu mềm"?

`appointments.patient_id` chỉ lưu **con số** (VD: `42`). Nó KHÔNG join được sang bảng `patients` vì bảng đó nằm ở database khác (`patient_db`).

Khi appointment-service cần tên bệnh nhân, nó **gọi API** của patient-service:
```
GET http://patient-service/api/patients/42
```

Riêng notification-service **không gọi API mỗi lần** mà lưu sẵn bản sao (`appointment_time`, `doctor_name`) lấy từ **event Kafka** lúc đặt lịch — để job nhắc lịch 24h chạy độc lập, không phụ thuộc service khác còn sống hay không.

---

## 4. Chỉ số (Index) nên tạo — cho hiệu năng

| Bảng | Index | Lý do |
|------|-------|------|
| `appointments` | `(doctor_id, appointment_time)` | Truy vấn chống trùng lịch (BR-01) chạy nhanh |
| `appointments` | `(patient_id)` | Lấy danh sách lịch của 1 bệnh nhân |
| `medical_records` | `(appointment_id)` unique | 1 lịch chỉ 1 hồ sơ |
| `patients` / `doctors` | `(keycloak_id)` unique | Map user đăng nhập sang hồ sơ |
| `notifications` | `(status, scheduled_at)` | Job quét mail cần gửi |
```

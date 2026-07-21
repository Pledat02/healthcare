# PRD — Hệ thống Đặt lịch khám bệnh (Healthcare Booking System)

**Phiên bản:** 1.0 | **Ngày:** 21/07/2026 | **Người viết:** BA
**Đối tượng đọc:** Đội phát triển (fresher/junior fullstack)

---

## ⚡ TÓM TẮT 2 PHÚT (đọc cái này trước)

| Mục | Nội dung |
|-----|----------|
| **Làm cái gì** | Web đặt lịch khám bệnh online: bệnh nhân tự đặt lịch, bác sĩ quản lý lịch, lưu hồ sơ khám điện tử, nhắc lịch tự động |
| **Giải quyết vấn đề** | Đặt lịch thủ công gây trùng lịch, quên lịch, hồ sơ rải rác trên giấy |
| **3 vai trò** | PATIENT (bệnh nhân), DOCTOR (bác sĩ), ADMIN (quản trị) |
| **6 service** | patient · doctor · appointment · medical-record · notification · api-gateway |
| **Chức năng lõi** | Đặt/hủy lịch + chống trùng lịch bác sĩ + nhắc lịch qua email |
| **Luật quan trọng nhất** | BR-01: không cho 2 lịch trùng giờ cùng 1 bác sĩ (mỗi ca 30 phút) |
| **Quy tắc phân quyền** | Mỗi người chỉ xem/sửa dữ liệu của mình; ADMIN xem tất cả |
| **KHÔNG làm** | Thanh toán online, video call, app mobile, bảo hiểm, xếp hàng trong ngày |
| **Công nghệ** | Spring Boot microservice · PostgreSQL (Supabase) · Kafka/RabbitMQ |
| **Nguyên tắc vàng** | Mỗi service 1 database riêng; service không đụng DB của nhau, cần thì gọi API |

> 💡 *PRD là gì?* Tài liệu mô tả **sản phẩm cần làm gì và vì sao**, KHÔNG mô tả code viết thế nào.

---

## 1. Tổng quan

### 1.1. Bối cảnh
Hiện nay việc đặt lịch khám ở nhiều phòng khám vẫn làm thủ công: bệnh nhân gọi điện, lễ tân ghi sổ. Cách này gây ra: trùng lịch, bác sĩ bị đặt 2 người cùng giờ, bệnh nhân quên lịch, hồ sơ khám nằm rải rác trên giấy.

### 1.2. Sản phẩm này giải quyết gì
Một hệ thống web cho phép **bệnh nhân tự đặt lịch khám online**, **bác sĩ quản lý lịch của mình**, và **lưu hồ sơ khám điện tử** — tất cả tập trung một chỗ, có nhắc lịch tự động.

### 1.3. Mục tiêu (Goals)
| # | Mục tiêu | Đo lường thế nào (thành công khi) |
|---|----------|-----------------------------------|
| G1 | Bệnh nhân đặt lịch không cần gọi điện | Đặt xong 1 lịch trong < 2 phút |
| G2 | Không còn trùng lịch bác sĩ | 0 trường hợp 2 bệnh nhân cùng giờ/cùng bác sĩ |
| G3 | Giảm bệnh nhân quên lịch | Có email nhắc trước 24h cho 100% lịch hẹn |
| G4 | Hồ sơ khám tra cứu được | Bác sĩ xem lại lịch sử khám của bệnh nhân trong < 5 giây |

---

## 2. Phạm vi (Scope)

### 2.1. Có làm (In scope)
- Đăng ký / đăng nhập, phân quyền 3 vai trò
- Quản lý hồ sơ bệnh nhân, hồ sơ bác sĩ
- Đặt / hủy / xem lịch hẹn
- Chống trùng lịch
- Nhắc lịch qua email
- Hồ sơ khám điện tử (chẩn đoán, đơn thuốc)

### 2.2. KHÔNG làm (Out of scope) — quan trọng để dev không làm dư
- **Thanh toán online** (không tích hợp cổng thanh toán)
- **Video call khám từ xa**
- **App mobile** (chỉ làm web)
- **Bảo hiểm y tế / xuất hóa đơn**
- **Xếp hàng chờ khám trong ngày** (chỉ đặt lịch trước theo giờ)

> ⚠️ *Vì sao ghi rõ cái không làm?* Để tránh dev "làm thêm cho hoành tráng" rồi trễ deadline. Nếu sau này cần, sẽ đưa vào phiên bản 2.0.

---

## 3. Người dùng & Vai trò (User Roles)

| Vai trò | Là ai | Làm được gì |
|---------|-------|-------------|
| **PATIENT** (Bệnh nhân) | Người đi khám | Quản lý hồ sơ bản thân, đặt/hủy lịch của mình, xem hồ sơ khám của mình |
| **DOCTOR** (Bác sĩ) | Người khám bệnh | Xem lịch hẹn của mình, khai báo giờ làm việc, ghi hồ sơ khám cho bệnh nhân đã khám |
| **ADMIN** (Quản trị) | Nhân viên quản lý phòng khám | Quản lý danh sách bác sĩ, xem toàn bộ lịch hẹn, quản lý tài khoản |

**Quy tắc phân quyền cốt lõi (nhớ kỹ):** Bệnh nhân A **chỉ** được xem/sửa dữ liệu **của chính A**, không được đụng vào dữ liệu bệnh nhân B — kể cả khi biết ID của B.

---

## 4. Yêu cầu chức năng (Functional Requirements)

Viết theo dạng **User Story**: *"Là [vai trò], tôi muốn [làm gì], để [đạt được gì]"*. Kèm **Tiêu chí chấp nhận (Acceptance Criteria)** — điều kiện để coi là làm xong đúng.

### 4.1. Quản lý tài khoản & Đăng nhập

**US-01** — Là **người dùng**, tôi muốn **đăng nhập bằng tài khoản**, để **truy cập hệ thống an toàn**.
- ✅ Đăng nhập sai mật khẩu → báo lỗi, không cho vào.
- ✅ Đăng nhập đúng → nhận token, giữ đăng nhập.
- ✅ Gọi API mà không đăng nhập → trả lỗi **401 Unauthorized**.
- ✅ Gọi API không đúng quyền (VD: bệnh nhân gọi chức năng admin) → trả lỗi **403 Forbidden**.

### 4.2. Hồ sơ bệnh nhân (Patient)

**US-02** — Là **bệnh nhân**, tôi muốn **tạo/cập nhật hồ sơ cá nhân**, để **bác sĩ biết thông tin của tôi**.
- Trường thông tin: Họ tên, Ngày sinh, Giới tính, Số điện thoại, Email, Địa chỉ.
- ✅ Thiếu Họ tên hoặc SĐT → báo lỗi **400**, không lưu.
- ✅ Email sai định dạng → báo lỗi.
- ✅ Bệnh nhân chỉ sửa được hồ sơ của mình.

### 4.3. Hồ sơ & Lịch làm việc bác sĩ (Doctor)

**US-03** — Là **admin**, tôi muốn **thêm bác sĩ vào hệ thống**, để **bệnh nhân đặt lịch được**.
- Trường: Họ tên, Chuyên khoa, Giờ bắt đầu làm việc, Giờ kết thúc làm việc.

**US-04** — Là **bệnh nhân**, tôi muốn **xem danh sách bác sĩ theo chuyên khoa**, để **chọn đúng người cần khám**.

### 4.4. Đặt lịch hẹn (Appointment) — TRÁI TIM CỦA HỆ THỐNG

**US-05** — Là **bệnh nhân**, tôi muốn **đặt lịch khám với 1 bác sĩ vào giờ cụ thể**, để **được khám**.

Trường của 1 lịch hẹn: `bệnh nhân, bác sĩ, thời gian hẹn, lý do khám, trạng thái`.

Trạng thái lịch hẹn: `PENDING` (chờ) → `CONFIRMED` (đã xác nhận) → `CANCELLED` (đã hủy) hoặc `COMPLETED` (đã khám xong).

**Tiêu chí chấp nhận (đây là phần khó nhất, đọc kỹ):**
- ✅ Bác sĩ hoặc bệnh nhân không tồn tại → báo lỗi, không đặt.
- ✅ Giờ hẹn nằm ngoài giờ làm việc của bác sĩ → từ chối.
- ✅ **CHỐNG TRÙNG LỊCH:** bác sĩ đã có lịch trùng khung giờ đó → từ chối với lỗi **409 Conflict**. (Xem Business Rule BR-01)
- ✅ Đặt lịch trong quá khứ → từ chối.
- ✅ Đặt thành công → tạo lịch, gửi email xác nhận (US-08).

**US-06** — Là **bệnh nhân**, tôi muốn **hủy lịch hẹn của mình**, để **linh hoạt khi có việc**.
- ✅ Chỉ hủy được lịch của chính mình.
- ✅ Không hủy được lịch đã ở trạng thái `COMPLETED`.

**US-07** — Là **bác sĩ**, tôi muốn **xem danh sách lịch hẹn của tôi theo ngày**, để **chuẩn bị khám**.

### 4.5. Nhắc lịch (Notification)

**US-08** — Là **bệnh nhân**, tôi muốn **nhận email xác nhận khi đặt lịch**, để **yên tâm là đã đặt được**.

**US-09** — Là **bệnh nhân**, tôi muốn **nhận email nhắc trước 24 giờ**, để **không quên lịch khám**.
- ✅ Hệ thống tự quét và gửi, không cần ai bấm nút.

### 4.6. Hồ sơ khám (Medical Record)

**US-10** — Là **bác sĩ**, tôi muốn **ghi kết quả khám (chẩn đoán, đơn thuốc) sau buổi khám**, để **lưu lại lịch sử điều trị**.
- Gắn với 1 lịch hẹn cụ thể.
- ✅ Chỉ bác sĩ khám buổi đó mới ghi được.

**US-11** — Là **bệnh nhân**, tôi muốn **xem lại lịch sử khám của mình**, để **theo dõi sức khỏe**.
- ✅ Chỉ xem hồ sơ của chính mình.

---

## 5. Quy tắc nghiệp vụ (Business Rules)

> Đây là các "luật" hệ thống phải tuân theo. Dev hay bỏ sót phần này.

| Mã | Quy tắc |
|----|---------|
| **BR-01** | **Chống trùng lịch:** 2 lịch hẹn cùng 1 bác sĩ không được đè khung giờ lên nhau. Hai khung `[bắt đầu_1, kết thúc_1)` và `[bắt đầu_2, kết thúc_2)` bị coi là trùng khi: `bắt đầu_1 < kết thúc_2` VÀ `bắt đầu_2 < kết thúc_1`. Mỗi buổi khám mặc định kéo dài **30 phút**. |
| **BR-02** | Không cho đặt lịch vào thời điểm trong quá khứ. |
| **BR-03** | Giờ hẹn phải nằm trong khoảng giờ làm việc của bác sĩ (giờ bắt đầu → giờ kết thúc). |
| **BR-04** | Lịch đã `COMPLETED` không được hủy, không được sửa giờ. |
| **BR-05** | Chỉ tạo được hồ sơ khám cho lịch hẹn đã diễn ra (trạng thái `COMPLETED`). |
| **BR-06** | Một người dùng chỉ truy cập được dữ liệu thuộc về mình (trừ ADMIN xem được tất cả). |

---

## 6. Yêu cầu phi chức năng (Non-functional Requirements)

> Chức năng = "làm được gì". Phi chức năng = "làm tốt tới mức nào".

| Loại | Yêu cầu |
|------|---------|
| **Bảo mật** | Mật khẩu không lưu dạng text thô. Mọi API (trừ đăng nhập) phải có token hợp lệ. |
| **Hiệu năng** | Trang danh sách bác sĩ tải < 2 giây. |
| **Độ tin cậy** | Notification service chết KHÔNG được làm việc đặt lịch thất bại (gửi mail là việc phụ, tách riêng). |
| **Khả năng mở rộng** | Kiến trúc microservice — mỗi chức năng lớn là 1 service độc lập, database riêng. |

---

## 7. Kiến trúc & Phân rã hệ thống

Hệ thống chia thành các **service độc lập** (microservice), mỗi service lo 1 mảng nghiệp vụ và có database riêng:

| Service | Chịu trách nhiệm | Dữ liệu chính quản lý |
|---------|------------------|----------------------|
| **patient-service** | Hồ sơ bệnh nhân | Bệnh nhân |
| **doctor-service** | Hồ sơ + lịch làm việc bác sĩ | Bác sĩ |
| **appointment-service** | Đặt/hủy/xem lịch hẹn, chống trùng | Lịch hẹn |
| **medical-record-service** | Hồ sơ khám điện tử | Chẩn đoán, đơn thuốc |
| **notification-service** | Gửi email xác nhận & nhắc lịch | (không có DB nghiệp vụ riêng) |
| **api-gateway** | Cửa ngõ nhận mọi request, kiểm tra đăng nhập | — |

**Nguyên tắc vàng:** service này KHÔNG được truy cập thẳng vào database của service khác. Cần dữ liệu thì gọi qua API của nhau.

---

## 8. Mô hình dữ liệu (Data Model — mức khái niệm)

Mô tả các "bảng" chính và quan hệ, để dev hình dung. (Không phải schema SQL cuối cùng.)

```
PATIENT (Bệnh nhân)
  - id, họ tên, ngày sinh, giới tính, sđt, email, địa chỉ

DOCTOR (Bác sĩ)
  - id, họ tên, chuyên khoa, giờ_bắt_đầu_làm, giờ_kết_thúc_làm

APPOINTMENT (Lịch hẹn)
  - id, patient_id, doctor_id, thời_gian_hẹn, lý_do, trạng_thái
  - (patient_id trỏ tới PATIENT, doctor_id trỏ tới DOCTOR)

MEDICAL_RECORD (Hồ sơ khám)
  - id, appointment_id, chẩn_đoán, đơn_thuốc, ghi_chú, ngày_tạo
  - (appointment_id trỏ tới APPOINTMENT)
```

**Quan hệ:**
- 1 bệnh nhân có nhiều lịch hẹn.
- 1 bác sĩ có nhiều lịch hẹn.
- 1 lịch hẹn (đã khám xong) có 1 hồ sơ khám.

> Lưu ý: vì là microservice, `patient_id` trong bảng Appointment chỉ là **con số tham chiếu**, không phải khóa ngoại nối cứng sang DB của patient-service.

---

## 9. Giả định & Ràng buộc (Assumptions & Constraints)

**Giả định:**
- Mỗi buổi khám cố định 30 phút.
- Bác sĩ làm việc theo 1 khung giờ cố định mỗi ngày (chưa xử lý ngày nghỉ, ca gãy).
- Chỉ có 1 cơ sở phòng khám (chưa hỗ trợ nhiều chi nhánh).

**Ràng buộc:**
- Công nghệ: Spring Boot (backend microservice), PostgreSQL (Supabase), Kafka/RabbitMQ cho gửi mail bất đồng bộ.
- Ngân sách hạ tầng: dùng gói miễn phí (Supabase free) → giới hạn số database.

---

## 10. Thuật ngữ (Glossary) — cho fresher

| Thuật ngữ | Nghĩa dễ hiểu |
|-----------|---------------|
| **PRD** | Tài liệu mô tả sản phẩm cần làm gì |
| **User Story** | 1 câu mô tả nhu cầu của người dùng |
| **Acceptance Criteria** | Điều kiện để coi là "đã làm xong đúng" |
| **Business Rule** | Luật nghiệp vụ hệ thống bắt buộc tuân theo |
| **In/Out of scope** | Cái có làm / không làm trong phiên bản này |
| **Microservice** | Chia app lớn thành nhiều app nhỏ độc lập |
| **Token** | "Vé" chứng minh đã đăng nhập, gửi kèm mỗi request |
| **401 / 403 / 409** | Mã lỗi: chưa đăng nhập / không đủ quyền / dữ liệu bị xung đột (trùng lịch) |

---

*Hết PRD v1.0. Mọi thay đổi yêu cầu sẽ cập nhật vào bảng lịch sử phiên bản ở các bản sau.*

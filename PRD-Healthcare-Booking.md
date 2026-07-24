# PRD — Hệ thống Đặt lịch khám bệnh (Healthcare Booking System)

**Phiên bản:** 1.1 | **Ngày:** 23/07/2026 | **Người viết:** BA + Dev
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
| **Công nghệ** | Spring Boot microservice · PostgreSQL (Supabase) · Kafka/RabbitMQ · **Keycloak** (đăng nhập/OAuth2) |
| **Đăng nhập** | KHÔNG tự viết service tài khoản. Toàn bộ đăng ký/đăng nhập/mật khẩu/token do **Keycloak** lo; api-gateway chỉ kiểm tra token Keycloak |
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

> 🔑 *Ai lo việc này?* Hệ thống **không tự viết service quản lý tài khoản**. Việc đăng ký, lưu mật khẩu (đã băm), đăng nhập và phát hành **token JWT** đều giao cho **Keycloak** (một Identity Provider ngoài, chuẩn OAuth2/OIDC).
> - Client đăng nhập trực tiếp với Keycloak → nhận **JWT** (bên trong có `sub` = ID người dùng và `role`).
> - Mỗi request kèm JWT này. **api-gateway** xác thực chữ ký token với Keycloak: thiếu/sai token → **401**; sai role → **403**; hợp lệ → chuyển tiếp xuống service con kèm `userId` + `role` (qua header).
> - Các service con (patient/doctor/...) **không tự kiểm tra mật khẩu**, chỉ tin thông tin gateway đã xác thực để áp quyền (BR-06).
> - Mỗi hồ sơ `patient`/`doctor` lưu `keycloak_id` để nối user Keycloak với hồ sơ nghiệp vụ.

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
| **api-gateway** | Cửa ngõ nhận mọi request, xác thực token Keycloak, định tuyến | — |

> 🔑 **Đăng nhập / tài khoản không phải là một service ta tự viết.** Nó do **Keycloak** (Identity Provider ngoài) đảm nhiệm — lưu tài khoản, băm mật khẩu, phát hành JWT, quản lý 3 role PATIENT/DOCTOR/ADMIN. Vì vậy danh sách trên **không có** "auth-service" hay "account-service". Luồng: *Client → Keycloak (login, lấy JWT) → gửi JWT kèm request → api-gateway verify → service con.*

**Nguyên tắc vàng:** service này KHÔNG được truy cập thẳng vào database của service khác. Cần dữ liệu thì gọi qua API của nhau.

---

## 8. Mô hình dữ liệu (Data Model — mức khái niệm)

Mô tả các "bảng" chính và quan hệ, để dev hình dung. (Không phải schema SQL cuối cùng.)

```
PATIENT (Bệnh nhân)
  - id, keycloak_id (nối tới user đăng nhập bên Keycloak), họ tên, ngày sinh, giới tính, sđt, email, địa chỉ

DOCTOR (Bác sĩ)
  - id, keycloak_id (nối tới user đăng nhập bên Keycloak), họ tên, chuyên khoa, giờ_bắt_đầu_làm, giờ_kết_thúc_làm

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
- Xác thực: dùng **Keycloak** (OAuth2/OIDC) làm Identity Provider — không tự xây dựng cơ chế lưu mật khẩu/token.
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
| **Keycloak** | Phần mềm quản lý danh tính (Identity Provider) ngoài: lo đăng ký, đăng nhập, lưu mật khẩu, phát hành token — thay cho việc tự viết account service |
| **OAuth2 / OIDC** | Chuẩn mở để đăng nhập và cấp token; Keycloak nói theo chuẩn này |
| **JWT** | Loại token dạng chuỗi tự chứa thông tin (ID người dùng, role), gateway đọc để biết bạn là ai |
| **keycloak_id** | ID của user bên Keycloak, lưu trong hồ sơ patient/doctor để nối user đăng nhập với dữ liệu nghiệp vụ |
| **401 / 403 / 409** | Mã lỗi: chưa đăng nhập / không đủ quyền / dữ liệu bị xung đột (trùng lịch) |

---

# PHẦN B — ĐẶC TẢ KỸ THUẬT (cho dev)

> Phần A ở trên trả lời *"làm cái gì và vì sao"*. Phần B trả lời *"làm chính xác như thế nào"* — đủ chi tiết để code mà không phải đoán.

---

## 11. Bản đồ cổng & hạ tầng

| Thành phần | Cổng | Ghi chú |
|---|---|---|
| Keycloak | **8080** | Chạy Docker, realm `healthcare` |
| patient-service | **8081** | |
| doctor-service | **8082** | |
| appointment-service | **8083** | |
| notification-service | 8084 | *(chưa làm)* |
| api-gateway | 8090 | *(chưa làm)* |

**Database (Supabase, gói free):**

| Service | Supabase project | Schema | Bảng |
|---|---|---|---|
| patient-service | Singapore (`ap-southeast-1`) | `public` | `patients` |
| doctor-service | Mumbai (`ap-south-1`) | `public` | `doctors` |
| appointment-service | Mumbai — **dùng chung project với doctor** | `public` | `appointments` |

> ⚠️ **Thỏa hiệp có chủ ý:** appointment và doctor dùng chung Supabase project vì gói free giới hạn số project. Điều này **KHÔNG** cho phép hai service truy vấn bảng của nhau — nguyên tắc "gọi qua API" (mục 7) vẫn giữ nguyên. Dùng chung chỉ là chuyện hạ tầng, không phải chuyện thiết kế.

**Kết nối DB:** dùng **Session pooler cổng 5432**, KHÔNG dùng Transaction pooler (6543) — pooler 6543 không hỗ trợ prepared statement, gây lỗi khi Hibernate chạy CRUD.

---

## 12. Đặc tả API

Mọi response bọc trong `ApiResponse<T>`:
```json
{ "code": 200, "message": "Mô tả kết quả", "data": { ... } }
```

### 12.1. patient-service — `/api/patients`

| Method | Đường dẫn | Vai trò | Mô tả |
|---|---|---|---|
| POST | `/api/patients/` | PATIENT | Tạo hồ sơ của chính mình (US-02) |
| GET | `/api/patients/me` | PATIENT | Xem hồ sơ của chính mình |
| GET | `/api/patients/{id}` | chủ hồ sơ / ADMIN | Xem 1 hồ sơ (BR-06) |
| PUT | `/api/patients/{id}` | chủ hồ sơ / ADMIN | Sửa hồ sơ (BR-06) |
| GET | `/api/patients` | ADMIN | Danh sách toàn bộ |
| DELETE | `/api/patients/{id}` | ADMIN | Xóa hồ sơ |

### 12.2. doctor-service — `/api/doctors`

| Method | Đường dẫn | Vai trò | Mô tả |
|---|---|---|---|
| POST | `/api/doctors` | ADMIN | Thêm bác sĩ (US-03) |
| GET | `/api/doctors` | đã đăng nhập | Danh sách bác sĩ, lọc `?specialization=` (US-04) |
| GET | `/api/doctors/{id}` | đã đăng nhập | Chi tiết bác sĩ |
| GET | `/api/doctors/me` | DOCTOR | Hồ sơ của chính bác sĩ đang đăng nhập |
| PUT | `/api/doctors/{id}` | ADMIN / bác sĩ chính chủ | Sửa hồ sơ, giờ làm việc |
| DELETE | `/api/doctors/{id}` | ADMIN | Xóa bác sĩ |

> `GET /api/doctors/**` phải cho **mọi vai trò đã đăng nhập**, vì bệnh nhân cần xem bác sĩ để đặt lịch (US-04).

### 12.3. appointment-service — `/api/appointments`

| Method | Đường dẫn | Vai trò | Mô tả |
|---|---|---|---|
| POST | `/api/appointments` | PATIENT | Đặt lịch (US-05) |
| GET | `/api/appointments/patients/me` | PATIENT | Lịch hẹn của tôi |
| GET | `/api/appointments/doctors/me` | DOCTOR | Lịch của tôi, lọc `?date=` (US-07) |
| GET | `/api/appointments/{id}` | chủ lịch / ADMIN | Chi tiết (BR-06) |
| GET | `/api/appointments` | ADMIN | Tra cứu toàn bộ |
| PUT | `/api/appointments/{id}` | chủ lịch | Dời giờ hẹn / sửa lý do |
| PATCH | `/api/appointments/{id}/cancel` | chủ lịch | Hủy lịch (US-06) |
| PATCH | `/api/appointments/{id}/confirm` | DOCTOR / ADMIN | Xác nhận lịch |
| PATCH | `/api/appointments/{id}/complete` | DOCTOR | Đánh dấu đã khám xong |

> **Quy ước đường dẫn:** mọi endpoint về lịch hẹn nằm dưới `/api/appointments`, KHÔNG đặt dưới `/api/doctors/...` — vì api-gateway định tuyến theo tiền tố, đặt sai sẽ bị đẩy nhầm service.

---

## 13. Ma trận phân quyền

Ký hiệu: ✅ được — ❌ không — 🔒 chỉ dữ liệu của mình (BR-06)

| Hành động | PATIENT | DOCTOR | ADMIN |
|---|---|---|---|
| Tạo hồ sơ bệnh nhân | ✅ (của mình) | ❌ | ❌ |
| Xem hồ sơ bệnh nhân | 🔒 | ❌ | ✅ tất cả |
| Sửa hồ sơ bệnh nhân | 🔒 | ❌ | ✅ tất cả |
| Xem danh sách bác sĩ | ✅ | ✅ | ✅ |
| Thêm/xóa bác sĩ | ❌ | ❌ | ✅ |
| Sửa hồ sơ + giờ làm bác sĩ | ❌ | 🔒 | ✅ tất cả |
| Đặt lịch | ✅ | ❌ | ❌ |
| Xem lịch hẹn | 🔒 | 🔒 (của mình) | ✅ tất cả |
| Dời lịch | 🔒 | ❌ | ❌ |
| Hủy lịch | 🔒 | ❌ | ❌ |
| Xác nhận lịch | ❌ | ✅ | ✅ |
| Đánh dấu đã khám | ❌ | ✅ | ❌ |

**Hai tầng kiểm tra — phải làm ĐỦ CẢ HAI:**

| Tầng | Kiểm gì | Ở đâu |
|---|---|---|
| 1. Vai trò | *"Anh có phải PATIENT không?"* | `SecurityConfig` (theo URL) |
| 2. Chủ sở hữu (BR-06) | *"Có phải dữ liệu **của anh** không?"* | Trong service, so ID |

> ⚠️ Chỉ làm tầng 1 là **thủng BR-06**: bệnh nhân A biết ID của B vẫn xem/sửa được dữ liệu B. Tầng 2 bắt buộc phải có trong `getOne`, `update`, `cancel`.

---

## 14. Vòng đời trạng thái lịch hẹn

```
          đặt lịch (PATIENT)
                 │
                 ▼
            ┌─────────┐   confirm (DOCTOR/ADMIN)   ┌───────────┐
            │ PENDING │ ─────────────────────────▶ │ CONFIRMED │
            └─────────┘                            └───────────┘
                 │                                       │
                 │ cancel (PATIENT)                      │ cancel (PATIENT)
                 │                                       │ complete (DOCTOR)
                 ▼                                       ▼
           ┌───────────┐                    ┌───────────┐  ┌───────────┐
           │ CANCELLED │                    │ CANCELLED │  │ COMPLETED │
           └───────────┘                    └───────────┘  └───────────┘
              (kết thúc)                                      (kết thúc)
```

| Chuyển trạng thái | Ai làm | Điều kiện |
|---|---|---|
| *(mới)* → `PENDING` | PATIENT | Qua hết BR-01, BR-02, BR-03 |
| `PENDING` → `CONFIRMED` | DOCTOR / ADMIN | — |
| `PENDING`/`CONFIRMED` → `CANCELLED` | PATIENT (chủ lịch) | BR-04: chưa `COMPLETED` |
| `CONFIRMED` → `COMPLETED` | DOCTOR | Buổi khám đã diễn ra |

**Quy tắc bắt buộc:**
- `COMPLETED` và `CANCELLED` là **trạng thái cuối** — không chuyển đi đâu nữa.
- **Client KHÔNG được gửi `status` lên.** Trạng thái chỉ đổi qua các endpoint `/confirm`, `/cancel`, `/complete`. Nếu cho client gửi, bệnh nhân sẽ tự đặt lịch thành `COMPLETED` → phá BR-05.
- Lịch `CANCELLED` **không tính** khi kiểm trùng BR-01 (khung giờ đó đã trống).

---

## 15. Danh mục mã lỗi

| Mã lỗi | HTTP | Khi nào | Luật |
|---|---|---|---|
| `PATIENT_NOT_FOUND` | 404 | Không tìm thấy bệnh nhân | US-05 |
| `DOCTOR_NOT_FOUND` | 404 | Không tìm thấy bác sĩ | US-05 |
| `APPOINTMENT_NOT_FOUND` | 404 | Không tìm thấy lịch hẹn | — |
| `PATIENT_ALREADY_EXISTS` | 409 | Tài khoản đã có hồ sơ | US-02 |
| `APPOINTMENT_TIME_IN_PAST` | 400 | Đặt lịch vào quá khứ | **BR-02** |
| `OUTSIDE_WORKING_HOURS` | 400 | Ngoài giờ làm bác sĩ | **BR-03** |
| `APPOINTMENT_CONFLICT` | **409** | Bác sĩ đã có lịch trùng giờ | **BR-01** |
| `CANNOT_MODIFY_COMPLETED` | 400 | Sửa/hủy lịch đã khám xong | **BR-04** |
| `INVALID_STATUS_TRANSITION` | 400 | Chuyển trạng thái sai luồng | Mục 14 |
| `FORBIDDEN` | 403 | Đụng dữ liệu người khác | **BR-06** |
| `INVALID_INPUT` | 400 | Dữ liệu không hợp lệ | — |
| `DOCTOR_SERVICE_UNAVAILABLE` | 503 | Không gọi được service khác | Mục 16 |

> `GlobalExceptionHandler` phải trả **đúng HTTP status theo mã trong enum**, không được trả cứng 400/500 cho mọi lỗi. Riêng BR-01 bắt buộc **409** (US-05 ghi rõ).

---

## 16. Giao tiếp giữa các service

```
appointment-service ──HTTP+JWT──▶ doctor-service   (kiểm tồn tại + giờ làm việc)
appointment-service ──HTTP+JWT──▶ patient-service  (biết bệnh nhân đang đăng nhập là ai)
```

**Quy tắc bắt buộc:**

1. **Gọi REST** (`RestClient`), không truy vấn thẳng DB của service khác — kể cả khi dùng chung Supabase project.
2. **Phải chuyển tiếp JWT** của người gọi sang service đích:
   ```
   Authorization: Bearer <token đang cầm>
   ```
   Quên bước này → service đích trả **401**. Đây là lỗi hay gặp nhất.
3. **Địa chỉ service khai trong `application.yaml`**, không viết cứng trong code:
   ```yaml
   services:
     doctor:
       url: http://localhost:8082
   ```
4. Service đích chết → trả **503 `DOCTOR_SERVICE_UNAVAILABLE`**, không để lỗi 500 lọt ra ngoài.

**Một lần gọi `GET /api/doctors/{id}` phục vụ 2 việc:** kiểm bác sĩ tồn tại (US-05) *và* lấy giờ làm việc (BR-03). Không cần gọi 2 lần.

**Thứ tự kiểm tra khi đặt lịch — rẻ trước, tốn kém sau:**
```
1. BR-02 quá khứ?        → tự kiểm, không tốn gì
2. Bác sĩ tồn tại + BR-03 → gọi mạng sang doctor-service
3. BR-01 trùng lịch?      → truy vấn DB của mình
4. Lấy patientId từ token → gọi patient-service
5. Lưu
```

---

## 17. Quy ước kỹ thuật bắt buộc

### 17.1. Dữ liệu client KHÔNG được gửi lên

Đây là nguyên tắc bảo mật cốt lõi — **thứ gì server tự quyết được thì đừng nhận từ client**:

| Trường | Vì sao cấm | Server lấy từ đâu |
|---|---|---|
| `keycloakId` | Gửi ID người khác → chiếm hồ sơ | `jwt.getSubject()` |
| `patientId` (khi đặt lịch) | Đặt lịch dưới tên người khác | Gọi patient-service `/me` |
| `durationMinutes` | Gửi 1 phút → **né được BR-01** | Đọc từ `application.yaml` |
| `status` | Tự đặt `COMPLETED` → phá BR-05 | Chỉ đổi qua endpoint riêng |

### 17.2. Múi giờ

| Dữ liệu | Kiểu | Lý do |
|---|---|---|
| `appointmentTime` | `Instant` (lưu UTC) | Mốc thời gian tuyệt đối |
| `workStartTime` / `workEndTime` | `LocalTime` | Chỉ là giờ, lặp lại mỗi ngày |
| `createdAt` / `updatedAt` | `Instant` | Mốc hệ thống |

⚠️ **Khi kiểm BR-03 phải quy đổi múi giờ**, không so trực tiếp:
```java
static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
LocalTime gioHen = appointmentTime.atZone(CLINIC_ZONE).toLocalTime();
```
Gọi thẳng `LocalTime.from(instant)` sẽ **ném exception lúc chạy**.

### 17.3. Hằng số nghiệp vụ ra file cấu hình

```yaml
appointment:
  duration-minutes: 30      # BR-01
```
Đọc bằng `@Value("${appointment.duration-minutes}")`. **Không gán cứng số 30** trong entity lẫn trong câu SQL — nếu không, đổi config một nơi mà nơi kia vẫn dùng giá trị cũ → lọt lịch trùng.

### 17.4. Quản lý mật khẩu

- `application.yaml` (được commit) chứa mọi cấu hình **trừ mật khẩu**.
- Mật khẩu nằm trong `secrets.yaml` — **đã gitignore, không bao giờ push**.
- `application.yaml` nạp bằng: `spring.config.import: optional:classpath:secrets.yaml`

### 17.5. Công thức BR-01 (chi tiết cài đặt)

Hai khoảng thời gian trùng nhau khi: `bắt_đầu_1 < kết_thúc_2` **VÀ** `bắt_đầu_2 < kết_thúc_1`

```sql
SELECT EXISTS (
  SELECT 1 FROM appointments a
  WHERE a.doctor_id = :doctorId
    AND a.status <> 'CANCELLED'                    -- lịch đã hủy không tính
    AND a.appointment_time < :appointmentTime + (:durationMinutes * INTERVAL '1 minute')
    AND :appointmentTime < a.appointment_time + (a.duration_minutes * INTERVAL '1 minute')
)
```

⚠️ **Khi DỜI lịch** phải loại trừ chính nó, nếu không lịch sẽ tự trùng với chính mình và không bao giờ dời được:
```sql
AND a.id <> :currentId
```

**Index bắt buộc** (đã ghi trong ERD): `appointments(doctor_id, appointment_time)`.

---

## 18. Xác thực với Keycloak — cấu hình thực tế

| Mục | Giá trị |
|---|---|
| Realm | `healthcare` |
| Client | `healthcare-app` (public, bật Direct access grants) |
| Roles | `PATIENT`, `DOCTOR`, `ADMIN` |
| issuer-uri | `http://localhost:8080/realms/healthcare` |

**Cách tạo tài khoản theo từng vai trò:**

| Vai trò | Cách tạo | Ghi chú |
|---|---|---|
| PATIENT | **Tự đăng ký** trên trang Keycloak | Bật *User registration*; `PATIENT` là **default role** nên user mới tự có |
| DOCTOR | **ADMIN tạo** qua Keycloak Admin API | US-03 — bác sĩ không tự đăng ký |
| ADMIN | Tạo tay trong Keycloak Console | |

**Đọc role trong Spring:** Keycloak đặt role tại claim `realm_access.roles`. Spring **không tự đọc chỗ này** — bắt buộc viết `KeycloakRoleConverter` để map sang `ROLE_*`:
```java
List<String> roles = ((Map<String,Object>) jwt.getClaim("realm_access")).get("roles");
// -> "PATIENT" thành "ROLE_PATIENT"
```
Thiếu bước này thì `hasRole()` **luôn trả 403** dù token hoàn toàn hợp lệ.

---

*Hết PRD v1.1. Phần A (nghiệp vụ) giữ nguyên từ v1.0; Phần B (kỹ thuật) bổ sung ngày 23/07/2026 từ các quyết định phát sinh trong quá trình cài đặt.*

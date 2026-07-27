# Cấu hình Google Sign-In và email

Mã nguồn đã hỗ trợ đăng nhập Google qua Keycloak và gửi email xác nhận, đổi lịch,
xác nhận, hủy, hoàn tất và nhắc lịch trước 24 giờ. Hai phần dưới đây cần thông tin
bí mật của môi trường nên không được ghi trực tiếp vào Git.

## 1. Google Sign-In qua Keycloak

1. Trong Google Cloud Console, tạo OAuth Client loại **Web application**.
2. Thêm Authorized redirect URI:
   `http://localhost:8080/realms/healthcare/broker/google/endpoint`.
3. Trong Keycloak Admin Console, chọn realm `healthcare` → **Identity providers** →
   **Google**, nhập Client ID và Client Secret. Giữ alias là `google`.
4. Trong client `healthcare-app`, cấu hình:
   - Valid redirect URIs: `http://localhost:3000/*`
   - Web origins: `http://localhost:3000`
   - Standard flow: bật; PKCE method: `S256`
5. Đặt realm role `PATIENT` làm default role để tài khoản Google mới có đúng quyền.
6. Sao chép [frontend/.env.example](frontend/.env.example) thành `frontend/.env.local`
   nếu URL, realm, client hoặc alias của bạn khác giá trị mặc định.

Sau khi đăng nhập lần đầu, email từ Google sẽ được tự điền vào hồ sơ bệnh nhân.

## 2. SMTP Gmail

Gmail yêu cầu bật xác minh hai bước và tạo **App password**; không dùng mật khẩu
Google thông thường.

Sao chép
[notification-service/src/main/resources/secrets.example.yaml](notification-service/src/main/resources/secrets.example.yaml)
thành `secrets.yaml` trong cùng thư mục rồi điền:

- `spring.datasource.password`: mật khẩu database;
- `spring.mail.username`: địa chỉ Gmail gửi đi;
- `spring.mail.password`: App password 16 ký tự;
- `notification.mail-from`: cùng địa chỉ Gmail hoặc alias đã được Gmail cho phép.

Có thể dùng các biến môi trường `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`,
`MAIL_PASSWORD`, `MAIL_FROM` thay cho file secrets. Nhật ký mail được lưu trong bảng
`notifications` với trạng thái `PENDING`, `SENT`, `FAILED` hoặc `CANCELLED`.

## 3. Kiểm tra nhanh

1. Khởi động Keycloak, các service và frontend.
2. Đăng nhập bằng Google, hoàn tất hồ sơ bệnh nhân (email là bắt buộc).
3. Đặt một lịch cách hiện tại hơn 24 giờ.
4. Kiểm tra email xác nhận và bảng `notifications`: một bản ghi mail tức thì cùng
   một bản ghi `REMINDER` chờ gửi trước lịch 24 giờ.

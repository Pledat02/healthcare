# JMeter race test: 10 patient accounts

Test plan nay de 10 Keycloak patient account dang nhap rieng, lay 10 access token
khac nhau, sau do cung gui request dat mot doctor/appointment time.

## Truoc khi chay

1. Tao 10 tai khoan test va patient profile (co the chay lai):

   ```powershell
   powershell -ExecutionPolicy Bypass -File tools/jmeter-race-test/provision-test-users.ps1
   ```

   Script tao mat khau ngau nhien trong `users.csv`; file nay da duoc `.gitignore`.
2. Mo `appointment-race-10-users.jmx` trong JMeter.
3. Chon node goc `Healthcare - 10 Users Appointment Race` va sua:
   - `CLIENT_ID`: mac dinh `jmeter-test`, client rieng co Direct Access Grants.
   - `DOCTOR_ID`: da gan bac si test ton tai; doi neu can.
   - `APPOINTMENT_TIME`: dang la `2026-08-20T03:00:00Z` (10:00 Asia/Bangkok); doi sau moi lan test.
4. Dam bao ca 10 Keycloak user co role `PATIENT` va co profile trong patient-service.

Mo nhanh:

```powershell
powershell -ExecutionPolicy Bypass -File .\open-jmeter.ps1
```

Neu dang o thu muc goc du an, chay:

```powershell
powershell -ExecutionPolicy Bypass -File tools/jmeter-race-test/open-jmeter.ps1
```

## Ket qua mong doi

- 10 mau `02 - Login Keycloak`: HTTP 200.
- 1 mau `03 - Create Same Appointment`: HTTP 200.
- 9 mau `03 - Create Same Appointment`: HTTP 409.

Neu co tu 2 request dat lich HTTP 200 tro len thi co race condition. Neu ca 10 deu
409, slot da ton tai truoc test; doi `APPOINTMENT_TIME`.

## Loi thuong gap

- `unauthorized_client`: `CLIENT_ID` chua bat Direct Access Grants. Chi bat flow
  nay tren local/test, hoac tao client `jmeter-test` rieng.
- `invalid_grant`: sai username/password, user disabled, hoac user phai doi mat khau.
- HTTP 401/403 khi dat lich: token thieu role `PATIENT`.
- HTTP 404 tu patient-service: Keycloak user chua co patient profile.
- HTTP 400: doctor/time khong hop le, ngoai gio lam viec, ngay nghi, hoac qua khu.

GUI chi phu hop de tao va debug plan. Khi chay tai lon, dung JMeter CLI theo
khuyen nghi cua Apache.

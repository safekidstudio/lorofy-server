# Lorofy Server 🚀

Backend API cho ứng dụng **Lorofy** (Ứng dụng tập trung Pomodoro tích hợp Game hóa và Mạng xã hội). Dự án được xây dựng bằng **Java Spring Boot 4.x (Spring Framework 7.x)** và kết nối tới cơ sở dữ liệu **Neon PostgreSQL Serverless**.

---

## 🛠️ Công nghệ sử dụng (Tech Stack)

* **Core Framework:** Java 21, Spring Boot 4.1.0, Spring Security (JWT)
* **Database:** Neon PostgreSQL (Serverless), Flyway (Database Migration)
* **File Storage:** Cloudinary (Lưu trữ ảnh đại diện, media assets)
* **API Documentation:** Springdoc OpenAPI (Swagger UI)
* **Build Tool:** Gradle

---

## 📁 Cấu trúc thư mục dự án

```text
src/main/java/com/lorofy/server/
├── core/                        # Các thành phần cấu hình dùng chung toàn hệ thống
│   ├── config/                  # Cấu hình Security, Web, Swagger
│   ├── exception/               # Xử lý ngoại lệ tập trung (GlobalExceptionHandler)
│   ├── security/                # Cấu hình JWT, Custom User Details
│   └── storage/                 # Chiến lược lưu trữ (Strategy Pattern)
│       └── cloudinary/          # Module tích hợp Cloudinary
│
└── features/                    # Các mô-đun nghiệp vụ (Feature-First)
    ├── auth/                    # Đăng ký, Đăng nhập, Token JWT
    ├── profile/                 # Quản lý hồ sơ người dùng, Onboarding, Quốc gia
    └── media/                   # Quản lý tệp tin, liên kết Media Asset
```

---

## 💻 Hướng dẫn chạy dự án cục bộ (Local Setup)

### 1. Chuẩn bị môi trường
* **JDK 21** trở lên.
* Một database PostgreSQL (khuyên dùng tài khoản miễn phí trên [Neon.tech](https://neon.tech/)).
* Một tài khoản Cloudinary miễn phí để lưu ảnh.

### 2. Cấu hình file bí mật cục bộ
Tạo một file mới tên là **`application-local.properties`** tại thư mục:
👉 `src/main/resources/application-local.properties` (File này đã được cấu hình trong `.gitignore` để không bị push lên Git).

Điền các thông số kết nối của bạn vào file đó:
```properties
# Kết nối Neon PostgreSQL
spring.datasource.url=jdbc:postgresql://<your-neon-host>/neondb?sslmode=require&channelBinding=require
spring.datasource.username=<your-username>
spring.datasource.password=<your-password>

# Cấu hình JWT (Chuỗi secret ngẫu nhiên dài tối thiểu 256-bit)
app.jwt.secret=<your-256bit-base64-secret-key>

# Cấu hình Cloudinary
app.cloudinary.cloud-name=<your-cloud-name>
app.cloudinary.api-key=<your-api-key>
app.cloudinary.api-secret=<your-api-secret>
```

### 3. Khởi động ứng dụng
Chạy lệnh Gradle trong thư mục gốc của dự án:

* **Trên Windows (PowerShell):**
  ```powershell
  $env:JAVA_TOOL_OPTIONS="-Djava.net.preferIPv4Stack=true"
  ./gradlew clean bootRun
  ```
* **Trên Linux / macOS:**
  ```bash
  ./gradlew clean bootRun
  ```

---

## 📝 Tài liệu API (Swagger UI)

Sau khi server khởi chạy thành công trên cổng `8080`, bạn truy cập đường dẫn sau để xem tài liệu và test API trực quan:
👉 **`http://localhost:8080/api-docs`**

* **Đường dẫn JSON đặc tả:** `http://localhost:8080/api-docs-json`

---

## 🔐 Luồng xác thực API (JWT Flow)
1. Thực hiện Đăng ký qua `POST /api/v1/auth/register`.
2. Thực hiện Đăng nhập qua `POST /api/v1/auth/login` -> Nhận về `accessToken`.
3. Nhấp vào nút **Authorize** ở góc phải Swagger UI, nhập Token nhận được để mở khóa các API bảo mật.

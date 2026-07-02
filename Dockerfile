# Stage 1: Build stage (Sử dụng ảnh Gradle chính thức chạy trên Alpine Linux để biên dịch ứng dụng)
FROM gradle:8-jdk21-alpine AS builder
WORKDIR /app

# Copy mã nguồn dự án vào container
COPY --chown=gradle:gradle . .

# Cấp quyền thực thi cho gradlew (Sửa lỗi Permission Denied khi đẩy code từ Windows)
RUN chmod +x gradlew

# Thực hiện biên dịch sinh file .jar và bỏ qua unit test để tăng tốc build khi deploy
RUN ./gradlew build -x test --no-daemon

# Stage 2: Runtime stage (Sử dụng JRE 21 siêu nhẹ để chạy ứng dụng)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Chỉ copy file .jar đã được biên dịch từ Stage 1 sang Stage 2
COPY --from=builder /app/build/libs/*.jar app.jar

# Mở cổng 8080
EXPOSE 8080

# Khởi chạy ứng dụng với cấu hình tối ưu hóa RAM cho các server free tier (Giới hạn 512MB)
# -Xmx384m: Giới hạn dung lượng bộ nhớ Heap tối đa của Java ở mức 384MB (tránh bị crash do tràn RAM 512MB của host)
# -XX:+UseG1GC: Sử dụng bộ dọn rác G1GC hiệu năng cao, tối ưu cho bộ nhớ nhỏ
ENTRYPOINT ["java", "-Xmx384m", "-XX:+UseG1GC", "-jar", "app.jar"]

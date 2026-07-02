# Kế hoạch phát triển Lorofy Backend (Java Spring Boot + Neon PostgreSQL)

Tài liệu này tập trung hoàn toàn vào kiến trúc, thiết kế cơ sở dữ liệu và kế hoạch triển khai chi tiết cho phần **Backend (API Server)** của Lorofy.

---

## 1. Tech Stack Backend

| Hạng mục | Công nghệ | Ghi chú / Mục đích sử dụng |
|---|---|---|
| **Ngôn ngữ** | **Java (JDK 21+)** | Đảm bảo hiệu năng cao, kiểu dữ liệu an toàn và hỗ trợ các tính năng Java hiện đại (Virtual Threads, Pattern Matching). |
| **Framework** | **Spring Boot 3.x / 4.x** | Sử dụng Spring Web, Spring Data JPA, Spring Security, Spring Validation. |
| **Database** | **PostgreSQL (Neon.tech)** | Serverless database với tự động điều chỉnh tài nguyên và kết nối mã hóa SSL. |
| **Migration** | **Flyway** | Quản lý và đồng bộ phiên bản Database Schema tự động qua mã SQL định dạng `V<num>__<name>.sql`. |
| **Security** | **Spring Security & JWT** | Cơ chế xác thực stateless bằng Access Token (ngắn hạn) và Refresh Token (dài hạn). |
| **Real-time** | **Spring WebSocket + STOMP** | Đồng bộ trạng thái phiên tập trung cùng bạn bè theo thời gian thực. |
| **Notification** | **Firebase Admin SDK** | Gửi thông báo đẩy (Push Notifications) đến các thiết bị di động. |
| **Dev Tools** | **Lombok & MapStruct** | Giảm thiểu mã boilerplate (getter/setter) và map DTO - Entity tự động. |
| **Testing** | **JUnit 5 + Mockito + Testcontainers** | Viết Unit Test và Integration Test kiểm thử tương tác DB. |

---

## 2. Thiết kế Cơ sở dữ liệu (Neon PostgreSQL DDL)

Sơ đồ bảng dữ liệu tối ưu hóa cho toàn bộ các tính năng của Lorofy (bao gồm các chế độ chặn, lịch biểu phức tạp, nhóm học tập, bảng xếp hạng, và cơ chế gamification):

```sql
-- 1. Bảng Profiles (Liên kết với User trong hệ thống Auth)
CREATE TABLE profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(255) UNIQUE NOT NULL,      -- ID người dùng từ hệ thống Auth (ví dụ Firebase Auth / Auth0)
    username VARCHAR(50) UNIQUE NOT NULL,
    avatar_url TEXT,
    school_name VARCHAR(100),
    class_name VARCHAR(50),
    is_anonymous BOOLEAN DEFAULT FALSE,
    rank_points INT DEFAULT 0,                 -- Điểm xếp hạng (dùng cho Leaderboard)
    gold_coins INT DEFAULT 0,                  -- Xu vàng dùng mua vật phẩm ảo
    current_badge VARCHAR(50) DEFAULT 'BRONZE', -- BRONZE, SILVER, GOLD, PLATINUM, DIAMOND
    total_focus_minutes INT DEFAULT 0,
    current_streak INT DEFAULT 0,
    longest_streak INT DEFAULT 0,
    streak_freeze_count INT DEFAULT 0,         -- Số lượng vật phẩm đóng băng streak hiện có
    last_streak_freeze_used TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 2. Bảng Focus Sessions (Lưu lịch sử tập trung)
CREATE TABLE focus_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID REFERENCES profiles(id) ON DELETE CASCADE,
    category VARCHAR(50) NOT NULL,             -- STUDY, WORK, READ, HEALTH, OTHERS
    block_mode VARCHAR(20) NOT NULL,           -- LIGHT, MEDIUM, STRICT
    planned_minutes INT NOT NULL,
    actual_minutes INT DEFAULT 0,
    status VARCHAR(20) NOT NULL,               -- RUNNING, COMPLETED, FAILED, PAUSED
    pause_count INT DEFAULT 0,
    failure_reason TEXT,                       -- Lý do thoát (nếu có)
    started_at TIMESTAMPTZ NOT NULL,
    ended_at TIMESTAMPTZ,
    friend_session_id UUID,                    -- Mã nhóm nếu tập trung cùng bạn bè (Buddy focus)
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 3. Bảng App/Web Violations (Lưu lịch sử vi phạm mở app/web bị chặn)
CREATE TABLE app_violations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    focus_session_id UUID REFERENCES focus_sessions(id) ON DELETE CASCADE,
    app_identifier VARCHAR(255) NOT NULL,      -- Tên package app (com.facebook.katana) hoặc domain web
    app_name VARCHAR(100),
    attempted_at TIMESTAMPTZ DEFAULT NOW()
);

-- 4. Bảng Break Glass Logs (Lịch sử sử dụng nút thoát khẩn cấp)
CREATE TABLE break_glass_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    focus_session_id UUID REFERENCES focus_sessions(id) ON DELETE CASCADE,
    penalty_points INT DEFAULT 0,              -- Số điểm bị trừ phạt
    penalty_streak_lost INT DEFAULT 0,         -- Số ngày streak bị mất
    reason TEXT,
    triggered_at TIMESTAMPTZ DEFAULT NOW()
);

-- 5. Bảng Blocked Apps (Danh sách cấu hình chặn tùy chọn)
CREATE TABLE blocked_apps (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID REFERENCES profiles(id) ON DELETE CASCADE,
    app_identifier VARCHAR(255) NOT NULL,      -- Tên package ứng dụng cần chặn
    app_name VARCHAR(100) NOT NULL,
    block_mode VARCHAR(20) NOT NULL,           -- Chế độ áp dụng (LIGHT, MEDIUM, STRICT)
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 6. Bảng Scheduled Sessions (Lập lịch tập trung định kỳ)
CREATE TABLE scheduled_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID REFERENCES profiles(id) ON DELETE CASCADE,
    title VARCHAR(100) NOT NULL,
    template_type VARCHAR(30) NOT NULL,        -- CUSTOM, POMODORO_CLASSIC
    recurrence_rule TEXT,                      -- Chuỗi định dạng iCal RRULE (ví dụ: FREQ=DAILY)
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 7. Bảng Friendships (Hệ thống bạn bè)
CREATE TABLE friendships (
    profile_id UUID REFERENCES profiles(id) ON DELETE CASCADE,
    friend_id UUID REFERENCES profiles(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL,               -- PENDING, ACCEPTED, BLOCKED
    created_at TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (profile_id, friend_id)
);

-- 8. Bảng Groups & Group Members (Phòng/Nhóm thi đua)
CREATE TABLE groups (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    invite_code VARCHAR(20) UNIQUE NOT NULL,    -- Mã mời 6 ký tự viết hoa ngẫu nhiên
    created_by UUID REFERENCES profiles(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE group_members (
    group_id UUID REFERENCES groups(id) ON DELETE CASCADE,
    profile_id UUID REFERENCES profiles(id) ON DELETE CASCADE,
    role VARCHAR(20) DEFAULT 'MEMBER',         -- OWNER, ADMIN, MEMBER
    joined_at TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (group_id, profile_id)
);

-- 9. Bảng Challenges & Participants (Thử thách cộng đồng)
CREATE TABLE challenges (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(100) NOT NULL,
    description TEXT,
    target_focus_minutes INT NOT NULL,         -- Số phút yêu cầu mỗi ngày
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    created_by UUID REFERENCES profiles(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE challenge_participants (
    challenge_id UUID REFERENCES challenges(id) ON DELETE CASCADE,
    profile_id UUID REFERENCES profiles(id) ON DELETE CASCADE,
    is_active BOOLEAN DEFAULT TRUE,            -- FALSE nếu không hoàn thành mục tiêu ngày (bị loại)
    joined_at TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (challenge_id, profile_id)
);

-- 10. Bảng Gamification Shop Items
CREATE TABLE gamification_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    type VARCHAR(30) NOT NULL,                 -- AVATAR, THEME, SOUND
    cost_points INT NOT NULL,
    asset_url TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE unlocked_items (
    profile_id UUID REFERENCES profiles(id) ON DELETE CASCADE,
    item_id UUID REFERENCES gamification_items(id) ON DELETE CASCADE,
    unlocked_at TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (profile_id, item_id)
);

-- 11. Bảng User Quests (Nhiệm vụ hàng ngày/tuần)
CREATE TABLE user_quests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID REFERENCES profiles(id) ON DELETE CASCADE,
    quest_type VARCHAR(50) NOT NULL,           -- FOCUS_DAILY, FOCUS_WEEKLY, FRIEND_ADDED
    target_value INT NOT NULL,                 -- Số phút hoặc số lượt cần đạt
    current_value INT DEFAULT 0,
    reward_points INT NOT NULL,
    reward_coins INT NOT NULL,
    is_claimed BOOLEAN DEFAULT FALSE,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);
```

---

## 3. Các Phase phát triển Backend (Java Spring Boot)

### **Phase 0 — Cấu hình & Database Kết nối (Tuần 1)**
*   **Công việc:**
    - Cài đặt cấu hình kết nối tối ưu với Neon PostgreSQL qua HikariCP (SSL bắt buộc).
    - Tạo cấu trúc thư mục tiêu chuẩn: `core` (config, exception, security) và `features` (auth, focus, v.v.).
    - Tích hợp Flyway và chạy file migration đầu tiên `V1__init_schema.sql` để tạo toàn bộ bảng.
    - Viết `@ControllerAdvice` xử lý lỗi tập trung.

### **Phase 1 — Xác thực & Khởi tạo Profile (Tuần 1)**
*   **Công việc:**
    - Cấu hình Spring Security lọc xác thực Stateless JWT (Access & Refresh Token).
    - Xây dựng API Đăng nhập / Đăng ký.
    - Phát triển cơ chế tự động tạo thực thể `Profile` trong cơ sở dữ liệu khi tài khoản Auth được xác thực thành công lần đầu.
    - Các API cập nhật thông tin cá nhân (onboarding) và whitelist/blacklist app riêng lẻ.

### **Phase 2 — Core Focus Engine & WebSocket (Tuần 2)**
*   **Công việc:**
    - API bắt đầu, tạm dừng, tiếp tục và kết thúc phiên tập trung (`/start`, `/pause`, `/resume`, `/end`).
    - Logic tính toán phần thưởng điểm xếp hạng (`rank_points`) và xu vàng (`gold_coins`) dựa trên thời lượng và độ khó của chế độ chặn (LIGHT = x1.0, MEDIUM = x1.2, STRICT = x1.5).
    - Tích hợp **Spring WebSocket** với giao thức **STOMP** để xử lý đồng bộ thời gian thực cho tính năng "Tập trung cùng bạn bè".

### **Phase 3 — Xử phạt & Lịch sử vi phạm (Tuần 1.5)**
*   **Công việc:**
    - API lưu nhận log vi phạm ứng dụng từ client `/violations/report` (được lưu vào bảng `app_violations` để tạo Failure Report).
    - API kích hoạt nút thoát khẩn cấp `/break-glass`.
    - Thiết lập logic phạt:
      - Trừ điểm rank.
      - Kiểm tra và tự động sử dụng `streak_freeze_count` nếu có để bảo toàn chuỗi streak, nếu không có hoặc hết lượt sẽ reset `current_streak` về 0.

### **Phase 4 — Lập lịch & Giải quyết Trùng lịch (Tuần 1.5)**
*   **Công việc:**
    - API CRUD quản lý thời khóa biểu học tập cố định (`scheduled_sessions`).
    - Viết thuật toán phân tích chuỗi **iCal RRULE** trên server để xác định các ngày lặp lại.
    - Phát triển logic **Conflict Resolver** kiểm tra trùng lặp thời gian biểu khi người dùng cố gắng thêm hoặc cập nhật một khung giờ mới.

### **Phase 5 — Xếp hạng & Tương tác Nhóm (Tuần 1.5)**
*   **Công việc:**
    - Xây dựng tính năng bạn bè (Gửi yêu cầu, chấp nhận, từ chối, danh sách bạn bè).
    - Phát triển tính năng Nhóm: Tạo nhóm mới (tự tạo mã mời 6 chữ số ngẫu nhiên), tham gia nhóm bằng mã mời.
    - API Bảng xếp hạng (`/leaderboard`): Truy vấn dữ liệu có phân trang, lọc theo Global, Friends, Group, và theo thời gian (Daily, Weekly, Monthly, All-time).
    - Tối ưu hóa câu lệnh SQL (sử dụng Index trên `rank_points`, `profiles.school_name`, `profiles.class_name`) để đảm bảo tốc độ phản hồi < 100ms.

### **Phase 6 — Cửa hàng ảo, Nhiệm vụ & Báo cáo Analytics (Tuần 1.5)**
*   **Công việc:**
    - API Cửa hàng: Mua vật phẩm ảo (Avatar, Theme, Sound) và vật phẩm phụ trợ (Streak Freeze).
    - Xây dựng **Quests Engine**: Dùng `@Scheduled` chạy ngầm để sinh nhiệm vụ ngẫu nhiên hàng ngày/tuần và reset trạng thái nhiệm vụ.
    - API Analytics: Trả về phân tích tổng quan số phút tập trung, số lần thất bại, và danh sách các app/web hay vi phạm nhiều nhất làm cơ sở vẽ biểu đồ.

### **Phase 7 — Push Notification & Đưa lên Cloud (Tuần 2)**
*   **Công việc:**
    - Tích hợp SDK Firebase Admin để push thông báo (Ví dụ: nhắc lịch trước 5 phút, báo có lời mời kết bạn, báo cáo tuần).
    - Đóng gói container Docker (Dockerfile, docker-compose).
    - Hướng dẫn cấu hình biến môi trường và deploy lên các dịch vụ PaaS như Render, Railway, hoặc Koyeb.

-- ===================================================================
-- LOROFY DATABASE SCHEMA — FULL MIGRATION (v3 — chạy 1 lần duy nhất)
-- Java + PostgreSQL + Cloudinary (mở rộng multi-provider qua media_assets)
--
-- Gồm: 19 bảng, seed data, toàn bộ index cho FK, CHECK constraint cho
-- enum giả, bảng media_assets trung tâm (provider-agnostic).
--
-- LƯU Ý QUAN TRỌNG: Postgres thuần KHÔNG có RLS như Supabase — mọi điều
-- kiện "chỉ đọc/ghi data của chính mình" PHẢI được enforce ở tầng Java
-- (Spring Security / Service layer). Schema này không tự bảo vệ điều đó.
--
-- Khuyến nghị: chạy qua Flyway/Liquibase thay vì chạy tay để track version.
-- ===================================================================


-- ===================================================================
-- PHASE 0: MEDIA ASSETS (TRUNG TÂM) & BẢNG TRA CỨU
-- ===================================================================

-- 0. Media Assets — nguồn sự thật DUY NHẤT cho mọi asset (ảnh/video/file),
--    bất kể lưu ở provider nào. Đổi provider cho 1 asset chỉ cần UPDATE
--    đúng 1 dòng ở đây, không đụng tới bất kỳ bảng nghiệp vụ nào khác.
CREATE TABLE media_assets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Provider hiện tại đang giữ asset này (mặc định Cloudinary cho v1)
    provider VARCHAR(20) NOT NULL DEFAULT 'CLOUDINARY',

    -- Định danh asset TRONG PHẠM VI provider — ý nghĩa khác nhau tuỳ provider:
    --   CLOUDINARY        -> public_id      (vd: 'lorofy/avatars/8f19a2e3')
    --   S3 / R2            -> object key      (vd: 'avatars/8f19a2e3.webp')
    --   FIREBASE_STORAGE  -> storage path
    --   SUPABASE_STORAGE  -> "{bucket}/{path}"
    asset_key VARCHAR(500) NOT NULL,

    resource_type VARCHAR(10) NOT NULL DEFAULT 'IMAGE',   -- IMAGE, VIDEO, RAW
    format VARCHAR(10),                                    -- png, jpg, webp, mp3, mp4...

    -- Cache-busting: Cloudinary có version built-in; provider khác để NULL
    -- và tự cache-bust bằng cách đổi asset_key hoặc query param riêng.
    version BIGINT,

    width INT,
    height INT,
    bytes INT,

    -- Nhóm logic của asset (folder Cloudinary / bucket S3-R2 / bucket Supabase)
    collection VARCHAR(255),

    -- Domain CDN riêng nếu asset không dùng domain mặc định của provider.
    -- NULL = tầng Java tự build URL theo domain mặc định của provider đó.
    custom_domain VARCHAR(255),

    -- FK tới profiles được ALTER thêm ở cuối Phase 1 (profiles tạo sau
    -- media_assets vì countries/ranks/gamification_items cần bảng này trước)
    uploaded_by UUID,

    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),

    CONSTRAINT chk_media_provider CHECK (
        provider IN ('CLOUDINARY','S3','R2','FIREBASE_STORAGE','SUPABASE_STORAGE')
    ),
    CONSTRAINT chk_media_resource_type CHECK (
        resource_type IN ('IMAGE','VIDEO','RAW')
    ),
    CONSTRAINT uq_media_assets_provider_key UNIQUE (provider, asset_key)
);

CREATE INDEX idx_media_assets_uploaded_by ON media_assets(uploaded_by);
CREATE INDEX idx_media_assets_provider ON media_assets(provider);

-- 1. Bảng Quốc Gia
CREATE TABLE countries (
    code CHAR(2) PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,
    flag_asset_id UUID REFERENCES media_assets(id) ON DELETE SET NULL
);

CREATE INDEX idx_countries_flag_asset_id ON countries(flag_asset_id);

-- 2. Bảng Cấp Bậc Rank
CREATE TABLE ranks (
    id UUID PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL,
    min_points INT UNIQUE NOT NULL CHECK (min_points >= 0),
    badge_asset_id UUID REFERENCES media_assets(id) ON DELETE SET NULL
);

CREATE INDEX idx_ranks_badge_asset_id ON ranks(badge_asset_id);

-- 3. Bảng Vật Phẩm Cửa Hàng (mọi item bắt buộc có asset hiển thị)
CREATE TABLE gamification_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    type VARCHAR(30) NOT NULL,
    cost_points INT NOT NULL CHECK (cost_points >= 0),
    asset_id UUID NOT NULL REFERENCES media_assets(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT chk_item_type CHECK (type IN ('AVATAR','THEME','SOUND','STREAK_FREEZE'))
);

CREATE INDEX idx_gamification_items_asset_id ON gamification_items(asset_id);

-- ===================================================================
-- SEED: Media Assets tĩnh trước, rồi mới seed các bảng tham chiếu tới nó
-- ===================================================================

INSERT INTO media_assets (id, provider, asset_key, resource_type, format, collection) VALUES
('10000000-0000-0000-0000-000000000001'::uuid, 'CLOUDINARY', 'lorofy/flags/vn', 'IMAGE', 'png', 'flags'),
('10000000-0000-0000-0000-000000000002'::uuid, 'CLOUDINARY', 'lorofy/flags/us', 'IMAGE', 'png', 'flags'),
('10000000-0000-0000-0000-000000000003'::uuid, 'CLOUDINARY', 'lorofy/flags/jp', 'IMAGE', 'png', 'flags'),
('10000000-0000-0000-0000-000000000004'::uuid, 'CLOUDINARY', 'lorofy/flags/sg', 'IMAGE', 'png', 'flags'),
('10000000-0000-0000-0000-000000000005'::uuid, 'CLOUDINARY', 'lorofy/flags/kr', 'IMAGE', 'png', 'flags');

INSERT INTO media_assets (id, provider, asset_key, resource_type, format, collection) VALUES
('20000000-0000-0000-0000-000000000001'::uuid, 'CLOUDINARY', 'lorofy/ranks/bronze', 'IMAGE', 'png', 'ranks'),
('20000000-0000-0000-0000-000000000002'::uuid, 'CLOUDINARY', 'lorofy/ranks/silver', 'IMAGE', 'png', 'ranks'),
('20000000-0000-0000-0000-000000000003'::uuid, 'CLOUDINARY', 'lorofy/ranks/gold', 'IMAGE', 'png', 'ranks'),
('20000000-0000-0000-0000-000000000004'::uuid, 'CLOUDINARY', 'lorofy/ranks/platinum', 'IMAGE', 'png', 'ranks'),
('20000000-0000-0000-0000-000000000005'::uuid, 'CLOUDINARY', 'lorofy/ranks/diamond', 'IMAGE', 'png', 'ranks');

INSERT INTO media_assets (id, provider, asset_key, resource_type, format, collection) VALUES
('30000000-0000-0000-0000-000000000001'::uuid, 'CLOUDINARY', 'lorofy/shop/avatars/magic-tree', 'IMAGE', 'png', 'shop_avatars'),
('30000000-0000-0000-0000-000000000002'::uuid, 'CLOUDINARY', 'lorofy/shop/themes/pine-sunset', 'IMAGE', 'png', 'shop_themes'),
('30000000-0000-0000-0000-000000000003'::uuid, 'CLOUDINARY', 'lorofy/shop/sounds/heavy-rain', 'RAW', 'mp3', 'shop_sounds'),
('30000000-0000-0000-0000-000000000004'::uuid, 'CLOUDINARY', 'lorofy/shop/items/streak-freeze', 'IMAGE', 'png', 'shop_items');

INSERT INTO countries (code, name, flag_asset_id) VALUES
('VN', 'Vietnam', '10000000-0000-0000-0000-000000000001'::uuid),
('US', 'United States', '10000000-0000-0000-0000-000000000002'::uuid),
('JP', 'Japan', '10000000-0000-0000-0000-000000000003'::uuid),
('SG', 'Singapore', '10000000-0000-0000-0000-000000000004'::uuid),
('KR', 'South Korea', '10000000-0000-0000-0000-000000000005'::uuid);

INSERT INTO ranks (id, name, min_points, badge_asset_id) VALUES
('e1b0451a-1234-4321-abcd-5f9876543210'::uuid, 'BRONZE', 0, '20000000-0000-0000-0000-000000000001'::uuid),
('e1b0451a-1234-4321-abcd-5f9876543211'::uuid, 'SILVER', 1000, '20000000-0000-0000-0000-000000000002'::uuid),
('e1b0451a-1234-4321-abcd-5f9876543212'::uuid, 'GOLD', 2500, '20000000-0000-0000-0000-000000000003'::uuid),
('e1b0451a-1234-4321-abcd-5f9876543213'::uuid, 'PLATINUM', 5000, '20000000-0000-0000-0000-000000000004'::uuid),
('e1b0451a-1234-4321-abcd-5f9876543214'::uuid, 'DIAMOND', 10000, '20000000-0000-0000-0000-000000000005'::uuid);

INSERT INTO gamification_items (id, name, type, cost_points, asset_id) VALUES
(gen_random_uuid(), 'Cây Cổ Thụ Thần Kỳ', 'AVATAR', 500, '30000000-0000-0000-0000-000000000001'::uuid),
(gen_random_uuid(), 'Rừng Thông Hoàng Hôn', 'THEME', 1000, '30000000-0000-0000-0000-000000000002'::uuid),
(gen_random_uuid(), 'Tiếng Mưa Rào', 'SOUND', 300, '30000000-0000-0000-0000-000000000003'::uuid),
(gen_random_uuid(), 'Lá Chắn Đóng Băng', 'STREAK_FREEZE', 800, '30000000-0000-0000-0000-000000000004'::uuid);


-- ===================================================================
-- PHASE 1: HỒ SƠ NGƯỜI DÙNG & MỐI QUAN HỆ
-- ===================================================================

-- 4. Bảng Profiles
CREATE TABLE profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(255) UNIQUE NOT NULL,
    username VARCHAR(50) UNIQUE NOT NULL,
    avatar_asset_id UUID REFERENCES media_assets(id) ON DELETE SET NULL,
    country_code CHAR(2) DEFAULT 'VN' REFERENCES countries(code) ON DELETE SET DEFAULT,
    timezone VARCHAR(50) DEFAULT 'Asia/Ho_Chi_Minh' NOT NULL,
    current_rank_id UUID DEFAULT 'e1b0451a-1234-4321-abcd-5f9876543210'::uuid REFERENCES ranks(id) ON DELETE SET NULL,
    is_anonymous BOOLEAN DEFAULT FALSE,
    rank_points INT DEFAULT 0 CHECK (rank_points >= 0),
    gold_coins INT DEFAULT 0 CHECK (gold_coins >= 0),
    total_focus_minutes INT DEFAULT 0 CHECK (total_focus_minutes >= 0),
    current_streak INT DEFAULT 0 CHECK (current_streak >= 0),
    longest_streak INT DEFAULT 0 CHECK (longest_streak >= 0),
    streak_freeze_count INT DEFAULT 0 CHECK (streak_freeze_count >= 0),
    last_streak_freeze_used TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_profiles_country_code ON profiles(country_code);
CREATE INDEX idx_profiles_current_rank_id ON profiles(current_rank_id);
CREATE INDEX idx_profiles_avatar_asset_id ON profiles(avatar_asset_id);

-- Giờ profiles đã tồn tại — bổ sung lại FK uploaded_by đã hoãn ở Phase 0
ALTER TABLE media_assets
    ADD CONSTRAINT fk_media_assets_uploaded_by
    FOREIGN KEY (uploaded_by) REFERENCES profiles(id) ON DELETE SET NULL;

-- 5. Bảng Friendships
CREATE TABLE friendships (
    profile_id UUID REFERENCES profiles(id) ON DELETE CASCADE,
    friend_id UUID REFERENCES profiles(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (profile_id, friend_id),
    CHECK (profile_id <> friend_id),
    CONSTRAINT chk_friend_status CHECK (status IN ('PENDING','ACCEPTED','BLOCKED'))
);

CREATE INDEX idx_friendships_friend_id ON friendships(friend_id);

-- Chặn duplicate quan hệ 2 chiều (A mời B và B tự tạo row mời A song song)
CREATE UNIQUE INDEX idx_friendships_unique_pair
ON friendships (LEAST(profile_id, friend_id), GREATEST(profile_id, friend_id));

-- ===================================================================
-- PHASE 2: DANH MỤC & PHIÊN TẬP TRUNG
-- ===================================================================

-- 6. Bảng Categories
CREATE TABLE categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID REFERENCES profiles(id) ON DELETE CASCADE,
    name VARCHAR(50) NOT NULL,
    icon_name VARCHAR(50),
    color_hex VARCHAR(7),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_categories_profile_id ON categories(profile_id);

-- 7. Bảng Focus Sessions
CREATE TABLE focus_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID REFERENCES profiles(id) ON DELETE CASCADE NOT NULL,
    category_id UUID REFERENCES categories(id) ON DELETE SET NULL,
    block_mode VARCHAR(20) NOT NULL,
    planned_minutes INT NOT NULL CHECK (planned_minutes > 0),
    actual_minutes INT DEFAULT 0 CHECK (actual_minutes >= 0),
    status VARCHAR(20) NOT NULL,
    pause_count INT DEFAULT 0 CHECK (pause_count >= 0),
    failure_reason TEXT,
    started_at TIMESTAMPTZ NOT NULL,
    ended_at TIMESTAMPTZ,
    -- Cố ý KHÔNG có FK — room học nhóm chỉ tồn tại tạm ở tầng ứng dụng
    -- (Redis/WebSocket session), không lưu vĩnh viễn trong Postgres.
    friend_session_id UUID,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    CHECK (ended_at >= started_at),
    CONSTRAINT chk_block_mode CHECK (block_mode IN ('LIGHT','MEDIUM','STRICT')),
    CONSTRAINT chk_session_status CHECK (status IN ('RUNNING','COMPLETED','FAILED','CANCELLED'))
);

CREATE INDEX idx_focus_sessions_profile_id ON focus_sessions(profile_id);
CREATE INDEX idx_focus_sessions_category_id ON focus_sessions(category_id);

CREATE INDEX idx_focus_sessions_leaderboard
ON focus_sessions(profile_id, started_at)
WHERE status = 'COMPLETED';

-- ===================================================================
-- PHASE 3: CHẶN ỨNG DỤNG, VI PHẠM & THOÁT KHẨN
-- ===================================================================

-- 8. Bảng Blocked Apps
CREATE TABLE blocked_apps (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID REFERENCES profiles(id) ON DELETE CASCADE NOT NULL,
    app_identifier VARCHAR(255) NOT NULL,
    app_name VARCHAR(100) NOT NULL,
    block_mode VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT chk_blocked_apps_mode CHECK (block_mode IN ('LIGHT','MEDIUM','STRICT')),
    CONSTRAINT uq_blocked_apps UNIQUE (profile_id, app_identifier)
);

CREATE INDEX idx_blocked_apps_profile_id ON blocked_apps(profile_id);

-- 9. Bảng App Violations
CREATE TABLE app_violations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    focus_session_id UUID REFERENCES focus_sessions(id) ON DELETE CASCADE NOT NULL,
    app_identifier VARCHAR(255) NOT NULL,
    app_name VARCHAR(100),
    attempted_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_app_violations_session_id ON app_violations(focus_session_id);

-- 10. Bảng Break Glass Logs
CREATE TABLE break_glass_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    focus_session_id UUID REFERENCES focus_sessions(id) ON DELETE CASCADE NOT NULL,
    penalty_points INT DEFAULT 0 CHECK (penalty_points >= 0),
    penalty_streak_lost INT DEFAULT 0 CHECK (penalty_streak_lost >= 0),
    reason TEXT,
    triggered_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_break_glass_session_id ON break_glass_logs(focus_session_id);

-- ===================================================================
-- PHASE 4: LẬP LỊCH TẬP TRUNG
-- ===================================================================

-- 11. Bảng Scheduled Sessions
CREATE TABLE scheduled_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID REFERENCES profiles(id) ON DELETE CASCADE NOT NULL,
    category_id UUID REFERENCES categories(id) ON DELETE SET NULL,
    title VARCHAR(100) NOT NULL,
    template_type VARCHAR(30) NOT NULL,
    recurrence_rule TEXT,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT chk_template_type CHECK (
        template_type IN ('CUSTOM','POMODORO_CLASSIC','POMODORO_50_10','POMODORO_15_3')
    )
);

CREATE INDEX idx_scheduled_sessions_profile_id ON scheduled_sessions(profile_id);
CREATE INDEX idx_scheduled_sessions_category_id ON scheduled_sessions(category_id);

-- ===================================================================
-- PHASE 5: NHÓM HỌC TẬP & THỬ THÁCH
-- ===================================================================

-- 12. Bảng Groups
CREATE TABLE groups (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    invite_code VARCHAR(20) UNIQUE NOT NULL,
    created_by UUID REFERENCES profiles(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_groups_created_by ON groups(created_by);

-- 13. Bảng Group Members
CREATE TABLE group_members (
    group_id UUID REFERENCES groups(id) ON DELETE CASCADE,
    profile_id UUID REFERENCES profiles(id) ON DELETE CASCADE,
    role VARCHAR(20) DEFAULT 'MEMBER',
    joined_at TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (group_id, profile_id),
    CONSTRAINT chk_member_role CHECK (role IN ('OWNER','ADMIN','MEMBER'))
);

CREATE INDEX idx_group_members_profile_id ON group_members(profile_id);

-- 14. Bảng Challenges
CREATE TABLE challenges (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(100) NOT NULL,
    description TEXT,
    target_focus_minutes INT NOT NULL CHECK (target_focus_minutes > 0),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    created_by UUID REFERENCES profiles(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT chk_challenge_dates CHECK (start_date <= end_date)
);

CREATE INDEX idx_challenges_created_by ON challenges(created_by);
CREATE INDEX idx_challenges_date_range ON challenges(start_date, end_date);

-- 15. Bảng Challenge Participants
CREATE TABLE challenge_participants (
    challenge_id UUID REFERENCES challenges(id) ON DELETE CASCADE,
    profile_id UUID REFERENCES profiles(id) ON DELETE CASCADE,
    is_active BOOLEAN DEFAULT TRUE,
    joined_at TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (challenge_id, profile_id)
);

CREATE INDEX idx_challenge_participants_profile_id ON challenge_participants(profile_id);

-- ===================================================================
-- PHASE 6: NHIỆM VỤ & KHÓA VẬT PHẨM
-- ===================================================================

-- 16. Bảng User Quests
CREATE TABLE user_quests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID REFERENCES profiles(id) ON DELETE CASCADE NOT NULL,
    quest_type VARCHAR(50) NOT NULL,
    target_value INT NOT NULL CHECK (target_value > 0),
    current_value INT DEFAULT 0 CHECK (current_value >= 0),
    reward_points INT NOT NULL CHECK (reward_points >= 0),
    reward_coins INT NOT NULL CHECK (reward_coins >= 0),
    is_claimed BOOLEAN DEFAULT FALSE,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT chk_quest_expiry CHECK (expires_at > created_at),
    CONSTRAINT chk_quest_type CHECK (quest_type IN ('FOCUS_DAILY','FOCUS_WEEKLY','FRIEND_ADDED'))
);

CREATE INDEX idx_user_quests_profile_id ON user_quests(profile_id);

CREATE INDEX idx_user_quests_active
ON user_quests(profile_id, expires_at)
WHERE is_claimed = FALSE;

-- 17. Bảng Unlocked Items
CREATE TABLE unlocked_items (
    profile_id UUID REFERENCES profiles(id) ON DELETE CASCADE,
    item_id UUID REFERENCES gamification_items(id) ON DELETE CASCADE,
    unlocked_at TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (profile_id, item_id)
);

CREATE INDEX idx_unlocked_items_item_id ON unlocked_items(item_id);

-- ===================================================================
-- PHASE 7: THÔNG BÁO ĐẨY (PUSH NOTIFICATION)
-- ===================================================================

-- 18. Bảng Device Tokens
CREATE TABLE device_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID REFERENCES profiles(id) ON DELETE CASCADE NOT NULL,
    fcm_token VARCHAR(255) UNIQUE NOT NULL,
    platform VARCHAR(10) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    last_used_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT chk_platform CHECK (platform IN ('IOS','ANDROID'))
);

CREATE INDEX idx_device_tokens_profile_id ON device_tokens(profile_id);


-- ===================================================================
-- SEED DATA CUỐI: Danh mục mặc định
-- ===================================================================

INSERT INTO categories (id, profile_id, name, icon_name, color_hex) VALUES
(gen_random_uuid(), NULL, 'Học tập', 'book', '#4A90E2'),
(gen_random_uuid(), NULL, 'Làm việc', 'laptop', '#50E3C2'),
(gen_random_uuid(), NULL, 'Đọc sách', 'open-book', '#F5A623'),
(gen_random_uuid(), NULL, 'Sức khỏe', 'heart', '#E28499'),
(gen_random_uuid(), NULL, 'Khác', 'grid', '#9B9B9B');

-- ===================================================================
-- HẾT — 19 bảng, đầy đủ index FK, CHECK constraint, media_assets
-- provider-agnostic, seed data cơ bản. Sẵn sàng chạy 1 lần trên môi trường mới.
-- ===================================================================
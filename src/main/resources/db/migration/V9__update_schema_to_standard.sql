-- 1. Cập nhật bảng gamification_items
ALTER TABLE gamification_items ADD COLUMN cost_coins INTEGER DEFAULT 0 NOT NULL;
ALTER TABLE gamification_items ADD COLUMN is_premium_only BOOLEAN DEFAULT false NOT NULL;

-- Cập nhật ràng buộc loại vật phẩm
ALTER TABLE gamification_items DROP CONSTRAINT IF EXISTS chk_item_type;
ALTER TABLE gamification_items ADD CONSTRAINT chk_item_type CHECK (type IN ('AVATAR', 'THEME', 'SOUND', 'STREAK_FREEZE', 'PET', 'PET_DECORATION'));

-- 2. Tạo bảng user_sound_presets
CREATE TABLE "user_sound_presets" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
	"profile_id" uuid NOT NULL,
	"name" varchar(100) NOT NULL,
	"config_json" jsonb NOT NULL,
	"created_at" timestamp with time zone DEFAULT now(),
	CONSTRAINT "user_sound_presets_profile_id_fkey" FOREIGN KEY ("profile_id") REFERENCES "profiles"("id") ON DELETE CASCADE
);

-- 3. Cập nhật bảng blocked_apps
-- Thêm các cột mới
ALTER TABLE blocked_apps ADD COLUMN platform VARCHAR(10) DEFAULT 'IOS' NOT NULL;
ALTER TABLE blocked_apps ADD COLUMN category_group VARCHAR(50);
ALTER TABLE blocked_apps ADD COLUMN is_whitelisted BOOLEAN DEFAULT false NOT NULL;

-- Thêm check constraint cho platform
ALTER TABLE blocked_apps ADD CONSTRAINT chk_blocked_apps_platform CHECK (platform IN ('IOS', 'ANDROID'));

-- Xóa cột block_mode và ràng buộc cũ liên quan
ALTER TABLE blocked_apps DROP CONSTRAINT IF EXISTS chk_blocked_apps_mode;
ALTER TABLE blocked_apps DROP COLUMN IF EXISTS block_mode;

-- Xóa unique constraint cũ và tạo uq_blocked_apps mới
ALTER TABLE blocked_apps DROP CONSTRAINT IF EXISTS uq_blocked_apps;
ALTER TABLE blocked_apps ADD CONSTRAINT uq_blocked_apps UNIQUE (profile_id, platform, app_identifier);

-- 4. Cập nhật bảng focus_sessions
ALTER TABLE focus_sessions ADD COLUMN device_platform VARCHAR(10) DEFAULT 'IOS';
ALTER TABLE focus_sessions ADD COLUMN reward_multiplier NUMERIC(3,2) DEFAULT 1.00 NOT NULL;

-- 5. Cập nhật bảng app_violations
ALTER TABLE app_violations ADD COLUMN violation_type VARCHAR(30) DEFAULT 'APP_OPENED' NOT NULL;
ALTER TABLE app_violations ALTER COLUMN app_identifier DROP NOT NULL;

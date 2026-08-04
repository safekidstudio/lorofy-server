-- 1. Thêm cột default_block_mode vào bảng profiles mặc định là 'MEDIUM'
ALTER TABLE profiles ADD COLUMN default_block_mode VARCHAR(20) DEFAULT 'MEDIUM' NOT NULL;

-- 2. Cập nhật các dòng dữ liệu cũ có block_mode = 'LIGHT' thành 'MEDIUM'
UPDATE focus_sessions SET block_mode = 'MEDIUM' WHERE block_mode = 'LIGHT';
UPDATE blocked_apps SET block_mode = 'MEDIUM' WHERE block_mode = 'LIGHT';

-- 3. Xóa và tạo lại ràng buộc CHECK cho bảng focus_sessions chỉ cho phép MEDIUM và STRICT
ALTER TABLE focus_sessions DROP CONSTRAINT IF EXISTS chk_block_mode;
ALTER TABLE focus_sessions ADD CONSTRAINT chk_block_mode CHECK (block_mode IN ('MEDIUM', 'STRICT'));

-- 4. Xóa và tạo lại ràng buộc CHECK cho bảng blocked_apps chỉ cho phép MEDIUM và STRICT
ALTER TABLE blocked_apps DROP CONSTRAINT IF EXISTS chk_blocked_apps_mode;
ALTER TABLE blocked_apps ADD CONSTRAINT chk_blocked_apps_mode CHECK (block_mode IN ('MEDIUM', 'STRICT'));

-- 5. Xóa cấu hình hệ số nhân LIGHT khỏi bảng settings
DELETE FROM settings WHERE setting_key = 'focus.reward.multiplier.light';

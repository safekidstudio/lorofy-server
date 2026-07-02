-- 1. Thêm cột is_onboarded (trạng thái hoàn thành onboarding)
ALTER TABLE profiles ADD COLUMN is_onboarded BOOLEAN DEFAULT FALSE NOT NULL;

-- 2. Thêm cột display_name (Tên hiển thị thân thiện, cho phép dấu cách, unicode, không cần unique)
ALTER TABLE profiles ADD COLUMN display_name VARCHAR(100);

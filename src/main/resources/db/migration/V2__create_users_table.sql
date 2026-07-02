-- 1. Tạo bảng users lưu trữ thông tin đăng nhập (Email & Mật khẩu)
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) DEFAULT 'USER' NOT NULL,
    is_enabled BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT chk_user_role CHECK (role IN ('USER', 'ADMIN'))
);

-- 2. Tái cấu trúc cột user_id trong bảng profiles để trỏ tới bảng users mới
-- Xóa cột cũ kiểu VARCHAR(255)
ALTER TABLE profiles DROP COLUMN user_id;

-- Thêm lại cột kiểu UUID liên kết khóa ngoại với users(id)
ALTER TABLE profiles ADD COLUMN user_id UUID UNIQUE NOT NULL REFERENCES users(id) ON DELETE CASCADE;

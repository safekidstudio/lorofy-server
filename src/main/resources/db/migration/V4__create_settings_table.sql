CREATE TABLE settings (
    setting_key VARCHAR(100) PRIMARY KEY,
    setting_value VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Seed dữ liệu cấu hình đợt đầu
INSERT INTO settings (setting_key, setting_value, description) VALUES
('focus.reward.base_points_per_min', '1', 'Điểm cơ bản nhận được mỗi phút tập trung'),
('focus.reward.base_coins_per_min', '1', 'Xu cơ bản nhận được mỗi phút tập trung'),
('focus.reward.multiplier.light', '1.0', 'Hệ số nhân chế độ LIGHT'),
('focus.reward.multiplier.medium', '1.2', 'Hệ số nhân chế độ MEDIUM'),
('focus.reward.multiplier.strict', '1.5', 'Hệ số nhân chế độ STRICT');

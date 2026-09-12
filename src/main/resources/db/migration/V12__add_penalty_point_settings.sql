INSERT INTO settings (setting_key, setting_value, description)
VALUES 
('focus.penalty.points.medium', '20', 'Số point bị trừ khi bỏ cuộc ở chế độ MEDIUM'),
('focus.penalty.points.strict', '40', 'Số point bị trừ khi bỏ cuộc ở chế độ STRICT')
ON CONFLICT (setting_key) DO UPDATE SET setting_value = EXCLUDED.setting_value;

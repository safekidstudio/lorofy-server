-- Thêm cột lưu điểm thưởng kiếm được của phiên tập trung
ALTER TABLE focus_sessions ADD COLUMN earned_points INT DEFAULT 0;

-- Thêm cột lưu xu thưởng kiếm được của phiên tập trung
ALTER TABLE focus_sessions ADD COLUMN earned_coins INT DEFAULT 0;

-- Update reward multiplier for MEDIUM block mode to 1.0 (normal, no point increase)
UPDATE settings
SET setting_value = '1.0'
WHERE setting_key = 'focus.reward.multiplier.medium';

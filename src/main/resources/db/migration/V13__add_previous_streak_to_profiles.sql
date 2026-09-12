-- Add previous_streak and streak_broken_at columns for Manual Streak Repair mechanism
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS previous_streak INT DEFAULT 0 CHECK (previous_streak >= 0);
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS streak_broken_at TIMESTAMPTZ;

-- Add version columns to profiles and focus_sessions tables for JPA Optimistic Locking (@Version)
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE focus_sessions ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

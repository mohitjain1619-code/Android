-- Migration: Add Device Tracking and Anti-Abuse Tables
ALTER TABLE users ADD COLUMN IF NOT EXISTS has_free_trial BOOLEAN NOT NULL DEFAULT true;

CREATE TABLE IF NOT EXISTS user_devices (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  device_id TEXT NOT NULL,
  user_id UUID REFERENCES users(id) ON DELETE CASCADE,
  platform TEXT NOT NULL, -- 'android' or 'web'
  ip_address TEXT DEFAULT '',
  user_agent TEXT DEFAULT '',
  first_seen_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  last_seen_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE(device_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_user_devices_device_id ON user_devices(device_id);

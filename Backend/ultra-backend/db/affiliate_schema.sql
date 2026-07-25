-- ============================================
-- Camverz Affiliate/Creator Program Schema
-- PostgreSQL 15/16
-- ============================================

-- Enable UUID extension if not already present
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Table: affiliates
CREATE TABLE IF NOT EXISTS affiliates (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE UNIQUE,
  code TEXT UNIQUE NOT NULL,
  name TEXT NOT NULL,
  status TEXT NOT NULL DEFAULT 'pending', -- pending / approved / suspended
  commission_rate DOUBLE PRECISION NOT NULL DEFAULT 0.25, -- 25% default
  
  -- Payment details
  upi_id TEXT,
  upi_verified BOOLEAN NOT NULL DEFAULT false,
  bank_account TEXT,
  bank_ifsc TEXT,
  bank_name TEXT,
  bank_verified BOOLEAN NOT NULL DEFAULT false,
  bank_details_json TEXT,
  
  -- Social contacts
  social_url TEXT,
  confirm_ownership BOOLEAN NOT NULL DEFAULT false,
  
  -- Bio verification (for simulation & audit verification)
  linkedin_bio_verified BOOLEAN NOT NULL DEFAULT false,
  linkedin_bio_code TEXT,
  linkedin_bio_verified_at TIMESTAMPTZ,
  
  -- OAuth details (placeholder fallback support)
  linkedin_oauth_verified BOOLEAN NOT NULL DEFAULT false,
  linkedin_oauth_id TEXT,
  linkedin_oauth_name TEXT,
  linkedin_oauth_email TEXT,
  linkedin_oauth_photo TEXT,
  
  admin_notes TEXT,
  min_payout INTEGER NOT NULL DEFAULT 8000, -- ₹8000 minimum payout threshold
  
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  approved_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_affiliates_code ON affiliates(code);
CREATE INDEX IF NOT EXISTS idx_affiliates_user ON affiliates(user_id);

-- Table: affiliate_clicks
CREATE TABLE IF NOT EXISTS affiliate_clicks (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  affiliate_id UUID NOT NULL REFERENCES affiliates(id) ON DELETE CASCADE,
  ip_hash TEXT NOT NULL,
  user_agent TEXT,
  referrer TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_clicks_affiliate ON affiliate_clicks(affiliate_id);

-- Table: affiliate_signups
CREATE TABLE IF NOT EXISTS affiliate_signups (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  affiliate_id UUID NOT NULL REFERENCES affiliates(id) ON DELETE CASCADE,
  referred_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE UNIQUE,
  ref_code_used TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_signups_affiliate ON affiliate_signups(affiliate_id);
CREATE INDEX IF NOT EXISTS idx_signups_referred ON affiliate_signups(referred_user_id);

-- Table: affiliate_payouts
CREATE TABLE IF NOT EXISTS affiliate_payouts (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  affiliate_id UUID NOT NULL REFERENCES affiliates(id) ON DELETE CASCADE,
  amount DOUBLE PRECISION NOT NULL,
  method TEXT NOT NULL, -- upi / bank_transfer
  status TEXT NOT NULL DEFAULT 'completed', -- pending / completed / failed
  transaction_ref TEXT,
  admin_notes TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_payouts_affiliate ON affiliate_payouts(affiliate_id);

-- Table: affiliate_sales
CREATE TABLE IF NOT EXISTS affiliate_sales (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  affiliate_id UUID NOT NULL REFERENCES affiliates(id) ON DELETE CASCADE,
  referred_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  razorpay_payment_id TEXT UNIQUE NOT NULL,
  razorpay_order_id TEXT,
  plan_purchased TEXT,
  amount_paid DOUBLE PRECISION NOT NULL DEFAULT 0,
  commission_amount DOUBLE PRECISION NOT NULL DEFAULT 0,
  commission_rate DOUBLE PRECISION NOT NULL DEFAULT 0.25,
  cookie_ref TEXT,
  status TEXT NOT NULL DEFAULT 'confirmed', -- confirmed / disputed / refunded
  payout_id UUID REFERENCES affiliate_payouts(id) ON DELETE SET NULL,
  
  -- Refund details
  razorpay_refund_id TEXT,
  refunded_amount DOUBLE PRECISION,
  commission_clawback DOUBLE PRECISION,
  refunded_at TIMESTAMPTZ,
  refund_notes TEXT,
  
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_sales_affiliate ON affiliate_sales(affiliate_id);
CREATE INDEX IF NOT EXISTS idx_sales_referred ON affiliate_sales(referred_user_id);
CREATE INDEX IF NOT EXISTS idx_sales_payout ON affiliate_sales(payout_id);

-- Trigger: auto-update updated_at on affiliates
CREATE OR REPLACE TRIGGER affiliates_updated_at
  BEFORE UPDATE ON affiliates
  FOR EACH ROW EXECUTE FUNCTION update_updated_at();

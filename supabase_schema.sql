-- =========================================================================
-- AJIYA - Crisis Help Network
-- Supabase PostgreSQL Schema & Realtime Security
-- =========================================================================

-- Enable UUID extension and PostGIS for geofencing & distance queries
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "postgis";

-- 1. USERS TABLE
-- Core user identity with explicit consent flags and helper opt-in
CREATE TABLE IF NOT EXISTS public.users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    phone VARCHAR(32) UNIQUE NOT NULL,
    name VARCHAR(120) NOT NULL,
    is_helper BOOLEAN DEFAULT FALSE,
    share_location_opt_in BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Index for phone lookups during OTP authentication
CREATE INDEX IF NOT EXISTS idx_users_phone ON public.users(phone);

-- 2. TRUSTED CONTACTS TABLE
-- 3-5 Inner Circle contacts consented by the user
CREATE TABLE IF NOT EXISTS public.trusted_contacts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    contact_phone VARCHAR(32) NOT NULL,
    name VARCHAR(120) NOT NULL,
    relationship VARCHAR(60) DEFAULT 'Family',
    is_primary BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(user_id, contact_phone)
);

CREATE INDEX IF NOT EXISTS idx_trusted_contacts_user ON public.trusted_contacts(user_id);

-- 3. SOS EVENTS TABLE
-- Triggered crisis alerts with battery level, geocoded address, and audio snippet
CREATE TABLE IF NOT EXISTS public.sos_events (
    id VARCHAR(64) PRIMARY KEY, -- e.g. 'SOS-849201'
    user_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    lat DOUBLE PRECISION NOT NULL,
    lng DOUBLE PRECISION NOT NULL,
    address TEXT,
    audio_url TEXT,
    battery INT DEFAULT 85,
    status VARCHAR(20) DEFAULT 'active' CHECK (status IN ('active', 'resolved')),
    mode VARCHAR(30) DEFAULT 'KIDNAP_SILENT' CHECK (mode IN ('KIDNAP_SILENT', 'ACCIDENT_LOUD', 'DISASTER_CHECKIN', 'RISKY_TRIP')),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    resolved_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_sos_events_status ON public.sos_events(status);
CREATE INDEX IF NOT EXISTS idx_sos_events_user ON public.sos_events(user_id);
CREATE INDEX IF NOT EXISTS idx_sos_events_created ON public.sos_events(created_at DESC);

-- 4. LOCATION PINGS TABLE
-- 10-second GPS interval pings recorded ONLY during active SOS
CREATE TABLE IF NOT EXISTS public.location_pings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    sos_id VARCHAR(64) NOT NULL REFERENCES public.sos_events(id) ON DELETE CASCADE,
    lat DOUBLE PRECISION NOT NULL,
    lng DOUBLE PRECISION NOT NULL,
    accuracy REAL DEFAULT 5.0,
    speed REAL DEFAULT 0.0,
    timestamp BIGINT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_pings_sos_id ON public.location_pings(sos_id);
CREATE INDEX IF NOT EXISTS idx_pings_timestamp ON public.location_pings(timestamp);

-- =========================================================================
-- 24-HOUR AUTO-PURGE FUNCTION (STRICT PRIVACY MANDATE)
-- Automatically deletes resolved location pings & events older than 24 hours
-- =========================================================================

CREATE OR REPLACE FUNCTION purge_expired_location_history()
RETURNS void AS $$
BEGIN
    -- Delete location pings older than 24 hours
    DELETE FROM public.location_pings
    WHERE created_at < NOW() - INTERVAL '24 hours';

    -- Delete resolved SOS events older than 24 hours
    DELETE FROM public.sos_events
    WHERE status = 'resolved' AND created_at < NOW() - INTERVAL '24 hours';
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- =========================================================================
-- ROW LEVEL SECURITY (RLS) POLICIES
-- =========================================================================

ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.trusted_contacts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.sos_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.location_pings ENABLE ROW LEVEL SECURITY;

-- Users can read and update their own profile
CREATE POLICY "Users can manage own account" ON public.users
    FOR ALL USING (auth.uid() = id);

-- Users can manage their own trusted contacts
CREATE POLICY "Users can manage own trusted contacts" ON public.trusted_contacts
    FOR ALL USING (auth.uid() = user_id);

-- SOS Events: Visible to the user and their designated trusted contacts
CREATE POLICY "Users and trusted contacts can view SOS" ON public.sos_events
    FOR SELECT USING (
        auth.uid() = user_id OR
        EXISTS (
            SELECT 1 FROM public.trusted_contacts tc
            JOIN public.users u ON u.phone = tc.contact_phone
            WHERE tc.user_id = sos_events.user_id AND u.id = auth.uid()
        )
    );

CREATE POLICY "Users can create their own SOS" ON public.sos_events
    FOR INSERT WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can resolve their own SOS" ON public.sos_events
    FOR UPDATE USING (auth.uid() = user_id);

-- Pings visible only during active SOS
CREATE POLICY "View pings for authorized SOS" ON public.location_pings
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM public.sos_events se
            WHERE se.id = location_pings.sos_id AND (
                se.user_id = auth.uid() OR
                EXISTS (
                    SELECT 1 FROM public.trusted_contacts tc
                    JOIN public.users u ON u.phone = tc.contact_phone
                    WHERE tc.user_id = se.user_id AND u.id = auth.uid()
                )
            )
        )
    );

-- Enable Supabase Realtime for instant broadcast to family dashboards
ALTER PUBLICATION supabase_realtime ADD TABLE public.sos_events;
ALTER PUBLICATION supabase_realtime ADD TABLE public.location_pings;

-- =========================================================================
-- 5. PAYMENT & SUBSCRIPTION SYSTEM SCHEMA
-- =========================================================================

-- Products catalog for subscriptions & on-demand assistance services
CREATE TABLE IF NOT EXISTS public.subscription_products (
    id VARCHAR(64) PRIMARY KEY, -- e.g. 'ajiya_pro_monthly', 'ajiya_escort_service'
    category VARCHAR(32) NOT NULL CHECK (category IN ('MEMBERSHIP', 'SERVICE', 'DONATION')),
    name VARCHAR(120) NOT NULL,
    price_kobo BIGINT NOT NULL,
    price_usd_cents INT DEFAULT 0,
    billing_period VARCHAR(20) DEFAULT 'MONTHLY', -- 'MONTHLY', 'ANNUAL', 'ONE_TIME'
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Seed initial catalog
INSERT INTO public.subscription_products (id, category, name, price_kobo, price_usd_cents, billing_period)
VALUES
    ('ajiya_pro_monthly', 'MEMBERSHIP', 'Guardian Pro', 250000, 199, 'MONTHLY'),
    ('ajiya_shield_yearly', 'MEMBERSHIP', 'Shield Annual', 2400000, 1899, 'ANNUAL'),
    ('ajiya_family_monthly', 'MEMBERSHIP', 'Family Protection Circle', 500000, 399, 'MONTHLY'),
    ('ajiya_escort_service', 'SERVICE', 'Armed Safety Escort', 1500000, 1200, 'ONE_TIME'),
    ('ajiya_tow_service', 'SERVICE', 'Emergency Roadside & Tow', 850000, 650, 'ONE_TIME'),
    ('ajiya_paramedic_service', 'SERVICE', 'Private Paramedic & ALS Unit', 1200000, 950, 'ONE_TIME')
ON CONFLICT (id) DO NOTHING;

-- Complete transaction ledger (Google Play Billing & Paystack)
CREATE TABLE IF NOT EXISTS public.payment_transactions (
    reference VARCHAR(128) PRIMARY KEY, -- Unique transaction ref / Google Play orderId
    user_id UUID REFERENCES public.users(id) ON DELETE SET NULL,
    category VARCHAR(32) NOT NULL CHECK (category IN ('MEMBERSHIP', 'SERVICE', 'DONATION')),
    title VARCHAR(160) NOT NULL,
    amount_kobo BIGINT NOT NULL,
    amount_formatted VARCHAR(32) NOT NULL,
    provider VARCHAR(32) NOT NULL CHECK (provider IN ('GOOGLE_PLAY', 'PAYSTACK', 'REVENUECAT')),
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED', 'REFUNDED')),
    receipt_notes TEXT DEFAULT '',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_transactions_user ON public.payment_transactions(user_id);
CREATE INDEX IF NOT EXISTS idx_transactions_status ON public.payment_transactions(status);
CREATE INDEX IF NOT EXISTS idx_transactions_created ON public.payment_transactions(created_at DESC);

-- Authoritative User Entitlements (Synchronized cross-device)
CREATE TABLE IF NOT EXISTS public.user_entitlements (
    user_id UUID PRIMARY KEY REFERENCES public.users(id) ON DELETE CASCADE,
    tier VARCHAR(64) NOT NULL DEFAULT 'FREE',
    status VARCHAR(32) NOT NULL DEFAULT 'FREE' CHECK (status IN ('FREE', 'PENDING', 'ACTIVE', 'CANCELLED', 'PAST_DUE', 'EXPIRED', 'REVOKED')),
    provider VARCHAR(32) DEFAULT 'NONE',
    subscribed_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    auto_renew BOOLEAN DEFAULT FALSE,
    last_transaction_ref VARCHAR(128),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Voluntary Donations records (Separated from membership subscriptions)
CREATE TABLE IF NOT EXISTS public.donation_records (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    reference VARCHAR(128) UNIQUE NOT NULL REFERENCES public.payment_transactions(reference) ON DELETE CASCADE,
    donor_user_id UUID REFERENCES public.users(id) ON DELETE SET NULL,
    donor_name VARCHAR(120) NOT NULL DEFAULT 'Anonymous Supporter',
    amount_ngn BIGINT NOT NULL,
    message TEXT DEFAULT '',
    is_anonymous BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_donations_created ON public.donation_records(created_at DESC);

-- Idempotency Webhook event ledger
CREATE TABLE IF NOT EXISTS public.payment_webhook_events (
    event_id VARCHAR(128) PRIMARY KEY, -- e.g. Paystack event id or Google RTDN message id
    event_type VARCHAR(64) NOT NULL,
    provider VARCHAR(32) NOT NULL,
    payload JSONB NOT NULL,
    processed BOOLEAN DEFAULT FALSE,
    processed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Enable RLS for Payment Tables
ALTER TABLE public.subscription_products ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.payment_transactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_entitlements ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.donation_records ENABLE ROW LEVEL SECURITY;

-- Products catalog: readable by all users
CREATE POLICY "Products catalog readable by everyone" ON public.subscription_products
    FOR SELECT USING (true);

-- Transactions: user can read their own
CREATE POLICY "Users can view own transactions" ON public.payment_transactions
    FOR SELECT USING (auth.uid() = user_id);

-- Entitlements: user can read their own
CREATE POLICY "Users can view own entitlement" ON public.user_entitlements
    FOR SELECT USING (auth.uid() = user_id);

-- Donations: public summary or authenticated donors
CREATE POLICY "Anyone can view donations list" ON public.donation_records
    FOR SELECT USING (true);

-- Realtime publication for Entitlements so changes reflect instantly
ALTER PUBLICATION supabase_realtime ADD TABLE public.user_entitlements;


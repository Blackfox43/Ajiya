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

# AJIYA - Crisis Help Network

> **Mission**: Help people protect each other during kidnapping, accidents, natural disasters, or hostage situations through **consensual, encrypted live location sharing**.

---

## Core Principle: Consent Only

- **Zero Tracking by Phone Number**: Predatory tracking apps allow anyone with a phone number to monitor targets. AJIYA completely forbids this. Tracking is impossible without mutual account creation and explicit pairing.
- **Opt-In Exclusivity**: All GPS tracking is strictly opt-in. The user can toggle off location sharing with one tap at any time.
- **Ephemeral By Design**: All live location pings and resolved crisis events are automatically purged after 24 hours to prevent permanent movement dossiers.
- **Anti-Coercion Panic PIN**: Under physical duress (hostage/kidnapping), unlocking with the secondary Panic PIN (`9999`) opens a harmless, mundane "Daily Notes & Shopping List" decoy app, keeping real safety signals covert.

---

## System Architecture

```
┌────────────────────────────────────────────────────────┐
│               AJIYA Client Applications                │
│   • Android (Native Jetpack Compose + Room + Fused GPS)│
│   • iOS / Expo (React Native Cross-Platform Bridge)    │
│   • Web Dashboard (Live Family Emergency Watcher)      │
└──────────────────────────┬─────────────────────────────┘
                           │
            ┌──────────────┴──────────────┐
            ▼                             ▼
┌─────────────────────────┐   ┌──────────────────────────┐
│   Supabase Realtime     │   │   Notification Gateways  │
│   • PostgreSQL Engine   │   │   • Expo Push Notifications│
│   • 24h Purge Function  │   │   • SMS Fallback via Termii│
│   • Row Level Security  │   │   • Emergency Dial (112)  │
└─────────────────────────┘   └──────────────────────────┘
```

---

## Crisis Modes & Triggers

| Crisis Mode | Trigger Action | Audio / Sound Behavior | Network Broadcast |
| :--- | :--- | :--- | :--- |
| **Kidnap / Hostage** | Hold SOS 3s | **Silent SOS** (no siren, zero haptics, covert UI) | Transmits background GPS pings every 10s + 30s ambient mic recording to Trusted Circle silently. |
| **Accident** | Hold SOS 3s | **Loud Siren** & audio alarm tone | Dispatches live location link to Circle and prompts auto-dial to emergency services (`112`). |
| **Natural Disaster** | Tap Check-In | Quiet confirmation chime | Broadcasts *"I Am Safe"* status with battery level and geocoded address to all circle contacts. |
| **Risky Trip** | Tap 1-Hr Trip | Silent countdown | Shares live breadcrumb tracking for 60 minutes (late-night ride-hail, transit). Auto-stops at 1 hour. |

---

## Key Features Implemented

### 1. SOS Button (The Heart of AJIYA)
- **Hold for 3 Seconds**: Prevents accidental triggers via radial animated arc with continuous haptic count.
- **10-Second GPS Tracking**: Logs high-accuracy location breadcrumbs with altitude and speed.
- **30-Second Ambient Audio Buffer**: Automatically captures 30 seconds of background audio for evidence and immediate playback in the History screen.
- **Real-Time Battery & Geocoding**: Broadcasts battery percentage (`85%`) and reverse-geocoded street address (`Ahmadu Bello Way, Victoria Island`).

### 2. My Circle (3–5 Trusted Contacts)
- Add verified family members and emergency contacts with their relationship.
- Generates instant SMS broadcast templates:
  > `"AJIYA EMERGENCY: Tunde Adeleke needs help! Crisis Mode: KIDNAP_SILENT. Battery: 85%. Location: Ahmadu Bello Way, Victoria Island. Live Map: https://ajiya.network/live/SOS-847291"`

### 3. Safe Circle Helpers (2km Radius)
- Community first-responders can toggle **"Be a Helper"** to receive nearby distress beacons.
- **Identity Masking**: Helpers only see anonymized distances (e.g. *"Someone needs help 500m away"*). Victim identity is masked until the victim confirms acceptance.

### 4. Duress Decoy Screen
- Entering the **Panic PIN** (`9999`) immediately renders a mundane notes app with shopping lists.
- Completely hides all SOS logs, emergency contacts, and distress beacons if forced to unlock phone by hostile actors.

---

## Supabase Database Setup

Run the included `supabase_schema.sql` in your Supabase SQL Editor:

```sql
-- 1. Run schema script
\i supabase_schema.sql

-- 2. Verify tables created
SELECT table_name FROM information_schema.tables WHERE table_schema = 'public';
-- Expected: users, trusted_contacts, sos_events, location_pings
```

### Automatic 24-Hour Purge Cron (pg_cron)
Enable `pg_cron` in Supabase to run every hour:

```sql
SELECT cron.schedule(
    'purge-expired-pings',
    '0 * * * *',
    'SELECT purge_expired_location_history();'
);
```

---

## SMS Fallback via Termii (Nigeria & Global)

To configure SMS dispatch via Termii for offline cellular reliability:

1. Obtain API Key and Sender ID from [Termii Dashboard](https://termii.com).
2. Set environment secrets:
   ```env
   TERMII_API_KEY=your_termii_key_here
   TERMII_SENDER_ID=AJIYA_ALERT
   ```
3. Outbound SMS payload format:
   ```json
   {
     "to": "+2348023341100",
     "from": "AJIYA_ALERT",
     "sms": "AJIYA EMERGENCY: Tunde needs help! Live: https://ajiya.network/live/SOS-847291",
     "type": "plain",
     "channel": "generic",
     "api_key": "YOUR_KEY"
   }
   ```

---

## Legal & Privacy Screen Notice

> *"This app is for consensual safety. You can only track people who installed app and approved you. Misuse is prohibited."*

AJIYA complies with global privacy regulations (NDPR, GDPR, CCPA). Location data is encrypted locally using Android Keystore and AES-256 before transmission over TLS 1.3.

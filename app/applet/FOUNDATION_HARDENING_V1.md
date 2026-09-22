# AJIYA - Foundation Hardening & Crisis Architecture V1

## Architectural Specifications

### 1. Dual-Persona Threat Model & Anti-Coercion Panic PIN
- **Real Vault PIN**: Grants full access to real crisis circles, tracking histories, safe zone configurations, and security settings.
- **Decoy PIN (`9999`)**: If coerced by bad actors, entering `9999` immediately replaces the crisis dashboard with an innocent Personal Notes/Checklist screen (`DecoyScreen`).
- **Silent Background Broadcast**: While displaying the benign decoy UI, the app secretly boots `SosForegroundService` and transmits silent SOS alerts with live coordinates and audio recording to trusted circle members.

### 2. Triple-Tier Telecom Fallback Pipeline
- **Tier 1 (HTTP Carrier API)**: Dispatches automated emergency SMS via Termii directly to African carriers (MTN, Airtel, Glo, 9mobile).
- **Tier 2 (Native Cellular SMS)**: When mobile data/WiFi is offline, uses native Android `SmsManager` if `SEND_SMS` permission is granted.
- **Tier 3 (Zero-Permission Intent Fallback)**: If SMS permission is denied or restricted by OS policies, prepares and opens `Intent.ACTION_SENDTO` via the system default SMS client with recipient emergency contacts and GPS distress links pre-filled.

### 3. Fail-Safe Telemetry & Battery Death Receiver
- **`BatteryDeathReceiver`**: Automatically captures `ACTION_BATTERY_LOW` and `ACTION_SHUTDOWN`.
- **Last Stand Dispatch**: Before the battery completely dies, dispatches a final coordinate snapshot to the remote backend (`/last-location`) so loved ones know the exact final fix.

### 4. Continuous Background GPS & Doze Mode Immunity
- Uses `SosForegroundService` with `foregroundServiceType="location|microphone"` and an ongoing Android notification.
- Survives device sleep and Android battery optimization during active emergency tracking.

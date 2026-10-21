# Google Play Store Listing & Data Safety Documentation

**Application:** AJIYA - Crisis Help & SOS  
**Package Name:** `com.aistudio.ajiya.crisissafety`  
**Support Contact:** emma432cent@gmail.com  
**Version:** 1.0 (Version Code: 1)  

---

## 1. Store Listing Metadata

### App Title (Max 30 characters)
```text
AJIYA - Crisis Help & SOS
```
*(25 characters — complies with Google Play 30-char limit)*

### Short Description (Max 80 characters)
```text
Instant crisis SOS, live GPS tracking on Google Maps, and emergency helper alert.
```
*(80 characters)*

### Full Description (Max 4,000 characters)
```text
When seconds count, AJIYA is your personal safety lifeline and community emergency response network. Engineered for critical situations, AJIYA empowers you to instantly broadcast SOS alerts, share live GPS locations with trusted contacts and nearby verified helpers, and preserve essential emergency documentation with complete privacy.

KEY CAPABILITIES:

🚨 ONE-TOUCH EMERGENCY SOS
Activate an immediate distress alert in seconds. AJIYA notifies your pre-selected emergency contacts with your real-time GPS coordinates, device status, and emergency audio.

🗺️ LIVE GOOGLE MAPS TRACKING FOR HELPERS
Helpers can open live tracking directly inside Google Maps with a single click. Turn-by-turn navigation guides responders straight to your exact coordinates, eliminating delays in locating someone in distress.

🛡️ DURESS PIN & SILENT ALARM
If forced to cancel an alert under coercion, entering your secret Duress PIN displays a benign "all clear" screen on the device while silently continuing to transmit distress telemetry and location updates in the background to your emergency contacts.

🎙️ AMBIENT AUDIO EVIDENCE CAPTURE
During an active SOS, AJIYA automatically records ambient audio evidence to document the situation, safeguarding critical records for law enforcement and first responders.

📶 OFFLINE SMS FAILSAFE DISPATCH
Even without mobile internet or Wi-Fi connectivity, AJIYA uses automated SMS dispatch to send emergency texts with coordinates directly to your emergency contacts.

🔋 BATTERY DEATH BROADCAST
If your device battery drops below critical levels during an emergency or initiates an unexpected shutdown, AJIYA automatically dispatches a final distress beacon with your last confirmed GPS location.

🚶 JOURNEY WATCH & GEOFENCE SAFETY
Set arrival timers and safe zones. If you do not check in safely before your timer expires, AJIYA automatically escalates an alert to your circle.

---

PRIVACY & SAFETY FIRST:
• No Advertising or Data Brokering: We never sell or monetize your location or personal data.
• On-Demand Tracking: Background location operates exclusively during an active, user-initiated emergency or journey session.
• Total Data Control: Easily delete incident records and audio logs at any time.

Stay protected. Download AJIYA today for trusted personal and community safety.

For support, feedback, or data privacy inquiries: emma432cent@gmail.com
```

---

## 2. Google Play Sensitive Permissions Declarations

Google Play reviewers require clear technical justifications for sensitive permissions:

### A. Location in the Background (`ACCESS_BACKGROUND_LOCATION`)
* **Category:** Personal Safety & Emergency Response
* **Justification for Reviewers:**
  > "AJIYA is a personal safety and emergency distress app. The `ACCESS_BACKGROUND_LOCATION` permission is strictly required to continuously update emergency contacts and verified nearby helpers on Google Maps when a user triggers an Emergency SOS or starts an active safe-walk journey. If the user locks their screen or switches to another app during a crisis, background location updates ensure helpers do not lose the victim's location. Location tracking stops immediately when the user or emergency responders terminate the incident."
* **Video Demonstration Path:** Demonstrate triggering SOS > locking screen > showing receiver device updating pin on Google Maps.

### B. Send SMS (`SEND_SMS`)
* **Category:** Emergency & Disaster Response / Failsafe Notification
* **Justification for Reviewers:**
  > "In high-stress situations or remote environments, cellular data and Wi-Fi networks may be offline or congested. The `SEND_SMS` permission enables the app to automatically transmit an emergency SMS with the user's latest GPS coordinates and distress status to their designated emergency contacts as a life-safety failsafe."

### C. Audio Recording (`RECORD_AUDIO`)
* **Category:** Incident Evidence Documentation
* **Justification for Reviewers:**
  > "Utilized solely during an active Emergency SOS session to capture ambient audio at the scene of the crisis, preserving evidence for emergency responders and legal protection. Clearly indicated to the user via a prominent Android Foreground Service notification."

---

## 3. Data Safety Form Responses (Google Play Console)

| Data Type | Collected? | Shared? | Purpose | Ephemeral? | User Opt-Out / Deletion? |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Approximate Location** | Yes | Yes (Emergency contacts/helpers) | Emergency SOS & helper dispatch | No | Yes (Delete in-app or via email) |
| **Precise Location** | Yes | Yes (Emergency contacts/helpers) | Real-time rescue navigation | No | Yes (Delete in-app or via email) |
| **Contacts** | Yes | No | Transmitting SMS & alert dispatch | No | Yes (Editable in settings) |
| **Audio Recordings** | Yes | Yes (Transmitted with incident) | Emergency evidence preservation | No | Yes (Can be deleted immediately) |
| **Device IDs / Diagnostics** | Yes | No | Foreground service health | No | Yes |

### Additional Data Safety Questions:
* **Is data encrypted in transit?**  
  **Yes.** All communication uses TLS 1.3 / HTTPS.
* **Do you provide a way for users to request data deletion?**  
  **Yes.** Users can delete logs inside the app or email `emma432cent@gmail.com`.
* **Is this app designed for children under 13?**  
  **No.** (Target audience: Ages 13+ / General public).
* **Does this app contain Ads?**  
  **No.** Completely ad-free.

---

## 4. Visual Assets Checklist

- [x] **App Icon:** 512×512 high-resolution icon (Vector adaptive launcher icon configured in app resources).
- [x] **Feature Graphic:** 1024×500 PNG banner saved at `store_assets/feature_graphic_1024x500.png` (Shield radar motif, dark navy & emergency crimson).
- [ ] **Screenshots:** Min 2 screenshots (Max 8) per device type (Phone, 7-inch tablet, 10-inch tablet) uploaded directly in Play Console.

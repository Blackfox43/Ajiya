# Privacy Policy for AJIYA

**Effective Date:** October 21, 2026  
**Last Updated:** October 21, 2026  
**Contact Email:** emma432cent@gmail.com  
**Application:** AJIYA - Crisis Help & Emergency Safety Network  

---

## 1. Introduction

AJIYA ("we", "our", or "us") provides a personal safety, crisis tracking, and community emergency response platform. Your privacy, personal autonomy, and digital security are our highest priorities. 

This Privacy Policy explains how AJIYA collects, uses, protects, and discloses personal information when you use our Android mobile application and associated crisis network services.

By using AJIYA, you consent to the data practices described in this policy. If you do not agree with any part of this policy, please discontinue using the application.

---

## 2. Information We Collect

### A. Location Data (Foreground & Background)
- **Precise Location (GPS and Network-based):** AJIYA collects precise geographical coordinates (latitude, longitude, accuracy, and timestamp) strictly to facilitate emergency rescue operations and consensual live tracking.
- **Background Location (`ACCESS_BACKGROUND_LOCATION`):** AJIYA requests access to your location in the background. **Background location is activated solely during an explicit, user-initiated emergency state** (such as an active SOS alert, silent duress alert, or active safe-walk / journey tracking session). When no crisis or tracking session is active, AJIYA does not monitor, log, or track your background location.
- **Purpose:** Enabling trusted emergency contacts and verified nearby community helpers to navigate directly to your live position on Google Maps during life-threatening crises or emergencies.

### B. Emergency Contacts & SMS Information
- **Contact Details:** Phone numbers, names, and relationship tags of contacts designated by you to receive emergency notifications.
- **SMS Messaging (`SEND_SMS`):** When you trigger an Emergency SOS (or when mobile data/Wi-Fi is unavailable), AJIYA initiates automated SMS emergency alerts containing your distress status and a live Google Maps tracking link directly to your pre-configured emergency contacts. AJIYA does not read, intercept, or access your private non-app text messages.

### C. Audio & Camera Records
- **Microphone (`RECORD_AUDIO`):** When an SOS is triggered, AJIYA can record ambient audio to document the crisis scene and preserve vital evidence for emergency responders. Audio recording is clearly indicated via an Android foreground service notification.
- **Camera (`CAMERA`):** Utilized if you choose to attach visual scene evidence, capture incident documentation, or scan emergency verification codes.

### D. Incident & Device Data
- **Crisis Incident Logs:** Timestamps, battery status at time of dispatch, battery death alerts (sent automatically if the phone powers down during an emergency), and user-provided incident notes.
- **Device Telemetry:** Device model, operating system version, and app version, used strictly to ensure reliable foreground service operation and push notification delivery.

---

## 3. How We Use Your Information

We use collected information solely for emergency safety functions:
1. **Immediate Emergency Dispatch:** Transmitting SOS alerts, live GPS coordinates, and distress audio to your designated emergency contacts and verified community helpers.
2. **Interactive Map Navigation:** Rendering real-time pins and routing options to allow authorized helpers to locate you via Google Maps.
3. **Battery Depletion Protection:** Sending a final distress ping with your last known coordinates if your device battery drops below critical threshold during an emergency.
4. **Safety Verification:** Validating helper proximity and authenticating safe check-ins or duress PIN cancellations.

**We never sell, rent, monetize, or trade your personal data, location traces, or incident logs to third-party advertisers, data brokers, or commercial marketers.**

---

## 4. Sharing & Disclosure of Information

Your information is only shared under the following conditions:
- **Designated Emergency Contacts:** Your chosen contacts receive your alert status, live GPS location, and dispatch messages.
- **Verified Responders / Helpers:** If community helper mode is enabled, nearby helpers who have accepted a crisis request receive your live coordinates to provide physical assistance.
- **Legal & Safety Requirements:** We may disclose relevant data if required by law, subpoena, or where good-faith belief indicates disclosure is immediately necessary to prevent severe bodily injury or loss of human life.
- **Service Providers:** We employ industry-standard cloud infrastructure (Google Cloud Platform and Firebase) for encrypted database storage, secure token exchange, and notification delivery. These providers are bound by strict confidentiality and data protection agreements.

---

## 5. Data Security & Storage

- **End-to-End Encryption:** All location broadcasts and incident data transmitted between your device and our servers use TLS 1.3 / HTTPS encryption.
- **Local Device Storage:** Sensitive credentials (such as duress PINs and local incident caches) are stored using secure Android Room databases with restricted sandbox permissions.
- **Access Controls:** Only authenticated sessions with active authorization tokens may query emergency session endpoints.

---

## 6. Data Retention & Your Rights

- **Incident Deletion:** You may delete past incident logs, recordings, and contact configurations directly within the app settings at any time.
- **Account Deletion & Data Purge:** You have the right to request a complete deletion of all personal information and associated incident records from our servers. To exercise this right, contact us at **emma432cent@gmail.com** with the subject line "AJIYA Data Deletion Request". We will process and confirm your request within 7 business days.
- **Revoking Permissions:** You can revoke Location, Camera, Audio, or SMS permissions at any time through your Android System Settings (`Settings > Apps > AJIYA > Permissions`). Note that revoking location or SMS permissions may limit the app's ability to alert contacts during a crisis.

---

## 7. Children's Privacy

AJIYA is not directed to children under 13 years of age. If a parent or guardian discovers that their child has provided personal information without consent, please contact **emma432cent@gmail.com**, and we will delete the data immediately.

---

## 8. International Data Transfers & Compliance

AJIYA complies with global privacy regulations including:
- **GDPR (General Data Protection Regulation):** Providing users in the European Economic Area with rights to access, rectification, erasure, and data portability.
- **CCPA / CPRA (California Consumer Privacy Act):** Providing California residents rights against data sale (we do not sell data) and rights to delete personal information.

---

## 9. Changes to This Privacy Policy

We may update this Privacy Policy from time to time to reflect operational or legal updates. When changes occur, the "Last Updated" date at the top will be updated, and a notification will be posted in the app.

---

## 10. Contact Us

If you have questions, feedback, or concerns regarding this Privacy Policy or AJIYA's data practices, please reach out to:

**AJIYA Safety Support**  
**Email:** emma432cent@gmail.com  
**Developer:** AJIYA Emergency Response Systems  

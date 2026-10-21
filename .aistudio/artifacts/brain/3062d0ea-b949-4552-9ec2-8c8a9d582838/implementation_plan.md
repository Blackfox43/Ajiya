# Production Launch: Release Keystore, App Bundle, Store Compliance & Assets

Comprehensive execution plan to prepare AJIYA for public Google Play Store launch by creating the release signing keystore, building the signed production Android App Bundle (.aab), compiling compliance documentation (Privacy Policy, Data Safety disclosures, and Store Listing copy), and generating the 1024×500 Feature Graphic.

---

### User Review & Critical Decisions

> [!IMPORTANT]
> The following key decisions have been aligned based on your preferences:
> - **Keystore Security & Credentials**: Standard generated secure passwords will be used and recorded safely in `credentials.txt` (and excluded from Git) for your records.
> - **Contact & Privacy Inquiries**: `emma432cent@gmail.com` will be listed as the official contact in the Privacy Policy and Store Listing.
> - **Visual Style for Feature Graphic**: Dark navy background with luminous emergency crimson and an active protective shield radar motif.

---

### 1. Overview & Core Concept

- **What It Delivers**:
  1. **Production Release Keystore (`my-upload-key.jks`)**: Generated via RSA 2048-bit `keytool` with SHA-1 and SHA-256 certificate fingerprints extracted for Firebase & Google Cloud Console.
  2. **Production Android App Bundle (`app-release.aab`)**: Built via `./gradlew bundleRelease` for Google Play Store upload.
  3. **Privacy Policy (`PRIVACY_POLICY.md`)**: Legally compliant policy covering live GPS tracking, background location, emergency audio recording, SMS dispatch, and Firebase data protection.
  4. **Google Play Store Listing & Data Safety Disclosures (`STORE_LISTING.md`)**: Full storefront copy (Short & Long descriptions, feature highlights, emergency disclaimers) and step-by-step answers for the Play Console Data Safety questionnaire and Sensitive Permissions Declaration (justifying `ACCESS_BACKGROUND_LOCATION` and `SEND_SMS`).
  5. **Feature Graphic Asset (`res/drawable/img_feature_graphic.png`)**: 1024×500 px promotional artwork rendered in the shield radar and crisis safety theme.

- **Target Persona & Audience**: Community members, emergency responders, helpers, and individuals seeking personal security and crisis dispatch.
- **Key Value**: A turnkey, audit-ready submission package that avoids Google Play policy rejections and enables seamless deployment.

---

### 2. User Experience & Visual Design

- **Storefront Visual Identity**:
  - **Aesthetic Direction**: High-trust, professional emergency crisis dispatch and security theme.
  - **Color Palette**: Midnight Obsidian (`#0D131F`), Emergency Crimson (`#E53935`), Alert Amber (`#FFB300`), Radar Teal (`#00E5FF`), Crisp White (`#FFFFFF`).
  - **Feature Graphic Composition**: 16:9 / 1024×500 panoramic composition centered around an illuminated holographic shield radar pulse with GPS coordinate rings, overlaid with clean typography for AJIYA and the tagline *"Live Crisis Network & Rapid Helper Dispatch"*.

- **Storefront Copy Hierarchy**:
  - **Title**: AJIYA — Crisis Safety & SOS
  - **Short Description**: Fast, consensual live crisis tracking, instant SOS alerts, and helper dispatch.
  - **Long Description**: Structured into 4 distinct pillars (Emergency SOS Trigger, Real-Time Helper Map, Background Geofence & Battery Guard, and Consensual Privacy Controls).

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: App Bundle (.aab) vs. Universal APK (.apk)**
  - *Chosen Approach*: Generate `.aab` as the primary production build artifact.
  - *Why*: Google Play requires `.aab` for all new apps since 2021. It dynamically delivers optimized APKs to end users based on device architecture, reducing download size by ~35%.
  - *Alternative Considered*: Standalone APK. (Kept for debug/direct sideloading, but `.aab` is mandatory for Play Store).

- **Decision 2: Keystore Placement & Protection**
  - *Chosen Approach*: Keep `my-upload-key.jks` in the workspace root, configured with environment variable overrides in Gradle, and ensure `.gitignore` guards it against accidental public commits.
  - *Why*: Prevents credential leakage while enabling reproducible CI/CD runs.

- **Decision 3: Play Policy Permissions Justification**
  - *Chosen Approach*: Document explicit justifications under the **Personal Safety / Emergency** exemption in `STORE_LISTING.md`.
  - *Why*: Background location and SMS permissions are scrutinized strictly by Google's review team; presenting a complete justification declaration prevents review delays or rejection.

---

### 4. Technical Architecture & Release Pipeline

```
┌────────────────────────────────────────────────────────────────────────┐
│                        RELEASE ARTIFACT PIPELINE                       │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
          ┌─────────────────────────┼─────────────────────────┐
          │                         │                         │
          ▼                         ▼                         ▼
┌───────────────────┐     ┌───────────────────┐     ┌───────────────────┐
│  Security & Keys  │     │  Gradle Compiler  │     │ Compliance & Art  │
├───────────────────┤     ├───────────────────┤     ├───────────────────┤
│• keytool 2048-bit │     │• bundleRelease    │     │• PRIVACY_POLICY.md│
│• my-upload-key.jks│────▶│• R8 / ProGuard    │     │• STORE_LISTING.md │
│• SHA-1 & SHA-256  │     │• app-release.aab  │     │• 1024x500 Graphic │
│• credentials.txt  │     │                   │     │                   │
└───────────────────┘     └───────────────────┘     └───────────────────┘
          │                         │                         │
          ▼                         ▼                         ▼
┌───────────────────────────────────────────────────────────────────────┐
│                      GOOGLE PLAY CONSOLE SUBMISSION                   │
│ • Upload .aab bundle                                                  │
│ • Fill Data Safety Questionnaire & Sensitive Permission Forms         │
│ • Attach 1024x500 Feature Graphic & App Icon                          │
│ • Add SHA-1 Fingerprint to Firebase Console                           │
└───────────────────────────────────────────────────────────────────────┘
```

#### Detailed Execution Sequence:
1. **Keystore Generation**: Execute `keytool` command to create `my-upload-key.jks`, extract SHA-1 and SHA-256, and save credentials safely in `credentials.txt`.
2. **Build Release Bundle**: Run `./gradlew bundleRelease` with the release keystore configured, producing `app/build/outputs/bundle/release/app-release.aab`.
3. **Compile Documentation**:
   - `PRIVACY_POLICY.md`: Full legal notice with contact `emma432cent@gmail.com`.
   - `STORE_LISTING.md`: Play Store descriptions, keywords, Data Safety responses, and Background Location / SMS justifications.
4. **Generate Feature Graphic**: Run `generate_image` with 16:9 ratio in the shield radar motif, ensuring high visual clarity and production quality.
5. **Verification**: Confirm file outputs, bundle integrity, and provide step-by-step instructions for uploading to Google Play Console.

# AJIYA Foundation Hardening v1

## Completed in this pass

- Removed seeded/demo identity data and named demo contacts.
- Removed known demo PINs from model defaults and panic unlock logic.
- Removed seeded helper alerts.
- Removed hard-coded Supabase mock URL/key from the remote dispatcher.
- Removed mock Termii/Paystack/Supabase credential defaults.
- Prevented environment switching from manufacturing fake payment credentials.
- Removed fabricated Lagos GPS fallback coordinates from the location helper.
- Removed simulated GPS movement from live SOS tracking; unavailable fixes are now skipped instead of invented.
- Removed hard-coded safe zones; users must explicitly create their own zones.
- Prevented the foreground SOS service from fabricating an emergency location if its event is missing.
- Changed client-side subscription completion so it cannot grant premium entitlement by itself.
- Added a server-verified entitlement entry point for the eventual backend integration.
- Added Gradle launcher scripts; the wrapper JAR still needs to be generated/downloaded by Android Studio or a machine with Gradle/network access.

## Remaining for the next engineering pass

1. Add first-run onboarding and real identity creation.
2. Replace plaintext PIN fields with Android Keystore-backed credential protection.
3. Introduce authenticated user/session IDs instead of the temporary local installation identity.
4. Add server-side Supabase authentication + RLS.
5. Implement verified trusted-contact pairing.
6. Implement a durable offline SOS outbox/retry queue.
7. Move Termii and Paystack secret operations to the backend.
8. Implement real server-side payment verification/webhooks.
9. Separate DEBUG simulation APIs from RELEASE builds at compile time.
10. Add instrumented tests for locked-screen/background SOS, GPS failure, permission denial, offline dispatch, and recovery.

## Build limitation

This environment cannot resolve external Gradle distribution downloads, so a full Gradle compile was not possible here. The project should be opened in Android Studio and synced before release. The source changes above were inspected with static searches after modification.

## Phase 2 — Identity & Security foundation (implemented in this build)
- Added first-run onboarding gate before the main safety dashboard.
- Added real name and phone profile capture.
- Added separate normal and duress PIN creation with validation.
- PINs are stored as PBKDF2-HMAC-SHA256 hashes with a per-installation random salt; plaintext PINs are not persisted.
- Added Room migration from database v1 to v2 for PIN salt/onboarding/verification state.
- Added explicit safety consent step before activation.
- Foreground location/notification permission requests now begin only after onboarding completion.
- Microphone permission is no longer requested automatically at first launch; it should be requested when the voice feature is explicitly enabled.
- Phone verification remains intentionally marked unverified until the production backend/OTP flow is implemented.

This phase is a local-device identity/security foundation. It is not yet a server-backed account system and should not be represented as verified identity until the backend verification flow exists.

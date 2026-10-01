# AJIYA — Elevated Android Rebuild

## What I found in the supplied project
AJIYA is a native Jetpack Compose safety app with Room persistence, fused location, a foreground SOS service, trusted contacts, helper alerts, history, a duress-decoy flow, shake/voice triggers, geofence/battery hooks, and a subscription/paywall layer.

### Navigation
Persistent bottom navigation:
1. Safety — emergency action, crisis mode, location/battery state, risky trip.
2. Circle — trusted contacts, emergency-message preview, add/remove contacts.
3. Helper — opt-in community helper mode and anonymized nearby alerts.
4. History — active/resolved events and privacy purge.
5. Security — privacy promises, PIN/duress controls.

Secondary destination:
- Premium/paywall is reachable from the Safety screen in the original architecture.
- Panic PIN opens the decoy notes surface instead of the main navigation.

## Elevated design direction
- High-contrast midnight safety UI.
- Cyan = trusted/network state; crimson = emergency; amber = attention; green = safe.
- Large, thumb-friendly emergency action.
- Clear hierarchy with short operational copy.
- Rounded 18–24dp cards, low-noise surfaces, restrained borders.
- Bottom navigation uses labels + familiar icons and avoids decorative clutter.
- Safety-critical controls are visually distinct from secondary features.

## Important implementation truth
The supplied project contains real Android integrations, but some production services are configuration-dependent. In particular, Paystack requires a real server/public configuration, and the remote SOS/SMS stack requires credentials/backend configuration. This rebuild preserves the native local safety architecture while improving the UI; it does not pretend missing production credentials are magically configured.

## Production checklist
Before release:
- Replace seeded demo contacts/user data with authenticated account creation.
- Configure Firebase/Supabase backend and RLS.
- Put payment initialization/verification behind a secure server; never ship secret Paystack credentials in the APK.
- Complete Google Play policy review for background location, microphone, SMS, and emergency behavior.
- Test foreground-service behavior across Android 10–16 devices and OEM battery restrictions.
- Add automated UI tests for every navigation destination and SOS/decoy flow.
- Perform a privacy/security audit before handling real crisis data.

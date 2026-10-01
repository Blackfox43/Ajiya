# AJIYA Phase 2 — Identity & Security Foundation

## Implemented
- First-run onboarding gate
- Profile name + phone capture
- Normal PIN + duress PIN setup
- PBKDF2-HMAC-SHA256 PIN hashing with per-installation salt
- Room database migration v1 -> v2
- Explicit safety consent
- Permission prompts deferred until onboarding completion
- Microphone permission deferred until voice safety is explicitly enabled

## Deliberate limitation
Phone verification is not claimed as complete. The current build records `phoneVerified=false` until a production OTP/backend flow is connected.

## Next engineering step
Production identity backend:
1. Account/session model
2. OTP verification
3. Trusted-contact invitation/acceptance
4. Server-side authorization/RLS
5. Secure session/token storage
6. Account recovery and device re-enrollment

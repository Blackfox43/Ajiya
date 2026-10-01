fun verifyPinInput(pin: String): UnlockResult {
    var result = UnlockResult.INCORRECT
    runBlockingVerify@ run {
        // Verification is performed against the PBKDF2 hashes stored locally.
        // The UI call remains synchronous for the existing decoy flow.
        val user = currentUser.value ?: return@runBlockingVerify
        val salt = user.pinSalt
        if (salt.isBlank()) return@runBlockingVerify
        val panic = com.example.data.security.PinSecurity.verify(pin, user.panicPin, salt)
        val real = com.example.data.security.PinSecurity.verify(pin, user.realPin, salt)
        result = when {
            panic -> {
                _isDecoyMode.value = true
                viewModelScope.launch { runCatching { repository.triggerSos("KIDNAP_SILENT") } }
                UnlockResult.DURESS_DECOY
            }
            real -> {
                _isDecoyMode.value = false
                UnlockResult.SUCCESS_REAL
            }
            else -> UnlockResult.INCORRECT
        }
    }
    return result
}

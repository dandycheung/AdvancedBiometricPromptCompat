package dev.skomlach.biometric.compat.engine.internal.face.tensorflow

/**
 * Maps a completed FaceTF comparison to the callback contract used by the
 * biometric engine. A non-match is recoverable: the frame stream must stay
 * active so a later frame can still match.
 */
internal enum class FaceAuthenticationAttemptOutcome {
    SUCCESS,
    MATCH_IN_PROGRESS,
    RETRY
}

internal data class FaceAuthenticationAttemptState(
    val matchedId: String? = null,
    val consecutiveMatches: Int = 0
)

internal data class FaceAuthenticationAttempt(
    val outcome: FaceAuthenticationAttemptOutcome,
    val state: FaceAuthenticationAttemptState
)

internal fun evaluateFaceAuthenticationAttempt(
    state: FaceAuthenticationAttemptState,
    candidateId: String?,
    distance: Float,
    maximumDistance: Float,
    requiredConsecutiveMatches: Int
): FaceAuthenticationAttempt {
    if (candidateId == null || !distance.isFinite() || distance < 0f || distance >= maximumDistance) {
        return FaceAuthenticationAttempt(
            FaceAuthenticationAttemptOutcome.RETRY,
            FaceAuthenticationAttemptState()
        )
    }

    val consecutiveMatches = if (candidateId == state.matchedId) {
        state.consecutiveMatches + 1
    } else {
        1
    }
    val nextState = FaceAuthenticationAttemptState(candidateId, consecutiveMatches)
    return FaceAuthenticationAttempt(
        if (consecutiveMatches >= requiredConsecutiveMatches) {
            FaceAuthenticationAttemptOutcome.SUCCESS
        } else {
            FaceAuthenticationAttemptOutcome.MATCH_IN_PROGRESS
        },
        nextState
    )
}

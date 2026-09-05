package dev.skomlach.biometric.compat.engine.internal.face.tensorflow

import org.junit.Assert.assertEquals
import org.junit.Test

class FaceAuthenticationAttemptPolicyTest {

    @Test
    fun `matching frames complete FaceTF authentication`() {
        val first = evaluateFaceAuthenticationAttempt(
            state = FaceAuthenticationAttemptState(),
            candidateId = "enrolled-face",
            distance = 0.14f,
            maximumDistance = 0.30f,
            requiredConsecutiveMatches = 2
        )
        val second = evaluateFaceAuthenticationAttempt(
            state = first.state,
            candidateId = "enrolled-face",
            distance = 0.15f,
            maximumDistance = 0.30f,
            requiredConsecutiveMatches = 2
        )

        assertEquals(FaceAuthenticationAttemptOutcome.MATCH_IN_PROGRESS, first.outcome)
        assertEquals(FaceAuthenticationAttemptOutcome.SUCCESS, second.outcome)
    }

    @Test
    fun `mismatch resets the sequence and remains retryable`() {
        val mismatch = evaluateFaceAuthenticationAttempt(
            state = FaceAuthenticationAttemptState("enrolled-face", 1),
            candidateId = "other-face",
            distance = 0.42f,
            maximumDistance = 0.30f,
            requiredConsecutiveMatches = 2
        )

        assertEquals(FaceAuthenticationAttemptOutcome.RETRY, mismatch.outcome)
        assertEquals(FaceAuthenticationAttemptState(), mismatch.state)
    }
    @Test
    fun `invalid scores and missing identity never count as a match`() {
        for (distance in listOf(Float.NaN, Float.POSITIVE_INFINITY, -1f)) {
            val attempt = evaluateFaceAuthenticationAttempt(
                FaceAuthenticationAttemptState("face", 1), "face", distance, 0.3f, 2
            )
            assertEquals(FaceAuthenticationAttemptOutcome.RETRY, attempt.outcome)
        }
        assertEquals(FaceAuthenticationAttemptOutcome.RETRY,
            evaluateFaceAuthenticationAttempt(FaceAuthenticationAttemptState(), null, 0.1f, 0.3f, 1).outcome)
    }
}

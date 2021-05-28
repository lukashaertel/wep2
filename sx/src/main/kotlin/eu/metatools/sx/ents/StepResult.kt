package eu.metatools.sx.ents

/**
 * The result of running a step.
 * @property consumed The consumed time.
 * @property outcome True if successful, false if failed, null if not done yet.
 */
data class StepResult(val consumed: Double, val outcome: Boolean?) {
    companion object {
        /**
         * Step is not yet completed.
         */
        fun more(consumed: Double): StepResult {
            require(consumed.rgz()) {
                "Non-completion steps must consume time"
            }
            return StepResult(consumed, null)
        }

        /**
         * Step is done successfully. Can optionally consume time.
         */
        fun ok(consumed: Double = 0.0) = StepResult(consumed, true)

        /**
         * Step failed. Can optionally consume time.
         */
        fun fail(consumed: Double = 0.0) = StepResult(consumed, false)
    }
}
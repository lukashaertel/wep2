package eu.metatools.fio.immediate

import eu.metatools.fio.data.Mat
import eu.metatools.fio.end
import eu.metatools.fio.playable.Playable

/**
 * Standard implementation of [Immediate].
 * @property radiusLimit How far outside of the view a center must lie for the subject to be ignored.
 */
class StandardPlay(trimExcess: Float) : ProjectionTrimmed(trimExcess), Play {
    /**
     * Play in the current [begin]/[end] block.
     */
    private data class Entry<T>(val subject: Playable<T>, val args: T, val handle: Any) {
        fun play(time: Double, transform: Mat) {
            // Play subject itself.
            subject.play(args, handle, time, transform)
        }

        fun cancel() {
            // Cancel the handle for the subject.
            subject.cancel(handle)
        }
    }

    /**
     * All plays in the current block, differs in implementation, as sounds are updated from continuous, while
     * captures and draws are always passed through. Therefore an identity is needed, i.e., what to play, what
     * argument and what handle. The value of this association is the transformation to update to.
     */
    private var plays = hashMapOf<Entry<*>, Mat>()

    /**
     * Plays from the last block.
     */
    private var playsSecondary = hashMapOf<Entry<*>, Mat>()

    /**
     * Starts the block with the given matrices.
     */
    fun begin(projection: Mat) {
        this.projection = projection
    }

    /**
     * Plays all play calls in this block.
     */
    fun play(time: Double) {
        // Play all collected entries.
        for ((play, transform) in plays)
            play.play(time, transform)
    }

    /**
     * Ends the block, cancels all non-renewed sounds.
     */
    fun end() {
        // Cancel previous plays that were not renewed.
        for (play in playsSecondary.keys subtract plays.keys)
            play.cancel()

        // Swap plays.
        plays.let {
            plays = playsSecondary
            playsSecondary = it
        }

        // Reset buffer.
        plays.clear()
    }

    /**
     * Submits a play call.
     */
    override fun <T> submit(subject: Playable<T>, args: T, handle: Any, time: Double, transform: Mat) {
        // Validate time (no bounds for sound).
        validate(subject, time) {
            // Add to plays, sound is played in active projection space.
            plays[Entry(subject, args, handle)] = projection * transform
        }
    }
}
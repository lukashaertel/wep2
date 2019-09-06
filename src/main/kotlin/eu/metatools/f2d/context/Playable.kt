package eu.metatools.f2d.context

/**
 * A playable instance.
 */
interface Playable<in T> : Timed {
    /**
     * Starts a sound, returns the internal handle.
     */
    fun start(args: T, time: Double): Long

    /**
     * Generates an update for the given internal handle.
     */
    fun generate(args: T, id: Long, time: Double, x: Float, y: Float, z: Float)

    /**
     * Stops the sound with the given internal handle.
     */
    fun stop(args: T, id: Long)
}

/**
 * Returns a playable instance that is fixed to end after the given time.
 */
infix fun <T> Playable<T>.limit(duration: Double) = object : Playable<T> {
    override fun start(args: T, time: Double) =
        this@limit.start(args, time)

    override fun generate(args: T, id: Long, time: Double, x: Float, y: Float, z: Float) =
        this@limit.generate(args, id, time, x, y, z)

    override fun stop(args: T, id: Long) =
        this@limit.stop(args, id)

    override val start: Double
        get() = this@limit.start

    override val duration: Double
        get() = minOf(this@limit.duration, duration)
}

/**
 * Returns a playable instance that is offset by the given time.
 */
infix fun <T> Playable<T>.offset(offset: Double) = object : Playable<T> {
    override fun start(args: T, time: Double) =
        this@offset.start(args, time - offset)

    override fun generate(args: T, id: Long, time: Double, x: Float, y: Float, z: Float) =
        this@offset.generate(args, id, time - offset, x, y, z)

    override fun stop(args: T, id: Long) =
        this@offset.stop(args, id)

    override val start: Double
        get() = this@offset.start + offset

    override val duration: Double
        get() = this@offset.duration
}

/**
 * Chains the receiver with the next [Playable]. If receiver does not end, [next] will never play.
 */
infix fun <T> Playable<T>.then(next: Playable<T>) = object : Playable<T> {
    /**
     * Mid-play there will be a switch happening, track redirection of IDs
     */
    private var redirect = mutableMapOf<Long, Long>()

    override fun start(args: T, time: Double) =
        // Check if first should play.
        if (time < this@then.end) {
            // First should play, start it with the given time.
            this@then.start(args, time)
        } else {
            // Time already exceeded, start next and subtract duration.
            next.start(args, time - this@then.duration).also {
                // Also, since the returned ID is already the target ID, add redirection to itself.
                redirect[it] = it
            }
        }

    override fun generate(args: T, id: Long, time: Double, x: Float, y: Float, z: Float) {
        // Check if first should generate.
        if (time < this@then.end) {
            // First generates, simply direct it.
            this@then.generate(args, id, time, x, y, z)
            return
        }

        // Check if redirection is already given.
        redirect[id]?.let { otherId ->
            // Play next with redirection ID and relative time.
            next.generate(args, otherId, time - this@then.duration, x, y, z)
            return
        }

        // Switch must occur at this point, stop the first with the given ID.
        this@then.stop(args, id)

        // Start the next item with relative time.
        next.start(args, time - this@then.duration).also {
            // Assign redirection to the switch ID.
            redirect[id] = it

            // Also, generate one call at the relative time for next.
            next.generate(args, it, time - this@then.duration, x, y, z)
        }
    }

    override fun stop(args: T, id: Long) {
        // Get a redirection ID.
        redirect[id]?.let {
            // Redirection is present, stop next.
            next.stop(args, id)
            return
        }

        // If no redirection is present, stop first.
        this@then.stop(args, id)
    }

    override val duration: Double
        get() = this@then.duration + next.duration

    override val start: Double
        get() = this@then.start
}
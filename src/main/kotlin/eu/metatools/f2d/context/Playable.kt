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

// TODO: Maybe offer sequence arguments as Pair, needs to respect nullity eventually.

/**
 * Chains the receiver with the next [Playable]. If receiver does not end, [next] will never play.
 * The argument type is generic and arguments are extracted via [firstArg] and [secondArg].
 */
fun <T, A, B> Playable<A>.then(
    next: Playable<B>,
    firstArg: (T) -> A,
    secondArg: (T) -> B
) = object : Playable<T> {
    /**
     * Mid-play there will be a switch happening, track redirection of IDs
     */
    private var redirect = mutableMapOf<Long, Long>()

    override fun start(args: T, time: Double) =
        // Check if first should play.
        if (time < this@then.end) {
            // First should play, start it with the given time.
            this@then.start(firstArg(args), time)
        } else {
            // Time already exceeded, start next and subtract duration.
            next.start(secondArg(args), time - this@then.duration).also {
                // Also, since the returned ID is already the target ID, add redirection to itself.
                redirect[it] = it
            }
        }

    override fun generate(args: T, id: Long, time: Double, x: Float, y: Float, z: Float) {
        // Check if first should generate.
        if (time < this@then.end) {
            // First generates, simply direct it.
            this@then.generate(firstArg(args), id, time, x, y, z)
            return
        }

        // Check if redirection is already given.
        redirect[id]?.let { otherId ->
            // Play next with redirection ID and relative time.
            next.generate(secondArg(args), otherId, time - this@then.duration, x, y, z)
            return
        }

        // Switch must occur at this point, stop the first with the given ID.
        this@then.stop(firstArg(args), id)

        // Start the next item with relative time.
        next.start(secondArg(args), time - this@then.duration).also {
            // Assign redirection to the switch ID.
            redirect[id] = it

            // Also, generate one call at the relative time for next.
            next.generate(secondArg(args), it, time - this@then.duration, x, y, z)
        }
    }

    override fun stop(args: T, id: Long) {
        // Get a redirection ID.
        redirect[id]?.let {
            // Redirection is present, stop next.
            next.stop(secondArg(args), id)
            return
        }

        // If no redirection is present, stop first.
        this@then.stop(firstArg(args), id)
    }

    override val duration: Double
        get() = this@then.duration + next.duration

    override val start: Double
        get() = this@then.start
}

/**
 * Concatenates two [Playable]s with equal argument types, using the same argument.
 */
infix fun <T> Playable<T>.then(next: Playable<T>) = then<T, T, T>(next, { it }, { it })

/**
 * Concatenates two [Playable]s with different argument types, resulting in a pair.
 */
infix fun <T, U> Playable<T>.thenPair(next: Playable<U>) =
    then<Pair<T, U>, T, U>(next, { it.first }, { it.second })


/**
 * Concatenates two [Playable]s with different argument types, resulting in a pair.
 */
@JvmName("thenPairNullable")
infix fun <T : Any, U : Any> Playable<T?>.thenPair(next: Playable<U?>) =
    then<Pair<T, U>?, T?, U?>(next, { it?.first }, { it?.second })
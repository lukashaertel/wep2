package eu.metatools.f2d.context

/**
 * A playable instance.
 */
interface Playable<in T> : Lifetime<T> {
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
infix fun <T> Playable<T>.limit(endTime: Double) = object : Playable<T> {
    override fun start(args: T, time: Double) =
        this@limit.start(args, time)

    override fun generate(args: T, id: Long, time: Double, x: Float, y: Float, z: Float) =
        this@limit.generate(args, id, time, x, y, z)

    override fun stop(args: T, id: Long) =
        this@limit.stop(args, id)

    override fun hasStarted(args: T, time: Double) =
        this@limit.hasStarted(args, time)

    override fun hasEnded(args: T, time: Double) =
        // Ended if source has ended or time exceeded.
        time > endTime || this@limit.hasEnded(args, time)
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

    override fun hasStarted(args: T, time: Double) =
        this@offset.hasStarted(args, time - offset)

    override fun hasEnded(args: T, time: Double) =
        this@offset.hasEnded(args, time - offset)
}
package eu.metatools.up.dt

/**
 * Time scope that binds time for subsequent invocations.
 */
data class TimeScope(val bound: Time) {
    /**
     * Invokes the receiver with a fresh time obtained from [take].
     */
    operator fun ((Time) -> Unit).invoke() =
        invoke(bound)

    /**
     * Invokes the receiver with a fresh time obtained from [take].
     */
    operator fun <T> ((Time, T) -> Unit).invoke(arg: T) =
        invoke(bound, arg)

    /**
     * Invokes the receiver with a fresh time obtained from [take].
     */
    operator fun <T, U> ((Time, T, U) -> Unit).invoke(arg1: T, arg2: U) =
        invoke(bound, arg1, arg2)

    /**
     * Invokes the receiver with a fresh time obtained from [take].
     */
    operator fun <T, U, V> ((Time, T, U, V) -> Unit).invoke(arg1: T, arg2: U, arg3: V) =
        invoke(bound, arg1, arg2, arg3)

    /**
     * Invokes the receiver with a fresh time obtained from [take].
     */
    operator fun <T, U, V, W> ((Time, T, U, V, W) -> Unit).invoke(arg1: T, arg2: U, arg3: V, arg4: W) =
        invoke(bound, arg1, arg2, arg3, arg4)
}

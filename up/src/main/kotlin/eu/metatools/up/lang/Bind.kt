package eu.metatools.up.lang

/**
 * Binding scope that binds the first argument for subsequent invocations.
 */
interface Bind<X> {
    /**
     * Invokes the receiver with a fresh time obtained from [take].
     */
    operator fun ((X) -> Unit).invoke()

    /**
     * Invokes the receiver with a fresh time obtained from [take].
     */
    operator fun <T> ((X, T) -> Unit).invoke(arg: T)

    /**
     * Invokes the receiver with a fresh time obtained from [take].
     */
    operator fun <T, U> ((X, T, U) -> Unit).invoke(arg1: T, arg2: U)

    /**
     * Invokes the receiver with a fresh time obtained from [take].
     */
    operator fun <T, U, V> ((X, T, U, V) -> Unit).invoke(arg1: T, arg2: U, arg3: V)

    /**
     * Invokes the receiver with a fresh time obtained from [take].
     */
    operator fun <T, U, V, W> ((X, T, U, V, W) -> Unit).invoke(arg1: T, arg2: U, arg3: V, arg4: W)
}

/**
 * Binding scope that binds a constant for subsequent invocations.
 */
data class BindConstant<X>(val constant: X) : Bind<X> {
    /**
     * Invokes the receiver with a fresh time obtained from [take].
     */
    override operator fun ((X) -> Unit).invoke() =
        invoke(constant)

    /**
     * Invokes the receiver with a fresh time obtained from [take].
     */
    override operator fun <T> ((X, T) -> Unit).invoke(arg: T) =
        invoke(constant, arg)

    /**
     * Invokes the receiver with a fresh time obtained from [take].
     */
    override operator fun <T, U> ((X, T, U) -> Unit).invoke(arg1: T, arg2: U) =
        invoke(constant, arg1, arg2)

    /**
     * Invokes the receiver with a fresh time obtained from [take].
     */
    override operator fun <T, U, V> ((X, T, U, V) -> Unit).invoke(arg1: T, arg2: U, arg3: V) =
        invoke(constant, arg1, arg2, arg3)

    /**
     * Invokes the receiver with a fresh time obtained from [take].
     */
    override operator fun <T, U, V, W> ((X, T, U, V, W) -> Unit).invoke(arg1: T, arg2: U, arg3: V, arg4: W) =
        invoke(constant, arg1, arg2, arg3, arg4)
}

/**
 * Binding scope that binds generator invocations for subsequent invocations.
 */
data class BindGenerator<X>(val generator: () -> X) : Bind<X> {
    /**
     * Invokes the receiver with a fresh time obtained from [take].
     */
    override operator fun ((X) -> Unit).invoke() =
        invoke(generator())

    /**
     * Invokes the receiver with a fresh time obtained from [take].
     */
    override operator fun <T> ((X, T) -> Unit).invoke(arg: T) =
        invoke(generator(), arg)

    /**
     * Invokes the receiver with a fresh time obtained from [take].
     */
    override operator fun <T, U> ((X, T, U) -> Unit).invoke(arg1: T, arg2: U) =
        invoke(generator(), arg1, arg2)

    /**
     * Invokes the receiver with a fresh time obtained from [take].
     */
    override operator fun <T, U, V> ((X, T, U, V) -> Unit).invoke(arg1: T, arg2: U, arg3: V) =
        invoke(generator(), arg1, arg2, arg3)

    /**
     * Invokes the receiver with a fresh time obtained from [take].
     */
    override operator fun <T, U, V, W> ((X, T, U, V, W) -> Unit).invoke(arg1: T, arg2: U, arg3: V, arg4: W) =
        invoke(generator(), arg1, arg2, arg3, arg4)
}

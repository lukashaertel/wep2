package eu.metatools.sx.util

import kotlinx.coroutines.GlobalScope

/**
 * Default job count.
 */
const val defaultJobCount = 32

/**
 * Default minimum parallel element limit.
 */
const val defaultMinParallel = 1_000

/**
 * The default coroutine scope.
 */
val defaultPool = GlobalScope


/**
 * Default merge by plus operator.
 */
private val byteAdd = { a: Byte, b: Byte -> (a + b).toByte() }

/**
 * Default merge by plus operator.
 */
private val shortAdd = { a: Short, b: Short -> (a + b).toShort() }

/**
 * Default merge by plus operator.
 */
private val intAdd = { a: Int, b: Int -> a + b }

/**
 * Default merge by plus operator.
 */
private val longAdd = { a: Long, b: Long -> a + b }

/**
 * Default merge by plus operator.
 */
private val floatAdd = { a: Float, b: Float -> a + b }

/**
 * Default merge by plus operator.
 */
private val doubleAdd = { a: Double, b: Double -> a + b }

/**
 * Default merge by plus operator.
 */
private val stringAdd = { a: String, b: String -> a + b }

/**
 * Gets the default merge by plus operator for [Byte].
 */
val Byte.Companion.add
    get() = byteAdd

/**
 * Gets the default merge by plus operator for [Short].
 */
val Short.Companion.add
    get() = shortAdd

/**
 * Gets the default merge by plus operator for [Int].
 */
val Int.Companion.add
    get() = intAdd

/**
 * Gets the default merge by plus operator for [Long].
 */
val Long.Companion.add
    get() = longAdd

/**
 * Gets the default merge by plus operator for [Float].
 */
val Float.Companion.add
    get() = floatAdd

/**
 * Gets the default merge by plus operator for [Double].
 */
val Double.Companion.add
    get() = doubleAdd

/**
 * Gets the default merge by plus operator for [String].
 */
val String.Companion.add
    get() = stringAdd
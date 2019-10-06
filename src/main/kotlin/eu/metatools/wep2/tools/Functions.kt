package eu.metatools.wep2.tools

import eu.metatools.wep2.track.rec
import kotlin.reflect.KFunction1
import kotlin.reflect.KFunction2


/**
 * Utility to [rec] a call to a function taking no arguments.
 */
fun <T : Comparable<T>> rec(time: T, function: KFunction1<T, *>) = rec {
    function(time)
}

/**
 * Utility to [rec] a call to a function taking an argument, automatically casting that argument.
 */
fun <T : Comparable<T>, A> rec(time: T, arg: Any?, function: KFunction2<T, A, *>) = rec {
    function.call(time, arg)
}
package eu.metatools.mk.util

/**
 * Compares [firstUpper] to [secondUpper]. If that fails, rotates [firstLower] and [secondLower] by shifting
 * in module [mod], then comparing them.
 */
fun <I, M> compareRound(firstUpper: I, firstLower: M, secondUpper: I, secondLower: M, mod: M): Int
        where  M : Number, M : Comparable<M>, I : Number, I : Comparable<I> {
    // Try to get the result without creating module.
    val compareUpper = firstUpper.compareTo(secondUpper)
    if (compareUpper != 0)
        return compareUpper

    // Generate module and shift.
    val longMod = mod.toLong()
    val shiftA = (((firstUpper.toLong() + firstLower.toLong()) % longMod) + longMod) % longMod
    val shiftB = (((secondUpper.toLong() + secondLower.toLong()) % longMod) + longMod) % longMod

    // Return result of comparing that.
    return shiftA.compareTo(shiftB)
}
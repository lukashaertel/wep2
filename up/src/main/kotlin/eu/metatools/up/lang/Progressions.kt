package eu.metatools.up.lang

fun frequencyProgression(base: Long, frequency: Long, from: Long, to: Long): LongProgression {
    val lr = from - base
    val cr = to - base

    val lm = lr % frequency
    val cm = cr % frequency

    val first = if (lm == 0L) (base + lr) else (base + lr - lm + frequency)
    val lastExclusive = if (cm == 0L) (base + cr) else (base + cr - cm + frequency)
    return first until lastExclusive step frequency
}
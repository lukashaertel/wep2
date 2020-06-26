package eu.metatools.fio.data

import kotlin.math.round


@Suppress("nothing_to_inline")
inline fun roundForPrint(value: Float) =
    round(value * 1e5) / 1e5




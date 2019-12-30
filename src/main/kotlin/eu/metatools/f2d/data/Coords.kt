package eu.metatools.f2d.data

import kotlin.math.round


@Suppress("nothing_to_inline")
internal inline fun roundForPrint(value: Float) =
    round(value * 1e5) / 1e5




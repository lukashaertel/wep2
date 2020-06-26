package eu.metatools.fio.capturable

import eu.metatools.fio.Timed
import eu.metatools.fio.data.Vec

interface Capturable<in T> : Timed {
    fun capture(args: T, time: Double, origin: Vec, direction: Vec): Vec?
}
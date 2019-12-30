package eu.metatools.f2d.capturable

import eu.metatools.f2d.Timed
import eu.metatools.f2d.data.Vec

interface Capturable<in T> : Timed {
    fun capture(args: T, time: Double, origin: Vec, direction: Vec): Vec?
}
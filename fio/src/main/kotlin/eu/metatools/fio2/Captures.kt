package eu.metatools.fio2

import eu.metatools.fio.data.Vec

interface CapturesTarget {
    fun hit(at: Vec, data: Any? = null)
}

interface Captures {
    fun capture(time: Double, origin: Vec, direction: Vec, target: CapturesTarget)
}
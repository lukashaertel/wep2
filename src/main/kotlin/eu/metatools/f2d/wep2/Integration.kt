package eu.metatools.f2d.wep2

import eu.metatools.f2d.CoordsAt
import eu.metatools.f2d.context.*
import eu.metatools.wep2.track.undo

/**
 * Queues a [Drawable] in the [Once], undoes by closing the resulting auto-closable (preventing it from drawing).
 */
fun <T> Once.recDraw(subject: Drawable<T>, args: T, coordinates: CoordsAt) {
    val closable = draw(subject, args, coordinates)
    undo {
        closable.close()
    }
}

/**
 * Queues a [Drawable] in the [Once], undoes by closing the resulting auto-closable (preventing it from drawing).
 */
fun <T> Once.recDraw(subject: Drawable<T?>, coordinates: CoordsAt) {
    val closable = draw(subject, coordinates)
    undo {
        closable.close()
    }
}

/**
 * Queues a [Playable] in the [Once], undoes by closing the resulting auto-closable (preventing it from playing).
 */
fun <T> Once.recPlay(subject: Playable<T>, args: T, coordinates: CoordsAt) {
    val closable = play(subject, args, coordinates)
    undo {
        closable.close()
    }
}

/**
 * Queues a [Playable] in the [Once], undoes by closing the resulting auto-closable (preventing it from playing).
 */
fun <T> Once.recPlay(subject: Playable<T?>, coordinates: CoordsAt) {
    val closable = play(subject, coordinates)
    undo {
        closable.close()
    }
}

/**
 * Queues a [Capturable] in the [Once], undoes by closing the resulting auto-closable (preventing it from capturing).
 */
fun <T> Once.recCapture(result: Any?, subject: Capturable<T>, args: T, coordinates: CoordsAt) {
    val closable = capture(result, subject, args, coordinates)
    undo {
        closable.close()
    }
}

/**
 * Queues a [Capturable] in the [Once], undoes by closing the resulting auto-closable (preventing it from capturing).
 */
fun <T> Once.recCapture(result: Any?, subject: Capturable<T?>, coordinates: CoordsAt) {
    val closable = capture(result, subject, coordinates)
    undo {
        closable.close()
    }
}
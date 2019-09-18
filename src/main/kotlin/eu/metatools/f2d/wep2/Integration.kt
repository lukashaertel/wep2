package eu.metatools.f2d.wep2

import eu.metatools.f2d.context.Capturable
import eu.metatools.f2d.context.Drawable
import eu.metatools.f2d.context.Once
import eu.metatools.f2d.context.Playable
import eu.metatools.f2d.math.Mat
import eu.metatools.wep2.track.undo

/**
 * Queues a [Drawable] in the [Once], undoes by closing the resulting auto-closable (preventing it from drawing).
 */
fun <T> Once.recEnqueue(subject: Drawable<T>, args: T, transformAt: (Double) -> Mat) {
    val closable = enqueue(subject, args, transformAt)
    undo {
        closable.close()
    }
}

/**
 * Queues a [Drawable] in the [Once], undoes by closing the resulting auto-closable (preventing it from drawing).
 */
fun <T> Once.recEnqueue(subject: Drawable<T?>, transformAt: (Double) -> Mat) {
    val closable = enqueue(subject, transformAt)
    undo {
        closable.close()
    }
}

/**
 * Queues a [Playable] in the [Once], undoes by closing the resulting auto-closable (preventing it from playing).
 */
fun <T> Once.recEnqueue(subject: Playable<T>, args: T, transformAt: (Double) -> Mat) {
    val closable = enqueue(subject, args, transformAt)
    undo {
        closable.close()
    }
}

/**
 * Queues a [Playable] in the [Once], undoes by closing the resulting auto-closable (preventing it from playing).
 */
fun <T> Once.recEnqueue(subject: Playable<T?>, transformAt: (Double) -> Mat) {
    val closable = enqueue(subject, transformAt)
    undo {
        closable.close()
    }
}

/**
 * Queues a [Capturable] in the [Once], undoes by closing the resulting auto-closable (preventing it from capturing).
 */
fun <T> Once.recEnqueue(subject: Capturable<T>, args: T, result: Any, transformAt: (Double) -> Mat) {
    val closable = enqueue(subject, args, result, transformAt)
    undo {
        closable.close()
    }
}

/**
 * Queues a [Capturable] in the [Once], undoes by closing the resulting auto-closable (preventing it from capturing).
 */
fun <T> Once.recEnqueue(subject: Capturable<T?>, result: Any, transformAt: (Double) -> Mat) {
    val closable = enqueue(subject, result, transformAt)
    undo {
        closable.close()
    }
}
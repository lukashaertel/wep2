package eu.metatools.f2d.up

import eu.metatools.f2d.context.Capturable
import eu.metatools.f2d.context.Drawable
import eu.metatools.f2d.context.Once
import eu.metatools.f2d.context.Playable
import eu.metatools.f2d.math.Mat
import eu.metatools.up.Ent
import eu.metatools.up.dt.div
import eu.metatools.up.dt.lx
import eu.metatools.wep2.track.undo
import java.util.*

/**
 * Queues a [Drawable] in the [Once], undoes by closing the resulting auto-closable (preventing it from drawing).
 */
fun <T> Ent.enqueue(once: Once, subject: Drawable<T>, args: T, transformAt: (Double) -> Mat) {
    val uuid = UUID.randomUUID()
    val closable = once.enqueue(subject, args, transformAt)
    shell.engine.capture(lx / uuid) {
        closable.close()
    }
}

/**
 * Queues a [Drawable] in the [Once], undoes by closing the resulting auto-closable (preventing it from drawing).
 */
fun <T> Ent.enqueue(once: Once, subject: Drawable<T?>, transformAt: (Double) -> Mat) {
    val uuid = UUID.randomUUID()
    val closable = once.enqueue(subject, transformAt)
    shell.engine.capture(lx / uuid) {
        closable.close()
    }
}

/**
 * Queues a [Playable] in the [Once], undoes by closing the resulting auto-closable (preventing it from playing).
 */
fun <T> Ent.enqueue(once: Once, subject: Playable<T>, args: T, transformAt: (Double) -> Mat) {
    val uuid = UUID.randomUUID()
    val closable = once.enqueue(subject, args, transformAt)
    shell.engine.capture(lx / uuid) {
        closable.close()
    }
}

/**
 * Queues a [Playable] in the [Once], undoes by closing the resulting auto-closable (preventing it from playing).
 */
fun <T> Ent.enqueue(once: Once, subject: Playable<T?>, transformAt: (Double) -> Mat) {
    val uuid = UUID.randomUUID()
    val closable = once.enqueue(subject, transformAt)
    shell.engine.capture(lx / uuid) {
        closable.close()
    }
}

/**
 * Queues a [Capturable] in the [Once], undoes by closing the resulting auto-closable (preventing it from capturing).
 */
fun <T> Ent.enqueue(once: Once, subject: Capturable<T>, args: T, result: Any, transformAt: (Double) -> Mat) {
    val uuid = UUID.randomUUID()
    val closable = once.enqueue(subject, args, result, transformAt)
    shell.engine.capture(lx / uuid) {
        closable.close()
    }
}

/**
 * Queues a [Capturable] in the [Once], undoes by closing the resulting auto-closable (preventing it from capturing).
 */
fun <T> Ent.enqueue(once: Once, subject: Capturable<T?>, result: Any, transformAt: (Double) -> Mat) {
    val uuid = UUID.randomUUID()
    val closable = once.enqueue(subject, result, transformAt)
    shell.engine.capture(lx / uuid) {
        closable.close()
    }
}
package eu.metatools.f2d.up

import eu.metatools.f2d.capturable.Capturable
import eu.metatools.f2d.drawable.Drawable
import eu.metatools.f2d.data.Mat
import eu.metatools.f2d.playable.Playable
import eu.metatools.f2d.queued.QueuedCapture
import eu.metatools.f2d.queued.QueuedDraw
import eu.metatools.f2d.queued.QueuedPlay
import eu.metatools.f2d.queued.enqueue
import eu.metatools.up.Ent

/**
 * Queues a [Drawable] in the [Queued], undoes by closing the resulting auto-closable (preventing it from drawing).
 */
fun <T> Ent.enqueue(queued: QueuedDraw, subject: Drawable<T>, args: T, transformAt: (Double) -> Mat) {
    val closable = queued.enqueue(subject, args, transformAt)
    shell.engine.capture {
        closable.close()
    }
}

/**
 * Queues a [Drawable] in the [Queued], undoes by closing the resulting auto-closable (preventing it from drawing).
 */
fun <T> Ent.enqueue(queued: QueuedDraw, subject: Drawable<T?>, transformAt: (Double) -> Mat) {
    val closable = queued.enqueue(subject, transformAt)
    shell.engine.capture {
        closable.close()
    }
}

/**
 * Queues a [Playable] in the [Queued], undoes by closing the resulting auto-closable (preventing it from playing).
 */
fun <T> Ent.enqueue(queued: QueuedPlay, subject: Playable<T>, args: T, transformAt: (Double) -> Mat) {
    val closable = queued.enqueue(subject, args, transformAt)
    shell.engine.capture {
        closable.close()
    }
}

/**
 * Queues a [Playable] in the [Queued], undoes by closing the resulting auto-closable (preventing it from playing).
 */
fun <T> Ent.enqueue(queued: QueuedPlay, subject: Playable<T?>, transformAt: (Double) -> Mat) {
    val closable = queued.enqueue(subject, transformAt)
    shell.engine.capture {
        closable.close()
    }
}

/**
 * Queues a [Capturable] in the [Queued], undoes by closing the resulting auto-closable (preventing it from capturing).
 */
fun <T> Ent.enqueue(queued: QueuedCapture, subject: Capturable<T>, args: T, result: Any, transformAt: (Double) -> Mat) {
    val closable = queued.enqueue(subject, args, result, transformAt)
    shell.engine.capture {
        closable.close()
    }
}

/**
 * Queues a [Capturable] in the [Queued], undoes by closing the resulting auto-closable (preventing it from capturing).
 */
fun <T> Ent.enqueue(queued: QueuedCapture, subject: Capturable<T?>, result: Any, transformAt: (Double) -> Mat) {
    val closable = queued.enqueue(subject, result, transformAt)
    shell.engine.capture {
        closable.close()
    }
}
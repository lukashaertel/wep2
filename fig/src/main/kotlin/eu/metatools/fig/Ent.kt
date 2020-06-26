package eu.metatools.fig

import eu.metatools.fio.capturable.Capturable
import eu.metatools.fio.drawable.Drawable
import eu.metatools.fio.data.Mat
import eu.metatools.fio.playable.Playable
import eu.metatools.fio.queued.QueuedCapture
import eu.metatools.fio.queued.QueuedDraw
import eu.metatools.fio.queued.QueuedPlay
import eu.metatools.fio.queued.enqueue
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
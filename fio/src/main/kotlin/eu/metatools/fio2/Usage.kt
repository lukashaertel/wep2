package eu.metatools.fio2

import com.badlogic.gdx.utils.Disposable
import java.util.*
import kotlin.concurrent.thread

/**
 * Resource association.
 */
private val used = WeakHashMap<Any, MutableList<AutoCloseable>>().also {
    // Add hook to close all yet unclosed resources.
    Runtime.getRuntime().addShutdownHook(thread(false) {
        // Close all unclosed resources.
        for (unclosed in it.values)
            for (closable in unclosed)
                closable.close()

        // Clear the set.
        it.clear()
    })
}

/**
 * Uses a resource and associates it to the receiver.
 */
fun <T : AutoCloseable> Any.use(closable: T): T {
    used.getOrPut(this, ::ArrayList).add(closable)
    return closable
}

/**
 * Uses a disposable resource and associates it to the receiver.
 */
fun <T : Disposable> Any.use(disposable: T): T {
    used.getOrPut(this, ::ArrayList).add(AutoCloseable {
        disposable.dispose()
    })
    return disposable
}

/**
 * Closes all resources associated with the receiver.
 */
fun Any.closeUsed() {
    val used = used.remove(this) ?: return
    for (closable in used.asReversed())
        closable.close()
}
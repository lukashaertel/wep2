package eu.metatools.up.aspects

import eu.metatools.up.dt.Lx
import eu.metatools.up.dt.parent
import eu.metatools.up.notify.Handler

/**
 * Stores and restores identified components..
 */
interface Store : Aspect {
    /**
     * True if the store should be used to initialize values.
     */
    var isLoading: Boolean

    /**
     * Listen to for saving.
     */
    val handleSave: Handler<(Lx, Any?) -> Unit>

    /**
     * Saves the data.
     */
    fun save()

    /**
     * Loads data as [id], counterpart of [handleSave].
     */
    fun load(id: Lx): Any?

    /**
     * Lists all IDs under the given [parent], not excluding recursively nested elements.
     */
    fun lsr(parent: Lx): Sequence<Lx>
}

/**
 * Lists only the direct children of [parent].
 */
fun Store.ls(parent: Lx) =
    lsr(parent).filter { it.parent == parent }
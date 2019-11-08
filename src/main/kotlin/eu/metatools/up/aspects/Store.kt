package eu.metatools.wep2.nes.aspects

import eu.metatools.wep2.nes.dt.Lx
import eu.metatools.wep2.nes.dt.div
import eu.metatools.wep2.nes.dt.lx
import eu.metatools.wep2.nes.dt.parent
import eu.metatools.wep2.nes.lang.label
import eu.metatools.wep2.nes.lang.never
import eu.metatools.wep2.nes.notify.Handler
import java.util.*

/**
 * Unique ID of the system domain. Do not use this key as a root node.
 */
val systemDomain = (UUID.fromString("6aa03267-b187-415d-8b8f-2e93ae27cc1b") ?: never)
    .label("systemDomain")

/**
 * Primary entity table.
 */
val PET = lx / systemDomain / "PET"

interface Store : Aspect {
    /**
     * True if the store should be used to initialize values.
     */
    val isLoading:Boolean

    /**
     * Listen to for saving.
     */
    val save: Handler<(Lx, Any?) -> Unit>

    /**
     * Loads data as [id], counterpart of [save].
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
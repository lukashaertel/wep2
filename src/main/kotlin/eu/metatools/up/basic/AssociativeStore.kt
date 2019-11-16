package eu.metatools.up.basic

import eu.metatools.up.aspects.Aspects
import eu.metatools.up.aspects.Store
import eu.metatools.up.aspects.With
import eu.metatools.up.dt.Lx
import eu.metatools.up.dt.contains
import eu.metatools.up.lang.validate
import eu.metatools.up.notify.HandlerList

/**
 * Stores and lists values in a mutable map.
 */
class AssociativeStore(on: Aspects?, val data: MutableMap<Lx, Any?> = hashMapOf()) : With(on), Store {
    override var isLoading: Boolean = false

    override val handleSave = HandlerList<(Lx, Any?) -> Unit>()

    override fun save() {
        handleSave(data::set)
    }

    override fun load(id: Lx) =
        validate(id in data) { "Load to non-present value $id" } ?: data[id]

    override fun lsr(parent: Lx) =
        data.keys.asSequence().filter { it in parent }
}
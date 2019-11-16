package eu.metatools.up

import eu.metatools.up.aspects.Aspects
import eu.metatools.up.aspects.Store
import eu.metatools.up.aspects.invoke
import eu.metatools.up.dt.*
import eu.metatools.up.lang.constructBy
import kotlin.reflect.KClass
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSupertypeOf

private val aspectType = Aspects::class.createType(nullable = true)
private val lxType = Lx::class.createType(nullable = false)
/**
 * Reconstructs all [Ent]s that are stored in the receivers [Store] aspects, constructs them on the receiver.
 */
fun Aspects.reconstructPET(receive: (Lx, Ent) -> Unit) {
    this<Store> {

        // Iterate PET entries.
        for (petEntry in lsr(PET)) {
            // Load class and data.
            @Suppress("unchecked_cast")
            val data = load(petEntry) as Pair<KClass<out Ent>, Map<String, Any?>>

            // Get the ID as relative to the PET.
            val id = petEntry subtract PET

            // Receive new entity, constructed by the data and the receiver aspects.
            receive(id subtract PET, data.first.constructBy(data.second) {
                when {
                    // If type is the aspects receiver, return the aspects passed to the function.
                    it.type.isSupertypeOf(aspectType) -> Box(this@reconstructPET)

                    // If type is identity receiver, return the given id.
                    it.type.isSupertypeOf(lxType) -> Box(id)

                    // Other parameters should not be assigned.
                    else -> null
                }
            })
        }
    }
}


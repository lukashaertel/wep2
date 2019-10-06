package eu.metatools.wep2.examples

import eu.metatools.wep2.components.prop
import eu.metatools.wep2.entity.*
import eu.metatools.wep2.storage.Restore
import eu.metatools.wep2.storage.restoreBy
import eu.metatools.wep2.storage.storeBy
import eu.metatools.wep2.tools.ReclaimableSequence
import eu.metatools.wep2.tools.shortNat
import eu.metatools.wep2.track.Claimer

/**
 * A container entity with a child.
 */
class Container(context: Context<Int, Int, SI>, restore: Restore?) : RestoringEntity<Int, Int, SI>(context, restore) {
    var element by prop {
        Element(context, restore)
    }

    override fun evaluate(name: Int, time: Int, args: Any?): () -> Unit {
        return {}
    }

    override fun toString() = "(Container $id, element=${element.id})"
}

/**
 * A child entity with some properties.
 */
class Element(context: Context<Int, Int, SI>, restore: Restore?) : RestoringEntity<Int, Int, SI>(context, restore) {
    var xCoord by prop { 0 }

    var yCoord by prop { 0 }

    override fun evaluate(name: Int, time: Int, args: Any?): () -> Unit {
        return {}
    }

    override fun toString() = "(Element $id, xCoord=$xCoord, yCoord=$yCoord)"
}

/**
 * This example shows how [RestoringEntity] can be used to equivalently restore the status of an entity index.
 */
fun main() {
    // Generate basic status, entity map with simple names and identities.
    val index by entityMap<Int, Int, SI>()
    val idgen = shortNat()
    val ids = Claimer(idgen)

    // Create the context on the properties for construction.
    val context = context(index, ids::claim, ids::release) { _, _, _, _ -> }

    // Create the existing entity, assign the element values.
    val root = Container(context, null).also {
        // Delete the element, creating a state that would not be equal to the default.
        it.element.delete()

        // Assign a new element.
        it.element = Element(it.context, null)

        // Set it's values.
        it.element.xCoord = 2
        it.element.yCoord = 5
    }


    // The map that will be stored to, implementations can use files, network objects, etc.
    val map = mutableMapOf<String, Any?>()

    // Use map for storage.
    storeBy(map::set) {
        // Store the index of the context to the map.
        storeIndex(context, it)

        // Also put ID of the standard root object.
        it.save("root id", root.id)
    }

    // Create the index to restore.
    val resIndex by entityMap<Int, Int, SI>()

    // Restore the identity generator.
    val resIdgen = ReclaimableSequence.restore(
        idgen.sequence, idgen.zero, idgen.inc,
        idgen.generatorHead, idgen.recycled
    )

    // Restore the identifier from the restored identity genrator.
    val resIds = Claimer(resIdgen)

    // Link the context.
    val resContext = context(resIndex, resIds::claim, resIds::release) { _, _, _, _ -> }

    // Track field where root ID is stored.
    // Use map for restoring.
    val resRootId = restoreBy(map::getValue) {
        // Use the map with all the values to restore the index of the context.
        restoreIndex(resContext, it)

        // Get the ID of the standard root object.
        it.load<SI>("root id")
    }

    // Print both indices.
    println(index)
    println(resIndex)

    // Get the entity corresponding to the stored one.
    val resRoot = resIndex[resRootId] as Container

    // Print the values.
    println(resRoot)
    println(resRoot.element)

    // Claim to more IDs to show that no new IDs are generated from entity restoration.
    println(ids.claim())
    println(resIds.claim())

    // Expected output:

    // {(0, 0)=(Container (0, 0), element=(1, 1)), (1, 1)=(Element (1, 1), xCoord=2, yCoord=5)}
    // {(0, 0)=(Container (0, 0), element=(1, 1)), (1, 1)=(Element (1, 1), xCoord=2, yCoord=5)}
    // (Container (0, 0), element=(1, 1))
    // (Element (1, 1), xCoord=2, yCoord=5)
    // (2, 0)
    // (2, 0)
}
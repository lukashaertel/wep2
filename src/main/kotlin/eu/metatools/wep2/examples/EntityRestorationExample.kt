package eu.metatools.wep2.examples

import eu.metatools.wep2.coord.Coordinator
import eu.metatools.wep2.entity.Context
import eu.metatools.wep2.entity.RestoringEntity
import eu.metatools.wep2.entity.SN
import eu.metatools.wep2.entity.bind.*
import eu.metatools.wep2.entity.entityMap
import eu.metatools.wep2.tools.ReclaimableSequence
import eu.metatools.wep2.track.SI
import eu.metatools.wep2.track.bind.prop
import eu.metatools.wep2.track.bind.ref
import eu.metatools.wep2.track.identifier
import eu.metatools.wep2.track.smallSequence

/**
 * Does not do anything, coordination is not the focus here, operations are never invoked.
 */
class NonCoordinator : Coordinator<SN<Int>, Int>() {
    override fun register(block: (SN<Int>, Int, Any?) -> Unit) = throw NotImplementedError()

    override fun receive(name: SN<Int>, time: Int, args: Any?) = throw NotImplementedError()

    override fun publish(name: SN<Int>, time: Int, args: Any?) = throw NotImplementedError()
}

/**
 * A container entity with a child.
 */
class Container(context: Context<Int, Int, SI>, restore: Restore?) : RestoringEntity<Int, Int, SI>(context, restore) {
    var element by ref(restore) {
        Element(context, restore)
    }

    override fun evaluate(name: Int, time: Int, args: Any?): () -> Unit {
        return {}
    }

    override fun toString() = "(Container $id, element=${element?.id})"
}

/**
 * A child entity with some properties.
 */
class Element(context: Context<Int, Int, SI>, restore: Restore?) : RestoringEntity<Int, Int, SI>(context, restore) {
    var xCoord by prop(restore) { 0 }

    var yCoord by prop(restore) { 0 }

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
    val index = entityMap<Int, Int, SI>()
    val idgen = smallSequence()
    val ids = identifier(idgen)

    // Create the context on the properties for construction.
    val context = Context(NonCoordinator(), index, ids)

    // Create the existing entity, assign the element values.
    val root = Container(context, null).also {
        // Delete the element, creating a state that would not be equal to the default.
        it.element?.delete()

        // Assign a new element.
        it.element = Element(it.context, null)

        // Set it's values.
        it.element?.xCoord = 2
        it.element?.yCoord = 5
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
    val resIndex = entityMap<Int, Int, SI>()

    // Restore the identity generator.
    val resIdgen = ReclaimableSequence.restore(
        idgen.sequence, idgen.zero, idgen.inc,
        idgen.generatorHead, idgen.recycled
    )

    // Restore the identifier from the restored identity genrator.
    val resIds = identifier(resIdgen)

    // Link the context.
    val resContext = Context(NonCoordinator(), resIndex, resIds)

    // Track field where root ID is stored.
    lateinit var resRootId: SI

    // Use map for restoring.
    restoreBy(map::getValue) {
        // Use the map with all the values to restore the index of the context.
        restoreIndex(resContext, it)

        // Get the ID of the standard root object.
        resRootId = it.load("root id")
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

    //{(0, 0)=(Container (0, 0), element=(1, 1)), (1, 1)=(Element (1, 1), xCoord=2, yCoord=5)}
    //{(0, 0)=(Container (0, 0), element=(1, 1)), (1, 1)=(Element (1, 1), xCoord=2, yCoord=5)}
    //(Container (0, 0), element=(1, 1))
    //(Element (1, 1), xCoord=2, yCoord=5)
    //(2, 0)
    //(2, 0)
}
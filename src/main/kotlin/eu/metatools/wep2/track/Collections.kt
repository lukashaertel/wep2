package eu.metatools.wep2.track

import eu.metatools.wep2.util.*

/**
 * A set partaking in change based undo-tracking. Alternatively, prop of an immutable set can be used.
 */
fun <E : Comparable<E>> set() = observableSet<E>({ e ->
    // Run on undos if assigned.
    undos.get()?.let {
        // Add removing the just added element.
        it.add({ silent.remove(e); Unit } labeledAs {
            "remove $e on $this"
        })
    }
}, { e ->
    undos.get()?.let {
        // Add re-adding the just removed element.
        it.add({ silent.add(e); Unit } labeledAs {
            "add $e on $this"
        })
    }
})

/**
 * A map partaking in change based undo-tracking. Alternatively, prop of an immutable map can be used.
 */
fun <K : Comparable<K>, V> map() = observableMap<K, V>({ k, _ ->
    // Run on undos if assigned.
    undos.get()?.let {
        // Add removing the just added entry.
        it.add({ silent.remove(k); Unit } labeledAs {
            "remove $k on $this"
        })
    }
}, { k, v, _ ->
    undos.get()?.let {
        // Add resetting entry to previous value.
        it.add({ silent[k] = v } labeledAs {
            "set $k=$v on $this"
        })
    }
}, { k, v ->
    undos.get()?.let {
        // Add re-adding the just removed entry.
        it.add({ silent[k] = v } labeledAs {
            "add $k=$v on $this"
        })
    }
})
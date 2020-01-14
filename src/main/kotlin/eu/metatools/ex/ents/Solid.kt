package eu.metatools.ex.ents

import eu.metatools.f2d.data.Q

/**
 * Entity has a solid, circular hull.
 */
interface Solid : All {
    /**
     * The radius of the entity.
     */
    val radius: Q
}
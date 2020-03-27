package eu.metatools.fio

import eu.metatools.fio.resource.Lifecycle

/**
 * Provides inputs/outputs and resources.
 */
interface Fio {
    /**
     * UI relative inputs/outputs.
     */
    val ui: InOut

    /**
     * World relative inputs/outputs.
     */
    val world: InOut

    /**
     * Gets the current time.
     */
    val time: Double

    /**
     * Marks an element as used, will be initialized and disposed of in the corresponding methods.
     */
    fun <T : Lifecycle> use(element: T): T
}
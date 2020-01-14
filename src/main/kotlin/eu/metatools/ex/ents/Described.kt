package eu.metatools.ex.ents

/**
 * Entity has a description.
 */
interface Described : All {
    /**
     * The description for an entity.
     */
    val describe: String
}
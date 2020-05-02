package eu.metatools.sx.ents

import eu.metatools.sx.lang.FP

/**
 * Cell definition.
 * @property capacity The capacity for compressible mass.
 * @property resistance The resistance of flowing through that cell.
 */
data class Cell(
    val capacity: FP,
    val resistance: FP
)
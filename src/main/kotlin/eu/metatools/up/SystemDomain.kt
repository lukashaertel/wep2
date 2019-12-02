package eu.metatools.up

import eu.metatools.up.dt.div
import eu.metatools.up.dt.lx
import eu.metatools.up.lang.never
import java.util.*

/**
 * Unique ID of the system domain. Do not use this key as a root node.
 */
val systemDomain = ".sd"

/**
 * Primary entity table.
 */
val PET = lx / systemDomain / "PET"

/**
 * Presence attribute.
 */
val presence = lx / systemDomain / "EX"

/**
 * Local time per global.
 */
val LPG = lx / systemDomain / "LPG"

/**
 * Instruction replay table.
 */
val IRT = lx / systemDomain / "IRT"
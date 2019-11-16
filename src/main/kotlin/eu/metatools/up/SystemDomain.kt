package eu.metatools.up

import eu.metatools.up.dt.div
import eu.metatools.up.dt.lx
import eu.metatools.up.lang.never
import java.util.*

/**
 * Unique ID of the system domain. Do not use this key as a root node.
 */
val systemDomain = (UUID.fromString("6aa03267-b187-415d-8b8f-2e93ae27cc1b") ?: never)
/**
 * Primary entity table.
 */
val PET = lx / systemDomain / "PET"
/**
 * Presence attribute.
 */
val presence = lx / systemDomain / "presence"
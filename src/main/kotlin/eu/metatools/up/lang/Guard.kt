package eu.metatools.up.lang

/**
 * Uses the boolean as a guard for execution.
 */
inline operator fun <T> Boolean.invoke(block: () -> T): T? =
    if (this) block() else null
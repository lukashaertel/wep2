package eu.metatools.wep2.system

import eu.metatools.wep2.tools.Time
import eu.metatools.wep2.util.ComparablePair
import java.io.Serializable

/**
 * A standard initializer.
 */
data class StandardInitializer<N, P>(
    val playerHead: Short?,
    val playerRecycled: List<ComparablePair<Short, Short>>,
    val idsHead: Short?,
    val idsRecycled: List<ComparablePair<Short, Short>>,
    val playerSelf: ComparablePair<Short, Short>,
    val playerCount: Short,
    val scopes: Map<Long, Byte>,
    val instructions: List<Triple<StandardName<N>, Time, Any?>>,
    val parameter: P,
    val saveData: Map<String, Any?>
) : Serializable {
    /**
     * Creates a summary string.
     */
    fun summarize() = buildString {
        // Caption.
        appendln("StandardInitializer:")

        // Player IDs.
        append("\tLast generated player ID: ")
        appendln(playerHead)
        appendln("\tRecycled player IDs:")
        for ((p, r) in playerRecycled) {
            append("\t\tPlayer ")
            append(p)
            append(", ID used ")
            append(r)
            appendln(" times")
        }
        if (playerRecycled.isEmpty())
            appendln("\t\t< empty >")

        // Entity IDs.
        append("\tLast generated entity ID: ")
        appendln(idsHead)
        appendln("\tRecycled entity IDs:")
        for ((e, r) in idsRecycled) {
            append("\t\tEntity ")
            append(e)
            append(", ID used ")
            append(r)
            appendln(" times")
        }
        if (idsRecycled.isEmpty())
            appendln("\t\t< empty >")

        // Self player, if restoring into.
        append("\tLoad player slot: ")
        appendln(playerSelf)

        // Number of players.
        append("\tTracked player count: ")
        appendln(playerCount)

        // Time slots
        appendln("\tTime synchronization slots:")
        for ((t, i) in scopes) {
            append("\t\tTime ")
            append(t)
            append(", last slot ")
            appendln(i)
        }
        if (scopes.isEmpty())
            appendln("\t\t< empty >")

        // Instructions.
        appendln("\tInstructions:")
        for ((n, t, a) in instructions) {
            append("\t\t")
            when (n) {
                ClaimPlayer -> append("Claim player")
                ReleasePlayer -> append("Release player")
                is ActiveName -> {
                    append(n.name.second)
                    append(" on ")
                    append(n.name.first)
                }
            }
            append(", at ")
            append(t)
            if (a != Unit) {
                append(", argument: ")
                append(a)
            }
            appendln()
        }
        if (instructions.isEmpty())
            appendln("\t\t< empty >")

        // Parameter.
        append("\tParameter: ")
        appendln(parameter)

        // Save data.
        appendln("\tSave data:")
        for ((k, v) in saveData) {
            append("\t\t")
            append(k)
            append('=')
            appendln(v)
        }
        if (saveData.isEmpty())
            appendln("\t\t< empty >")
    }
}
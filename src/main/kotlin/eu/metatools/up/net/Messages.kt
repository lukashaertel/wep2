package eu.metatools.up.net

import eu.metatools.up.dt.Instruction
import java.util.*

/**
 * A net message.
 */
sealed class NetMessage

/**
 * Request to claims table.
 */
object NetReqClaims : NetMessage()

/**
 * Request to time delta.
 */
object NetReqOffset : NetMessage()

/**
 * Request to data bundle.
 */
object NetReqBundle : NetMessage()

/**
 * Network exchanged instruction.
 */
data class NetInstruction(val instruction: Instruction) : NetMessage()

/**
 * Claim poke (reassure that [uuid] claims the slot it has or a new one).
 */
data class NetTouch(val uuid: UUID, val time: Long) : NetMessage()

/**
 * Result of touching a claim.
 */
data class Claim(val id: Short, val expires: Long) {
    /**
     * Clones the claim with a new expiry.
     */
    fun renew(newExpires: Long) =
        Claim(id, newExpires)
}
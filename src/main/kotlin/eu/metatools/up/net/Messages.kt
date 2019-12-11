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
object NetReqClaims : NetMessage() {
    override fun toString() =
        "NetReqClaims"
}

/**
 * Request to sign-off.
 */
object NetReqSignOff : NetMessage() {
    override fun toString() =
        "NetReqSignOff"
}

/**
 * Request to data bundle.
 */
object NetReqBundle : NetMessage() {
    override fun toString() =
        "NetReqBundle"
}

/**
 * Request to time delta and update sign-off.
 */
data class NetPing(val time: Long) : NetMessage()

/**
 * Network exchanged instruction.
 */
data class NetInstruction(val instruction: Instruction) : NetMessage() {
    override fun toString() =
        "NetInstruction($instruction)"
}

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
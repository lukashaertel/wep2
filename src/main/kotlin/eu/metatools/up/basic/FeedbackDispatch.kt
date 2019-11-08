package eu.metatools.up.basic

import eu.metatools.up.aspects.*
import eu.metatools.up.dt.Instruction
import eu.metatools.up.dt.Lx
import eu.metatools.up.notify.Event

/**
 * Runs the [Dispatch] by feeding [send] back into [handleReceive] directly and transferring a potentially [Proxify]
 * converted version to [xferOut]. The inward handle is passed to [xferIn], proxies passed to it will be resolved if
 * the [Proxify] aspect is present.
 *
 * @param on The aspects to use.
 * @property xferOut The outward connection.
 * @property xferIn The inward connection receiver.
 */
class FeedbackDispatch(
    on: Aspects?,
    val xferOut: (Lx, Instruction) -> Unit,
    val xferIn: ((Lx, Instruction) -> Unit) -> Unit
) : With(on), Dispatch {
    override val handleReceive = Event<Lx, Instruction>()

    init {
        // Transfer in as calling the handler.
        xferIn { id, instruction ->
            // If proxification is available, use it
            this<Proxify> {
                handleReceive(id, Instruction(instruction.name, instruction.time, toValue(instruction.args)))
            } ?: run {
                handleReceive(id, instruction)
            }
        }
    }

    override fun send(id: Lx, instruction: Instruction) {
        // Feedback.
        handleReceive(id, instruction)

        // If proxification is available, use it.
        this<Proxify> {
            xferOut(id, Instruction(instruction.name, instruction.time, toProxy(instruction.args)))
        } ?: run {
            xferOut(id, instruction)
        }
    }
}
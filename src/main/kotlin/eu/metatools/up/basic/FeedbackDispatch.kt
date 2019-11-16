package eu.metatools.up.basic

import eu.metatools.up.aspects.*
import eu.metatools.up.dt.Instruction
import eu.metatools.up.dt.Lx
import eu.metatools.up.notify.EventList

/**
 * Runs the [Dispatch] by feeding [send] back into [handlePerform] directly and transferring a potentially [Proxify]
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
    override val handlePrepare = EventList<Lx, Instruction>()

    override val handlePerform = EventList<Lx, Instruction>()

    override val handleComplete = EventList<Lx, Instruction>()

    private fun invokeHandlers(id: Lx, instruction: Instruction) {
        handlePrepare(id, instruction)
        handlePerform(id, instruction)
        handleComplete(id, instruction)
    }

    init {
        // Transfer in as calling the handler.
        xferIn { id, instruction ->
            // If proxification is available, use it
            this<Proxify> {
                invokeHandlers(id, Instruction(instruction.name, instruction.time, toValue(instruction.args)))
            } ?: run {
                invokeHandlers(id, instruction)
            }
        }
    }

    override fun send(id: Lx, instruction: Instruction) {
        // Feedback.
        invokeHandlers(id, instruction)

        // If proxification is available, use it.
        this<Proxify> {
            xferOut(id, Instruction(instruction.name, instruction.time, toProxy(instruction.args)))
        } ?: run {
            xferOut(id, instruction)
        }
    }
}
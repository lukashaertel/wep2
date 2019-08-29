package eu.metatools.mk.coord

import eu.metatools.mk.util.Choice
import eu.metatools.mk.util.Left
import eu.metatools.mk.util.Right

/**
 * Switches a choice into two coordinators.
 * @property left The left coordinator that uses the [Left] part of the [Choice]
 * @property right The right coordinator that uses the [Right] part of the [Choice]
 */
class Switcher<N1, N2, T : Comparable<T>>(
    val left: Coordinator<N1, T>,
    val right: Coordinator<N2, T>
) : Coordinator<Choice<N1, N2>, T>() {
    override fun publish(name: Choice<N1, N2>, time: T, args: Any?) {
        when (name) {
            is Left -> left.publish(name.item, time, args)
            is Right -> right.publish(name.item, time, args)
        }
    }

    override fun register(block: (Choice<N1, N2>, T, Any?) -> Unit): AutoCloseable {
        // Register listener for left.
        val leftListener = left.register { name, time, args ->
            // Wrap signalled name in Left.
            block(Left(name), time, args)
        }

        // Register listener for right.
        val rightListener = left.register { name, time, args ->
            // Wrap signalled name in Right.
            block(Right(name), time, args)
        }

        // On close, remove both part listeners.
        return AutoCloseable {
            leftListener.close()
            rightListener.close()
        }
    }

    override fun receive(name: Choice<N1, N2>, time: T, args: Any?) {
        when (name) {
            is Left -> left.receive(name.item, time, args)
            is Right -> right.receive(name.item, time, args)
        }
    }

    override fun receiveAll(namesTimesAndArgs: Sequence<Triple<Choice<N1, N2>, T, Any?>>) {
        // Find all left instances.
        val lefts = namesTimesAndArgs.mapNotNull { (name, time, args) ->
            if (name is Left)
                Triple(name.item, time, args)
            else
                null
        }

        // Find all right instances.
        val rights = namesTimesAndArgs.mapNotNull { (name, time, args) ->
            if (name is Right)
                Triple(name.item, time, args)
            else
                null
        }

        // Dispatch respectively.
        left.receiveAll(lefts)
        right.receiveAll(rights)
    }
}
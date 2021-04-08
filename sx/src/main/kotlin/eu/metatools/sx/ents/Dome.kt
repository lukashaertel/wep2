package eu.metatools.sx.ents

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.InputEvent
import eu.metatools.reaktor.ex.component
import eu.metatools.reaktor.gdx.utils.inputListener
import eu.metatools.sx.SX
import eu.metatools.sx.ui.VSpriteActor
import eu.metatools.up.Shell
import eu.metatools.up.dt.Lx
import kotlin.properties.Delegates.observable

fun easeInOutCubic(x: Float): Float {
    if (x < 0.5f)
        return 4f * x * x * x
    val u = -2f * x + 2f
    return 1f - u * u * u / 2f
}

val renderDome = component { xy: Pair<Float, Float>, radius: Float, selected: Boolean, select: () -> Unit ->
    val listener = inputListener {
        if (it.type == InputEvent.Type.touchDown)
            true.also { select() }
        else
            false
    }

    VSpriteActor(if (selected) WorldRes.roundDrawableRed else WorldRes.roundDrawable,
            width = radius * 2f,
            height = radius * 2f,
            x = xy.first - radius, y = xy.second - radius,
            listeners = listOf(listener))
}

interface ResourceProducing {
    val rps: Map<String, Double>
}

class Dome(
        shell: Shell, id: Lx, sx: SX,
        val x: Float, val y: Float, val radius: Float,
) : SXEnt(shell, id, sx), Reakted, Selectable, ResourceProducing {
    override val extraArgs = mapOf(
            "x" to x,
            "y" to y,
            "radius" to radius
    )

    override var selected by observable(false) { _, _, _ ->
        sx.updateUiRequested = true
    }

    override fun renderPrimary() {
        renderDome(id, x to y, radius, selected) {
            // On clicked.
            val currentSelected = shell.selection
            if (currentSelected is Dome) {
                if (currentSelected != this && canConnectTo(currentSelected)) {
                    sx.withTime { connectTo(currentSelected) }
                    shell.deselect()
                }
            } else {
                select()
            }
        }
    }

    private fun canConnectTo(other: Dome): Boolean =
            Vector2.dst2(x, y, other.x, other.y) < (200f * 200f)

    val connectTo = exchange { other: Dome ->
        if (canConnectTo(other))
            constructed(Connection(shell, newId(), sx)).also {
                it.from = this
                it.to = other
            }
    }

    override val rps =
            if (radius < 50f)
                mapOf("Oxygen" to -0.1, "Energy" to -0.1, "Influence" to 0.1)
            else
                mapOf("Oxygen" to -0.2, "Energy" to -0.2, "Influence" to 0.3)
}
package eu.metatools.sx.ents

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.scenes.scene2d.utils.TransformDrawable
import eu.metatools.reaktor.ex.component
import eu.metatools.reaktor.gdx.utils.inputListener
import eu.metatools.sx.SX
import eu.metatools.sx.ui.VSpriteActor
import eu.metatools.up.Shell
import eu.metatools.up.constructed
import eu.metatools.up.dt.Lx
import kotlin.properties.Delegates.observable

fun easeInOutCubic(x: Float): Float {
    if (x < 0.5f)
        return 4f * x * x * x
    val u = -2f * x + 2f
    return 1f - u * u * u / 2f
}

val renderCircle = component { x: Float, y: Float, radius: Float, drawable: TransformDrawable?, select: () -> Unit ->
    val listener = inputListener {
        if (it.type == InputEvent.Type.touchDown)
            true.also { select() }
        else
            false
    }

    VSpriteActor(
        drawable,
        width = radius * 2f,
        height = radius * 2f,
        x = x - radius, y = y - radius,
        listeners = listOf(listener)
    )
}

interface ResSupply {
    val supply: Map<String, Double>
}

interface ResFlow {
    val resPerSecond: Map<String, Double>
}

class Dome(
    shell: Shell, id: Lx, sx: SX,
    val x: Float, val y: Float, val radius: Float,
) : SXEnt(shell, id, sx), Reakted, Selectable, ResSupply, ResFlow {
    override val extraArgs = mapOf(
        "x" to x,
        "y" to y,
        "radius" to radius
    )

    override var selected by observable(false) { _, _, _ ->
        sx.updateUiRequested = true
    }

    override val layer = Layer.Main

    override fun renderPrimary() {
        renderCircle(id, x, y, radius, if (selected) WorldRes.roundDrawableRed else WorldRes.roundDrawable,) {
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
            Connection(shell, newId(), sx).constructed().also {
                it.from = this
                it.to = other
            }
    }

    override val supply =
        if (radius < 50f)
            mapOf("Living spaces" to 5.0, "Energy" to -1.0)
        else
            mapOf("Living spaces" to 10.0, "Energy" to -1.75)

    override val resPerSecond =
        if (radius < 50f)
            mapOf("Waste" to 0.1)
        else
            mapOf("Waste" to 0.2)
}
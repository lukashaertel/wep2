package eu.metatools.sx.ents

import com.badlogic.gdx.math.Vector2
import eu.metatools.reaktor.ex.component
import eu.metatools.sx.SX
import eu.metatools.sx.ui.VSpriteActor
import eu.metatools.up.Shell
import eu.metatools.up.dsl.listenProp
import eu.metatools.up.dsl.prop
import eu.metatools.up.dt.Lx


val renderConnection = component { x1: Float, y1: Float, x2: Float, y2: Float ->
    val v = Vector2(x2 - x1, y2 - y1)
    val a = v.angle()
    val d = v.len()

    VSpriteActor(
        WorldRes.boxDrawable,
        x = (x1 + x2) * 0.5f,
        y = (y1 + y2) * 0.5f,
        originX = 0.5f,
        originY = 0.5f,
        width = d,
        height = 20f,
        rotation = a
    )
}

class Connection(
    shell: Shell, id: Lx, sx: SX,
) : SXEnt(shell, id, sx), Reakted {
    var from by prop<Dome?> { null }
    var to by prop<Dome?> { null }

    init {
        this::from.listenProp {
            println("Assigned from to ${it.to}")
        }
    }

    override val layer = Layer.Lower

    override fun renderPrimary() {
        val from = from
        val to = to
        if (from != null && to != null)
            renderConnection(id, from.x, from.y, to.x, to.y)
    }
}
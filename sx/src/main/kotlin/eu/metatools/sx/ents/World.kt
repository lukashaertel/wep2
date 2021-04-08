package eu.metatools.sx.ents

import com.badlogic.gdx.math.Vector2
import eu.metatools.reaktor.gdx.*
import eu.metatools.reaktor.gdx.data.ExtentValues
import eu.metatools.reaktor.gdx.data.Extents
import eu.metatools.sx.SX
import eu.metatools.up.Shell
import eu.metatools.up.dsl.map
import eu.metatools.up.dsl.set
import eu.metatools.up.dt.Lx
import eu.metatools.up.dt.div
import eu.metatools.up.list
import java.util.*
import kotlin.math.roundToInt

/**
 * Entity takes part in global Reaktor rendering.
 */
interface Reakted {
    /**
     * Renders the receiver.
     */
    fun renderPrimary()
}

fun Number.roundForDisplay() =
        (toDouble() * 10.0).roundToInt() / 10.0

class World(shell: Shell, id: Lx, sx: SX) : SXEnt(shell, id, sx), Reakted {
    val domes by set<Dome>()

    val resources by map<String, Double>()

    fun computeTotalRPS(): Map<String, Double> = TreeMap<String, Double>().apply {
        shell.list<ResourceProducing>().forEach {
            for ((r, p) in it.rps)
                compute(r) { _, x -> (x ?: 0.0) + p }
        }
    }

    override fun renderPrimary() {
        val totalRps = computeTotalRPS()

        table(key = id, fillParent = true) {
            cell(row = 0, expandX = 1, fillX = 1f, fillY = 1f) {
                container(background = WorldRes.whiteBorder, pad = ExtentValues(4f), fillX = 1f) {
                    horizontalGroup(pad = Extents(8f, 2f), space = 20f) {
                        (resources.keys union totalRps.keys).forEach { r ->
                            val total = (resources[r] ?: 0.0).roundForDisplay()
                            val rps = (totalRps[r] ?: 0.0).roundForDisplay()
                            msdfLabel(
                                    text = "$r: $total ($rps/s)",
                                    shader = WorldRes.msdfShader, font = WorldRes.msdfFont,
                                    fontStyle = WorldRes.fontWhite)
                        }
                    }
                }
            }
            cell(row = 1, expandY = 1) {
                container(fillParent = true)
            }
            cell(row = 2, expandX = 1, fillX = 1f)
        }
    }

    val buildDome = exchange { x: Float, y: Float, large: Boolean ->
        val radius = if (large) 50f else 25f
        if (shell.list<Dome>().none { Vector2.dst2(x,y,it.x, it.y) < (radius + it.radius).let { it*it } })
            domes.add(constructed(Dome(shell, newId() / 1, sx, x, y, radius)))
    }

    /**
     * Periodic world update.
     */
    val worldUpdate = repeating(Short.MAX_VALUE, 1000L, shell::initializedTime) {
        val totalRps = computeTotalRPS()

        for ((r, p) in totalRps)
            resources.compute(r) { _, r -> (r ?: 0.0) + p }

        shell.list<Updating>().forEach {
            it.update(this)
        }
    }

    /**
     * Renders all actors.
     */
    fun render(time: Double, delta: Double) {

    }
}
package eu.metatools.sx.ents

import com.badlogic.gdx.math.Vector2
import eu.metatools.reaktor.gdx.*
import eu.metatools.reaktor.gdx.data.ExtentValues
import eu.metatools.reaktor.gdx.data.Extents
import eu.metatools.reaktor.gdx.utils.px
import eu.metatools.sx.SX
import eu.metatools.up.Shell
import eu.metatools.up.dsl.map
import eu.metatools.up.dsl.set
import eu.metatools.up.dt.Lx
import eu.metatools.up.dt.div
import eu.metatools.up.list
import java.util.*
import kotlin.math.IEEErem
import kotlin.math.absoluteValue
import kotlin.math.roundToInt


enum class Layer(val z: Int) {
    Background(0),
    Lower(1),
    Main(2),
    Upper(3),
    Objects(4),
    Actors(5),
    Sky(6),
    Menu(7)
}

/**
 * Entity takes part in global Reaktor rendering.
 */
interface Reakted {
    val layer: Layer

    /**
     * Renders the receiver.
     */
    fun renderPrimary()

}

fun Double.roundForDisplay() =
    if (IEEErem(1.0) == 0.0) roundToInt().toString() else ((toDouble() * 10.0).roundToInt() / 10.0).toString()

fun Double.epsilonZero() =
    absoluteValue < 1e-8

class World(shell: Shell, id: Lx, sx: SX) : SXEnt(shell, id, sx), Reakted {
    val domes by set<Dome>()

    val stack by map<String, Double>()

    fun computeSupply() = TreeMap<String, Double>().apply {
        shell.list<ResSupply>().forEach {
            for ((res, supply) in it.supply)
                compute(res) { _, x -> ((x ?: 0.0) + supply).takeUnless(Double::epsilonZero) }
        }
    }

    fun computeResFlow(): Map<String, Double> = TreeMap<String, Double>().apply {
        shell.list<ResFlow>().forEach {
            for ((res, flow) in it.resPerSecond)
                compute(res) { _, x -> ((x ?: 0.0) + flow).takeUnless(Double::epsilonZero) }
        }
    }

    override val layer = Layer.Menu

    override fun renderPrimary() {
        val supply = computeSupply()
        val flow = computeResFlow()

        table(key = id, fillParent = true) {
            cell(row = 0, expandX = 1, fillX = 1f, fillY = 1f) {
                container(pad = ExtentValues(4f), fillX = 1f) {
                    container(background = WorldRes.menuBorder, fillX = 1f, minHeight = 20.px) {
                        horizontalGroup(pad = Extents(4f, 4f), space = 20f) {
                            supply.forEach { (res, supply) ->
                                val display = supply.roundForDisplay()
                                val style = if (supply < 0.0) WorldRes.fontWarn else WorldRes.fontPrimary
                                msdfLabel(
                                    text = "$display $res",
                                    shader = WorldRes.msdfShader, font = WorldRes.msdfFont,
                                    fontStyle = style
                                )
                            }
                            (stack.keys union flow.keys).forEach { r ->
                                val total = (stack[r] ?: 0.0).roundForDisplay()
                                val rps = (flow[r] ?: 0.0).roundForDisplay()
                                val style = if ((flow[r] ?: 0.0) < 0.0) WorldRes.fontWarn else WorldRes.fontPrimary
                                msdfLabel(
                                    text = "$r: $total ($rps/s)",
                                    shader = WorldRes.msdfShader, font = WorldRes.msdfFont,
                                    fontStyle = style
                                )
                            }
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
        if (shell.list<Dome>().none { Vector2.dst2(x, y, it.x, it.y) < (radius + it.radius).let { it * it } })
            domes.add(constructed(Dome(shell, newId() / 1, sx, x, y, radius)))
    }

    /**
     * Periodic world update.
     */
    val worldUpdate = repeating(Short.MAX_VALUE, 1000L, shell::initializedTime) {
        val totalRps = computeResFlow()

        for ((res, flow) in totalRps)
            stack.compute(res) { _, x -> ((x ?: 0.0) + flow).takeUnless(Double::epsilonZero) }

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
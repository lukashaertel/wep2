package eu.metatools.f2d.ex

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import eu.metatools.f2d.context.refer
import eu.metatools.f2d.math.Mat
import eu.metatools.f2d.math.Pt
import eu.metatools.f2d.tools.AtlasResource
import eu.metatools.f2d.tools.SolidResource
import eu.metatools.f2d.tools.tint
import eu.metatools.wep2.components.map
import eu.metatools.wep2.components.prop
import eu.metatools.wep2.components.set
import eu.metatools.wep2.components.ticker
import eu.metatools.wep2.storage.Restore
import eu.metatools.wep2.tools.TickGenerator
import eu.metatools.wep2.tools.Time
import eu.metatools.wep2.track.rec
import eu.metatools.wep2.track.undo
import eu.metatools.wep2.util.first
import eu.metatools.wep2.util.listeners.mapListener
import eu.metatools.wep2.util.then
import eu.metatools.wep2.util.within
import java.io.Serializable

data class XY(val x: Int, val y: Int) : Comparable<XY>, Serializable {
    val isEmpty get() = x == 0 && y == 0

    override fun compareTo(other: XY) =
        first(other) {
            y.compareTo(other.y)
        } then {
            x.compareTo(other.x)
        }

    override fun toString() = "($x, $y)"
}

data class Dimension(val minX: Int, val minY: Int, val maxX: Int, val maxY: Int) {
    val width get() = 1 + maxX - minX

    val height get() = 1 + maxY - minY

    fun include(xy: XY) =
        Dimension(
            minOf(minX, xy.x), minOf(minY, xy.y),
            maxOf(maxX, xy.x), maxOf(maxY, xy.y)
        )
}

var viewUnderground = false


object Constants {
    /**
     * Width of the tile.
     */
    val tileWidth = 32f

    /**
     * Height of the tile.
     */
    val tileHeight = 32f

    /**
     * Depth of the tile, i.e., how much is an underground tile displaced.
     */
    val tileDepth = 24f
}

object Resources {
    val solid by lazy { frontend.use(SolidResource()) }

    val terrain by lazy { frontend.use(AtlasResource { Gdx.files.internal("terrain.atlas") }) }
}

interface Rendered {
    fun render(time: Double)
}

interface Ticking {
    val ticker: TickGenerator
}

class World(context: GameContext, restore: Restore?) : GameEntity(context, restore), Rendered {
    val clipping = SDFComposer()

    private val sdfs = mutableMapOf<Float, (Pt) -> Float>()

    fun sdf(radius: Float) =
        sdfs.getOrPut(radius) {
            clipping.sdf(radius)
        }


    val tiles by map<XY, TileKind>(
        mapListener(
            added = { k, v ->
                if (!v.passable) {
                    clipping.add(k.x, k.y)
                    sdfs.clear()
                }
            },
            changed = { k, o, n ->
                if (o.passable != n.passable) {
                    if (n.passable)
                        clipping.remove(k.x, k.y)
                    else
                        clipping.add(k.x, k.y)
                    sdfs.clear()
                }
            },
            removed = { k, v ->
                if (v.passable) {
                    clipping.remove(k.x, k.y)
                    sdfs.clear()
                }
            })
    )

    val movers by set<Mover>()

    override fun render(time: Double) {
        for ((k, v) in tiles)
            frontend.continuous.submit(
                v.visual, time, Mat
                    .translation(Constants.tileWidth * k.x, Constants.tileHeight * k.y)
                    .scale(Constants.tileWidth, Constants.tileHeight)
            )
    }

    override fun evaluate(name: GameName, time: Time, args: Any?) = when (name) {
        "createMover" -> rec {
            val ent = Mover(context, time, this, Pt(5f, 5f), Movers.S, args as Short?)
            undo { ent.delete() }
            movers.add(ent)
        }
        else -> super.evaluate(name, time, args)
    }

    fun passable(x: Float, y: Float, radius: Float): Boolean {
        check(radius <= 1.0)
        for (a in sequenceOf(-radius, 0f, radius))
            for (b in sequenceOf(-radius, 0f, radius)) {
                val ix = (x + a + 0.5f).toInt()
                val iy = (y + b + 0.5f).toInt()
                if (tiles[XY(ix, iy)]?.passable == false)
                    return false
            }

        return true
    }
}

interface TraitRadius {
    val radius: Float
}

interface MoverKind {
    val radius: Float
}

enum class Movers : MoverKind {
    S {
        override val radius: Float
            get() = 0.1f
    }
}


interface TraitWorld {
    val world: World
}


interface TraitMove : TraitWorld, TraitRadius {
    var pos: Pt

    var moveTime: Double

    var vel: Pt

    fun posAt(time: Double): Pt {
        if (vel.isEmpty)
            return pos

        val dt = (time - moveTime).toFloat()
        return pos + vel * dt
    }

    fun receiveMove(time: Time, vel: Pt) {
        pos = posAt(time.time.sec)
        moveTime = time.time.sec
        this.vel = vel
    }

    fun updateMove(time: Time, freq: Long) {
        if (vel.isEmpty)
            return

        pos = posAt(time.time.sec)
        moveTime = time.time.sec

        val sdf = world.sdf(radius)
        val distance = sdf(pos)
        if (distance < 0.0) {
            val clip = root(sdf, pos)
            pos = clip * 2f - pos
        }
    }
}

class Mover private constructor(
    context: GameContext, restore: Restore?,
    time: () -> Time = undefined(),
    world: () -> World = undefined(),
    pos: () -> Pt = undefined(),
    kind: () -> MoverKind = undefined(),
    owner: () -> Short? = undefined()
) : GameEntity(context, restore), Rendered, Ticking, Comparable<Mover>, TraitMove {
    constructor(context: GameContext, time: Time, world: World, pos: Pt, kind: MoverKind, owner: Short?) :
            this(context, null, { time }, { world }, { pos }, { kind }, { owner })

    companion object {
        val colors = listOf(
            Color.RED,
            Color.GREEN,
            Color.BLUE,
            Color.YELLOW
        )
    }


    val time by prop(initial = time)

    override val world by prop(initial = world)

    override var pos by prop(initial = pos)

    override var moveTime by prop { 0.0 }

    override var vel by prop { Pt() }

    val kind by prop(initial = kind)

    val owner by prop(initial = owner)

    override val radius get() = kind.radius

    var look by prop { XY(0, 0) }

    override fun render(time: Double) {
        val owner = owner ?: return
        val (x, y) = posAt(time)
        val drawable = Resources.solid.refer().tint(colors[owner.toInt().within(0, colors.size)])
        frontend.continuous.submit(
            drawable, time, Mat
                .translation(Constants.tileWidth * x, Constants.tileHeight * y)
                .scale(Constants.tileWidth * kind.radius * 2f, Constants.tileHeight * kind.radius * 2f)
        )
    }

    override val ticker by ticker(1000L, this.time.time + 1)

    fun tick(time: Time, freq: Long) {
        updateMove(time, freq)
    }

    override fun evaluate(name: GameName, time: Time, args: Any?) = when (name) {
        "tick" -> rec(time, args, this::tick)
        "dir" -> rec {
            args as XY
            // Fix resets.
            receiveMove(time, Pt(args.x.toFloat(), args.y.toFloat()))
            if (!args.isEmpty)
                look = args
        }

        else -> super.evaluate(name, time, args)
    }

    override fun compareTo(other: Mover) =
        first(other) {
            id.compareTo(other.id)
        }
}
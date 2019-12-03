package eu.metatools.f2d.ex

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import eu.metatools.f2d.context.refer
import eu.metatools.f2d.math.Mat
import eu.metatools.f2d.math.Pt
import eu.metatools.f2d.math.Cell
import eu.metatools.f2d.tools.AtlasResource
import eu.metatools.f2d.tools.SolidResource
import eu.metatools.f2d.tools.tint
import eu.metatools.up.Ent
import eu.metatools.up.Shell
import eu.metatools.up.dsl.mapObserved
import eu.metatools.up.dsl.prop
import eu.metatools.up.dsl.set
import eu.metatools.up.dt.Lx
import eu.metatools.up.dt.Time
import eu.metatools.up.dt.div
import eu.metatools.up.dt.lx
import eu.metatools.up.list
import eu.metatools.up.lang.within

object Constants {
    /**
     * Width of the tile.
     */
    val tileWidth = 32f

    /**
     * Height of the tile.
     */
    val tileHeight = 32f
}

object Resources {
    val solid by lazy { frontend.use(SolidResource()) }

    val terrain by lazy { frontend.use(AtlasResource { Gdx.files.internal("terrain.atlas") }) }
}

interface Rendered {
    fun render(time: Double)
}

interface Ticking {
    fun update(sec: Double, freq: Long)
}

class World(shell: Shell, id: Lx) : Ent(shell, id), Rendered {
    val clipping = SDFComposer()

    private val sdfs = mutableMapOf<Float, (Pt) -> Float>()

    fun sdf(radius: Float) =
        sdfs.getOrPut(radius) {
            clipping.sdf(radius)
        }

    val worldUpdate = repeating(40, shell::initializedTime) {
        shell.list<Ticking>().forEach {
            it.update((time.global - shell.initializedTime).sec, 40)
        }
    }

    val tiles by mapObserved<Cell, TileKind>(::emptyMap) {
        for ((k, v) in it.removed)
            if (!v.passable)
                clipping.remove(k.x, k.y)
        for ((k, v) in it.added)
            if (!v.passable)
                clipping.add(k.x, k.y)
        sdfs.clear()
    }

    val movers by set<Mover>()

    override fun render(time: Double) {
        for ((k, v) in tiles)
            frontend.continuous.submit(
                v.visual, time, Mat
                    .translation(Constants.tileWidth * k.x, Constants.tileHeight * k.y)
                    .scale(Constants.tileWidth, Constants.tileHeight)
            )
    }

    val createMover = exchange(::onCreateMover)

    private fun onCreateMover(owner: Short) {
        movers.add(constructed(Mover(shell, id / "child" / time, time, Pt(5f, 5f), Movers.S, owner)))
    }

    fun passable(x: Float, y: Float, radius: Float): Boolean {
        check(radius <= 1.0)
        for (a in sequenceOf(-radius, 0f, radius))
            for (b in sequenceOf(-radius, 0f, radius)) {
                val ix = (x + a + 0.5f).toInt()
                val iy = (y + b + 0.5f).toInt()
                if (tiles[Cell(ix, iy)]?.passable == false)
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

    fun receiveMove(sec: Double, vel: Pt) {
        pos = posAt(sec)
        moveTime = sec
        this.vel = vel
    }

    fun updateMove(sec: Double, freq: Long) {
        if (vel.isEmpty)
            return

        pos = posAt(sec)
        moveTime = sec

        val sdf = world.sdf(radius)
        val distance = sdf(pos)
        if (distance < 0.0) {
            val clip = root(sdf, pos)
            pos = clip * 2f - pos
        }
    }
}

class Mover(
    shell: Shell, id: Lx,
    initCreated: Time,
    initPos: Pt,
    initKind: MoverKind,
    initOwner: Short
) : Ent(shell, id), Rendered, Ticking, TraitMove {
    override val extraArgs: Map<String, Any?>?
        get() = mapOf(
            "initCreated" to created,
            "initPos" to pos,
            "initKind" to kind,
            "initOwner" to owner
        )

    companion object {
        val colors = listOf(
            Color.RED,
            Color.GREEN,
            Color.BLUE,
            Color.YELLOW,
            Color.PINK,
            Color.PURPLE,
            Color.CHARTREUSE,
            Color.DARK_GRAY
        )
    }


    val created by prop { initCreated }

    override val world get() = shell.resolve(lx / "root") as World

    override var pos by prop { initPos }

    override var moveTime by prop { 0.0 }

    override var vel by prop { Pt() }

    val kind by prop { initKind }

    val owner by prop { initOwner }

    override val radius get() = kind.radius

    var look by prop { Cell(0, 0) }

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

    override fun update(sec: Double, freq: Long) {
        updateMove(sec, freq)
    }


    val dir = exchange(::doDir)

    private fun doDir(cell: Cell) {
        val sec = (time.global - shell.initializedTime).sec

        // Fix resets.
        receiveMove(sec, Pt(cell.x.toFloat(), cell.y.toFloat()))
        if (!cell.isEmpty)
            look = cell
    }
}
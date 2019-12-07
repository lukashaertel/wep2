package eu.metatools.f2d.ex

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import eu.metatools.f2d.context.limit
import eu.metatools.f2d.context.offset
import eu.metatools.f2d.context.refer
import eu.metatools.f2d.math.Mat
import eu.metatools.f2d.math.Pt
import eu.metatools.f2d.math.Cell
import eu.metatools.f2d.tools.*
import eu.metatools.f2d.up.enqueue
import eu.metatools.up.Ent
import eu.metatools.up.Shell
import eu.metatools.up.dsl.mapObserved
import eu.metatools.up.dsl.provideDelegate
import eu.metatools.up.dsl.ref
import eu.metatools.up.dsl.set
import eu.metatools.up.dt.Lx
import eu.metatools.up.dt.Time
import eu.metatools.up.dt.div
import eu.metatools.up.dt.lx
import eu.metatools.up.lang.never
import eu.metatools.up.lang.within
import eu.metatools.up.list
import java.lang.Math.sqrt
import java.lang.Math.toRadians
import kotlin.math.abs
import kotlin.math.atan2


// The messiest file.

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

    val segoe by lazy { frontend.use(TextResource { findDefinitions(Gdx.files.internal("segoe_ui")) }) }

    val consolas by lazy { frontend.use(TextResource { findDefinitions(Gdx.files.internal("consolas")) }) }

}

interface Rendered {
    fun render(time: Double)
}

interface Ticking {
    fun update(sec: Double, freq: Long)
}

class World(shell: Shell, id: Lx, map: Map<Cell, TileKind>) : Ent(shell, id), Rendered, TakesDamage {
    override val extraArgs = mapOf("map" to map)

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

    val tiles by mapObserved<Cell, TileKind>({ map }) {
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
        val rng = rng()
        val type = if (rng.nextBoolean()) Movers.S else Movers.L
        movers.add(constructed(Mover(shell, id / "child" / time, Pt(5f, 5f), type, owner)))
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

    override fun takeDamage() {
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
    },
    L {
        override val radius: Float
            get() = 0.25f
    },
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

    fun updateMove(sec: Double, freq: Long): List<Ent> {
        if (vel.isEmpty)
            return emptyList()

        pos = posAt(sec)
        moveTime = sec

        val hit = mutableListOf<Ent>()
        val sdf = world.sdf(radius)
        val distance = sdf(pos)
        if (distance < 0.0) {
            val clip = root(sdf, pos)
            pos = clip * 2f - pos
            hit += world
        }
        for (other in world.movers) {
            // Just as an example, not good code.
            if (other === this)
                continue
            val d = other.pos - pos
            val rs = radius + other.radius
            if (d.len < rs) {
                val pen = rs - d.len
                pos -= d.nor * pen
                hit += other
            }
        }

        return hit
    }
}

interface TakesDamage {
    fun takeDamage()
}

class Mover(
    shell: Shell, id: Lx, initPos: Pt, val kind: MoverKind, val owner: Short
) : Ent(shell, id), Rendered, Ticking, TraitMove, TakesDamage {
    override val extraArgs
        get() = mapOf(
            "initPos" to pos,
            "kind" to kind,
            "owner" to owner
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

    override val world by ref<World>(lx / "root")

    override val radius get() = kind.radius

    override var pos by { initPos }

    override var moveTime by { 0.0 }

    override var vel by { Pt() }

    var dead by { false }

    val color get() = colors[owner.toInt().within(0, colors.size)] ?: never

    var look by { Cell(0, 0) }

    var health by { 10 }

    override fun render(time: Double) {
        val (x, y) = posAt(time)
        val drawable = Resources.solid.refer().tint(if (dead) Color.GRAY else color)

        val mat = Mat
            .translation(Constants.tileWidth * x, Constants.tileHeight * y)
            .scale(Constants.tileWidth * kind.radius * 2f, Constants.tileHeight * kind.radius * 2f)

        frontend.continuous.submit(drawable, time, mat)
        frontend.continuous.submit(Cube, this, time, mat)

        if (frontend.consoleVisible) {
            val text = Resources.segoe[ReferText(
                horizontal = Location.Center,
                vertical = Location.Center,
                bold = owner == shell.player
            )]
            val mat2 = Mat
                .translation(Constants.tileWidth * x, Constants.tileHeight * y)
                .scale(sx = frontend.fontSize, sy = frontend.fontSize)
            frontend.continuous.submit(text, "$pos\n$look", time, mat2)
        }
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

    val shoot = exchange(::doShoot)

    private fun doShoot() {
        // TODO: Some problems with class registration, probably registration required or something, fucks up in
        //       the request correlator.
        if (!look.isEmpty) {
            val sec = (time.global - shell.initializedTime).sec
            val dir = Pt(look.x.toFloat(), look.y.toFloat())
            constructed(Bullet(shell, id / "bullet" / time, pos + dir * (radius + 0.1f), dir.nor * 5f, sec))
        }
    }

    var kill = exchange(::doKill)

    private fun doKill() {
        dead = true
    }

    override fun takeDamage() {
        health--
        if (health < 0) {
            world.movers.remove(this)
            delete(this)
        }

        // TODO: Better pattern for this.
        if (!frontendReady)
            return

        val sec = (time.global - shell.initializedTime).sec
        val text = Resources.segoe[ReferText(
            horizontal = Location.Center,
            vertical = Location.Center,
            bold = true, italic = true
        )].tint(Color.RED)

        val (x, y) = pos

        enqueue(frontend.once, text.limit(3.0).offset(sec), "-1") {
            Mat
                .translation(Constants.tileWidth * x, Constants.tileHeight * y + (it - sec).toFloat() * 10)
                .scale(sx = frontend.fontSize, sy = frontend.fontSize)
        }
    }
}

class Bullet(
    shell: Shell, id: Lx, initPos: Pt, initVel: Pt, initMoveTime: Double
) : Ent(shell, id), TraitMove, Ticking, Rendered {
    override val extraArgs
        get() = mapOf(
            "initPos" to pos,
            "initVel" to vel,
            "initMoveTime" to moveTime
        )

    override val world by ref<World>(lx / "root")
    override var pos by { initPos }
    override var moveTime by { initMoveTime }
    override var vel by { initVel }
    override val radius = 0.025f

    override fun render(time: Double) {
        val (x, y) = posAt(time)
        val drawable = Resources.solid.refer()

        val mat = Mat
            .translation(Constants.tileWidth * x, Constants.tileHeight * y)
            .rotateZ(atan2(vel.y, vel.x))
            .scale(Constants.tileWidth * radius * 5f, Constants.tileHeight * radius * 2f)

        frontend.continuous.submit(drawable, time, mat)
    }


    override fun update(sec: Double, freq: Long) {
        val hit = updateMove(sec, freq)
        if (hit.isNotEmpty())
            delete(this)

        hit.forEach {
            if (it is TakesDamage)
                it.takeDamage()
        }
    }

}
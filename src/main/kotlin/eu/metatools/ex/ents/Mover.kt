package eu.metatools.ex.ents

import com.badlogic.gdx.graphics.Color
import eu.metatools.f2d.context.limit
import eu.metatools.f2d.context.offset
import eu.metatools.f2d.context.refer
import eu.metatools.ex.*
import eu.metatools.f2d.math.Cell
import eu.metatools.f2d.math.Mat
import eu.metatools.f2d.math.Pt
import eu.metatools.f2d.tools.Cube
import eu.metatools.f2d.tools.Location
import eu.metatools.f2d.tools.ReferText
import eu.metatools.f2d.tools.tint
import eu.metatools.f2d.up.enqueue
import eu.metatools.up.Ent
import eu.metatools.up.Shell
import eu.metatools.up.dsl.provideDelegate
import eu.metatools.up.dt.Lx
import eu.metatools.up.dt.div
import eu.metatools.up.dt.lx
import eu.metatools.up.lang.never
import eu.metatools.up.lang.within

/**
 * A mover kind.
 */
interface MoverKind {
    /**
     * The radius.
     */
    val radius: Float

    /**
     * The damage it does when shooting.
     */
    val damage: Int
}

/**
 * Some instances of movers.
 */
enum class Movers : MoverKind {
    /**
     * Small mover.
     */
    Small {
        override val radius: Float
            get() = 0.1f
        override val damage: Int
            get() = 2
    },
    /**
     * Large mover.
     */
    Large {
        override val radius: Float
            get() = 0.25f
        override val damage: Int
            get() = 1
    },
}

/**
 * A moving shooting entity, controlled by an [owner].
 * @param shell The shell for the [Ent].
 * @param id The ID for the [Ent].
 * @param initPos The initial position.
 * @property kind Constant. The kind of this mover.
 * @property owner Constant. the owner of this mover.
 */
class Mover(
    shell: Shell, id: Lx, initPos: Pt, val kind: MoverKind, val owner: Short
) : Ent(shell, id), Rendered,
    Ticking, TraitMove,
    TraitDamageable {
    override val extraArgs = mapOf(
        "initPos" to initPos,
        "kind" to kind,
        "owner" to owner
    )

    companion object {
        /**
         * Colors to use when creating a mover.
         */
        private val colors = listOf(
            Color.RED,
            Color.GREEN,
            Color.BLUE,
            Color.YELLOW,
            Color.PINK,
            Color.PURPLE,
            Color.CHARTREUSE,
            Color.DARK_GRAY
        )

        /**
         * The solid drawable.
         */
        private val solid by lazy { Resources.solid.refer() }

        /**
         * The text drawable.
         */
        private val text by lazy {
            Resources.segoe[ReferText(
                horizontal = Location.Center,
                vertical = Location.Center
            )].tint(Color.RED)
        }
        /**
         * The text drawable.
         */
        private val boldText by lazy {
            Resources.segoe[ReferText(
                horizontal = Location.Center,
                vertical = Location.Center,
                bold = true
            )].tint(Color.RED)
        }
    }

    /**
     * Reference to the world.
     */
    override val world get() = shell.resolve(lx / "root") as World

    /**
     * Current position.
     */
    override var pos by { initPos }

    /**
     * Current move time.
     */
    override var moveTime by { 0.0 }

    /**
     * Current velocity.
     */
    override var vel by { Pt() }

    /**
     * Constant. Radius.
     */
    override val radius get() = kind.radius

    /**
     * The color to render this mover with.
     */
    private val color get() = colors[owner.toInt().within(0, colors.size)] ?: never

    /**
     * The last moved direction (the look-direction).
     */
    private var look by { Cell(0, 0) }

    /**
     * The current health.
     */
    private var health by { 10 }

    override fun render(time: Double) {
        // Get position of the mover.
        val (x, y) = posAt(time)

        // Create matrix for transformation.
        val mat = Mat.translation(
            Constants.tileWidth * x,
            Constants.tileHeight * y
        )
            .scale(Constants.tileWidth * kind.radius * 2f, Constants.tileHeight * kind.radius * 2f)

        // Submit the visual and the capture.
        frontend.continuous.submit(solid.tint(color), time, mat)
        frontend.continuous.submit(Cube, this, time, mat)

        // If debugging, render text.
        if (frontend.debug)
            frontend.continuous.submit(
                text, "$pos\n$look", time, Mat.translation(
                    Constants.tileWidth * x,
                    Constants.tileHeight * y
                )
                    .scale(sx = frontend.fontSize, sy = frontend.fontSize)
            )
    }

    override fun update(sec: Double, freq: Long) {
        // Update movement.
        updateMove(sec, freq)
    }

    /**
     * Moves this mover in the direction.
     */
    val moveInDirection = exchange(::doMoveInDirection)

    private fun doMoveInDirection(cell: Cell) {
        // Receive movement on trait.
        receiveMove(elapsed, Pt(cell.x.toFloat(), cell.y.toFloat()))

        // If movement non-empty, update look.
        if (!cell.isEmpty)
            look = cell
    }

    /**
     * Shoots from the mover, optionally in a direction.
     */
    val shoot = exchange(::doShoot)

    private fun doShoot(d: Pt?) {
        // TODO: Some problems with class registration, probably registration required or something, fucks up in
        //       the request correlator. TODO: Re-verify, think thats solved via the pooling of Kryo/Output/Input.

        // Get the direction to shoot.
        val dir = d ?: Pt(look.x.toFloat(), look.y.toFloat())

        // Check if not empty, then construct the bullet.
        if (!dir.isEmpty)
            constructed(Bullet(shell, newId(), pos + dir.nor * (radius + 0.1f), dir.nor * 5f, elapsed, kind.damage))
    }

    override fun takeDamage(amount: Int) {
        // Decrease health for taking a hit.
        health -= amount

        // Get time to start animation and position.
        val start = elapsed
        val (x, y) = pos

        // Render damage floating up.
        enqueue(frontend.once, boldText.limit(3.0).offset(start), amount.toString()) {
            Mat.translation(
                Constants.tileWidth * x,
                Constants.tileHeight * y + (it - start).toFloat() * 10
            )
                .scale(sx = frontend.fontSize, sy = frontend.fontSize)
        }

        // If dead now, remove the mover from the world and delete it.
        if (health < 0) {
            world.movers.remove(this)
            delete(this)
        }
    }
}
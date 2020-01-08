package eu.metatools.ex.ents

import com.badlogic.gdx.graphics.Color
import eu.metatools.ex.Frontend
import eu.metatools.ex.Resources
import eu.metatools.ex.ents.Constants.tileHeight
import eu.metatools.ex.ents.Constants.tileWidth
import eu.metatools.f2d.data.*
import eu.metatools.f2d.drawable.limit
import eu.metatools.f2d.drawable.offset
import eu.metatools.f2d.drawable.tint
import eu.metatools.f2d.immediate.submit
import eu.metatools.f2d.playable.offset
import eu.metatools.f2d.resource.get
import eu.metatools.f2d.tools.CaptureCube
import eu.metatools.f2d.tools.Location
import eu.metatools.f2d.tools.ReferText
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
    val radius: Q

    val shotCost: Int

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
        override val radius = 0.1f.toQ()
        override val shotCost: Int
            get() = 10
        override val damage: Int
            get() = 2
    },
    /**
     * Large mover.
     */
    Large {
        override val radius = 0.25f.toQ()
        override val shotCost: Int
            get() = 8
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
    shell: Shell, id: Lx, val ui: Frontend,
    initPos: QPt, initLevel: Q, val kind: MoverKind, val owner: Short
) : Ent(shell, id), Rendered,
    Walking, Solid, Blocking, HandlesHit, Damageable, HasDescription {
    override val extraArgs = mapOf(
        "initPos" to initPos,
        "initLevel" to initLevel,
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
        private val solid by lazy { Resources.solid.get() }

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
        private val captionText by lazy {
            Resources.segoe[ReferText(
                horizontal = Location.Center,
                vertical = Location.Start
            )]
        }

        /**
         * The text drawable.
         */
        private val hitText by lazy {
            Resources.segoe[ReferText(
                horizontal = Location.Center,
                vertical = Location.Center,
                bold = true
            )].tint(Color.RED)
        }

        private val fire by lazy { Resources.fire.get() }
    }

    /**
     * Reference to the world.
     */
    override val world get() = shell.resolve(lx / "root") as World

    /**
     * Current position.
     */
    override var pos by { initPos }

    override var level by { initLevel }

    /**
     * Current move time.
     */
    override var moveTime by { 0.0 }

    /**
     * Current velocity.
     */
    override var vel by { QPt() }

    /**
     * Constant. Radius.
     */
    override val radius get() = kind.radius

    val shotCost get() = kind.shotCost

    /**
     * The color to render this mover with.
     */
    private val color get() = colors[owner.toInt().within(0, colors.size)] ?: never

    /**
     * The last moved direction (the look-direction).
     */
    private var look by { QPt.ZERO }

    /**
     * The current health.
     */
    private var health by { 10 }

    private var ownResources by { 0 }

    override fun render(mat: Mat, time: Double) {
        // Get position and height.
        val (x, y, z) = xyz(time)

        // Transformation for displaying the mover.
        val local = mat
            .translate(x = tileWidth * x.toFloat(), y = tileHeight * y.toFloat())
            .translate(y = tileHeight * z.toFloat())
            .translate(z = toZ(level))
            .scale(tileWidth * kind.radius.toFloat() * 2f, tileHeight * kind.radius.toFloat() * 2f)

        // Get color.
        val activeColor = if (ui.isSelected(this)) Color.WHITE else color

        // Submit the visual and the capture.
        ui.world.submit(solid.tint(activeColor), time, local)
        ui.world.submit(CaptureCube, this, time, local)

        // Transformation for displaying the label.
        val localLabel = mat
            .translate(x = tileWidth * x.toFloat(), y = tileHeight * y.toFloat())
            .translate(y = tileHeight * z.toFloat())
            .translate(z = toZ(level))
            .translate(0f, -8f)
            .scale(12f)

        // Submit the label.
        ui.world.submit(captionText, "H: $health R: $ownResources", time, localLabel)
    }

    /**
     * Moves this mover in the direction.
     */
    val moveInDirection = exchange(::doMoveInDirection)

    private fun doMoveInDirection(direction: QPt) {
        // Receive movement on trait.
        move(elapsed, direction)

        // If movement non-empty, update look.
        if (direction.isNotEmpty())
            look = direction
    }

    /**
     * Shoots from the mover, optionally in a direction.
     */
    val shoot = exchange(::doShoot)

    private fun doShoot(d: QPt?) {
        // Get the direction to shoot.
        val dir = d ?: look

        // Check if not empty, then construct the bullet.
        if (dir.isEmpty())
            return

        if (world.res + ownResources < 10)
            return

        world.res -= shotCost
        if (world.res < 0) {
            ownResources += world.res
            world.res = 0
        }

        val level = world.map.height(level.toInt(), pos.x, pos.y).toQ()
        constructed(
            Bullet(
                shell, newId(), ui,
                pos + dir.nor * (radius + 0.1f.toQ()), dir.nor * 5f.toQ(), elapsed, level, kind.damage
            )
        )

        enqueue(ui.world, fire.offset(elapsed), null) { Mat.ID }
    }

    override fun takeDamage(amount: Int) {
        // Decrease health for taking a hit.
        health -= amount

        // Get time to start animation and position.
        val start = elapsed
        val (x, y) = pos
        val level = level

        // Render damage floating up.
        enqueue(ui.world, hitText.limit(3.0).offset(start), amount.toString()) {
            Mat.translation(
                Constants.tileWidth * x.toFloat(),
                Constants.tileHeight * y.toFloat() + (it - start).toFloat() * 10,
                -level.toFloat()
            ).scale(16f)
        }

        // If dead now, remove the mover from the world and delete it.
        if (health <= 0) {
            world.movers.remove(this)
            delete(this)
        }
    }

    override fun hitOther(other: Moves) {
        if (other is Respack) {
            ownResources += other.content
            delete(other)
        }
    }


    override val describe: String
        get() = if (shell.player == owner) "You ($level)" else "Enemy"

}
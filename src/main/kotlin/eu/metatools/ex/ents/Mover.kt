package eu.metatools.ex.ents

import com.badlogic.gdx.graphics.Color
import eu.metatools.ex.Frontend
import eu.metatools.ex.Resources
import eu.metatools.f2d.data.Mat
import eu.metatools.f2d.data.Real
import eu.metatools.f2d.data.RealPt
import eu.metatools.f2d.data.toReal
import eu.metatools.f2d.drawable.limit
import eu.metatools.f2d.drawable.offset
import eu.metatools.f2d.drawable.tint
import eu.metatools.f2d.immediate.submit
import eu.metatools.f2d.playable.offset
import eu.metatools.f2d.resource.refer
import eu.metatools.f2d.tools.Cube
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
    val radius: Real

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
        override val radius = 0.1f.toReal()
        override val shotCost: Int
            get() = 10
        override val damage: Int
            get() = 2
    },
    /**
     * Large mover.
     */
    Large {
        override val radius = 0.25f.toReal()
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
    initPos: RealPt, initLevel: Int, val kind: MoverKind, val owner: Short
) : Ent(shell, id), Rendered,
    Ticking, TraitMove,
    TraitDamageable, TraitCollects, HasDescription {
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

        private val fire by lazy { Resources.fire.refer() }
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
    override var vel by { RealPt() }

    /**
     * Constant. Radius.
     */
    override val radius get() = kind.radius

    override val blocking get() = true

    val shotCost get() = kind.shotCost

    /**
     * The color to render this mover with.
     */
    private val color get() = colors[owner.toInt().within(0, colors.size)] ?: never

    /**
     * The last moved direction (the look-direction).
     */
    private var look by { RealPt.ZERO }

    /**
     * The current health.
     */
    private var health by { 10 }

    private var ownResources by { 0 }

    override val clips = true

    override fun render(mat: Mat, time: Double) {
        // Get position of the mover.
        val (x, y) = posAt(time)

        // Create matrix for transformation.
        val mat2 = Mat.translation(
            Constants.tileWidth * x.toFloat(),
            Constants.tileHeight * y.toFloat(),
            -level.toFloat()
        ).scale(Constants.tileWidth * kind.radius.toFloat() * 2f, Constants.tileHeight * kind.radius.toFloat() * 2f)

        // Get color.
        val activeColor = if (ui.isSelected(this)) Color.WHITE else color

        // Submit the visual and the capture.
        ui.world.submit(solid.tint(activeColor), time, mat * mat2)
        ui.world.submit(Cube, this, time, mat * mat2)

        val mat3 = Mat.translation(
            Constants.tileWidth * x.toFloat(),
            Constants.tileHeight * y.toFloat(),
            -level.toFloat()
        ).translate(0f, -8f).scale(12f)
        ui.world.submit(captionText, "H: $health R: $ownResources", time, mat * mat3)
    }

    override fun update(sec: Double, freq: Long) {
        // Update movement.
        updateMove(sec, freq)
    }

    /**
     * Moves this mover in the direction.
     */
    val moveInDirection = exchange(::doMoveInDirection)

    private fun doMoveInDirection(direction: RealPt) {
        // Receive movement on trait.
        receiveMove(elapsed, direction)

        // If movement non-empty, update look.
        if (!direction.isEmpty)
            look = direction
    }

    /**
     * Shoots from the mover, optionally in a direction.
     */
    val shoot = exchange(::doShoot)

    private fun doShoot(d: RealPt?) {
        // Get the direction to shoot.
        val dir = d ?: look

        // Check if not empty, then construct the bullet.
        if (dir.isEmpty)
            return

        if (world.res + ownResources < 10)
            return

        world.res -= shotCost
        if (world.res < 0) {
            ownResources += world.res
            world.res = 0
        }

        constructed(
            Bullet(
                shell, newId(), ui,
                pos + dir.nor * (radius + 0.1f.toReal()), dir.nor * 5f.toReal(), elapsed, level, kind.damage
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

    override fun collectResource(amount: Int) {
        ownResources += amount
        enqueue(ui.world, fire.offset(elapsed), null) { Mat.ID }
    }

    override val describe: String
        get() = if (shell.player == owner) "You ($level)" else "Enemy"

}
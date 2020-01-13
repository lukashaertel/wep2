package eu.metatools.ex.ents

import com.badlogic.gdx.graphics.Color
import eu.metatools.ex.Frontend
import eu.metatools.ex.Resources
import eu.metatools.ex.data.Dir
import eu.metatools.ex.ents.Constants.tileHeight
import eu.metatools.ex.ents.Constants.tileWidth
import eu.metatools.f2d.data.*
import eu.metatools.f2d.drawable.Drawable
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
import kotlin.math.log10
import kotlin.math.pow

interface SpriteSet {
    fun idle(dir: Dir): Drawable<Unit?>
    fun move(dir: Dir): Drawable<Unit?>
    fun draw(dir: Dir): Drawable<Unit?>
}

enum class SpriteSets : SpriteSet {
    Pa {
        private val idleUp by atlas("pa_i_u")
        private val idleRight: Drawable<Unit?> by atlas("pa_i_r")
        private val idleDown: Drawable<Unit?> by atlas("pa_i_d")
        private val idleLeft: Drawable<Unit?> by atlas("pa_i_l")
        override fun idle(dir: Dir) =
            dir.select(idleUp, idleRight, idleDown, idleLeft)

        private val moveUp: Drawable<Unit?> by animation(0.8, "pa_w1_u", "pa_i_u", "pa_w2_u", "pa_i_u")
        private val moveRight: Drawable<Unit?> by animation(0.8, "pa_w1_r", "pa_i_r", "pa_w2_r", "pa_i_r")
        private val moveDown: Drawable<Unit?> by animation(0.8, "pa_w1_d", "pa_i_d", "pa_w2_d", "pa_i_d")
        private val moveLeft: Drawable<Unit?> by animation(0.8, "pa_w1_l", "pa_i_l", "pa_w2_l", "pa_i_l")
        override fun move(dir: Dir) =
            dir.select(moveUp, moveRight, moveDown, moveLeft)

        private val drawUp: Drawable<Unit?> by atlas("pa_d_u")
        private val drawRight: Drawable<Unit?> by atlas("pa_d_r")
        private val drawDown: Drawable<Unit?> by atlas("pa_d_d")
        private val drawLeft: Drawable<Unit?> by atlas("pa_d_l")

        override fun draw(dir: Dir) =
            dir.select(drawUp, drawRight, drawDown, drawLeft)
    }
}

/**
 * A mover kind.
 */
interface MoverKind {
    /**
     * The radius.
     */
    val radius: Q
    val initialHealth: Int

    /**
     * The damage it does when shooting.
     */
    fun damageForLevel(level: Int): Int

    val speed: Q
    val spriteSet: SpriteSet
    val hitXP: Int
    val deathXP: Int
    val label: String
}

object XP {
    private fun rangeFor(value: Int): IntRange {
        val level = levelFor(value)
        val a = 10.0.pow(level).toInt()
        val b = 10.0.pow(level.inc()).toInt()
        return a until b
    }

    fun fractionFor(value: Int): Number {
        val range = rangeFor(value)
        return (value.toDouble() - range.first) / (range.last - range.first)

    }

    fun levelFor(value: Int) =
        if (value <= 0) 0 else log10(value.toDouble()).toInt()
}

/**
 * Some instances of movers.
 */
enum class Movers : MoverKind {
    Pazu {
        override val radius = 0.3f.toQ()
        override val initialHealth = 10
        override fun damageForLevel(level: Int) = minOf(5, 2 + level)
        override val speed = 2.toQ()
        override val spriteSet = SpriteSets.Pa
        override val hitXP = 5
        override val deathXP = 50
        override val label = "Pazu"
    }
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
        val offset = 0.3.toQ()

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

    var lastVel by { QPt() }

    var drawn by { false }

    var xp by { 0 }

    val skillLevel get() = XP.levelFor(xp)

    val damage get() = kind.damageForLevel(skillLevel)

    /**
     * Constant. Radius.
     */
    override val radius get() = kind.radius

    private var look by { Dir.Right }

    /**
     * The current health.
     */
    var health by { kind.initialHealth }

    private var ownResources by { 0 }

    override fun render(mat: Mat, time: Double) {
        // Get position and height.
        val (x, y, z) = xyz(time)

        // Transformation for displaying the mover.
        val local = mat
            .translate(x = tileWidth * x.toFloat(), y = tileHeight * y.toFloat())
            .translate(y = tileHeight * z.toFloat())
            .translate(y = tileHeight * offset.toFloat())
            .translate(z = toZ(z))
            .scale(tileWidth, tileHeight)


        val visual = when {
            drawn -> kind.spriteSet.draw(look)
            vel.isNotEmpty() -> kind.spriteSet.move(look)
            else -> kind.spriteSet.idle(look)
        }

        // Submit the visual and the capture.
        ui.world.submit(visual, time, local)
        ui.world.submit(CaptureCube, this, time, local)
    }

    /**
     * Moves this mover in the direction.
     */
    val moveInDirection = exchange(::doMoveInDirection)

    private fun doMoveInDirection(direction: QPt) {
        lastVel = direction.times(kind.speed)
        if (!drawn)
            move(elapsed, lastVel)
    }

    val lookAt = exchange(::doLookAt)

    private fun doLookAt(direction: Dir) {
        look = direction
    }

    val draw = exchange(::doDraw)

    private fun doDraw() {
        drawn = true
        move(elapsed, QPt.ZERO)
    }

    /**
     * Shoots from the mover, optionally in a direction.
     */
    val release = exchange(::doRelease)

    private fun doRelease(dir: QPt) {
        if (!drawn)
            return

        // Check if not empty, then construct the bullet.
        if (dir.isEmpty())
            return

        val level = world.map.height(level.toInt(), pos.x, pos.y).toQ()
        constructed(
            Bullet(
                shell, newId(), ui, this,
                pos + dir.nor * (radius + 0.1f.toQ()), dir.nor * 10f.toQ(), elapsed, level, damage
            )
        )

        enqueue(ui.world, fire.offset(elapsed), null) { Mat.ID }

        drawn = false
        move(elapsed, lastVel)
    }

    override fun takeDamage(amount: Int): Int {
        // Decrease health for taking a hit.
        health -= amount

        // Get time to start animation and position.
        val start = elapsed
        val (x, y) = pos
        val level = level

        // Render damage floating up.
        enqueue(ui.world, hitText.limit(3.0).offset(start), amount.toString()) {
            Mat.translation(
                tileWidth * x.toFloat(),
                tileHeight * y.toFloat() + (it - start).toFloat() * 10,
                -level.toFloat()
            ).scale(16f)
        }

        // If dead now, remove the mover from the world and delete it.
        if (health <= 0) {
            world.movers.remove(this)
            delete(this)
            return kind.deathXP
        }

        return kind.hitXP
    }

    override fun hitOther(other: Moves) {
        if (other is Respack) {
            ownResources += other.content
            delete(other)
        }
    }


    override val describe: String
        get() = "Level ${XP.levelFor(xp)} ${kind.label}" + if (shell.player == owner) " (You)" else ""

}
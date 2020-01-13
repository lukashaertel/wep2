package eu.metatools.ex.ents

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.MathUtils
import eu.metatools.ex.*
import eu.metatools.ex.data.Dir
import eu.metatools.ex.ents.Constants.tileHeight
import eu.metatools.ex.ents.Constants.tileWidth
import eu.metatools.f2d.InOut
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
    val maxAmmo: Int

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
    fun rangeFor(value: Int): IntRange {
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
        override val radius = Q.THIRD
        override val initialHealth = 10
        override val maxAmmo = 20
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
        val offset = Q.THIRD

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
        private val hitText by lazy {
            Resources.segoe[ReferText(
                horizontal = Location.Center,
                vertical = Location.Center,
                bold = true
            )].tint(Color.RED)
        }

        private val levelUp by lazy {
            Resources.segoe[ReferText(
                horizontal = Location.Center,
                vertical = Location.Center,
                italic = true
            )].tint(Color.YELLOW)
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

    var xp by { 1 }

    val xpRange get() = XP.rangeFor(xp)

    val xpLevel get() = XP.levelFor(xp)


    val damage get() = kind.damageForLevel(xpLevel)

    /**
     * Constant. Radius.
     */
    override val radius get() = kind.radius

    private var look by { Dir.Right }

    /**
     * The current health.
     */
    var health by { kind.initialHealth }

    var ammo by { 10 }

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
        if (dir.isNotEmpty() && ammo > 0) {
            ammo--

            val level = world.map.height(level.toInt(), pos.x, pos.y).toQ()
            constructed(
                Bullet(
                    shell, newId(), ui, this,
                    pos + dir.nor * (radius + 0.1f.toQ()), dir.nor * 10f.toQ(), elapsed, level, damage
                )
            )

            enqueue(ui.world, fire.offset(elapsed), null) { Mat.ID }

        }

        drawn = false
        move(elapsed, lastVel)
    }

    val cancelDraw = exchange(::doCancelDraw)

    private fun doCancelDraw() {
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

    fun takeXP(amount: Int) {
        val before = xpLevel
        xp += amount
        if (xpLevel == before)
            return

        // Get time to start animation and position.
        val start = elapsed
        val (x, y) = pos
        val level = level

        // TODO: XYZ computation here can probably be fixed.
        // Render level up floating up.
        enqueue(ui.world, levelUp.limit(5.0).offset(start), "Level up!") {
            Mat.translation(
                tileWidth * x.toFloat(),
                tileHeight * y.toFloat() + (it - start).toFloat() * 10,
                -level.toFloat()
            ).scale(16f)
        }
    }

    override fun hitOther(other: Moves) {
        if (other is Respack) {
            ammo = minOf(kind.maxAmmo, ammo + other.content)
            delete(other)
        }
    }


    override val describe: String
        get() = "Level ${XP.levelFor(xp)} ${kind.label}" + if (shell.player == owner) " (You)" else ""

}

private val ammoCount by lazy {
    Resources.segoe[ReferText(
        horizontal = Location.End,
        vertical = Location.Center
    )]
}
private const val ammoInsetX = 100f
private const val ammoInsetY = 48f
private const val ammoLength = 600f
private const val ammoDisplaySize = 64f
private const val ammoCountInset = 16f
private const val ammoCountSize = 32f

fun InOut.submitAmmo(mover: Mover, time: Double) {
    if (mover.kind.maxAmmo <= 0) return
    val dx = ammoLength / (mover.kind.maxAmmo - 1)
    val y = Gdx.graphics.height - ammoInsetY
    var cx = ammoInsetX
    for (i in 1..mover.ammo) {
        submit(
            Bullet.arrow, time, Mat
                .translation(cx, y, uiZ)
                .rotateZ(MathUtils.PI / 2f)
                .scale(ammoDisplaySize)
        )
        cx += dx
    }
    shadowText(ammoCount, "${mover.ammo}/${mover.kind.maxAmmo}", time, ammoInsetX - ammoCountInset, y, ammoCountSize)
}

private const val xpBarHeight = 10f
private const val xpBarInset = 2f
private const val xpBarLevelSize = 32f
private const val xpBarRangeSize = 24f
private const val xpBarValueSize = 16f


private val xpRangeStart by lazy {
    Resources.segoe[ReferText(
        horizontal = Location.Start,
        vertical = Location.End
    )]
}
private val xpLevelValue by lazy {
    Resources.segoe[ReferText(
        horizontal = Location.End,
        vertical = Location.End,
        bold = true
    )]
}
private val xpValue by lazy {
    Resources.segoe[ReferText(
        horizontal = Location.Start,
        vertical = Location.End,
        bold = true
    )]
}
private val xpRangeEnd by lazy {
    Resources.segoe[ReferText(
        horizontal = Location.End,
        vertical = Location.End
    )]
}

fun InOut.submitXP(mover: Mover, time: Double) {

    val xpFraction = XP.fractionFor(mover.xp).toFloat()

    // XP bar.
    submit(
        solidDrawable.tint(Color.BLACK), time, Mat
            .translation(0f, 0f, uiZ)
            .scale(Gdx.graphics.width.toFloat(), xpBarHeight)
            .translate(0.5f, 0.5f)
    )

    submit(
        solidDrawable.tint(Color.YELLOW), time, Mat
            .translation(0f, 0f, uiZ)
            .scale(xpFraction * Gdx.graphics.width, xpBarHeight)
            .translate(0.5f, 0.5f)
    )

    // XP labels.
    val xpRange = mover.xpRange
    val xpLevelString = mover.xpLevel.toString()
    val xpRangeStartString = xpRange.first.toString()
    val xpRangeEndString = xpRange.last.inc().toString()
    val xpValueString = "(${mover.xp})"

    shadowText(
        xpRangeStart, xpRangeStartString, time,
        xpBarInset, xpBarHeight + xpBarInset,
        xpBarRangeSize
    )
    shadowText(
        xpLevelValue, xpLevelString, time,
        Gdx.graphics.width / 2f - xpBarInset,
        xpBarHeight + xpBarInset,
        xpBarLevelSize
    )
    shadowText(
        xpValue, xpValueString, time,
        Gdx.graphics.width / 2f + xpBarInset, xpBarHeight + xpBarInset,
        xpBarValueSize
    )
    shadowText(
        xpRangeEnd, xpRangeEndString, time,
        Gdx.graphics.width.toFloat() - xpBarInset,
        xpBarInset,
        xpBarRangeSize
    )
}
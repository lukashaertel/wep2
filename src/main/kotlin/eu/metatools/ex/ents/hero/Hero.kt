package eu.metatools.ex.ents.hero

import com.badlogic.gdx.graphics.Color
import eu.metatools.ex.Frontend
import eu.metatools.ex.Resources
import eu.metatools.ex.data.Dir
import eu.metatools.ex.ents.*
import eu.metatools.ex.ents.Constants.tileHeight
import eu.metatools.ex.ents.Constants.tileWidth
import eu.metatools.ex.subUiZ
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

/**
 * A moving shooting entity, controlled by an [owner].
 * @param shell The shell for the [Ent].
 * @param id The ID for the [Ent].
 * @param initPos The initial position.
 * @property kind Constant. The kind of this mover.
 * @property owner Constant. the owner of this mover.
 */
class Hero(
    shell: Shell, id: Lx, val ui: Frontend,
    initPos: QPt, initHeight: Q, val kind: HeroKind, val owner: Short
) : Ent(shell, id), Rendered,
    Walking, Solid,
    Blocking, HandlesHit,
    Damageable, Described {
    override val extraArgs = mapOf(
        "initPos" to initPos,
        "initHeight" to initHeight,
        "kind" to kind,
        "owner" to owner
    )

    companion object {
        val offset = Q.THIRD

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

    override val world get() = shell.resolve(lx / "root") as World

    override val radius get() = kind.radius

    override var pos by { initPos }

    override var moveTime by { 0.0 }

    override var moveVel by { QPt() }

    override var height by { initHeight }

    /**
     * Direction the hero is facing, defaults to [Dir.Right].
     */
    var look by { Dir.Right }
        private set

    /**
     * Last velocity before [draw].
     */
    private var lastVel by { QPt() }

    /**
     * The level of the hero.
     */
    val level get() = XP.levelFor(xp)

    /**
     * The stats of the hero at their level.
     */
    val stats: Stats get() = kind.stats(level)

    /**
     * The XP value.
     */
    var xp by { 1 }

    /**
     * The current health.
     */
    var health by { kind.stats(1).health }
        private set

    /**
     * The amount of ammo.
     */
    var ammo by { kind.stats(1).ammo }
        private set

    /**
     * Time at which bow was drawn.
     */
    var drawn by { null as Double? }
        private set

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
            drawn != null -> kind.spriteSet.draw(look)
            moveVel.isNotEmpty() -> kind.spriteSet.move(look)
            else -> kind.spriteSet.idle(look)
        }

        // Submit the visual and the capture.
        ui.world.submit(visual, time, local)
        ui.world.submit(CaptureCube, this, time, local)
    }

    /**
     * Sets the desired movement. If [drawn], movement will be applied after [release].
     */
    val move = exchange(::doMoveInDirection)

    private fun doMoveInDirection(direction: QPt) {
        // Set the desired velocity.
        lastVel = direction.times(stats.speed)

        // Don't move if drawn.
        if (drawn == null)
            takeMovement(elapsed, lastVel)
    }

    /**
     * Sets the [look] direction.
     */
    val lookAt = exchange(::doLookAt)

    private fun doLookAt(direction: Dir) {
        // Transfers the value.
        look = direction
    }

    /**
     * Begins the shooting process.
     */
    val draw = exchange(::doDraw)

    private fun doDraw() {
        // Set drawn time.
        drawn = elapsed

        // Clear movement.
        takeMovement(elapsed, QPt.ZERO)
    }

    /**
     * Releases the shot.
     */
    val release = exchange(::doRelease)

    /**
     * Factor of drawing the bow between zero for not ready to one for ready. `null` if not drawing.
     */
    fun bowDrawFactor(time: Number) =
        drawn?.let {
            minOf(Q.ONE, ((time.toQ() - it) / stats.bowInit))
        }

    /**
     * Applied damage factor after bow is drawn. `null` if not drawing.
     */
    fun bowDamageFactor(time: Number) =
        drawn?.let {
            maxOf(stats.bowMin, Q.ONE - maxOf(Q.ZERO, ((time.toQ() - it) - stats.bowInit) * stats.bowDegrade))
        }

    private fun doRelease(dir: QPt) {
        // Not drawing return.
        if (drawn == null)
            return

        // Get draw factor.
        val drawFactor = bowDrawFactor(elapsed)

        // Check if direction is given, ammo is present and bow has been drawn enough.
        if (dir.isNotEmpty() && ammo > 0 && drawFactor == Q.ONE) {
            // Reduce the ammo.
            ammo--

            // Get factor and compute damage.
            val factor = bowDamageFactor(elapsed) ?: never
            val damage = factor * stats.baseDamage

            // Compute the actual height at which to fire.
            val level = world.map.height(height.toInt(), pos.x, pos.y).toQ()

            // Construct the projectile.
            val projectile = constructed(
                Projectile(
                    shell, newId(), ui,
                    pos + dir.nor * (radius + 0.1f.toQ()), level, dir.nor * 10f.toQ(), elapsed, damage
                )
            )

            // Assign back reference.
            projectile.owner = this

            // Play firing sound.
            enqueue(ui.world, fire.offset(elapsed), null) { Mat.ID }
        }

        // Always reset drawing time.
        drawn = null

        // Re-allow movement.
        takeMovement(elapsed, lastVel)
    }

    /**
     * Cancels the current drawing process.
     */
    val cancelDraw = exchange(::doCancelDraw)

    private fun doCancelDraw() {
        // Reset drawing time.
        drawn = null

        // Re-allow movement.
        takeMovement(elapsed, lastVel)
    }

    override fun takeDamage(amount: Q): Int {
        // Decrease health for taking a hit.
        health -= amount

        // Get time to start animation and position.
        val start = elapsed
        val (x, y) = pos

        // Render damage floating up.
        enqueue(ui.world, hitText.limit(3.0).offset(start), amount.toString()) {
            Mat.translation(
                tileWidth * x.toFloat(),
                tileHeight * y.toFloat() + (it - start).toFloat() * 10,
                subUiZ
            ).scale(16f, 16f)
        }

        // Check if dead now, return XP for killing if true.
        if (health <= Q.ZERO)
            return stats.deathXP.also {
                // Also, remove hero and delete this.
                world.heroes.remove(this)
                delete(this)
            }

        // Otherwise, return XP for hitting this hero.
        return stats.hitXP
    }

    /**
     * Receives XP.
     */
    fun takeXP(amount: Int) {
        // Get level before and update XP.
        val before = level
        xp += amount

        // If level hasn't changed, return.
        if (level == before)
            return

        // Update health for level change.
        health = health * kind.stats(level).health / kind.stats(before).health

        // Get time to start animation and position.
        val start = elapsed
        val (x, y) = pos
        val level = height

        // TODO: XYZ computation here can probably be fixed.

        // Render level up floating up.
        enqueue(ui.world, levelUp.limit(5.0).offset(start), "Level up!") {
            Mat.translation(
                tileWidth * x.toFloat(),
                tileHeight * y.toFloat() + (it - start).toFloat() * 10,
                subUiZ
            ).scale(16f, 16f)
        }
    }

    override fun hitOther(other: Moves) {
        // If other entity is an ammo container, receive the ammo.
        if (other is AmmoContainer) {
            // TODO: Item logic somewhere else

            // Update ammo.
            ammo = minOf(stats.ammo, ammo + other.content)

            // Remove the other entity.
            delete(other)
        }
    }

    override fun describe(): String =
        "Level ${XP.levelFor(xp)} ${kind.label}" + if (shell.player == owner) " (You)" else ""

}
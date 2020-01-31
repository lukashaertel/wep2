package eu.metatools.ex.ents.hero

import com.badlogic.gdx.graphics.Color
import eu.metatools.ex.Frontend
import eu.metatools.ex.Resources
import eu.metatools.ex.atlas
import eu.metatools.ex.data.Dir
import eu.metatools.ex.ents.*
import eu.metatools.ex.ents.Constants.tileHeight
import eu.metatools.ex.ents.Constants.tileWidth
import eu.metatools.ex.ents.items.Container
import eu.metatools.ex.subUiZ
import eu.metatools.f2d.data.Mat
import eu.metatools.f2d.data.Pt
import eu.metatools.f2d.data.Vec
import eu.metatools.f2d.data.isNotEmpty
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
import kotlin.math.sqrt

/**
 * A moving shooting entity, controlled by an [owner].
 * @param shell The shell for the [Ent].
 * @param id The ID for the [Ent].
 * @param initXY The initial position.
 * @property kind Constant. The kind of this mover.
 * @property owner Constant. the owner of this mover.
 */
class Hero(
    shell: Shell, id: Lx, val ui: Frontend,
    initPos: Vec, val kind: HeroKind, val owner: Short
) : Ent(shell, id), Rendered, Moves, Solid, Blocking, HandlesHit, Damageable, Described {
    override val extraArgs = mapOf(
        "initPos" to initPos,
        "kind" to kind,
        "owner" to owner
    )

    companion object {
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

        private val shadow by atlas("shadow")
    }

    override val world get() = shell.resolve(lx / "root") as World

    override val radius get() = kind.radius

    override var pos by { initPos }

    override var t0 by { 0.0 }

    override var vel by { Vec.Zero }

    override var height by { 0f }

    /**
     * Direction the hero is facing, defaults to [Dir.Right].
     */
    var look by { Dir.Right }
        private set

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
    var health by { kind.stats(0).health }
        private set

    /**
     * The amount of ammo.
     */
    var ammo by { kind.stats(0).ammo }
        private set

    /**
     * Time at which bow was drawn.
     */
    var drawn by { null as Double? }
        private set

    override fun render(mat: Mat, time: Double) {
        // Get position and height.
        val pos = posAt(time)
        world.findGround(pos)?.let { (x, y, z) ->

            val localShadow = mat
                .translate(x = tileWidth * x, y = tileHeight * y)
                .translate(y = tileHeight * z)
                .translate(z = toZ(y, z))
                .scale(tileWidth, tileHeight)


            ui.world.submit(shadow, time, localShadow)
        }

        val (x, y, z) = pos


        // Transformation for displaying the mover and the shadow.
        val localSprite = mat
            .translate(x = tileWidth * x, y = tileHeight * y)
            .translate(y = tileHeight * z)
            .translate(z = toZ(y, z))
            .scale(tileWidth, tileHeight)


        val visual = when {
            !isGrounded() -> kind.spriteSet.air(look)
            drawn != null -> kind.spriteSet.draw(look)
            vel.lenSq > 0.1f * 0.1f -> kind.spriteSet.move(look)
            else -> kind.spriteSet.idle(look)
        }

        // Submit the visual and the capture.
        ui.world.submit(visual, time, localSprite)
        ui.world.submit(CaptureCube, this, time, localSprite)
    }

    val jump = exchange(::doJump)
    private fun doJump() {
        // Apply velocity so that the jump height is reached at the peak.
        vel += Vec.Z * sqrt(stats.jumpHeight * g / 2f)
    }

    val move = exchange(::doMove)

    private fun doMove(dir: Pt) {
        val speed = stats.speed
        takeMovement(elapsed, Vec(dir.x * speed, dir.y * speed, vel.z))
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
    }

    /**
     * Releases the shot.
     */
    val release = exchange(::doRelease)

    fun targetOf(any: Any?, time: Double) =
        when (any) {
            is Moves ->
                any.posAt(time)
            is BlockCapture -> {
                val x = any.tri.x.toFloat()
                val y = any.tri.y.toFloat()
                val z = any.tri.z.toFloat()// TODO Include height.
                Vec(x, y, z)
            }
            else -> null
        }

    /**
     * Factor of drawing the bow between zero for not ready to one for ready. `null` if not drawing.
     */
    fun bowDrawFactor(time: Double) =
        drawn?.let {
            minOf(1f, ((time - it).toFloat() / stats.bowInit))
        }

    /**
     * Applied damage factor after bow is drawn. `null` if not drawing.
     */
    fun bowDamageFactor(time: Double) =
        drawn?.let {
            maxOf(stats.bowMin, 1f - maxOf(0f, ((time - it).toFloat() - stats.bowInit) * stats.bowDegrade))
        }

    private fun doRelease(target: Vec) {
        // Not drawing return.
        if (drawn == null)
            return

        // Get position and direction.
        val pos = posAt(elapsed)
        val dir = target - pos

        // Get draw factor.
        val drawFactor = bowDrawFactor(elapsed)

        // Check if direction is given, ammo is present and bow has been drawn enough.
        if (dir.isNotEmpty() && ammo > 0 && drawFactor == 1f) {
            // Reduce the ammo.
            ammo--

            // Compute velocity from stats and action.
            val vel = dir.nor * stats.projectileSpeed

            // Get factor and compute damage.
            val factor = bowDamageFactor(elapsed) ?: never
            val damage = factor * stats.baseDamage

            // Construct the projectile.
            val projectile = constructed(
                Projectile(
                    shell, newId(), ui,
                    pos, vel,
                    elapsed, damage
                )
            )

            // Assign back reference.
            projectile.owner = this

            // Play firing sound.
            enqueue(ui.world, fire.offset(elapsed), null) { Mat.ID }
        }

        // Always reset drawing time.
        drawn = null
    }

    /**
     * Cancels the current drawing process.
     */
    val cancelDraw = exchange(::doCancelDraw)

    private fun doCancelDraw() {
        // Reset drawing time.
        drawn = null
    }

    override fun takeDamage(amount: Float): Int {
        // Decrease health for taking a hit.
        health -= amount

        // Get time to start animation and position.
        val start = elapsed
        val (x, y, z) = posAt(elapsed)

        // Render damage floating up.
        enqueue(ui.world, hitText.limit(3.0).offset(start), amount.toString()) {
            Mat.translation(
                tileWidth * x,
                tileHeight * (y + z) + (it - start).toFloat() * 10,
                subUiZ
            ).scale(16f, 16f)
        }

        // Check if dead now, return XP for killing if true.
        if (health <= 0f)
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
        val (x, y, z) = posAt(elapsed)

        // TODO: XYZ computation here can probably be fixed.

        // Render level up floating up.
        enqueue(ui.world, levelUp.limit(5.0).offset(start), "Level up!") {
            Mat.translation(
                tileWidth * x,
                tileHeight * (y + z) + (it - start).toFloat() * 10,
                subUiZ
            ).scale(16f, 16f)
        }
    }

    fun takeAmmo(amount: Int) {
        ammo = minOf(stats.ammo, ammo + amount)
    }

    fun takeHealth(amount: Float) {
        health = minOf(stats.health, health + amount)
    }

    override fun hitHull(velPrime: Vec) {
        if (velPrime.lenSq > 5f * 5f)
            takeDamage(velPrime.len - 4f)
    }

    override fun hitOther(other: Moves) {
        // If other entity is an ammo container, receive the ammo.
        if (other is Container) {
            // Apply effect of item.
            other.apply(this)

            // Remove the other entity.
            delete(other)
        }
    }

    override fun describe(): String =
        "Level ${XP.levelFor(xp)} ${kind.label}" + if (shell.player == owner) " (You)" else ""

}
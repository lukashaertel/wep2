package eu.metatools.f2d

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.InputEventQueue
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import eu.metatools.f2d.context.Drawable
import eu.metatools.f2d.context.offset
import eu.metatools.f2d.context.refer
import eu.metatools.f2d.tools.*
import eu.metatools.f2d.wep2.recPlay
import eu.metatools.wep2.entity.bind.Restore
import eu.metatools.wep2.system.*
import eu.metatools.wep2.tools.Time
import eu.metatools.wep2.tools.rec
import eu.metatools.wep2.track.bind.map
import eu.metatools.wep2.track.bind.prop
import org.lwjgl.opengl.GL11
import kotlin.math.cos
import kotlin.math.sin

// Shortened type declarations as aliases.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

typealias GameName = String
typealias GameParam = Unit
typealias GameSystem = StandardSystem<GameName, GameParam>
typealias GameInitializer = StandardInitializer<GameName, GameParam>
typealias GameEntity = StandardEntity<GameName>

// Child entity.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

class Field(val system: GameSystem, restore: Restore?) : GameEntity(system, restore) {
    var x by prop(restore) { 0 }

    var y by prop(restore) { 0 }

    var color by prop(restore) { Color.WHITE }

    private fun paintNext(time: Time) = with(Frontend) {
        // Get random offset.
        val xo = system.randomInt(-1, 2)
        val yo = system.randomInt(-1, 2)

        // Get the corresponding field.
        root.fields[(x + xo) to (y + yo)]?.let {
            // Get a new random color.
            val rc = system.randomOf(Root.randomColors)

            // Only if different, change it.
            if (rc != it.color) {
                it.color = rc

                // Play sound when actually changed.
                once.recPlay(fire offset toLocal(time.time)) {
                    // Translation of field, relative to listener at center.
                    Matrix4()
                        .translate((x + xo).toFloat(), (y + yo).toFloat(), 0f)
                        .translate(-1f, -1f, -0f)
                }
            }
        }
    }

    override fun evaluate(name: GameName, time: Time, args: Any?) = when (name) {
        "paintNext" -> rec(time, this::paintNext)
        else -> throw IllegalArgumentException("$name unrecognized")
    }

    fun render(time: Double) = with(Frontend) {
        continuous.draw(time, solid.blend(GL11.GL_ONE, GL11.GL_ONE), color) {
            Matrix4()
                .translate(x * 64f, y * 64f, 0f)
                .scale(64f, 64f, 1f)
                .translate(0.5f, 0.5f, 0f)
                .rotate(Vector3.Z, cos((it * 5).toFloat()) * 3f)
                .scale(
                    1.25f + sin(it.toFloat()) * 0.25f,
                    1.25f + sin(it.toFloat()) * 0.25f,
                    1f
                )
        }
    }
}

// Root entity.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

class Root(val system: GameSystem, restore: Restore?) : GameEntity(system, restore) {
    companion object {
        val randomColors = listOf(Color.RED, Color.GREEN, Color.BLUE, Color.PURPLE)
    }

    val fields by map<Pair<Int, Int>, Field>(restore)

    private fun initialize(time: Time) {
        for (x in 0..2)
            for (y in 0..2)
                fields[x to y] = Field(system, null).also {
                    it.x = x
                    it.y = y
                    it.color = system.randomOf(randomColors)
                }
    }

    override fun evaluate(name: GameName, time: Time, args: Any?) = when (name) {
        "initialize" -> rec(time, this::initialize)
        else -> throw IllegalArgumentException("$name unrecognized")
    }

    fun render(time: Double) {
        fields.forEach { (_, f) -> f.render(time) }
    }
}

// Frontend object.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

// Initializer, set if restore is wanted.
val gameInitializer: GameInitializer? = null

object Frontend : F2DListener(-100f, 100f) {
    /**
     * Time when the object was initialized.
     */
    private val initialized = System.currentTimeMillis()

    /**
     * The current local time, current system time milliseconds, passed through [toLocal]
     */
    override val time: Double
        get() = toLocal(System.currentTimeMillis())

    /**
     * Converts the wall-clock time to a local time.
     */
    fun toLocal(long: Long) =
        (long - initialized) / 1000.0

    /**
     * The game system.
     */
    val system = GameSystem(0L, Unit, gameInitializer)

    /**
     * Get or create the root entity.
     */
    val root = gameInitializer?.let {
        system.findRoot<GameName, Root>()
    } ?: Root(system, null).apply {
        signal("initialize", system.time(), Unit)
    }

    /**
     * Solid colors resource.
     */
    val solids = use(SolidResource())

    /**
     * Solid color.
     */
    val solid = solids.refer()

    val atlas = use(AnimatedAtlasResource { Gdx.files.internal("unnamed.atlas") })

    val jump = atlas.refer(AnimatedAtlasResourceArgs("cat-jump", 1.2))

    val stand = atlas.refer(AnimatedAtlasResourceArgs("cat-stand", 2.0))

    /**
     * Resource of an AK74 firing (?)
     */
    val fires = use(SoundResource { Gdx.files.internal("ak74-fire.wav") })

    /**
     * A sound.
     */
    val fire = fires.refer()

    override fun create() {
        super.create()

        // After creation, also connect the input processor.
        Gdx.input.inputProcessor = InputEventQueue(object : InputAdapter() {
            override fun keyUp(keycode: Int) = when (keycode) {
                Input.Keys.SPACE -> {
                    root.fields[1 to 1]?.signal("paintNext", system.time(), Unit)
                    true
                }
                else -> false
            }
        })
    }

    override fun render(time: Double) {
        // Drain the input processor as an input event queue.
        (Gdx.input.inputProcessor as InputEventQueue).drain()

        continuous.draw(time, jump, TextureArgs(keepSize = true)) {
            Matrix4().translate(100f, 100f, -10f + it.toFloat())
        }

        continuous.draw(time, object : Drawable<Unit> {
            override fun generate(args: Unit, time: Double, receiver: ((SpriteBatch) -> Unit) -> Unit) {
                receiver {
                    println("I was drawn.")
                }
            }
        }) {
            Matrix4().translate(2000f, 0f, 0f)
        }

        // Render the entities.
        root.render(time)
    }

    override fun pause() = Unit

    override fun resume() = Unit
}

fun main() {
    val config = LwjglApplicationConfiguration()
    LwjglApplication(Frontend, config)
}
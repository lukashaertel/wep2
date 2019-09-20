package eu.metatools.f2d.ex

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.badlogic.gdx.graphics.Color
import eu.metatools.f2d.F2DListener
import eu.metatools.f2d.context.Lifecycle
import eu.metatools.f2d.context.refer
import eu.metatools.f2d.math.Mat
import eu.metatools.f2d.math.Vec
import eu.metatools.f2d.math.deg
import eu.metatools.f2d.tools.*
import eu.metatools.f2d.util.contains
import eu.metatools.f2d.wep2.encoding.GdxEncoding
import eu.metatools.f2d.wep2.recEnqueue
import eu.metatools.nw.enter
import eu.metatools.wep2.entity.bind.Restore
import eu.metatools.wep2.entity.name
import eu.metatools.wep2.system.*
import eu.metatools.wep2.tools.Time
import eu.metatools.wep2.tools.bind.ticker
import eu.metatools.wep2.tools.rec
import eu.metatools.wep2.track.SI
import eu.metatools.wep2.track.bind.claimer
import eu.metatools.wep2.track.bind.prop
import eu.metatools.wep2.track.bind.refMap
import eu.metatools.wep2.track.randomInt
import eu.metatools.wep2.track.randomOf
import eu.metatools.wep2.util.randomInts
import org.lwjgl.opengl.GL11
import java.io.ObjectOutputStream
import kotlin.math.sin
import kotlin.math.cos

// Shortened type declarations as aliases.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

typealias GameName = String
typealias GameParam = Unit
typealias GameSystem = StandardSystem<GameName, GameParam>
typealias GameInitializer = StandardInitializer<GameName, GameParam>
typealias GameEntity = StandardEntity<GameName>
typealias GameContext = StandardContext<GameName>


val Long.sec get() = this / 1000.0

// Child entity.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

class Field(context: GameContext, restore: Restore?) : GameEntity(context, restore) {
    var x by prop(restore) { 0 }

    var y by prop(restore) { 0 }

    var color by prop(restore) { Color.WHITE }

    private fun paintNext(time: Time) = with(frontend) {
        // Get random offset.
        val xo = root.rnd.randomInt(-1, 2)
        val yo = root.rnd.randomInt(-1, 2)

        // Get the corresponding field.
        root.fields[XY(x + xo, y + yo)]?.let {
            // Get a new random color.
            val rc = root.rnd.randomOf(Root.randomColors)

            // Only if different, change it.
            if (rc != it.color) {
                it.color = rc

//                // Play sound when actually changed.
//                once.recEnqueue(fire offset time.time.sec) {
//                    // Translation of field, relative to listener at center.
//                    Mat()
//                        .translate((x + xo) * 264f, (y + yo) * 264f, 0f)
//                }
            }
        }
    }

    override fun evaluate(name: GameName, time: Time, args: Any?) = when (name) {
        "paintNext" -> rec(time, this::paintNext)
        else -> throw IllegalArgumentException("$name unrecognized")
    }

    fun render(time: Double) = with(frontend) {
        val coords = Mat()
            .translate(x * 264f, y * 264f, 0f)
            .scale(64f, 64f, 1f)
            .translate(0.5f, 0.5f, 0f)
            .rotateZ((cos((time * 5).toFloat()) * 3f).deg)
            .scale(
                1.25f + sin(time.toFloat()) * 0.25f,
                1.25f + sin(time.toFloat()) * 0.25f,
                1f
            )

//        continuous.submit(solid.blend(GL11.GL_ONE, GL11.GL_ONE), Variation(color), time, coords)
//        continuous.submit(Cube, this@Field, time, coords)
    }
}

// Root entity.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

class Root(context: GameContext, restore: Restore?) : GameEntity(context, restore) {
    companion object {
        val randomColors = listOf(Color.RED, Color.GREEN, Color.BLUE, Color.PURPLE)
    }

    val ticker by ticker(restore, 250L) {
        0
    }

    val rnd by claimer(restore, randomInts(0L))

    val fields by refMap<XY, Field>(restore)

    private fun initialize(time: Time) {
        for (x in 0..2)
            for (y in 0..2)
                fields[XY(x, y)] = Field(context, null).also {
                    it.x = x
                    it.y = y
                    it.color = rnd.randomOf(randomColors)
                }
    }

    private fun tick(time: Time) {
    }

    override fun evaluate(name: GameName, time: Time, args: Any?) = when (name) {
        "initialize" -> rec(time, this::initialize)
        "tick" -> rec(time, this::tick)
        else -> throw IllegalArgumentException("$name unrecognized")
    }

    fun render(time: Double) {
        fields.forEach { (_, f) -> f.render(time) }
    }
}

interface RootRender {
    fun render(time: Double)
}

// Frontend object.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

class Frontend : F2DListener(-100f, 100f) {
    /**
     * Cluster contribution methods.
     */
    val net = enter(GdxEncoding<GameName, GameParam>(), "game", Unit).also {
        it.system.claimNewPlayer(System.currentTimeMillis())
    }

    /**
     * The game system.
     */
    val system get() = net.system


    /**
     * The current time of the connected system.
     */
    override val time: Double
        get() = system.toSystemTime(System.currentTimeMillis()).sec

    /**
     * Get or create the root entity.
     */
    val root = if (system.wasRestored)
        system.findRoot()
    else
        Root(system, null).also {
            it.signal("initialize", system.time(), Unit)
        }

    /**
     * Map of resource keys to instance.
     */
    private val resources = mutableMapOf<Any, Lifecycle>()

    /**
     * Gets or creates the resource.
     */
    fun <T : Lifecycle> resource(key: Any, create: () -> T): T {
        @Suppress("unchecked_cast")
        return resources.getOrPut(key, { use(create()) }) as T
    }

    override fun create() {
        super.create()

        // After creation, also connect the input processor.
        Gdx.input.inputProcessor = object : InputAdapter() {
            override fun keyUp(keycode: Int): Boolean {
                when (keycode) {
                    Input.Keys.SPACE ->
                        root.fields[XY(1, 1)]?.signal("paintNext", system.time(), Unit)
                    Input.Keys.ESCAPE ->
                        Gdx.app.exit()
                    else -> return false
                }

                return true
            }
        }
    }

    override fun render(time: Double, delta: Double) {
        if (Keys.W in Gdx.input)
            model = model.translate(Vec.Y * (100 * delta).toFloat())
        if (Keys.S in Gdx.input)
            model = model.translate(-Vec.Y * (100 * delta).toFloat())
        if (Keys.A in Gdx.input)
            model = model.translate(-Vec.X * (100 * delta).toFloat())
        if (Keys.D in Gdx.input)
            model = model.translate(Vec.X * (100 * delta).toFloat())

        // Process the next messages.
        net.update()

        // Generate root ticks.
        root.ticker.tickToWith(system, root.name("tick"), System.currentTimeMillis())

        // Consolidate instruction cache.
        system.consolidate(System.currentTimeMillis() - 5000L)

        // Render the entities.
        root.render(time)
    }

    override fun capture(result: Any, intersection: Vec) {
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) && result is Field)
            result.signal("paintNext", system.time(), Unit)
    }

    override fun pause() = Unit

    override fun resume() = Unit

    override fun dispose() {
        super.dispose()

        system.releasePlayer(System.currentTimeMillis())

        net.stop()
//
//        ObjectOutputStream(Gdx.files.external("sg").write(false)).use {
//            system.save().summarize().let(::println)
//        }
    }
}

lateinit var frontend: Frontend

fun main() {
    frontend = Frontend()
    val config = LwjglApplicationConfiguration()
    LwjglApplication(frontend, config)
}
package eu.metatools.sx

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.esotericsoftware.kryo.Kryo
import eu.metatools.fig.BaseGame
import eu.metatools.fio.data.Vec
import eu.metatools.reaktor.ex.hostRoot
import eu.metatools.reaktor.gdx.VStage
import eu.metatools.reaktor.reconcileNode
import eu.metatools.sx.ents.Reakted
import eu.metatools.sx.ents.World
import eu.metatools.up.dt.Time
import eu.metatools.up.dt.div
import eu.metatools.up.dt.lx
import eu.metatools.up.lang.Bind
import eu.metatools.up.list

class SX : BaseGame(radiusLimit = 16f) {
    lateinit var root: World

    override fun configureNet(kryo: Kryo) =
            configureKryo(kryo)

    override fun shellResolve() {
        root = shell.resolve(lx / "root") as? World ?: error("Unsatisfied, root does not exist")
    }

    override fun shellCreate() {
        root = World(shell, lx / "root", this)
                .also(shell.engine::add)
    }

    override fun Bind<Time>.shellAlways() {
        updateUi()
    }

    override fun Bind<Time>.inputShell(time: Double, delta: Double) {

    }

    override fun inputRepeating(timeMs: Long) {
        // Update global time takers.
        root.worldUpdate(timeMs)
    }

    private var ui: Stage? = null

    private var uiNode by reconcileNode {
        ui = it as Stage
        Gdx.input.inputProcessor = ui
    }

    val updateUi = hostRoot({ uiNode = it }) {
        VStage(viewport = ScreenViewport().also { it.unitsPerPixel = 1f / (Gdx.graphics.density * 1.5f) }) {
            shell.list<Reakted>().forEach {
                receive(it.render())
            }
        }
    }

    override fun resize(width: Int, height: Int) {
        ui?.viewport?.update(width, height, true)
    }

    override fun inputOther(time: Double, delta: Double) {

    }


    override fun output(time: Double, delta: Double) {
        Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        ui?.act(Gdx.graphics.deltaTime)
        ui?.draw()
    }

    private var location: Any? = null
    override fun capture(layer: Layer, result: Any?, relative: Vec, absolute: Vec) {

    }
}

fun main() {
    // Set config values.
    val config = LwjglApplicationConfiguration()
    config.samples = 16
    config.width = 1920
    config.height = 1080

    // Start application.
    LwjglApplication(SX(), config)
}
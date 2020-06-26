package eu.metatools.sx

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.esotericsoftware.kryo.Kryo
import eu.metatools.fio.Fio
import eu.metatools.fio.data.Mat
import eu.metatools.fio.data.Vec
import eu.metatools.fio.tools.*
import eu.metatools.sx.ents.World
import eu.metatools.fig.BaseGame
import eu.metatools.up.dt.Time
import eu.metatools.up.dt.div
import eu.metatools.up.dt.lx
import eu.metatools.up.lang.Bind


class SXRes(val fio: Fio) {
    val solid by lazy { fio.use(SolidResource()) }

    val data by lazy { fio.use(DataResource()) }

    val segoe by lazy {
        fio.use(TextResource { findDefinitions(Gdx.files.internal("segoe_ui")) })
    }

    val infoText by lazy {
        segoe[ReferText(
            horizontal = Location.Start,
            vertical = Location.Start,
            bold = true
        )]
    }

    val box by lazy {
        fio.use(BoxResource())
    }

    val brick by lazy {
        fio.use(RawTextureResource { Gdx.files.internal("brick.png") })
    }
    val dirt by lazy {
        fio.use(RawTextureResource { Gdx.files.internal("dirt.png") })
    }

    val water by lazy {
        fio.use(RawTextureResource { Gdx.files.internal("water.png") })
    }


    val white by lazy {
        fio.use(RawTextureResource { Gdx.files.internal("white.png") })
    }

    val shader by lazy { fio.use(StandardShaderResource()) }

    val rock by lazy {
        ObjectResource(
            box, brick, shader,
            transform = "u_transform",
            projection = "u_projection",
            color = "u_color",
            material = "u_texture"
        )
    }

    val soil by lazy {
        ObjectResource(
            box, dirt, shader,
            transform = "u_transform",
            projection = "u_projection",
            color = "u_color",
            material = "u_texture"
        )
    }

    val fluid by lazy {
        ObjectResource(
            box, water, shader,
            transform = "u_transform",
            projection = "u_projection",
            color = "u_color",
            material = "u_texture"
        )
    }

    val highlight by lazy {
        ObjectResource(
            box, white, shader,
            transform = "u_transform",
            projection = "u_projection",
            color = "u_color",
            material = "u_texture"
        )
    }
}

class SX : BaseGame(radiusLimit = 16f) {
    val res = SXRes(this)

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

    override fun Bind<Time>.inputShell(time: Double, delta: Double) {

    }

    override fun inputRepeating(timeMs: Long) {
        // Update global time takers.
        root.worldUpdate(timeMs)
    }

    override fun outputInit() {
        world.view = Mat.lookAt(Vec(50f, 50f, 50f), Vec.Zero, Vec.Y)
    }

    override fun inputOther(time: Double, delta: Double) {
        val x = world.view.inv.x
        val y = world.view.inv.y
        val z = world.view.inv.z

        val speed = 30f
        if (Gdx.input.isKeyPressed(Input.Keys.D))
            world.view = world.view.translate(-x * delta.toFloat() * speed)
        if (Gdx.input.isKeyPressed(Input.Keys.A))
            world.view = world.view.translate(x * delta.toFloat() * speed)
        if (Gdx.input.isKeyPressed(Input.Keys.S))
            world.view = world.view.translate(-z * delta.toFloat() * speed)
        if (Gdx.input.isKeyPressed(Input.Keys.W))
            world.view = world.view.translate(z * delta.toFloat() * speed)

        if (Gdx.input.isKeyPressed(Input.Keys.Z))
            world.view = world.view.translate(y * delta.toFloat() * speed)
        if (Gdx.input.isKeyPressed(Input.Keys.Q))
            world.view = world.view.translate(-y * delta.toFloat() * speed)
    }

    val world = addLayer(0, layerPerspective(invertedSorting = true, zFar = 1000f))

    val ui = addLayer(1, layerOrthographic(true)).also {
        it.view = Mat.scaling(2f)
    }

    override fun output(time: Double, delta: Double) {
        world.begin()
        root.render(time, delta)
        world.end()
    }

    private var location: Any? = null
    override fun capture(layer: Layer, result: Any?, relative: Vec, absolute: Vec) {

    }
}

fun main() {
    // Set config values.
    val config = LwjglApplicationConfiguration()
    config.height = config.width

    // Start application.
    LwjglApplication(SX(), config)
}
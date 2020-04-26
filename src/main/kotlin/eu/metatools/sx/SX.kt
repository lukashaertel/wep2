package eu.metatools.sx

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.badlogic.gdx.graphics.Color
import com.esotericsoftware.kryo.Kryo
import eu.metatools.fio.Fio
import eu.metatools.fio.data.Mat
import eu.metatools.fio.data.Tri
import eu.metatools.fio.data.Vec
import eu.metatools.fio.drawable.tint
import eu.metatools.fio.resource.get
import eu.metatools.fio.tools.*
import eu.metatools.sx.ents.Fluid
import eu.metatools.sx.ents.World
import eu.metatools.ugs.BaseGame
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

    val shader by lazy { fio.use(StandardShaderResource()) }

    val mesh by lazy {
        ObjectResource(
            box, brick, shader,
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

        for (x in 0..10)
            for (y in 0..10) {
                root.solid[x, y, 0] = World.maxMass
                root.solid[x, y, -6] = World.maxMass
            }
        for (y in 1..9) {
            root.solid[6, y, 1] = World.maxMass
            root.solid[4, y, 1] = World.maxMass
        }

        root.solid.remove(6, 5, 1)
        root.solid.remove(4, 5, 1)
        root.viscosity[6, 5, 1] = 1000


        for (i in 0..10) {
            root.solid[i, 0, 1] = World.maxMass
            root.solid[i, 10, 1] = World.maxMass
            root.solid[0, i, 1] = World.maxMass
            root.solid[10, i, 1] = World.maxMass
            root.solid[i, 0, -5] = World.maxMass
            root.solid[i, 10, -5] = World.maxMass
            root.solid[0, i, -5] = World.maxMass
            root.solid[10, i, -5] = World.maxMass
        }
        root.solid.remove(8, 5, 0)
        root.solid.remove(2, 5, 0)

        root.fluid[8, 1, 1] = Fluid(World.maxMass)
    }

    override fun Bind<Time>.inputShell(time: Double, delta: Double) {
        if (Gdx.input.isButtonPressed(Input.Buttons.LEFT))
            (location as? Tri)?.let {
                root.add(it)
            }

        if (Gdx.input.isButtonPressed(Input.Buttons.RIGHT)) {
            val x = world.view.inv.x
            val y = world.view.inv.y
            val center = world.view.inv.center

            world.view = world.view
                .translate(center)
                .rotate(y, Gdx.input.deltaX.toFloat() / 1000f)
                .rotate(x, Gdx.input.deltaY.toFloat() / 1000f)
                .translate(-center)

        }
    }

    override fun inputRepeating(timeMs: Long) {
        root.worldUpdate(timeMs)
        // Update global time takers.
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

    fun cube(result: Any, color: Color, mat: Mat) {
        world.submit(res.mesh.get().tint(color), Unit, time, mat)
        world.submit(CaptureCube, Unit, result, time, mat)
    }

    override fun output(time: Double, delta: Double) {
        world.begin()
        root.render(time, delta)
        world.end()


        val a = root.fluid[6, 5, 1] ?: return
        val b = root.fluid[4, 5, 1] ?: return

        ui.begin()
        ui.submit(
            res.infoText, "Visc: ${a.mass}", time,
            Mat.translation(20f, 20f).scale(12f, 12f)
        )
        ui.submit(
            res.infoText, "In: ${a.inFlow}", time,
            Mat.translation(100f, 20f).scale(12f, 12f)
        )
        ui.submit(
            res.infoText, "Out: ${a.outFlow}", time,
            Mat.translation(180f, 20f).scale(12f, 12f)
        )

        ui.submit(
            res.infoText, "Non-visc: ${b.mass}", time,
            Mat.translation(20f, 60f).scale(12f, 12f)
        )
        ui.submit(
            res.infoText, "In: ${b.inFlow}", time,
            Mat.translation(100f, 60f).scale(12f, 12f)
        )
        ui.submit(
            res.infoText, "Out: ${b.outFlow}", time,
            Mat.translation(180f, 60f).scale(12f, 12f)
        )
        ui.end()
    }

    private var location: Any? = null
    override fun capture(layer: Layer, result: Any?, relative: Vec, absolute: Vec) {
        result?.let {
            location = (it as? Pair<Tri, Tri>)?.first
            //Gdx.graphics.setTitle(it.toString())
        }
    }
}

fun main() {
    // Set config values.
    val config = LwjglApplicationConfiguration()
    config.height = config.width

    // Start application.
    LwjglApplication(SX(), config)
}
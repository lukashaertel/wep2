package eu.metatools.sx

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.badlogic.gdx.graphics.Color
import com.esotericsoftware.kryo.Kryo
import eu.metatools.fio.Fio
import eu.metatools.fio.data.*
import eu.metatools.fio.drawable.Drawable
import eu.metatools.fio.drawable.color
import eu.metatools.fio.drawable.tint
import eu.metatools.fio.tools.*
import eu.metatools.sx.ents.CellTypes
import eu.metatools.sx.ents.World
import eu.metatools.ugs.BaseGame
import eu.metatools.up.dt.Time
import eu.metatools.up.dt.div
import eu.metatools.up.dt.lx
import eu.metatools.up.lang.Bind
import kotlin.collections.set

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

        val f0 = """
XxxxxxxxX
XxxxxxxxXxx
XxxxxxxxXXX
XxxxxxxxX
XxxxxxxxX
        """
        val f1 = """
X  X    X
X  X    S X
XXWXXXXWXXX
XXWWWWWWX
XXXXXXXXX"""
        val f2 = """
XxxxxxxxX
XxxxX XxXxx
XxxxXXXxXXX
XxxxxxxxX
XxxxxxxxX
        """
        val f3 = """
         
    X X  
    XXX  
         
         
        """

        fun atc(y: Int, s: String) =
            s.lineSequence().forEachIndexed { z, vs ->
                vs.forEachIndexed { x, v ->
                    val at = Tri(x, y, -z)
                    if (v.isLowerCase() && v.isLetter())
                        root.hidden.add(at)
                    if (v.toLowerCase() == 'x')
                        root.types[at] = CellTypes.Rock
                    if (v.toLowerCase() == 's')
                        root.types[at] = CellTypes.Soil
                    if (v.toLowerCase() == 'o')
                        root.types[at] = CellTypes.PipeY
                    if (v.toLowerCase() == '=')
                        root.types[at] = CellTypes.PipeX
                    if (v.toLowerCase() == '|')
                        root.types[at] = CellTypes.PipeZ
                    if (v.toLowerCase() == 'w')
                        root.level[at] = 1f
                }
            }

        atc(-1, f0)
        atc(0, f1)
        atc(1, f2)
        atc(2, f3)
    }

    override fun Bind<Time>.inputShell(time: Double, delta: Double) {

        (location as? Tri)?.let {
            val ct = Gdx.input.isButtonPressed(Input.Buttons.LEFT) && !Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)
            val os =
                Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) && Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)

            if (ct || os) {
                if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT))
                    root.add(it.over())
                else
                    root.add(it)
            }
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

    fun cube(cube: Drawable<Unit>, result: Any, color: Color, mat: Mat) {
        world.submit(cube.tint(color), Unit, time, mat)
        world.submit(CaptureCube, Unit, result, time, mat)
    }

    private fun Float.formatFlow() = if (this < 0f) "out ${roundForPrint(-this)}" else "in ${roundForPrint(this)}"

    override fun output(time: Double, delta: Double) {
        world.begin()
        root.render(time, delta)
        world.end()

        root.highlight = location as? Tri

        val (left, right, back, front, under, over) = (location as? Tri)?.let(root::flow) ?: return

        ui.begin()
        if (right != 0f)
            ui.submit(
                res.infoText.color(Color.RED), "Right: ${right.formatFlow()}", time,
                Mat.translation(20f, 60f).scale(12f, 12f)
            )
        if (front != 0f)
            ui.submit(
                res.infoText.color(Color.GREEN), "Front: ${front.formatFlow()}", time,
                Mat.translation(120f, 60f).scale(12f, 12f)
            )

        if (over != 0f)
            ui.submit(
                res.infoText.color(Color.BLUE), "Over: ${over.formatFlow()}", time,
                Mat.translation(220f, 60f).scale(12f, 12f)
            )

        if (left != 0f)
            ui.submit(
                res.infoText.color(Color.RED), "Left: ${left.formatFlow()}", time,
                Mat.translation(20f, 20f).scale(12f, 12f)
            )
        if (back != 0f)
            ui.submit(
                res.infoText.color(Color.GREEN), "Back: ${back.formatFlow()}", time,
                Mat.translation(120f, 20f).scale(12f, 12f)
            )

        if (under != 0f)
            ui.submit(
                res.infoText.color(Color.BLUE), "Under: ${under.formatFlow()}", time,
                Mat.translation(220f, 20f).scale(12f, 12f)
            )
        ui.end()
    }

    private var location: Any? = null
    override fun capture(layer: Layer, result: Any?, relative: Vec, absolute: Vec) {
        result?.let {
            location = (it as? Pair<Tri, Tri>)?.first
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
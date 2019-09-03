package eu.metatools.f2d

import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.math.Vector3
import eu.metatools.f2d.SolidColors.texture
import kotlin.math.sin


val once = Once()

val continuous = Continuous()


object SolidColors : DrawableResource<Color> {
    var texture: Texture? = null

    override fun initialize() {
        // Check if already initialized.
        if (texture == null)
            texture = Texture(Pixmap(1, 1, Pixmap.Format.RGBA8888).apply {
                setColor(Color.WHITE)
                drawPixel(0, 0)
            })
    }

    override fun dispose() {
        texture?.dispose()
        texture = null
    }

    override fun instantiate(arguments: Color) =
        object : Drawable {
            override fun generate(time: Double, receiver: ((SpriteBatch) -> Unit) -> Unit) {
                receiver {
                    val source = it.color.cpy()
                    it.color = arguments
                    it.draw(texture, 0f, 0f)
                    it.color = source
                }
            }

            override fun hasStarted(time: Double) =
                0.0 <= time //TODO Figure out a standard.

            override fun hasEnded(time: Double) =
                false
        }

}

fun Drawable.endingAfter(endTime: Double) = object : Drawable {
    override fun generate(time: Double, receiver: ((SpriteBatch) -> Unit) -> Unit) =
        this@endingAfter.generate(time, receiver)

    override fun hasStarted(time: Double) =
        this@endingAfter.hasStarted(time)

    override fun hasEnded(time: Double) =
        time > endTime
}

fun Drawable.offset(offset: Double) = object : Drawable {
    override fun generate(time: Double, receiver: ((SpriteBatch) -> Unit) -> Unit) =
        this@offset.generate(time - offset, receiver)

    override fun hasStarted(time: Double) =
        this@offset.hasStarted(time - offset)

    override fun hasEnded(time: Double) =
        this@offset.hasEnded(time - offset)
}

fun main() {
    val config = LwjglApplicationConfiguration()
    LwjglApplication(object : ApplicationListener {
        val firstTime = System.currentTimeMillis()

        var world = Matrix4()

        lateinit var spriteBatch: SpriteBatch

        val white = SolidColors.instantiate(Color.WHITE)

        fun render(time: Double, world: Coords, n: Int = 3) {
            continuous.draw(time, world, white) {
                Matrix4()
                    .scale(10f, 10f, 10f)
            }
            continuous.draw(time, world, white) {
                Matrix4()
                    .translate(-20f, 0f, 0f)
                    .scale(10f, 10f, 10f)
            }
            continuous.draw(time, world, white) {
                Matrix4()
                    .translate(20f, 0f, 0f)
                    .scale(10f, 10f, 10f)
            }
            continuous.draw(time, world, white) {
                Matrix4()
                    .translate(0f, -20f, 0f)
                    .scale(10f, 10f, 10f)
            }
            continuous.draw(time, world, white) {
                Matrix4()
                    .translate(0f, 20f, 0f)
                    .scale(10f, 10f, 10f)
            }

            if (n > 0) {
                render(time, world.cpy().translate(100f, 0f, 0f).rotate(Vector3.Z, 15f).scale(0.8f, 0.8f, 0.8f), n - 1)
            }
        }

        override fun render() {
            val time = (System.currentTimeMillis() - firstTime) / 1000.0

            render(time, world)
            once.send(continuous, time, world)

            Gdx.gl.glClearColor(1f, 0f, 0f, 1f)
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)
            spriteBatch.begin()
            continuous.send(spriteBatch)
            spriteBatch.end()
        }

        override fun pause() {

        }

        override fun resume() {

        }

        override fun resize(width: Int, height: Int) {
            spriteBatch.projectionMatrix =
                Matrix4().setToOrtho2D(0f, 0f, width.toFloat(), height.toFloat());
        }

        override fun create() {

            spriteBatch = SpriteBatch()

            SolidColors.initialize()

            once.draw(white.endingAfter(5.0).offset(5.0)) {
                Matrix4()
                    .translate(100f, 100f, 0f)
                    .scale(10f, 10f, 10f)
            }
        }

        override fun dispose() {
            SolidColors.dispose()
        }

    }, config)
}
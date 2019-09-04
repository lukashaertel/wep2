package eu.metatools.f2d

import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.math.Vector3
import eu.metatools.f2d.context.*
import eu.metatools.f2d.tools.SolidResource
import eu.metatools.f2d.tools.SoundArgs
import eu.metatools.f2d.tools.SoundResource
import kotlin.math.sin


fun main() {
    val config = LwjglApplicationConfiguration()
    LwjglApplication(object : ApplicationListener {
        val firstTime = System.currentTimeMillis()

        var world = Matrix4()

        lateinit var spriteBatch: SpriteBatch

        val once = Once()

        val continuous = Continuous()

        val colors = SolidResource()

        val fire = SoundResource { Gdx.files.internal("ak74-fire.wav") }

        val soundA = fire.refer().offset(2.0)

        val soundB = fire.refer().offset(4.0)

        val solid = colors.refer()


        val listener = Vector3(100f, 100f, 0f)

        fun render(time: Double, world: Coords, n: Int = 5) {
            continuous.draw(time, world, solid, Color(1f, 1f, 1f, 0.5f)) {
                Matrix4()
                    .scale(100f, 2f, 1f)
                    .translate(0.5f, 0f, 0f)
            }

            if (n == 5) {
                continuous.play(
                    "id", time, world, soundA, SoundArgs(
                        looping = true,
                        pitch = if (time > 5) 0.5f else 1.0f
                    )
                ) {
                    Matrix4().translate(20f, 0f, 0f).translate(Vector3().sub(listener))
                }
            }

            if (n > 0) {
                render(
                    time, world.cpy()
                        .translate(100f, 0f, 0f)
                        .rotate(Vector3.Z, 30f)
                        .scale(0.5f, 1f, 1f), n - 1
                )
            }
        }

        override fun render() {
            val time = (System.currentTimeMillis() - firstTime) / 1000.0

            render(time, world.cpy().translate(100f, 100f, 0f))
            once.render(continuous, time, world)

            Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)
            spriteBatch.begin()
            continuous.render(spriteBatch)
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
            spriteBatch.enableBlending()
            spriteBatch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

            colors.initialize()
            fire.initialize()

            once.draw(solid.limit(100.0).offset(2.0), Color(0.1f, 0.1f, 0.5f, 1.0f)) {
                Matrix4()
                    .translate(110f, 110f, -1f)
                    .scale(200f, 200f, 1f)
                    .rotate(Vector3.Z, it.toFloat() * 10f)
            }

            once.play(soundB, SoundArgs(looping = true)) {
                Matrix4().translate((100f * sin(Math.toRadians(it * 10f))).toFloat(), 0f, 0f)
            }
        }

        override fun dispose() {
            colors.dispose()
            fire.dispose()
        }

    }, config)
}
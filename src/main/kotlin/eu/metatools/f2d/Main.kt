package eu.metatools.f2d

import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Matrix4
import eu.metatools.f2d.tools.SolidResource
import eu.metatools.f2d.tools.SoundResource


fun main() {
    val config = LwjglApplicationConfiguration()
    val listener = object : F2DListener(0f, 100f) {
        /**
         * Time when the object was initialized.
         */
        private val initialized = System.currentTimeMillis()

        override val time: Double
            get() = (System.currentTimeMillis() - initialized) / 1000.0


        private val colors = use(SolidResource())

        private val color = colors.refer()

        private val fire = use(SoundResource { Gdx.files.internal("ak74-fire.wav") })

        override fun render(time: Double) {
            continuous.draw(time, color, Color.RED) {
                Matrix4().translate(260f, 220f, -10f).scale(400f, 100f, 1f)
            }

            continuous.draw(time, color, Color.GREEN) {
                Matrix4().translate(200f, 200f, -5f).scale(100f, 100f, 1f)
            }

            continuous.draw(time, color, Color.BLUE) {
                Matrix4().translate(140f, 180f, -15f).scale(100f, 100f, 1f)
            }
        }

        override fun pause() = Unit

        override fun resume() = Unit
    }
    LwjglApplication(listener, config)
}
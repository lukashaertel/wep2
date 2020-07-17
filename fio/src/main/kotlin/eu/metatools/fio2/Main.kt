package eu.metatools.fio2

import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g3d.decals.CameraGroupStrategy
import com.badlogic.gdx.utils.NumberUtils
import eu.metatools.fio.data.Col
import eu.metatools.fio.data.Mat

fun main() {
    LwjglApplication(
            object : ApplicationListener {
                private lateinit var cgs: CameraGroupStrategy
                private lateinit var tex: TextureAtlas
                private lateinit var region: TextureAtlas.AtlasRegion

                private lateinit var target: ShapeTarget

                private val start = System.currentTimeMillis()
                private val seconds get() = (System.currentTimeMillis() - start) / 1000f
                var lft = System.currentTimeMillis()

                override fun render() {
                    val cft = System.currentTimeMillis()
                    Gdx.graphics.setTitle("${cft - lft}")
                    lft = cft

                    Gdx.gl.glClearDepthf(1f)
                    Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1.0f)
                    Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
                    Gdx.gl.glEnable(GL20.GL_BLEND)

                    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)
                    updateData()
                    cgs.beforeGroups()
                    target.render(cgs.getGroupShader(0))
                    cgs.afterGroups()

                }

                override fun pause() {
                }

                override fun resume() {
                }

                override fun resize(width: Int, height: Int) {
                    cgs.camera.viewportWidth = width.toFloat()
                    cgs.camera.viewportHeight = height.toFloat()
                    cgs.camera.update()
                }

                fun updateData() {
                    val dl = 2000
                    target.beginData()
                    val range = (-dl..dl)//-target.limit / 2 until target.limit / 2
                    for (d in range) {
                        val f = d / 100f
                        val mat = Mat.rotationZ(seconds + f)
                        val color = Col.hsv(d.toFloat(), 0.5f, 1f)
                        val packed = NumberUtils.intToFloatColor(color.packed)

                        mat.times(0f, 0f, f) { x, y, z ->
                            target.addVertex(x, y, z, packed, region.u, region.v)
                        }
                        mat.times(0f, 2f, f) { x, y, z ->
                            target.addVertex(x, y, z, packed, region.u, region.v2)
                        }
                        mat.times(2f, 2f, f) { x, y, z ->
                            target.addVertex(x, y, z, packed, region.u2, region.v2)
                        }
                        mat.times(2f, 0f, f) { x, y, z ->
                            target.addVertex(x, y, z, packed, region.u2, region.v)
                        }
                    }
                    target.sortQuad(
                            0f,
                            0f,
                            1f)
                    target.endData()
                }

                override fun create() {
                    tex = use(TextureAtlas(Gdx.files.internal("ex/res/CTP.atlas")))
                    tex.textures.first().bind(0)
                    region = tex.findRegion("pa_i_d")

                    cgs = use(CameraGroupStrategy(PerspectiveCamera(45f,
                            Gdx.graphics.width.toFloat(),
                            Gdx.graphics.height.toFloat())))

                    val d = 15f
                    cgs.camera.position.set(d, d, d)
                    cgs.camera.lookAt(0f, 0f, 0f)
                    cgs.camera.update()

                    target = use(ShapeTarget(true, 10000))
                }

                override fun dispose() {
                    closeUsed()
                }

            }, LwjglApplicationConfiguration().apply {
        useGL30 = true
    })

}
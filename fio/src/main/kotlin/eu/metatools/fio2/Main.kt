package eu.metatools.fio2

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.utils.NumberUtils
import eu.metatools.fio.data.Col
import eu.metatools.fio.data.Mat

//private val isBoundToBuffer = BufferUtils.newIntBuffer(16)
//fun Texture.isBoundTo(unit: Int): Boolean {
//    Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0 + unit)
//    isBoundToBuffer.clear()
//    Gdx.gl.glGetIntegerv(GL30.GL_TEXTURE_BINDING_2D, isBoundToBuffer)
//    return glHandle == isBoundToBuffer.get()
//}

class QuadLayer(val shader: ShaderProgram, val combined: () -> Mat, val limit: Int = 8192) : AutoCloseable {
    private val quads = Quads(limit)

    private var currentCount = 0

    private var currentTexture: Texture? = null

    @Suppress("non_public_call_from_public_inline")
    inline fun push(texture: Texture, push: Quads.() -> Unit) {

        // Initial assign texture.
        if (currentTexture == null)
            currentTexture = texture

        // Flush if limit reached.
        if (currentCount == limit || currentTexture != texture) {
            flush()
            currentTexture = texture
        }

        // Run block on quads.
        push(quads)
    }

    fun flush() {
        if (currentCount == 0)
            return

        currentTexture?.bind(0)

        shader.begin()
        shader.setUniformMatrix("u_projectionViewMatrix", combined().asMatrix())
        shader.setUniformi("u_texture", 0)
        quads.render(shader, currentCount)
        shader.end()

        currentCount = 0
    }

    override fun close() {
        quads.close()
    }

}

fun main() {
    LwjglApplication(host {
        object : Controller {
            private val atlas = use(TextureAtlas(Gdx.files.internal("ex/res/CTP.atlas"))).also {
                it.textures.first().bind(0)
            }

            private val region = atlas.findRegion("pa_i_d")

            private val camera = PerspectiveCamera(45f,
                    Gdx.graphics.width.toFloat(),
                    Gdx.graphics.height.toFloat()).also {
                val d = 16f
                it.position.set(d, d, d)
                it.lookAt(0f, 0f, 0f)
                it.update()
            }

            private val shader = use(createDefaultShader())

            private val target = use(Quads(8000))

            private val commitGenerator = CommitGenerator()

            private val addressGenerator = AddressGenerator()

            private val start = System.currentTimeMillis()

            private val seconds get() = (System.currentTimeMillis() - start) / 1000f

            private var lastFrameTime = System.currentTimeMillis()


            override fun render() {
                val cft = System.currentTimeMillis()
                Gdx.graphics.setTitle("${cft - lastFrameTime}")
                lastFrameTime = cft

                Gdx.gl.glClearDepthf(1f)
                Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1.0f)
                Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
                Gdx.gl.glEnable(GL20.GL_DEPTH_TEST)
//                    Gdx.gl.glEnable(GL20.GL_BLEND)

                Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

                if (Gdx.input.isKeyJustPressed(Input.Keys.A))
                    addressGenerator.refer()

                if (Gdx.input.isKeyJustPressed(Input.Keys.D)) {
                    if (0 < addressGenerator.end) {
                        val a = addressGenerator.references().random()
                        addressGenerator.release(a)

                        target.position(a)
                        target.vertexEmpty()
                        target.vertexEmpty()
                        target.vertexEmpty()
                        target.vertexEmpty()
                        commitGenerator.touch(a)
                    }
                }

                updateSprites()
                if (commitGenerator.isNotEmpty()) {
                    target.commit(commitGenerator.currentMin, commitGenerator.currentMax)
                    commitGenerator.reset()
                }

                // DO NOT DO THIS WHEN USING MANAGED ADDRESSES, ONLY FOR PUSH AND SORT.
                // target.sortQuad(0, addressGenerator.end, 0f, 0f, 1f)
                // target.commit(0, addressGenerator.end)

                shader.begin()
                shader.setUniformMatrix("u_projectionViewMatrix", camera.combined)
                shader.setUniformi("u_texture", 0)
                target.render(shader, addressGenerator.end)
                shader.end()

            }

            override fun resize(width: Int, height: Int) {
                camera.viewportWidth = width.toFloat()
                camera.viewportHeight = height.toFloat()
                camera.update()
            }


            fun updateSprites() {
                for (n in addressGenerator.references())
                    updateSprite(n)
            }

            fun updateSprite(n: Int) {
                commitGenerator.touch(n)
                target.position(n)

                val f = n / 10f
                val mat = Mat.rotationZ(seconds + f)
                val packed = NumberUtils.intToFloatColor(Col.White.packed)
                mat.times(0f, 0f, f) { x, y, z ->
                    target.vertex(x, y, z, packed, region.u, region.v)
                }
                mat.times(0f, 2f, f) { x, y, z ->
                    target.vertex(x, y, z, packed, region.u, region.v2)
                }
                mat.times(2f, 2f, f) { x, y, z ->
                    target.vertex(x, y, z, packed, region.u2, region.v2)
                }
                mat.times(2f, 0f, f) { x, y, z ->
                    target.vertex(x, y, z, packed, region.u2, region.v)
                }
            }

            override fun dispose() {
                closeUsed()
            }
        }
    }, LwjglApplicationConfiguration().apply {
        useGL30 = true
    })

}
package eu.metatools.fio2

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.utils.NumberUtils
import com.maltaisn.msdfgdx.FontStyle
import com.maltaisn.msdfgdx.MsdfFont
import com.maltaisn.msdfgdx.MsdfShader
import eu.metatools.fio.data.Col
import eu.metatools.fio.data.Mat
import eu.metatools.fio.data.asMat

//private val isBoundToBuffer = BufferUtils.newIntBuffer(16)
//fun Texture.isBoundTo(unit: Int): Boolean {
//    Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0 + unit)
//    isBoundToBuffer.clear()
//    Gdx.gl.glGetIntegerv(GL30.GL_TEXTURE_BINDING_2D, isBoundToBuffer)
//    return glHandle == isBoundToBuffer.get()
//}

interface Layer {
    fun begin() = Unit
    fun end() = Unit
}

inline fun Layer.runWith(block: () -> Unit) {
    begin()
    block()
    end()
}

class TextLayer(val combined: () -> Mat) : Layer, AutoCloseable {
    private val shader by lazy {
        MsdfShader()
    }

    private val batch by lazy {
        SpriteBatch().also {
            it.shader = shader
        }
    }

    override fun begin() {
        batch.projectionMatrix = combined().asMatrix()
        batch.begin()
    }

    @Suppress("non_public_call_from_public_inline")
    inline fun push(font: MsdfFont, fontStyle: FontStyle, push: BitmapFont.(SpriteBatch) -> Unit) {
        val data = font.font.data
        val scaleX = data.scaleX
        val scaleY = data.scaleY
        data.setScale(fontStyle.size / font.glyphSize)
        shader.updateForFont(font, fontStyle)
        push(font.font, batch)
        data.setScale(scaleX, scaleY)
    }

    fun flush() {
        batch.flush()
    }

    override fun end() {
        batch.end()
    }

    override fun close() {
        shader.dispose()
        batch.dispose()
    }

}

class QuadLayer(val shader: ShaderProgram, val combined: () -> Mat, val limit: Int = 8192) : Layer, AutoCloseable {
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
        currentCount++
    }

    fun flush() {
        if (currentCount == 0)
            return

        currentTexture?.bind(0)

        quads.commit(0, currentCount)

        shader.begin()
        shader.setUniformMatrix("u_projectionViewMatrix", combined().asMatrix())
        shader.setUniformi("u_texture", 0)
        quads.render(shader, currentCount)
        shader.end()

        currentCount = 0
    }

    override fun end() {
        flush()
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

            private val cameraUi = OrthographicCamera(
                    Gdx.graphics.width.toFloat(),
                    Gdx.graphics.height.toFloat()
            )

            private val camera = PerspectiveCamera(45f,
                    Gdx.graphics.width.toFloat(),
                    Gdx.graphics.height.toFloat()).also {
                val d = 16f
                it.position.set(d, d, d)
                it.lookAt(0f, 0f, 0f)
                it.update()
            }

            private val shader = use(createDefaultShader())

            private val quads = use(QuadLayer(shader, { camera.combined.asMat() }))


            private val font = use(MsdfFont(Gdx.files.internal("fio/res/BarlowSemiCondensed-Regular.fnt"), 32f, 5f))

            private val fontStyle = FontStyle()
                    .setColor(Color.WHITE)
                    .setSize(48f)
                    .setShadowColor(Color.BLACK)

            private val fontStyleBold = FontStyle(fontStyle)
                    .setWeight(0.1f)

            private val texts = use(TextLayer { cameraUi.combined.asMat() })

            private val start = System.currentTimeMillis()

            private val seconds get() = (System.currentTimeMillis() - start) / 1000f

            private var lastFrameTime = System.currentTimeMillis()

            private val addresses = hashSetOf(1)

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
                    addresses += (addresses.max() ?: 0).inc()

                if (Gdx.input.isKeyJustPressed(Input.Keys.D))
                    if (addresses.isNotEmpty())
                        addresses -= addresses.random()

                quads.runWith {
                    for (i in addresses)
                        quads.push(region.texture) {
                            writeSprite(i)
                        }
                }

                texts.runWith {
                    var posX = 50f
                    var posY = 50f
                    texts.push(font, fontStyle) {
                        posX += draw(it, "Hello world.", posX, posY).width
                    }
                    texts.flush()
                    texts.push(font, fontStyleBold) {
                        posX += draw(it, " 4000", posX, posY).width
                    }
                    texts.flush()
                    texts.push(font, fontStyle) {
                        posX += draw(it, " gold.", posX, posY).width
                    }
                }


            }

            override fun resize(width: Int, height: Int) {
                camera.viewportWidth = width.toFloat()
                camera.viewportHeight = height.toFloat()
                camera.update()
                cameraUi.viewportWidth = width.toFloat()
                cameraUi.viewportHeight = height.toFloat()
                cameraUi.update()
            }

            fun Quads.writeSprite(n: Int) {
                val f = n / 10f
                val mat = Mat.rotationZ(seconds + f)
                val packed = NumberUtils.intToFloatColor(Col.White.packed)
                mat.times(0f, 0f, f) { x, y, z ->
                    vertex(x, y, z, packed, region.u, region.v)
                }
                mat.times(0f, 2f, f) { x, y, z ->
                    vertex(x, y, z, packed, region.u, region.v2)
                }
                mat.times(2f, 2f, f) { x, y, z ->
                    vertex(x, y, z, packed, region.u2, region.v2)
                }
                mat.times(2f, 0f, f) { x, y, z ->
                    vertex(x, y, z, packed, region.u2, region.v)
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
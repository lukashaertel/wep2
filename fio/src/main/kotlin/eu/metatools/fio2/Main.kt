package eu.metatools.fio2

import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.utils.NumberUtils
import eu.metatools.fio.data.Col
import eu.metatools.fio.data.Mat

fun createDefaultShader(): ShaderProgram {
    val vertexShader = """
attribute vec4 ${ShaderProgram.POSITION_ATTRIBUTE};
attribute vec4 ${ShaderProgram.COLOR_ATTRIBUTE};
attribute vec2 ${ShaderProgram.TEXCOORD_ATTRIBUTE}0;
uniform mat4 u_projectionViewMatrix;
varying vec4 v_color;
varying vec2 v_texCoords;

void main()
{
    v_color = ${ShaderProgram.COLOR_ATTRIBUTE};
    v_color.a = v_color.a * (255.0/254.0);
    v_texCoords = ${ShaderProgram.TEXCOORD_ATTRIBUTE}0;
    gl_Position = u_projectionViewMatrix * ${ShaderProgram.POSITION_ATTRIBUTE};
}
"""
    val fragmentShader = """
#ifdef GL_ES
precision mediump float;
#endif
varying vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;
void main()
{
    vec4 result = v_color * texture2D(u_texture, v_texCoords);
    if(result.a < 0.001)
        discard;
    gl_FragColor = result;
}"""
    return ShaderProgram(vertexShader, fragmentShader).also {
        require(it.isCompiled) { "couldn't compile shader: " + it.log }
    }
}

/**
 * Todo: how necessary is this, how strongly coupled must this be.
 *
 * Generally, quads, cg and ag will be exposed to generate remote update request and to obtain locations in quad.
 */
class CommitGenerator(val quads: Quads) {
    private var currentMin = Int.MAX_VALUE
    private var currentMax = Int.MIN_VALUE

    fun touch(shape: Int) {
        currentMin = minOf(shape, currentMin)
        currentMax = maxOf(shape.inc(), currentMax)
    }

    fun commit() {
        if (currentMax < currentMin)
            return

        quads.commit(currentMin, currentMax)

        currentMin = Int.MAX_VALUE
        currentMax = Int.MIN_VALUE
    }
}

fun main() {
    LwjglApplication(
            object : ApplicationListener {
                private lateinit var cgs: ShaderProgram
                private lateinit var cam: Camera
                private lateinit var tex: TextureAtlas
                private lateinit var region: TextureAtlas.AtlasRegion

                private lateinit var target: Quads
                private lateinit var commitGenerator: CommitGenerator
                private lateinit var addressGenerator: AddressGenerator

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
                    commitGenerator.commit()

                    // DO NOT DO THIS WHEN USING MANAGED ADDRESSES, ONLY FOR PUSH AND SORT.
                    // target.sortQuad(0, addressGenerator.end, 0f, 0f, 1f)
                    // target.commit(0, addressGenerator.end)

                    cgs.begin()
                    cgs.setUniformMatrix("u_projectionViewMatrix", cam.combined)
                    cgs.setUniformi("u_texture", 0)
                    target.render(cgs, addressGenerator.end)
                    cgs.end()

                }

                override fun pause() {
                }

                override fun resume() {
                }

                override fun resize(width: Int, height: Int) {
                    cam.viewportWidth = width.toFloat()
                    cam.viewportHeight = height.toFloat()
                    cam.update()
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

//                fun updateData(min: Int? = null, max: Int? = null) {
//                    val min = min ?: -target.limit / 2
//                    val max = max ?: target.limit / 2
//
//                    val base = min + target.limit / 2
//                    target.position(base)
//                    val rows = 4
//                    val range = min until max
//                    for (d in range.reversed()) {
//                        val f = d / 200f
//                        val mat = Mat.rotationZ(seconds + f)
//                        val color = Col.hsv(d.toFloat(), 0.5f, 1f)
//                        val packed = NumberUtils.intToFloatColor(color.packed)
//                        val dx = (((d % rows) + rows) % rows) * 3f - 3f * rows / 2f
//
//                        mat.times(0f, 0f, f) { x, y, z ->
//                            target.addVertex(dx + x, y, z, packed, region.u, region.v)
//                        }
//                        mat.times(0f, 2f, f) { x, y, z ->
//                            target.addVertex(dx + x, y, z, packed, region.u, region.v2)
//                        }
//                        mat.times(2f, 2f, f) { x, y, z ->
//                            target.addVertex(dx + x, y, z, packed, region.u2, region.v2)
//                        }
//                        mat.times(2f, 0f, f) { x, y, z ->
//                            target.addVertex(dx + x, y, z, packed, region.u2, region.v)
//                        }
//                    }
//                    target.commit(base, max - min)
//                }

                override fun create() {
                    tex = use(TextureAtlas(Gdx.files.internal("ex/res/CTP.atlas")))
                    tex.textures.first().bind(0)
                    region = tex.findRegion("pa_i_d")

                    cam = PerspectiveCamera(45f,
                            Gdx.graphics.width.toFloat(),
                            Gdx.graphics.height.toFloat())
                    cgs = use(createDefaultShader())

                    val d = 16f
                    cam.position.set(d, d, d)
                    cam.lookAt(0f, 0f, 0f)
                    cam.update()

                    target = use(Quads(8000))
                    commitGenerator = CommitGenerator(target)
                    addressGenerator = AddressGenerator()

                    //updateData()
                }

                override fun dispose() {
                    closeUsed()
                }

            }, LwjglApplicationConfiguration().apply {
        useGL30 = true
    })

}
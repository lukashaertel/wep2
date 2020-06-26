package eu.metatools.fio.tools

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g3d.utils.MeshBuilder
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.BoxShapeBuilder
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import eu.metatools.fio.context.Context
import eu.metatools.fio.drawable.Drawable
import eu.metatools.fio.resource.LifecycleResource
import eu.metatools.fio.resource.Resource

class StandardShaderResource : LifecycleResource<Unit, ShaderProgram> {
    private val vertexShader = """attribute vec4 a_position;    
attribute vec3 a_normal;    
attribute vec2 a_texCoord0;
uniform mat4 u_transform;
uniform mat4 u_projection;
varying vec2 v_texCoords;
varying vec3 v_normal;
void main()                  
{                            
   v_texCoords = a_texCoord0; 
   v_normal = a_normal;
   gl_Position =  u_projection * u_transform * a_position;  
}                            
"""

    private val fragmentShader = """#ifdef GL_ES
precision mediump float;
#endif
varying vec2 v_texCoords;
varying vec3 v_normal;
uniform sampler2D u_texture;
uniform vec4 u_color;
void main()                                  
{                                
    vec3 norm = normalize(v_normal);
    vec3 lightDir = vec3(0.5,1,0.5);
    float diff = max(dot(norm, lightDir), 0.0);
    
    vec4 ambient = vec4(0.3 * vec3(0.6f, 0.7f, 0.8f), 1f);
    vec4 diffuse = vec4(diff * vec3(1f, 0.9f, 0.8f), 1f);
      
  gl_FragColor = u_color * (ambient + diffuse) * texture2D(u_texture, v_texCoords);
}"""

    private var shader: ShaderProgram? = null

    override fun initialize() {
        shader?.dispose()
        shader = ShaderProgram(vertexShader, fragmentShader).apply {
            require(isCompiled) { log }
        }
    }

    override fun dispose() {
        shader?.dispose()
        shader = null

    }

    override fun get(argsResource: Unit): ShaderProgram {
        return shader!!
    }

}

class BoxResource() : LifecycleResource<Unit, Mesh> {
    private var mesh: Mesh? = null

    override fun initialize() {
        if (mesh == null)
            mesh = MeshBuilder().let {
                it.begin(
                    VertexAttributes(
                        VertexAttribute.Position(),
                        VertexAttribute.Normal(),
                        VertexAttribute.TexCoords(0)
                    ), GL20.GL_TRIANGLES
                )
                BoxShapeBuilder.build(it, 1f, 1f, 1f)
                it.end()
            }
    }

    override fun dispose() {
        mesh?.dispose()
        mesh = null
    }

    override fun get(argsResource: Unit): Mesh {
        return requireNotNull(mesh)
    }
}

class RawTextureResource(val location: () -> FileHandle) : LifecycleResource<Unit, Texture> {
    /**
     * The texture that is active.
     */
    private var texture: Texture? = null

    override fun initialize() {
        if (texture == null)
            texture = Texture(location())

    }

    override fun dispose() {
        texture?.dispose()
        texture = null
    }

    override fun get(argsResource: Unit): Texture {
        return requireNotNull(texture)
    }
}

class ObjectResource<T>(
    val meshResource: Resource<T, Mesh>,
    val textureResource: Resource<T, Texture>,
    val shaderProgramResource: Resource<T, ShaderProgram>,
    val transform: String? = null,
    val projection: String? = null,
    val color: String? = null,
    val material: String? = null
) : Resource<T, Drawable<Unit>> {

    override fun get(argsResource: T): Drawable<Unit> {
        val mesh = meshResource[argsResource]
        val texture = textureResource[argsResource]
        val shaderProgram = shaderProgramResource[argsResource]

        return object : Drawable<Unit> {
            override fun draw(args: Unit, time: Double, context: Context) {
                // TODO: Maybe pull in context.
                material?.let {
                    texture.bind()
                    shaderProgram.setUniformi(it, 0)
                }

                context.shader(shaderProgram, transform, projection, color)
                mesh.render(shaderProgram, GL20.GL_TRIANGLES)

            }

        }
    }

}
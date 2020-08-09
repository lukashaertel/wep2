package eu.metatools.fio2

import com.badlogic.gdx.graphics.glutils.ShaderProgram

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
package eu.metatools.sx.ents

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.maltaisn.msdfgdx.FontStyle
import com.maltaisn.msdfgdx.MsdfFont
import com.maltaisn.msdfgdx.MsdfShader
import eu.metatools.reaktor.gdx.shapes.Fill
import eu.metatools.reaktor.gdx.shapes.Line
import eu.metatools.reaktor.gdx.shapes.RectDrawable
import eu.metatools.reaktor.gdx.shapes.RectRoundedDrawable
import eu.metatools.reaktor.gdx.utils.hex

object WorldRes {
    val msdfShader by lazy { MsdfShader() }

    val msdfFont by lazy {
        MsdfFont(Gdx.files.internal("sx/res/BarlowSemiCondensed-Regular.fnt"), 32f, 5f)
    }

    val whiteBorder by lazy {
        RectRoundedDrawable(Line, 4f, Color.WHITE)
    }

    val fontWhite by lazy {
        FontStyle()
                .setColor("#ffffff".hex)
                .setShadowOffset(Vector2(1f, 1f))
                .setShadowColor("#00000060".hex)
                .setSize(18f)
    }

    val textureWhite by lazy {
        TextureRegionDrawable(Texture(Pixmap(32, 32, Pixmap.Format.RGBA8888).apply {
            setColor(Color.WHITE)
            fill()
        }))
    }

    val boxDrawable by lazy {
        RectDrawable(Fill, Color.GRAY)
    }

    val roundDrawable by lazy {
        RectRoundedDrawable(Fill, Float.MAX_VALUE, Color.WHITE)
    }

    val roundDrawableRed by lazy {
        RectRoundedDrawable(Fill, Float.MAX_VALUE, Color.RED)
    }
}
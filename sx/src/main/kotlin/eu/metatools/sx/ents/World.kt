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
import eu.metatools.reaktor.gdx.*
import eu.metatools.reaktor.gdx.data.ExtentValues
import eu.metatools.reaktor.gdx.data.Extents
import eu.metatools.reaktor.gdx.shapes.Line
import eu.metatools.reaktor.gdx.shapes.RectRoundedDrawable
import eu.metatools.reaktor.gdx.utils.hex
import eu.metatools.sx.SX
import eu.metatools.up.Ent
import eu.metatools.up.Shell
import eu.metatools.up.dsl.propObserved
import eu.metatools.up.dsl.setObserved
import eu.metatools.up.dt.Lx
import eu.metatools.up.list

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
                .setSize(24f)
    }

    val textureWhite by lazy {
        TextureRegionDrawable(Texture(Pixmap(32, 32, Pixmap.Format.RGBA8888).apply {
            setColor(Color.WHITE)
            fill()
        }))
    }
}

interface Reakted {
    fun render(): VActor<*>
}

interface TopBar {
    fun render(): VActor<*>
}

class World(shell: Shell, id: Lx, val sx: SX) : Ent(shell, id), Reakted {
    val players by setObserved<Player> { sx.updateUi() }

    var uiState: Int by propObserved({ 0 }, 0) { sx.updateUi() }

    override fun render() =
            VTable(fillParent = true) {
                cells {
                    +VCell(row = 0, expandX = 1, fillX = 1f, fillY = 1f) {
                        +VContainer(background = WorldRes.whiteBorder, pad = ExtentValues(4f), fillX = 1f) {
                            actor {
                                +VHorizontalGroup(pad = Extents(8f, 2f), space = 20f) {
                                    shell.list<TopBar>().forEach {
                                        receive(it.render())
                                    }
                                }
                            }
                        }
                    }
                    +VCell(row = 1, expandY = 1) {
                        +VContainer(fillParent = true)
                    }
                    +VCell(row = 2, expandX = 1, fillX = 1f)
                }
            }

    /**
     * Periodic world update.
     */
    val worldUpdate = repeating(Short.MAX_VALUE, 2000L, shell::initializedTime) {
        uiState++

        if (uiState == 3) {
            players.add(constructed(Player(shell, newId(), sx)))
        }
    }

    /**
     * Renders all actors.
     */
    fun render(time: Double, delta: Double) {

    }
}
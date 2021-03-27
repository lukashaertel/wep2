package eu.metatools.sx.ents

import eu.metatools.reaktor.gdx.VMsdfLabel
import eu.metatools.sx.SX
import eu.metatools.up.Ent
import eu.metatools.up.Shell
import eu.metatools.up.dt.Lx

class Player(
        shell: Shell,
        id: Lx,
        sx: SX
) : Ent(shell, id), TopBar {
    override fun render() = VMsdfLabel(
            text = "Player $id",
            shader = WorldRes.msdfShader, font = WorldRes.msdfFont,
            fontStyle = WorldRes.fontWhite
    )

//    override fun render() = VImage(
//            drawable = WorldRes.textureWhite,
//            fillParent = true,
//    )

}
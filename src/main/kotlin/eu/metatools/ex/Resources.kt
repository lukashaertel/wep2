package eu.metatools.ex

import com.badlogic.gdx.Gdx
import eu.metatools.f2d.tools.AtlasResource
import eu.metatools.f2d.tools.SolidResource
import eu.metatools.f2d.tools.TextResource
import eu.metatools.f2d.tools.findDefinitions

object Resources {
    val solid by lazy { frontend.use(SolidResource()) }

    val terrain by lazy { frontend.use(AtlasResource {
        Gdx.files.internal(
            "terrain.atlas"
        )
    }) }

    val segoe by lazy { frontend.use(TextResource {
        findDefinitions(
            Gdx.files.internal("segoe_ui")
        )
    }) }

    val consolas by lazy { frontend.use(TextResource {
        findDefinitions(
            Gdx.files.internal("consolas")
        )
    }) }

}
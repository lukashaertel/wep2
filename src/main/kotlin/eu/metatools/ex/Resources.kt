package eu.metatools.ex

import com.badlogic.gdx.Gdx
import eu.metatools.f2d.tools.*

object Resources {
    val solid by lazy { frontend.use(SolidResource()) }

    val data by lazy { frontend.use(DataResource()) }

    val fire by lazy {
        frontend.use(SoundResource {
            Gdx.files.internal("shoot.ogg")
        })
    }

    val atlas by lazy {
        frontend.use(AtlasResource {
            Gdx.files.internal(
                "CTP.atlas"
            )
        })
    }

    val segoe by lazy {
        frontend.use(TextResource {
            findDefinitions(
                Gdx.files.internal("segoe_ui")
            )
        })
    }

    val consolas by lazy {
        frontend.use(TextResource {
            findDefinitions(
                Gdx.files.internal("consolas")
            )
        })
    }

}
package eu.metatools.ex

import com.badlogic.gdx.Gdx
import eu.metatools.f2d.tools.*

object Resources {
    val solid by lazy { frontend.use(SolidResource()) }
    val shapes by lazy { frontend.use(ShapeResource()) }

    val data by lazy { frontend.use(DataResource()) }

    val fire by lazy {
        frontend.use(SoundResource {
            Gdx.files.internal("fire.wav")
        })
    }

    val terrain by lazy {
        frontend.use(AtlasResource {
            Gdx.files.internal(
                "terrain.atlas"
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
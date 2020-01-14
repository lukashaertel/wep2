package eu.metatools.ex

import com.badlogic.gdx.Gdx
import eu.metatools.f2d.drawable.over
import eu.metatools.f2d.drawable.shift
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

/**
 * Refers to the terrain atlas lazily.
 */
fun atlas(name: String) =
    lazy { Resources.atlas[Static(name)] }

/**
 * Refers to the terrain atlas lazily.
 */
fun atlas(first: String, overlay: String) =
    lazy {
        Resources.atlas[Static(overlay)] over
                Resources.atlas[Static(
                    first
                )]
    }

/**
 * Refers to the terrain atlas lazily.
 */
fun atlasStack(lower: String, upper: String) =
    lazy {
        Resources.atlas[Static(upper)].shift(0f, 1f) over
                Resources.atlas[Static(
                    lower
                )]
    }

/**
 * Refers to the terrain atlas lazily.
 */
fun animation(length: Double, vararg frames: String) =
    lazy {
        Resources.atlas[Frames(
            frames.toList(),
            length
        )]
    }

/**
 * Refers to the terrain atlas lazily.
 */
fun animation(length: Double, name: String) =
    lazy {
        Resources.atlas[Animated(
            name,
            length
        )]
    }
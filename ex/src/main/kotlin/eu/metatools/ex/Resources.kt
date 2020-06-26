package eu.metatools.ex

import com.badlogic.gdx.Gdx
import eu.metatools.fio.drawable.over
import eu.metatools.fio.drawable.shift
import eu.metatools.fio.tools.*

object Resources {
    val solid by lazy { ex.use(SolidResource()) }

    val data by lazy { ex.use(DataResource()) }

    val fire by lazy {
        ex.use(SoundResource {
            Gdx.files.internal("ex/res/shoot.ogg")
        })
    }

    val atlas by lazy {
        ex.use(AtlasResource {
            Gdx.files.internal(
                "ex/res/CTP.atlas"
            )
        })
    }

    val segoe by lazy {
        ex.use(TextResource {
            findDefinitions(
                Gdx.files.internal("res/segoe_ui")
            )
        })
    }

    val consolas by lazy {
        ex.use(TextResource {
            findDefinitions(
                Gdx.files.internal("res/consolas")
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
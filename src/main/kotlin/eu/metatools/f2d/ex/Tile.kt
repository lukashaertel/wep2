package eu.metatools.f2d.ex

import eu.metatools.f2d.context.Drawable
import eu.metatools.f2d.tools.Static
import java.io.Serializable

interface TileKind {
    val visual: Drawable<Unit?>

    val passable: Boolean
}

enum class Tiles : TileKind, Serializable {
    A {
        override val visual by lazy {
            Resources.terrain.get(Static("tile390"))
        }

        override val passable: Boolean
            get() = true

    },
    B {
        override val visual by lazy {
            Resources.terrain.get(Static("tile702"))
        }
        override val passable: Boolean
            get() = false

    }
}
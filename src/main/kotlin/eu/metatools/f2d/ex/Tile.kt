package eu.metatools.f2d.ex

import eu.metatools.f2d.context.Drawable
import eu.metatools.f2d.tools.Static

interface TileKind {
    val visual: Drawable<Unit?>

    val passable: Boolean
}

enum class Tiles : TileKind {
    A {
        override val visual by lazy {
            Resources.terrain[Static("tile390")]
        }

        override val passable: Boolean
            get() = true

    },
    B {
        override val visual by lazy {
            Resources.terrain[Static("tile702")]
        }
        override val passable: Boolean
            get() = false

    }
}
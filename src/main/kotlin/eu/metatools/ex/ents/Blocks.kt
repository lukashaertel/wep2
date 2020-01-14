package eu.metatools.ex.ents

import eu.metatools.ex.Resources
import eu.metatools.f2d.data.Q
import eu.metatools.f2d.data.Tri
import eu.metatools.f2d.data.plus
import eu.metatools.f2d.data.toQ
import eu.metatools.f2d.drawable.Drawable
import eu.metatools.f2d.drawable.over
import eu.metatools.f2d.drawable.shift
import eu.metatools.f2d.tools.Animated
import eu.metatools.f2d.tools.Frames
import eu.metatools.f2d.tools.Static

interface Block {
    val body: Drawable<Unit?>? get() = null

    val cap: Drawable<Unit?>? get() = null

    val solid: Boolean get() = true

    val walkable: Boolean get() = true

    val extras: Map<out Any, Any> get() = emptyMap()

    val intermediate: Boolean get() = false

    val lift: Int get() = 0

    fun height(x: Number, y: Number): Number = 0
}

/**
 * True if the block at the position is intermediate.
 */
fun Map<Tri, Block>.intermediate(level: Int, x: Number, y: Number): Boolean {
    val qx = x.toQ()
    val qy = y.toQ()

    val ax = (qx + Q.HALF).floor()
    val ay = (qy + Q.HALF).floor()

    val at = Tri(ax, ay, level)
    return get(at)?.intermediate ?: false
}

/**
 * Level raising or lowering per position.
 */
fun Map<Tri, Block>.lift(level: Int, x: Number, y: Number): Int {
    val qx = x.toQ()
    val qy = y.toQ()

    val ax = (qx + Q.HALF).floor()
    val ay = (qy + Q.HALF).floor()

    val at = Tri(ax, ay, level)
    return get(at)?.lift ?: 0
}

/**
 * Gets the absolute height at the position.
 */
fun Map<Tri, Block>.height(level: Int, x: Number, y: Number): Number {
    val qx = x.toQ()
    val qy = y.toQ()

    val ax = (qx + Q.HALF).floor()
    val ay = (qy + Q.HALF).floor()

    val dx = qx - ax.toQ()
    val dy = qy - ay.toQ()

    val at = Tri(ax, ay, level)
    return get(at)?.height(dx, dy)?.plus(level.toQ()) ?: level.toQ()
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
                Resources.atlas[Static(first)]
    }

/**
 * Refers to the terrain atlas lazily.
 */
fun atlasStack(lower: String, upper: String) =
    lazy {
        Resources.atlas[Static(upper)].shift(0f, 1f) over
                Resources.atlas[Static(lower)]
    }

/**
 * Refers to the terrain atlas lazily.
 */
fun animation(length: Double, vararg frames: String) =
    lazy {
        Resources.atlas[Frames(frames.toList(), length)]
    }

/**
 * Refers to the terrain atlas lazily.
 */
fun animation(length: Double, name: String) =
    lazy {
        Resources.atlas[Animated(name, length)]
    }


enum class Blocks : Block {
    Brick {
        override val body by atlas("brick")
        override val cap by atlas("brick_cap")
    },
    Slab {
        override val cap by atlas("slab")

        override val extras = mapOf("RSP" to true)
    },
    Stone {
        override val body by atlas("stone")
        override val cap by atlas("stone_cap")
    },
    StoneDoor {
        override val body by atlas("stone", "door")
        override val cap by atlas("stone_cap")
    },
    Dirt {
        override val body by atlas("dirt")
        override val cap by atlas("dirt_cap")
    },
    Grass {
        override val body by atlas("dirt")
        override val cap by atlasStack("grass_lower", "grass_upper")
    },
    Tree {
        override val body by atlas("tree")
        override val cap by atlas("tree_cap")
    },
    Chest {
        override val body by atlas("chest")
        override val extras = mapOf("container" to true)
        override val walkable = false
    },
    Crate {
        override val body by atlas("crate")
        override val extras = mapOf("container" to true)
        override val walkable = false
    },
    BigCrate {
        override val body by atlasStack("big_crate_lower", "big_crate_upper")
        override val extras = mapOf("container" to true)
        override val walkable = false
    },
    Fire {
        override val body by animation(0.3, "fire")
        override val walkable = false
    },
    BridgeH {
        override val body by atlas("bridge_h")
        override val cap by atlas("bridge_h_cap")
        override val solid = false
    },
    BridgeV {
        override val body by atlas("bridge_v")
        override val cap by atlas("bridge_v_cap")
        override val solid = false
    },
    StairsLeftUL {
        override val solid = false
        override val walkable = false
        override fun height(x: Number, y: Number) =
            (-Q.HALF - x.toQ()) / Q.TWO
    },
    StairsLeftUR {
        override val solid = false
        override val walkable = false
        override val intermediate = true
        override val lift = -1
        override fun height(x: Number, y: Number) =
            (-Q.HALF - x.toQ()) / Q.TWO - Q.HALF
    },
    StairsLeftLL {
        override val body by atlas("stairs_left_ll")
        override val cap by atlas("stairs_left_ul")
        override val solid = false
        override val walkable = true
        override val intermediate = true
        override val lift = 1
        override fun height(x: Number, y: Number) =
            (-Q.HALF - x.toQ()) / Q.TWO + Q.ONE
    },
    StairsLeftLR {
        override val body by atlas("stairs_left_lr")
        override val cap by atlas("stairs_left_ur")
        override val solid = false
        override val walkable = true
        override fun height(x: Number, y: Number) =
            (-Q.HALF - x.toQ()) / Q.TWO + Q.HALF
    },
    StairsRightUL {
        override val solid = false
        override val walkable = false
        override val intermediate = true
        override val lift = -1
        override fun height(x: Number, y: Number) =
            StairsLeftUR.height(-x.toQ(), y)
    },
    StairsRightUR {
        override val solid = false
        override val walkable = false
        override fun height(x: Number, y: Number) =
            StairsLeftUL.height(-x.toQ(), y)
    },
    StairsRightLL {
        override val body by atlas("stairs_right_ll")
        override val cap by atlas("stairs_right_ul")
        override val solid = false
        override val walkable = true
        override fun height(x: Number, y: Number) =
            StairsLeftLR.height(-x.toQ(), y)
    },
    StairsRightLR {
        override val body by atlas("stairs_right_lr")
        override val cap by atlas("stairs_right_ur")
        override val solid = false
        override val walkable = true
        override val intermediate = true
        override val lift = 1
        override fun height(x: Number, y: Number) =
            StairsLeftLL.height(-x.toQ(), y)
    }
}

fun MutableMap<Tri, Block>.stairsLeft(x: Int, y: Int, z: Int) {
    put(Tri(x, y, z + 1), Blocks.StairsLeftUL)
    put(Tri(x + 1, y, z + 1), Blocks.StairsLeftUR)
    put(Tri(x, y, z), Blocks.StairsLeftLL)
    put(Tri(x + 1, y, z), Blocks.StairsLeftLR)
}

fun MutableMap<Tri, Block>.stairsRight(x: Int, y: Int, z: Int) {
    put(Tri(x - 1, y, z + 1), Blocks.StairsRightUL)
    put(Tri(x, y, z + 1), Blocks.StairsRightUR)
    put(Tri(x - 1, y, z), Blocks.StairsRightLL)
    put(Tri(x, y, z), Blocks.StairsRightLR)
}
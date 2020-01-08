package eu.metatools.ex.ents

import eu.metatools.ex.Resources
import eu.metatools.f2d.data.Q
import eu.metatools.f2d.data.Tri
import eu.metatools.f2d.data.plus
import eu.metatools.f2d.data.toQ
import eu.metatools.f2d.drawable.Drawable
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

    val ax = qx + Q.HALF
    val ay = qy + Q.HALF

    val at = Tri(ax.toInt(), ay.toInt(), level)
    return get(at)?.intermediate ?: false
}

/**
 * Level raising or lowering per position.
 */
fun Map<Tri, Block>.lift(level: Int, x: Number, y: Number): Int {
    val qx = x.toQ()
    val qy = y.toQ()

    val ax = (qx + Q.HALF).toInt()
    val ay = (qy + Q.HALF).toInt()

    val at = Tri(ax, ay, level)
    return get(at)?.lift ?: 0
}

/**
 * Gets the absolute height at the position.
 */
fun Map<Tri, Block>.height(level: Int, x: Number, y: Number): Number {
    val qx = x.toQ()
    val qy = y.toQ()

    val ax = (qx + Q.HALF).toInt()
    val ay = (qy + Q.HALF).toInt()

    val dx = qx - ax.toQ()
    val dy = qy - ay.toQ()

    val at = Tri(ax, ay, level)
    return get(at)?.height(dx, dy)?.plus(level.toQ()) ?: level.toQ()
}

/**
 * Refers to the terrain atlas lazily.
 */
fun terrain(name: String) =
    lazy { Resources.terrain[Static(name)] }

enum class T2s : Block {
    Floor {
        override val body by terrain("tile518")
        override val cap by terrain("tile521")

        override val extras = mapOf("RSP" to true)
    },
    Wall {
        override val body by terrain("tile491")
        override val cap by terrain("tile452")
    },
    StairEntryU {
        override val solid = false
        override val walkable = false
        override val lift = -1
        override fun height(x: Number, y: Number) =
            -Q.ONE
    },
    StairEntryL {
        override val solid = false
    },
    StairsLeftUL {
        override val body by terrain("tile585")
        override val solid = false
        override val walkable = false
        override val intermediate = true
        override fun height(x: Number, y: Number) =
            (-Q.HALF - x.toQ()) / Q.TWO
    },
    StairsLeftUR {
        override val body by terrain("tile586")
        override val solid = false
        override val walkable = false
        override val intermediate = true

        override fun height(x: Number, y: Number) =
            (-Q.HALF - x.toQ()) / Q.TWO - Q.HALF
    },
    StairsLeftLL {
        override val body by terrain("tile617")
        override val solid = true
        override val walkable = true
        override val intermediate = true
        override fun height(x: Number, y: Number) =
            (-Q.HALF - x.toQ()) / Q.TWO + Q.ONE
    },
    StairsLeftLR {
        override val body by terrain("tile618")
        override val solid = false
        override val walkable = true
        override val intermediate = true
        override val lift = 1
        override fun height(x: Number, y: Number) =
            (-Q.HALF - x.toQ()) / Q.TWO + Q.HALF
    }
//    StairsRightA {
//        override val visual by lazy {
//            Resources.terrain[Static(
//                "tile615"
//            )]
//        }
//        override val cap by lazy {
//            Resources.terrain[Static(
//                "tile583"
//            )]
//        }
//        override val walkable = Dir.Left and Dir.Right
//        override val passable = T2.allDirs
//
//        override fun heightAt(dpt: RealPt) =
//            Real.HALF + (dpt.x - Real.HALF) / Real.TWO
//    },
//    StairsRightB {
//        override val visual by lazy {
//            Resources.terrain[Static(
//                "tile616"
//            )]
//        }
//        override val cap by lazy {
//            Resources.terrain[Static(
//                "tile584"
//            )]
//        }
//        override val walkable = Dir.Left and Dir.Right
//        override val passable = T2.allDirs
//        override val thresholds = only(Dir.Right)
//
//        override fun heightAt(dpt: RealPt) =
//            Real.ONE + (dpt.x - Real.HALF) / Real.TWO
//    }
}
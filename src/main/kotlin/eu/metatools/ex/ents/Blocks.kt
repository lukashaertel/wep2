package eu.metatools.ex.ents

import eu.metatools.ex.animation
import eu.metatools.ex.atlas
import eu.metatools.ex.atlasStack
import eu.metatools.f2d.data.Q
import eu.metatools.f2d.data.Tri
import eu.metatools.f2d.data.toQ

/**
 * Enum of shared block types.
 */
enum class Blocks : Block {
    /**
     * Brig ground.
     */
    Brick {
        override val body by atlas("brick")
        override val cap by atlas("brick_cap")
    },

    /**
     * Slab that has "RSP" set to true and spawns items.
     */
    Slab {
        override val cap by atlas("slab")

        override val extras = mapOf("RSP" to true)
    },
    /**
     * Stone wall and ground (bigger bricks).
     */
    Stone {
        override val body by atlas("stone")
        override val cap by atlas("stone_cap")
    },
    /**
     * Stone wall with a door decal.
     */
    StoneDoor {
        override val body by atlas("stone", "door")
        override val cap by atlas("stone_cap")
    },
    /**
     * Dirt block.
     */
    Dirt {
        override val body by atlas("dirt")
        override val cap by atlas("dirt_cap")
    },
    /**
     * Grass block.
     */
    Grass {
        override val body by atlas("dirt")
        override val cap by atlasStack("grass_lower", "grass_upper")
    },
    /**
     * Tree block.
     */
    Tree {
        override val body by atlas("tree")
        override val cap by atlas("tree_cap")
    },
    /**
     * Chest block.
     */
    Chest {
        override val body by atlas("chest")
        override val extras = mapOf("container" to true)
        override val walkable = false
    },
    /**
     * Crate block.
     */
    Crate {
        override val body by atlas("crate")
        override val extras = mapOf("container" to true)
        override val walkable = false
    },
    /**
     * Big crate block.
     */
    BigCrate {
        override val body by atlasStack("big_crate_lower", "big_crate_upper")
        override val extras = mapOf("container" to true)
        override val walkable = false
    },
    /**
     * Barrel.
     */
    Barrel {
        override val body by atlas("barrel")
        override val extras = mapOf("container" to true)
        override val walkable = false
    },
    /**
     * Animated fire pit.
     */
    Fire {
        override val body by animation(0.3, "fire")
        override val walkable = false
    },
    /**
     * Horizontal traverse.
     */
    BridgeH {
        override val body by atlas("bridge_h")
        override val cap by atlas("bridge_h_cap")
        override val solid = false
    },
    /**
     * Vertical traverse.
     */
    BridgeV {
        override val body by atlas("bridge_v")
        override val cap by atlas("bridge_v_cap")
        override val solid = false
    },
    /**
     * Stairs left, upper-left part.
     */
    StairsLeftUL {
        override val solid = false
        override val walkable = false
        override fun height(x: Number, y: Number) =
            (-Q.HALF - x.toQ()) / Q.TWO
    },
    /**
     * Stairs left, upper-right part.
     */
    StairsLeftUR {
        override val solid = false
        override val walkable = false
        override val intermediate = true
        override val lift = -1
        override fun height(x: Number, y: Number) =
            (-Q.HALF - x.toQ()) / Q.TWO - Q.HALF
    },
    /**
     * Stairs left, lower-left part.
     */
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
    /**
     * Stairs left, lower-right part.
     */
    StairsLeftLR {
        override val body by atlas("stairs_left_lr")
        override val cap by atlas("stairs_left_ur")
        override val solid = false
        override val walkable = true
        override fun height(x: Number, y: Number) =
            (-Q.HALF - x.toQ()) / Q.TWO + Q.HALF
    },
    /**
     * Stairs right, upper-left part.
     */
    StairsRightUL {
        override val solid = false
        override val walkable = false
        override val intermediate = true
        override val lift = -1
        override fun height(x: Number, y: Number) =
            StairsLeftUR.height(-x.toQ(), y)
    },
    /**
     * Stairs right, upper-right part.
     */
    StairsRightUR {
        override val solid = false
        override val walkable = false
        override fun height(x: Number, y: Number) =
            StairsLeftUL.height(-x.toQ(), y)
    },
    /**
     * Stairs right, lower-right part.
     */
    StairsRightLL {
        override val body by atlas("stairs_right_ll")
        override val cap by atlas("stairs_right_ul")
        override val solid = false
        override val walkable = true
        override fun height(x: Number, y: Number) =
            StairsLeftLR.height(-x.toQ(), y)
    },
    /**
     * Stairs right, upper-left part.
     */
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

/**
 * Places left facing stairs that end at the given coordinates.
 */
fun MutableMap<Tri, Block>.stairsLeft(x: Int, y: Int, z: Int) {
    put(Tri(x, y, z + 1), Blocks.StairsLeftUL)
    put(Tri(x + 1, y, z + 1), Blocks.StairsLeftUR)
    put(Tri(x, y, z), Blocks.StairsLeftLL)
    put(Tri(x + 1, y, z), Blocks.StairsLeftLR)
}

/**
 * Places right facing stairs that end at the given coordinates.
 */
fun MutableMap<Tri, Block>.stairsRight(x: Int, y: Int, z: Int) {
    put(Tri(x - 1, y, z + 1), Blocks.StairsRightUL)
    put(Tri(x, y, z + 1), Blocks.StairsRightUR)
    put(Tri(x - 1, y, z), Blocks.StairsRightLL)
    put(Tri(x, y, z), Blocks.StairsRightLR)
}
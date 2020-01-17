package eu.metatools.ex.ents

import eu.metatools.ex.animation
import eu.metatools.ex.atlas
import eu.metatools.ex.atlasStack
import eu.metatools.f2d.data.Q
import eu.metatools.f2d.data.Tri
import eu.metatools.f2d.data.toQ


// TODO: Bridges not working anymore for now.

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
     * Stairs left, left part.
     */
    StairsLeftA {
        override val body by atlas("stairs_left_ll")
        override val cap by atlas("stairs_left_ul")
        override val walkable = false
        override fun height(x: Number, y: Number) =
            (-Q.HALF - x) / Q.TWO + Q.ONE
    },
    /**
     * Stairs left, right part.
     */
    StairsLeftB {
        override val body by atlas("stairs_left_lr")
        override val cap by atlas("stairs_left_ur")
        override val walkable = false
        override fun height(x: Number, y: Number) =
            (-Q.HALF - x) / Q.TWO + Q.HALF
    },
    /**
     * Stairs right, left part.
     */
    StairsRightA {
        override val body by atlas("stairs_right_ll")
        override val cap by atlas("stairs_right_ul")
        override val solid = StairsLeftB.solid
        override val walkable = StairsLeftB.walkable
        override fun height(x: Number, y: Number) =
            StairsLeftB.height(-x.toQ(), y)
    },
    /**
     * Stairs right, right part.
     */
    StairsRightB {
        override val body by atlas("stairs_right_lr")
        override val cap by atlas("stairs_right_ur")
        override val solid = StairsLeftA.solid
        override val walkable = StairsLeftA.walkable
        override fun height(x: Number, y: Number) =
            StairsLeftA.height(-x.toQ(), y)
    }
}

/**
 * Places left facing stairs that end at the given coordinates.
 */
fun MutableMap<Tri, Block>.stairsLeft(x: Int, y: Int, z: Int) {
    put(Tri(x, y, z), Blocks.StairsLeftA)
    put(Tri(x + 1, y, z), Blocks.StairsLeftB)
}

/**
 * Places right facing stairs that end at the given coordinates.
 */
fun MutableMap<Tri, Block>.stairsRight(x: Int, y: Int, z: Int) {
    put(Tri(x - 1, y, z), Blocks.StairsRightA)
    put(Tri(x, y, z), Blocks.StairsRightB)
}
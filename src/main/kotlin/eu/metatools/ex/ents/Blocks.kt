package eu.metatools.ex.ents

import eu.metatools.ex.animation
import eu.metatools.ex.atlas
import eu.metatools.ex.atlasStack
import eu.metatools.ex.data.Dir
import eu.metatools.ex.data.Mesh
import eu.metatools.ex.data.box
import eu.metatools.ex.data.slope
import eu.metatools.f2d.data.Tri

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
    },
    /**
     * Crate block.
     */
    Crate {
        override val body by atlas("crate")
        override val extras = mapOf("container" to true)
    },
    /**
     * Big crate block.
     */
    BigCrate {
        override val body by atlasStack("big_crate_lower", "big_crate_upper")
        override val extras = mapOf("container" to true)
    },
    /**
     * Barrel.
     */
    Barrel {
        override val body by atlas("barrel")
        override val extras = mapOf("container" to true)
    },
    /**
     * Animated fire pit.
     */
    Fire {
        override val body by animation(0.3, "fire")
    },
    /**
     * Horizontal traverse.
     */
    BridgeH {
        override val body by atlas("bridge_h")
        override val cap by atlas("bridge_h_cap")
        override fun mesh(x: Float, y: Float, z: Float) = box(x, y, z, zn = 0.4f)
    },
    /**
     * Vertical traverse.
     */
    BridgeV {
        override val body by atlas("bridge_v")
        override val cap by atlas("bridge_v_cap")
        override fun mesh(x: Float, y: Float, z: Float) = box(x, y, z, zn = 0.4f)
    },
    /**
     * Stairs left, left part.
     */
    StairsLeftA {
        override val body by atlas("stairs_left_ll")
        override val cap by atlas("stairs_left_ul")
        override fun mesh(x: Float, y: Float, z: Float) = //TODO
            super.mesh(x, y, z)
    },
    /**
     * Stairs left, right part.
     */
    StairsLeftB {
        override val body by atlas("stairs_left_lr")
        override val cap by atlas("stairs_left_ur")
        override fun mesh(x: Float, y: Float, z: Float) =//TODO
            slope(x, y, z, Dir.Left)
    },
    /**
     * Stairs right, left part.
     */
    StairsRightA {
        override val body by atlas("stairs_right_ll")
        override val cap by atlas("stairs_right_ul")
        override fun mesh(x: Float, y: Float, z: Float) =//TODO
            slope(x, y, z, Dir.Right)
    },
    /**
     * Stairs right, right part.
     */
    StairsRightB {
        override val body by atlas("stairs_right_lr")
        override val cap by atlas("stairs_right_ur")
        override fun mesh(x: Float, y: Float, z: Float) = //TODO
            super.mesh(x, y, z)
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
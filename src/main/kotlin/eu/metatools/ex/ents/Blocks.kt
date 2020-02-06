package eu.metatools.ex.ents

import eu.metatools.ex.animation
import eu.metatools.ex.atlas
import eu.metatools.ex.atlasStack
import eu.metatools.ex.geom.*
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
        override fun mesh(x: Float, y: Float, z: Float) =
            box(
                x, y, z, xn = -0.25f, xp = 0.25f,
                yn = -0.25f, yp = 0.25f, zp = -0.2f
            )
    },
    /**
     * Horizontal traverse.
     */
    BridgeH {
        override val body by atlas("bridge_h")
        override val cap by atlas("bridge_h_cap")
        override fun mesh(x: Float, y: Float, z: Float) =
            box(x, y, z, zn = 0.4f)
    },
    /**
     * Vertical traverse.
     */
    BridgeV {
        override val body by atlas("bridge_v")
        override val cap by atlas("bridge_v_cap")
        override fun mesh(x: Float, y: Float, z: Float) =
            box(x, y, z, zn = 0.4f)
    },
    /**
     * Stairs left, left part.
     */
    StairsLeftA {
        override val body by atlas("stairs_left_ll")
        override val cap by atlas("stairs_left_ul")
        override fun mesh(x: Float, y: Float, z: Float) =
            StairsRightB.mesh(x, y, z).rotate180(x, y)
    },
    /**
     * Stairs left, right part.
     */
    StairsLeftB {
        override val body by atlas("stairs_left_lr")
        override val cap by atlas("stairs_left_ur")
        override fun mesh(x: Float, y: Float, z: Float) =
            StairsRightA.mesh(x, y, z).rotate180(x, y)
    },
    /**
     * Stairs right, left part.
     */
    StairsRightA {
        override val body by atlas("stairs_right_ll")
        override val cap by atlas("stairs_right_ul")
        override fun mesh(x: Float, y: Float, z: Float) =
            slope(x, y, z, zp = 0f).filter { !isXP(it) }
    },
    /**
     * Stairs right, right part.
     */
    StairsRightB {
        override val body by atlas("stairs_right_lr")
        override val cap by atlas("stairs_right_ur")
        override fun mesh(x: Float, y: Float, z: Float) =
            slopeStump(x, y, z, zpFrom = 0f, zpTo = 0.5f).filter { !isXN(it) }
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
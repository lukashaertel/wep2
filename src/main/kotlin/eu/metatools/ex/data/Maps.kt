package eu.metatools.ex.data

import eu.metatools.ex.ents.Block
import eu.metatools.ex.ents.Blocks
import eu.metatools.ex.ents.stairsLeft
import eu.metatools.ex.ents.stairsRight
import eu.metatools.fio.data.Tri
import java.util.*


/**
 * A simple map.
 */
val basicMap = mutableMapOf<Tri, Block>().also {

    // Ground level.
    for (x in -5..15)
        for (y in -5..15)
            it[Tri(x, y, -1)] = Blocks.Grass
    for (x in 0..10)
        for (y in 0..10)
            it[Tri(x, y, -1)] = Blocks.Brick

    it[Tri(3, 7, -1)] = Blocks.Slab
    it[Tri(8, 9, -1)] = Blocks.Slab
    it[Tri(-2, 2, -1)] = Blocks.Slab
    it[Tri(12, 2, -1)] = Blocks.Slab


    for (x in 11..15)
        it[Tri(x, 8, 0)] = Blocks.BridgeH

    for (i in -5..15) {
        it[Tri(i, -5, 0)] = Blocks.Tree
        it[Tri(i, 15, 0)] = Blocks.Tree
        it[Tri(-5, i, 0)] = Blocks.Tree
        it[Tri(15, i, 0)] = Blocks.Tree
    }

    val random = Random(0L)

    for (x in -5..15) for (y in -5..15) {
        if (x in -2..12 && y in -2..12)
            continue

        if (random.nextInt(10) < 6)
            it[Tri(x, y, 0)] = Blocks.Tree
    }

    // Walling.
    for (i in 0..10) {
        it[Tri(i, 0, 0)] = Blocks.Stone
        it[Tri(i, 10, 0)] = Blocks.Stone
        it[Tri(0, i, 0)] = Blocks.Stone
        it[Tri(10, i, 0)] = Blocks.Stone
    }

    it[Tri(4, 10, 0)] = Blocks.StoneDoor

    it.remove(Tri(5, 0, 0))

    it[Tri(4, 1, 0)] = Blocks.BridgeV
    it[Tri(4, 2, 0)] = Blocks.Stone
    it[Tri(4, 3, 0)] = Blocks.Stone
    it[Tri(5, 2, 0)] = Blocks.Stone
    it[Tri(5, 3, 0)] = Blocks.Stone
    it[Tri(6, 3, 0)] = Blocks.BridgeH
    it[Tri(7, 3, 0)] = Blocks.Stone
    it[Tri(8, 3, 0)] = Blocks.BridgeH
    it[Tri(9, 3, 0)] = Blocks.BridgeH

    it[Tri(4, 3, 1)] = Blocks.Fire

    it[Tri(9, 6, 0)] = Blocks.Stone

    it.stairsLeft(1, 4, 0)
    it.stairsRight(8, 6, 0)
    it.stairsRight(-1, 4, 0)


}.toMap()
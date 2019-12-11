package eu.metatools.ex.data

import eu.metatools.ex.ents.TileKind
import eu.metatools.ex.ents.Tiles
import eu.metatools.f2d.math.Cell


/**
 * A simple map.
 */
val stupidBox = mutableMapOf<Cell, TileKind>().also {
    for (x in 0..10)
        for (y in 0..10) {
            val xy = Cell(x, y)
            it[xy] = if (x in 1..9 && y in 1..9)
                Tiles.Ground
            else
                Tiles.Wall
        }

    it[Cell(3, 2)] = Tiles.Wall
    it[Cell(3, 3)] = Tiles.Wall
    it[Cell(4, 2)] = Tiles.Wall
    it[Cell(4, 3)] = Tiles.Wall
    it[Cell(5, 3)] = Tiles.Wall
    it[Cell(5, 6)] = Tiles.Wall
}.toMap()
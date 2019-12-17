package eu.metatools.ex.data

import eu.metatools.ex.ents.TileKind
import eu.metatools.ex.ents.Tiles
import eu.metatools.f2d.math.Tri


/**
 * A simple map.
 */
val stupidBox = mutableMapOf<Tri, TileKind>().also {
    for (x in 0..10)
        for (y in 0..10) {
            val at = Tri(x, y, 0)
            if (x in 1..9 && y in 1..9) {
                it[at] = Tiles.Ground
            } else {
                it[at] = Tiles.Wall
                it[at.copy(z = 1)] = Tiles.Cover
            }
        }

    it[Tri(3, 2, 0)] = Tiles.Wall
    it[Tri(3, 3, 0)] = Tiles.Wall
    it[Tri(4, 2, 0)] = Tiles.Wall
    it[Tri(4, 3, 0)] = Tiles.Wall
    it[Tri(5, 3, 0)] = Tiles.Wall
    it[Tri(5, 6, 0)] = Tiles.Wall

    it[Tri(3, 2, 1)] = Tiles.Cover
    it[Tri(3, 3, 1)] = Tiles.Cover
    it[Tri(4, 2, 1)] = Tiles.Cover
    it[Tri(4, 3, 1)] = Tiles.Cover
    it[Tri(5, 3, 1)] = Tiles.Cover
    it[Tri(5, 6, 1)] = Tiles.Cover

    it[Tri(1, 1, 0)] = Tiles.Edge
}.toMap()
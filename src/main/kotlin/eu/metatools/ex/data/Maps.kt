package eu.metatools.ex.data

import eu.metatools.ex.ents.StandardTile
import eu.metatools.ex.ents.Tile
import eu.metatools.f2d.math.Tri


/**
 * A simple map.
 */
val stupidBox = mutableMapOf<Tri, Tile>().also {
    for (x in 0..10)
        for (y in 0..10) {
            val at = Tri(x, y, 0)
            if (x in 1..9 && y in 1..9) {
                it[at] = StandardTile.Floor
            } else {
                it[at] = StandardTile.Wall
                //it[at.copy(z = 1)] = Tiles.Cover
            }
        }

    it[Tri(4, 2, 0)] = StandardTile.Wall
    it[Tri(4, 3, 0)] = StandardTile.Wall
    it[Tri(5, 2, 0)] = StandardTile.Wall
    it[Tri(5, 3, 0)] = StandardTile.Wall
    it[Tri(7, 3, 0)] = StandardTile.Wall
    it[Tri(7, 6, 0)] = StandardTile.Wall


    it[Tri(4, 2, 1)] = StandardTile.Tile
    it[Tri(4, 3, 1)] = StandardTile.Tile
    it[Tri(5, 2, 1)] = StandardTile.Tile
    it[Tri(5, 3, 1)] = StandardTile.Tile
    it[Tri(7, 3, 1)] = StandardTile.Tile
    it[Tri(7, 6, 1)] = StandardTile.Tile

//    it[Tri(3, 2, 1)] = Tiles.Cover
//    it[Tri(3, 3, 1)] = Tiles.Cover
//    it[Tri(4, 2, 1)] = Tiles.Cover
//    it[Tri(4, 3, 1)] = Tiles.Cover
//    it[Tri(5, 3, 1)] = Tiles.Cover
//    it[Tri(5, 6, 1)] = Tiles.Cover

    it[Tri(1, 2, 0)] = StandardTile.StairsSnd
    it[Tri(2, 2, 0)] = StandardTile.StairsFst


    it[Tri(1, 2, 1)] = StandardTile.StairsTopSnd
    it[Tri(2, 2, 1)] = StandardTile.StairsTopFst

    for (x in 1..9)
        for (y in 1..9)
            it.putIfAbsent(Tri(x, y, 1), StandardTile.Blocking)

    it.remove(Tri(1, 3, 1))
    it.remove(Tri(2, 3, 1))
    it.remove(Tri(3, 2, 1))

    for (i in -1..11) {
        it[Tri(-1, i, 1)] = StandardTile.Blocking
        it[Tri(11, i, 1)] = StandardTile.Blocking
        it[Tri(i, -1, 1)] = StandardTile.Blocking
        it[Tri(i, 11, 1)] = StandardTile.Blocking
    }

}.toMap()
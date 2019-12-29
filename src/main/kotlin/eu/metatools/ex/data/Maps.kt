package eu.metatools.ex.data

import eu.metatools.ex.ents.Template
import eu.metatools.ex.ents.Tiles
import eu.metatools.f2d.math.Tri


/**
 * A simple map.
 */
val stupidBox = mutableMapOf<Tri, Template>().also {
    // Ground level.
    for (x in 0..10)
        for (y in 0..10)
            it[Tri(x, y, -1)] = Tiles.Floor

    // Walling.
    for (i in 0..10) {
        it[Tri(i, 0, 0)] = Tiles.Wall
        it[Tri(i, 10, 0)] = Tiles.Wall
        it[Tri(0, i, 0)] = Tiles.Wall
        it[Tri(10, i, 0)] = Tiles.Wall
    }

    it[Tri(4, 2, 0)] = Tiles.Wall
    it[Tri(4, 3, 0)] = Tiles.Wall
    it[Tri(5, 2, 0)] = Tiles.Wall
    it[Tri(5, 3, 0)] = Tiles.Wall
    it[Tri(7, 3, 0)] = Tiles.Wall
    it[Tri(9, 6, 0)] = Tiles.Wall


    it[Tri(1, 2, 0)] = Tiles.StairLeft
    it[Tri(1, 6, 0)] = Tiles.StairLeft
    it[Tri(7, 6, 0)] = Tiles.StairRight

    for (x in 1..9)
        for (y in 1..9)
            it[Tri(x, y, 1)] = Tiles.Clip
    
    for (i in -1..11) {
        it[Tri(-1, i, 1)] = Tiles.Clip
        it[Tri(11, i, 1)] = Tiles.Clip
        it[Tri(i, -1, 1)] = Tiles.Clip
        it[Tri(i, 11, 1)] = Tiles.Clip
    }

}.toMap()
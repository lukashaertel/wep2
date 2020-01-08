package eu.metatools.ex.data

import eu.metatools.ex.ents.Block
import eu.metatools.ex.ents.T2s
import eu.metatools.f2d.data.Tri


/**
 * A simple map.
 */
val stupidBox = mutableMapOf<Tri, Block>().also {
    // Ground level.
    for (x in 0..10)
        for (y in 0..10)
            it[Tri(x, y, -1)] = T2s.Floor

    // Walling.
    for (i in 0..10) {
        it[Tri(i, 0, 0)] = T2s.Wall
        it[Tri(i, 10, 0)] = T2s.Wall
        it[Tri(0, i, 0)] = T2s.Wall
        it[Tri(10, i, 0)] = T2s.Wall
    }

    it.remove(Tri(5, 0, 0))

    it[Tri(4, 2, 0)] = T2s.Wall
    it[Tri(4, 3, 0)] = T2s.Wall
    it[Tri(5, 2, 0)] = T2s.Wall
    it[Tri(5, 3, 0)] = T2s.Wall
    it[Tri(7, 3, 0)] = T2s.Wall
    it[Tri(9, 6, 0)] = T2s.Wall


    it[Tri(1, 4, 1)] = T2s.StairsLeftUL
    it[Tri(2, 4, 1)] = T2s.StairsLeftUR
    it[Tri(3, 4, 1)] = T2s.StairEntryU
    it[Tri(1, 4, 0)] = T2s.StairsLeftLL
    it[Tri(2, 4, 0)] = T2s.StairsLeftLR
    it[Tri(3, 4, 0)] = T2s.StairEntryL
}.toMap()
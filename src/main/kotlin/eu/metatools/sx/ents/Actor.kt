package eu.metatools.sx.ents

import eu.metatools.sx.SX
import eu.metatools.up.Ent
import eu.metatools.up.Shell
import eu.metatools.up.dsl.propObserved
import eu.metatools.up.dt.Lx

//
//class Actor(
//    shell: Shell,
//    id: Lx,
//    val sx: SX,
//    val def: Def
//) : Ent(shell, id) {
//    override val extraArgs = mapOf(
//        "def" to def
//    )
//
//    private var attached = false
//    fun attach() {
//        sx.root.index.put(this, state)
//    }
//
//    val state by propObserved({ def.initial }, def.initial) { (from, to) ->
//        sx.root.index.put(this, to)
//    }
//
//    fun detach() {
//        sx.root.index.remove(this)
//    }
//
//
//}
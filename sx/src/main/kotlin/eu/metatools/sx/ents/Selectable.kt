package eu.metatools.sx.ents

import eu.metatools.up.Ent
import eu.metatools.up.Shell
import eu.metatools.up.list

interface Selectable {
    var selected: Boolean
}

val Shell.selection
    get() = list<Selectable>().firstOrNull {
        it.selected
    }

fun Shell.deselect() {
    list<Selectable>().forEach {
        it.selected = false
    }
}

fun Selectable.select() {
    require(this is Ent)
    shell.list<Selectable>().forEach {
        it.selected = this === it
    }
}
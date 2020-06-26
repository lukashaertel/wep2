package eu.metatools.up

import eu.metatools.up.dt.Lx

/**
 * Output of [Shell.store]. Takes a fully qualified name and the value to save.
 */
typealias ShellOut = (Lx, Any?) -> Unit

/**
 * Input of [Shell.load]. Takes a fully qualified name to load.
 */
typealias ShellIn = (Lx) -> Any?

/**
 * Output of [Driver.persist]. Takes the part name, the part-relative key and the value to save.
 */
typealias EntOut = (String, String, Any?) -> Unit

/**
 * Input of [Driver.connect]. Takes the part name and the part-relative key to load.
 */
typealias EntIn = (String, String) -> Any?

/**
 * Output of [Part.persist]. Takes the part relative key and the value to save.
 */
typealias PartOut = (String, Any?) -> Unit

/**
 * Input of [Part.connect]. Takes the relative key to load.
 */
typealias PartIn = (String) -> Any?
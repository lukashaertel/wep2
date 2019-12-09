package eu.metatools.up

import eu.metatools.up.dt.Lx

typealias ShellOut = (Lx, Any?) -> Unit

typealias ShellIn = (Lx) -> Any?

typealias EntOut = (String, String, Any?) -> Unit

typealias EntIn = (String, String) -> Any?

typealias PartOut = (String, Any?) -> Unit

typealias PartIn = (String) -> Any?
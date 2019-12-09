package eu.metatools.up.dt

import java.lang.Exception

class InconsistencyException(
    message: String,
    val unassignedIn: Set<Lx>,
    val unassignedOut: Set<Lx>,
    val valueDifference: Map<Lx, Pair<Any?, Any?>>
) : Exception(message)
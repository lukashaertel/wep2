package eu.metatools.wep2.util

import java.io.InputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.OutputStream

/**
 * Object input stream able to resolve stand-in objects.
 */
class ResolvingObjectInputStream(input: InputStream, val resolve: (Any?) -> Any?) : ObjectInputStream(input) {
    init {
        enableResolveObject(true)
    }

    override fun resolveObject(obj: Any?) =
        resolve(obj)
}

/**
 * Object output stream able to replace with stand-in objects.
 */
class ReplacingObjectOutputStream(output: OutputStream, val replace: (Any?) -> Any?) : ObjectOutputStream(output) {
    init {
        enableReplaceObject(true)
    }

    override fun replaceObject(obj: Any?) =
        replace(obj)
}
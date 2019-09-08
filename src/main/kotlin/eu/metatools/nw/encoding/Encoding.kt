package eu.metatools.nw.encoding

import eu.metatools.wep2.system.StandardInitializer
import eu.metatools.wep2.system.StandardName
import eu.metatools.wep2.tools.Time
import java.io.InputStream
import java.io.OutputStream

interface Encoding<N, P> {
    /**
     * Writes the initializer.
     */
    fun writeInitializer(output: OutputStream, standardInitializer: StandardInitializer<N, P>)

    /**
     * Reads the initializer.
     */
    fun readInitializer(input: InputStream): StandardInitializer<N, P>

    /**
     * Writes the instruction.
     */
    fun writeInstruction(output: OutputStream, instruction: Triple<StandardName<N>, Time, Any?>)

    /**
     * Reads the instruction.
     */
    fun readInstruction(input: InputStream): Triple<StandardName<N>, Time, Any?>
}
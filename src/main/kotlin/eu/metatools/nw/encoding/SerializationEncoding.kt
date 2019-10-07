package eu.metatools.nw.encoding

import eu.metatools.wep2.system.StandardName
import eu.metatools.wep2.tools.Time
import eu.metatools.wep2.util.ReplacingObjectOutputStream
import eu.metatools.wep2.util.ResolvingObjectInputStream
import java.io.InputStream
import java.io.OutputStream

open class SerializationEncoding<N, P> : Encoding<N, P> {
    /**
     * Converts the input before writing to bytes.
     */
    open fun replace(element: Any?) =
        element

    /**
     * Converts the result before reading from bytes.
     */
    open fun resolve(element: Any?) =
        element

    override fun writeInitializer(output: OutputStream, data: Map<String, Any?>) =
        writeInitializer(ReplacingObjectOutputStream(output, this::replace), data)

    private fun writeInitializer(output: ReplacingObjectOutputStream, data: Map<String, Any?>) {
        output.writeObject(data)
    }

    override fun readInitializer(input: InputStream) =
        readInitializer(ResolvingObjectInputStream(input, this::resolve))


    private fun readInitializer(input: ResolvingObjectInputStream): Map<String, Any?> {
        @Suppress("unchecked_cast")
        return input.readObject() as Map<String, Any?>
    }

    override fun writeInstruction(output: OutputStream, instruction: Triple<StandardName<N>, Time, Any?>) =
        writeInstruction(ReplacingObjectOutputStream(output, this::replace), instruction)


    private fun writeInstruction(output: ReplacingObjectOutputStream, instruction: Triple<StandardName<N>, Time, Any?>) {
        output.writeObject(instruction)
    }

    override fun readInstruction(input: InputStream) =
        readInstruction(ResolvingObjectInputStream(input, this::resolve))

    private fun readInstruction(input: ResolvingObjectInputStream): Triple<StandardName<N>, Time, Any?> {
        @Suppress("unchecked_cast")
        return input.readObject() as Triple<StandardName<N>, Time, Any?>
    }
}
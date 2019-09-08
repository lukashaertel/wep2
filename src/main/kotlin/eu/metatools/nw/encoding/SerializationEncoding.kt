package eu.metatools.nw.encoding

import eu.metatools.wep2.system.StandardInitializer
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

    override fun writeInitializer(output: OutputStream, standardInitializer: StandardInitializer<N, P>) =
        writeInitializer(ReplacingObjectOutputStream(output, this::replace), standardInitializer)

    fun writeInitializer(output: ReplacingObjectOutputStream, standardInitializer: StandardInitializer<N, P>) {
        output.writeObject(standardInitializer.playerHead)
        output.writeObject(standardInitializer.playerRecycled)
        output.writeObject(standardInitializer.idsHead)
        output.writeObject(standardInitializer.idsRecycled)
        output.writeObject(standardInitializer.playerSelf)
        output.writeObject(standardInitializer.playerCount)
        output.writeObject(standardInitializer.scopes)
        output.writeObject(standardInitializer.instructions)
        output.writeObject(standardInitializer.parameter)
        output.writeObject(standardInitializer.saveData)
    }

    override fun readInitializer(input: InputStream) =
        readInitializer(ResolvingObjectInputStream(input, this::resolve))


    fun readInitializer(input: ResolvingObjectInputStream): StandardInitializer<N, P> {
        val playerHead = input.readObject()
        val playerRecycled = input.readObject()
        val idsHead = input.readObject()
        val idsRecycled = input.readObject()
        val playerSelf = input.readObject()
        val playerCount = input.readObject()
        val scopes = input.readObject()
        val instructions = input.readObject()
        val parameter = input.readObject()
        val saveData = input.readObject()

        @Suppress("unchecked_cast")
        return StandardInitializer(
            playerHead as Short?,
            playerRecycled as List<Pair<Short, Short>>,
            idsHead as Short?,
            idsRecycled as List<Pair<Short, Short>>,
            playerSelf as Pair<Short, Short>,
            playerCount as Short,
            scopes as Map<Long, Byte>,
            instructions as List<Triple<StandardName<N>, Time, Any?>>,
            parameter as P,
            saveData as Map<String, Any?>
        )
    }

    override fun writeInstruction(output: OutputStream, instruction: Triple<StandardName<N>, Time, Any?>) =
        writeInstruction(ReplacingObjectOutputStream(output, this::replace), instruction)


    fun writeInstruction(output: ReplacingObjectOutputStream, instruction: Triple<StandardName<N>, Time, Any?>) {
        output.writeObject(instruction)
    }

    override fun readInstruction(input: InputStream) =
        readInstruction(ResolvingObjectInputStream(input, this::resolve))

    fun readInstruction(input: ResolvingObjectInputStream): Triple<StandardName<N>, Time, Any?> {
        @Suppress("unchecked_cast")
        return input.readObject() as Triple<StandardName<N>, Time, Any?>
    }
}
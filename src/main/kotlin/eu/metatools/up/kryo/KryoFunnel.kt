@file:Suppress("UnstableApiUsage")

package eu.metatools.up.kryo

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Output
import com.esotericsoftware.kryo.util.Pool
import com.google.common.hash.*
import eu.metatools.up.Ent
import eu.metatools.up.Part
import eu.metatools.up.Shell
import eu.metatools.up.dt.Lx
import eu.metatools.up.dt.div
import java.io.OutputStream
import java.util.*
import kotlin.math.roundToInt

/**
 * Uses a [Kryo] pool to funnel primitives.
 */
class KryoFunnel(val kryoPool: Pool<Kryo>) : Funnel<Any?> {
    /**
     * Redirects data writes to the primitive sink [target].
     */
    private class OutputPrimitiveSink(val target: PrimitiveSink) : Output() {
        val surrogatePrecision = 100.0

        /**
         * Puts surrogates for rounding errors.
         */
        private fun PrimitiveSink.putFloatSurrogate(float: Float) =
            putInt((float * surrogatePrecision).roundToInt())

        /**
         * Puts surrogates for rounding errors.
         */
        private fun PrimitiveSink.putDoubleSurrogate(double: Double) =
            putInt((double * surrogatePrecision).roundToInt())

        override fun writeShort(value: Int) {
            target.putShort(value.toShort())
        }

        override fun writeString(value: String) {
            target.putString(value, Charsets.UTF_8)
        }

        override fun writeBytes(bytes: ByteArray) {
            target.putBytes(bytes)
        }

        override fun writeBytes(bytes: ByteArray, offset: Int, count: Int) {
            target.putBytes(bytes, offset, count)
        }

        override fun writeFloats(array: FloatArray, offset: Int, count: Int) {
            for (i in 0 until count)
                target.putFloatSurrogate(array[offset + i])
        }

        override fun writeDoubles(array: DoubleArray, offset: Int, count: Int) {
            for (i in 0 until count)
                target.putDoubleSurrogate(array[offset + i])
        }

        override fun write(value: Int) {
            target.putInt(value)
        }

        override fun write(bytes: ByteArray) {
            target.putBytes(bytes)
        }

        override fun write(bytes: ByteArray, offset: Int, length: Int) {
            target.putBytes(bytes, offset, length)
        }

        override fun flush() {
        }

        override fun toBytes(): ByteArray {
            throw UnsupportedOperationException()
        }

        override fun writeChar(value: Char) {
            target.putChar(value)
        }

        override fun writeBoolean(value: Boolean) {
            target.putBoolean(value)
        }

        override fun writeInt(value: Int) {
            target.putInt(value)
        }

        override fun writeInt(value: Int, optimizePositive: Boolean): Int {
            target.putInt(value)
            return 0
        }

        override fun writeShorts(array: ShortArray, offset: Int, count: Int) {
            for (i in 0 until count)
                target.putShort(array[offset + i])
        }

        override fun writeLongs(array: LongArray, offset: Int, count: Int) {
            for (i in 0 until count)
                target.putLong(array[offset + i])
        }

        override fun writeLongs(array: LongArray, offset: Int, count: Int, optimizePositive: Boolean) {
            for (i in 0 until count)
                target.putLong(array[offset + i])
        }

        override fun writeVarFloat(value: Float, precision: Float, optimizePositive: Boolean): Int {
            target.putFloatSurrogate(value)
            return 0
        }

        override fun reset() {
        }

        override fun close() {
        }

        override fun writeInts(array: IntArray, offset: Int, count: Int) {
            for (i in 0 until count)
                target.putInt(array[offset + i])
        }

        override fun writeInts(array: IntArray, offset: Int, count: Int, optimizePositive: Boolean) {
            for (i in 0 until count)
                target.putInt(array[offset + i])
        }

        override fun writeLong(value: Long) {
            target.putLong(value)
        }

        override fun writeLong(value: Long, optimizePositive: Boolean): Int {
            target.putLong(value)
            return 0
        }

        override fun writeDouble(value: Double) {
            target.putDoubleSurrogate(value)
        }

        override fun writeByte(value: Byte) {
            target.putByte(value)
        }

        override fun writeByte(value: Int) {
            target.putByte(value.toByte())
        }

        override fun setVariableLengthEncoding(varEncoding: Boolean) {
        }

        override fun setOutputStream(outputStream: OutputStream?) {
            throw UnsupportedOperationException()
        }

        override fun writeFloat(value: Float) {
            target.putFloatSurrogate(value)
        }

        override fun require(required: Int): Boolean {
            return true
        }

        override fun intLength(value: Int, optimizePositive: Boolean): Int {
            return 0
        }

        override fun setBuffer(buffer: ByteArray?) {
        }

        override fun setBuffer(buffer: ByteArray?, maxBufferSize: Int) {
        }

        override fun getVariableLengthEncoding(): Boolean {
            return false
        }

        override fun writeChars(array: CharArray, offset: Int, count: Int) {
            for (i in 0 until count)
                target.putChar(array[offset + i])
        }

        override fun getBuffer(): ByteArray {
            throw UnsupportedOperationException()
        }

        override fun writeVarDouble(value: Double, precision: Double, optimizePositive: Boolean): Int {
            target.putDoubleSurrogate(value)
            return 0
        }

        override fun writeAscii(value: String) {
            target.putString(value, Charsets.US_ASCII)
        }

        override fun getMaxCapacity(): Int {
            return Int.MAX_VALUE
        }

        override fun writeVarInt(value: Int, optimizePositive: Boolean): Int {
            target.putInt(value)
            return 0
        }

        override fun setPosition(position: Int) {
        }

        override fun getOutputStream(): OutputStream {
            throw UnsupportedOperationException()
        }

        override fun position(): Int {
            return 0
        }

        override fun total(): Long {
            return 0
        }

        override fun writeVarIntFlag(flag: Boolean, value: Int, optimizePositive: Boolean): Int {
            target.putInt(value)
            return 0
        }

        override fun writeVarLong(value: Long, optimizePositive: Boolean): Int {
            target.putLong(value)
            return 0
        }

        override fun writeBooleans(array: BooleanArray, offset: Int, count: Int) {
            for (i in 0 until count)
                target.putBoolean(array[offset + i])
        }

        override fun longLength(value: Int, optimizePositive: Boolean): Int {
            return 0
        }
    }

    override fun funnel(from: Any?, into: PrimitiveSink) {
        // Get the Kryo instance.
        val kryo = kryoPool.obtain()

        // Write object to primitive sink.
        kryo.writeClassAndObject(OutputPrimitiveSink(into), from)

        // Release the instance.
        kryoPool.free(kryo)
    }
}

/**
 * Returns the hash for a part.
 */
fun Part.hashTo(target: Hasher, kryoPool: Pool<Kryo>) {
    val map = TreeMap<String, Any?>()
    persist(map::set)
    target.putObject(map, KryoFunnel(kryoPool))
}

/**
 * Returns the hash for a part.
 */
fun Part.hashTo(target: Hasher, configure: (Kryo) -> Unit) =
    hashTo(target, KryoConfiguredPool(configure, false))

/**
 * Returns the hash for an entity, including it's [Ent.id].
 */
fun Ent.hashTo(target: Hasher, kryoPool: Pool<Kryo>) {
    val map = TreeMap<Lx, Any?>()
    driver.persist { name, key, any -> map[id / name / key] = any }
    target.putObject(map, KryoFunnel(kryoPool))
}

/**
 * Returns the hash for an entity, including it's [Ent.id].
 */
fun Ent.hashTo(target: Hasher, configure: (Kryo) -> Unit) =
    hashTo(target, KryoConfiguredPool(configure, false))

/**
 * Returns the hash for a shell. As of now, this might not be stable for sign-offs, as [Ent.repeating] has local
 * parameters independent of sign-off.
 */
fun Shell.hashTo(target: Hasher, kryoPool: Pool<Kryo>) {
    val map = TreeMap<Lx, Any?>()
    store(map::set)
    target.putObject(map, KryoFunnel(kryoPool))
}

/**
 * Returns the hash for a shell. As of now, this might not be stable for sign-offs, as [Ent.repeating] has local
 * parameters independent of sign-off.
 */
fun Shell.hashTo(target: Hasher, configure: (Kryo) -> Unit) =
    hashTo(target, KryoConfiguredPool(configure, false))
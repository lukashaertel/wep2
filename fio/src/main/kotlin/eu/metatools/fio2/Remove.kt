package eu.metatools.fio2

import java.nio.ByteBuffer

private const val removeCacheSize = 2048
private val removeCache = ByteArray(2048)

/**
 * Removes a section of the byte buffer. [length] bytes after [start] will be erased, the limit will be adjusted.
 */
fun ByteBuffer.remove(start: Int, length: Int) {
    val posBefore = position()

    // Position at copy location.
    position(start + length)

    // While remaining data, shift.
    while (position() < limit()) {
        // Get current length of what to copy.
        val chunk = minOf(removeCacheSize, limit() - position())

        // Get data at source, position will be after chunk.
        get(removeCache, 0, chunk)

        // Back-skip over copied chunk and over gap to skip.
        position(position() - chunk - length)

        // Put copied data at target.
        put(removeCache, 0, chunk)

        // Skip gap again.
        position(position() + length)
    }

    // Remove skipped gap.
    position(posBefore)
    limit(limit() - length)
}
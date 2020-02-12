package eu.metatools.ex.ents

import com.google.common.hash.HashFunction
import com.google.common.hash.Hashing
import eu.metatools.ex.Resources
import eu.metatools.ex.configureKryo
import eu.metatools.ex.subUiZ
import eu.metatools.fio.InOut
import eu.metatools.fio.data.Mat
import eu.metatools.fio.immediate.submit
import eu.metatools.fio.resource.LifecycleDrawable
import eu.metatools.fio.tools.ReferData
import eu.metatools.fio.tools.hashImage
import eu.metatools.up.Shell
import eu.metatools.up.kryo.hashTo

/**
 * Provides shell to hash image conversion.
 */
@Suppress("UnstableAPIUsage")
class ShellHashes(
    private val hashFunction: HashFunction = Hashing.farmHashFingerprint64(),
    private val maxMemorizedHashes: Int = 5
) {
    /**
     * List of engine hash images.
     */
    private val hashes = mutableListOf<LifecycleDrawable<Unit?>>()

    /**
     * Gets a copy of the hash image list.
     */
    val hashImages get() = hashes.toList()

    /**
     * Hashes the shell to a data image, stores it and retains size constrains..
     */
    fun pushHash(shell: Shell) {
        // Create hash generator, hash to it and take it's bytes.
        val bytes = hashFunction
            .newHasher()
            .also { shell.hashTo(it, ::configureKryo) }
            .hash()
            .asBytes()

        // Add as first.
        hashes.add(0, Resources.data[ReferData(bytes, ::hashImage)])

        // Remove while over size.
        if (maxMemorizedHashes >= 0) while (hashes.size > maxMemorizedHashes)
            hashes.asReversed().removeAt(0).dispose()
    }

    /**
     * Pops the oldest hash image and disposes it.
     */
    fun popHash(): Boolean {
        if (hashes.isEmpty())
            return false

        hashes.asReversed().removeAt(0).dispose()
        return true
    }

    /**
     * Removes all hashes.
     */
    fun clear() {
        hashes.clear()
    }
}

/**
 * The inset to the left of the screen for displaying hash images.
 */
private const val hashesInsetX = 32f

/**
 * The inset to the bottom of the screen for displaying hash images.
 */
private const val hashesInsetY = 32f

/**
 * The size of one hash image.
 */
private const val hashesSize = 48f
/**
 * The spacing between two hash images.
 */
private const val hashesSpacing = 16f

/**
 * Draws all hash images at the side of the screen.
 */
fun InOut.submitHashes(shellHashes: ShellHashes, time: Double) {
    for ((i, h) in shellHashes.hashImages.withIndex())
        submit(
            h, time, Mat
                .translation(hashesInsetX, hashesInsetY + (i * (hashesSize + hashesSpacing)), subUiZ)
                .scale(hashesSize, hashesSize)
                .translate(0.5f, 0.5f)
        )
}
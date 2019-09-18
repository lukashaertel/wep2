package eu.metatools.f2d.tools

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.files.FileHandle
import eu.metatools.f2d.context.LifecycleResource
import eu.metatools.f2d.context.Playable
import eu.metatools.f2d.math.Mat
import eu.metatools.f2d.util.bufferId
import eu.metatools.f2d.util.sourceFromID
import org.lwjgl.openal.AL10
import org.lwjgl.openal.AL11
import kotlin.math.abs

/**
 *  Arguments to refer a [SoundResource].
 */
data class ReferSound(val looping: Boolean = DEFAULT.looping) {
    companion object {
        /**
         * The default value of referring to a sound.
         */
        val DEFAULT = ReferSound(false)
    }
}

/**
 * Modulation of how to play a sound.
 */
data class Modulation(val pitch: Float = DEFAULT.pitch, val volume: Float = DEFAULT.volume) {
    companion object {
        /**
         * The default modulation.
         */
        val DEFAULT = Modulation(1.0f, 1.0f)
    }
}

/**
 * A sound resource that loads the sound from a file.
 * @property location The location function.
 */
class SoundResource(
    val location: () -> FileHandle
) : LifecycleResource<ReferSound?, Playable<Modulation?>> {
    companion object {
        /**
         * Value that, if exceeded, will cause repositioning of a playable instance.
         */
        private const val REPOSITION_EPSILON = 0.1
    }

    /**
     * The sound if loaded or null.
     */
    private var sound: Sound? = null

    /**
     * Gets the length of the sound in seconds.
     */
    fun getLength(): Double {
        // Get loaded sound .
        val useSound = sound
            ?: throw IllegalStateException("Sound not loaded")

        // Get buffer ID from sound.
        val bufferId = useSound.bufferId()
            ?: throw IllegalStateException("Cannot retrieve buffer, audio system not OpenAL")

        // Get buffer parameters.
        val sizeInBytes = AL10.alGetBufferi(bufferId, AL10.AL_SIZE)
        val channels = AL10.alGetBufferi(bufferId, AL10.AL_CHANNELS)
        val bits = AL10.alGetBufferi(bufferId, AL10.AL_BITS)
        val frequency = AL10.alGetBufferi(bufferId, AL10.AL_FREQUENCY)

        // Compute length of the buffer in seconds.
        return (sizeInBytes * 8 / (channels * bits)).toDouble() / frequency
    }

    override fun initialize() {
        // Initialize sound if not yet initialized.
        if (sound == null)
            sound = Gdx.audio.newSound(location())
    }

    override fun dispose() {
        // Dispose sound if not disposed of yet.
        sound?.dispose()
        sound = null
    }

    override fun refer(argsResource: ReferSound?) =
        object : Playable<Modulation?> {
            private val activeArgsResource = argsResource ?: ReferSound.DEFAULT

            private val plays = mutableMapOf<Any, Long>()

            override fun play(args: Modulation?, handle: Any, time: Double, transform: Mat) {
                val activeArgs = args ?: Modulation.DEFAULT

                // Get or create handle, assign looping property once.
                val sound = sound ?: throw IllegalStateException("Sound not loaded")
                val id = plays.getOrPut(handle) {
                    sound.play().also {
                        sound.setLooping(it, activeArgsResource.looping)
                    }
                }

                // Get sound source to use in OpenAL.
                val source = Gdx.audio.sourceFromID(id)
                    ?: throw IllegalStateException("Cannot retrieve source, audio system not OpenAL")

                // Get play time, check if in boundaries, otherwise reposition.
                val current = AL10.alGetSourcef(source, AL11.AL_SEC_OFFSET)
                if (abs(time - current) >= REPOSITION_EPSILON)
                    AL10.alSourcef(source, AL11.AL_SEC_OFFSET, time.toFloat())

                // Set audio position.
                AL10.alSource3f(
                    source, AL10.AL_POSITION,
                    transform.center.x, transform.center.y, transform.center.z
                )

                // TODO: Investigate directions and scale.

                // Set extra arguments.
                AL10.alSourcef(source, AL10.AL_PITCH, activeArgs.pitch)
                AL10.alSourcef(source, AL10.AL_GAIN, activeArgs.volume)
            }

            override fun cancel(handle: Any) {
                val sound = sound ?: throw IllegalStateException("Sound not loaded")
                plays.remove(handle)?.let(sound::stop)
            }

            override val duration
                get() =
                    if (activeArgsResource.looping)
                        Double.POSITIVE_INFINITY
                    else
                        getLength()
        }
}
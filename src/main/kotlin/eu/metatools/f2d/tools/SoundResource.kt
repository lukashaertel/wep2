package eu.metatools.f2d.tools

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.files.FileHandle
import eu.metatools.f2d.context.*
import eu.metatools.f2d.util.bufferId
import eu.metatools.f2d.util.sourceFromID
import org.lwjgl.openal.AL10
import org.lwjgl.openal.AL11
import kotlin.math.abs

/**
 * Arguments for creating a [Playable] from a [SoundResource].
 */
data class SoundResourceArgs(val looping: Boolean = false)

/**
 * Arguments for rendering a [Playable] from a [SoundResource].
 */
data class SoundArgs(val pitch: Float = 1.0f, val volume: Float = 1.0f)

/**
 * A sound resource that loads the sound from a file.
 * @property location The location function.
 */
class SoundResource(
    val location: () -> FileHandle
) : LifecycleResource<SoundResourceArgs?, Playable<SoundArgs?>> {
    companion object {
        /**
         * Value that, if exceeded, will cause repositioning of a playable instance.
         */
        private const val REPOSITION_EPSILON = 0.1

        /**
         * The default arguments to [refer].
         */
        val defaultArgsResource = SoundResourceArgs()


        /**
         * The default arguments to [Playable.generate].
         */
        val defaultArgs = SoundArgs()
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

    override fun refer(argsResource: SoundResourceArgs?) =
        object : Playable<SoundArgs?> {
            val activeArgsResource = argsResource ?: defaultArgsResource

            override fun start(args: SoundArgs?, time: Double): Long {
                val activeArgs = args ?: defaultArgs

                // Get loaded sound.
                val useSound = sound
                    ?: throw IllegalStateException("Sound not loaded")

                // Play sound.
                val id = useSound.play()

                // Loop if arguments demand it.
                useSound.setLooping(id, activeArgsResource.looping)


                // Get the source value from the audio system.
                val source = Gdx.audio.sourceFromID(id)
                    ?: throw IllegalStateException("Cannot retrieve source, audio system not OpenAL")

                // Set offset in seconds.
                AL10.alSourcef(source, AL11.AL_SEC_OFFSET, time.toFloat())

                // Set extra arguments.
                AL10.alSourcef(source, AL10.AL_PITCH, activeArgs.pitch)
                AL10.alSourcef(source, AL10.AL_GAIN, activeArgs.volume)

                return id
            }

            override fun generate(args: SoundArgs?, id: Long, time: Double, x: Float, y: Float, z: Float) {
                val activeArgs = args ?: defaultArgs

                // Get the source value from the audio system.
                val source = Gdx.audio.sourceFromID(id)
                    ?: throw IllegalStateException("Cannot retrieve source, audio system not OpenAL")

                // Get play time, check if in boundaries, otherwise reposition.
                val playTime = AL10.alGetSourcef(source, AL11.AL_SEC_OFFSET)
                if (abs(time - playTime) >= REPOSITION_EPSILON)
                    AL10.alSourcef(source, AL11.AL_SEC_OFFSET, time.toFloat())

                // Set audio position.
                AL10.alSource3f(source, AL10.AL_POSITION, x, y, z)

                // Set extra arguments.
                AL10.alSourcef(source, AL10.AL_PITCH, activeArgs.pitch)
                AL10.alSourcef(source, AL10.AL_GAIN, activeArgs.volume)
            }

            override fun stop(args: SoundArgs?, id: Long) {
                sound?.stop(id)
            }

            override val duration
                get() =
                    if (activeArgsResource.looping)
                        Double.POSITIVE_INFINITY
                    else
                        getLength()
        }
}
package eu.metatools.f2d.tools

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.files.FileHandle
import eu.metatools.f2d.context.Playable
import eu.metatools.f2d.context.Resource
import eu.metatools.f2d.util.bufferId
import eu.metatools.f2d.util.sourceFromID
import org.lwjgl.openal.AL10
import org.lwjgl.openal.AL11
import kotlin.math.abs


data class SoundArgs(val looping: Boolean = false, val pitch: Float = 1.0f, val volume: Float = 1.0f)

/**
 * A sound resource that loads the sound from a file. The played sounds respect position, commits volume and pitch.
 */
class SoundResource(val location: () -> FileHandle) : Resource<Playable<SoundArgs?>> {
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

    override fun refer() =
        object : Playable<SoundArgs?> {
            override fun start(args: SoundArgs?, time: Double): Long {
                // Get loaded sound.
                val useSound = sound
                    ?: throw IllegalStateException("Sound not loaded")

                // Play sound.
                val id = useSound.play()

                // Loop if arguments demand it.
                args?.let {
                    useSound.setLooping(id, it.looping)
                }


                // Get the source value from the audio system.
                val source = Gdx.audio.sourceFromID(id)
                    ?: throw IllegalStateException("Cannot retrieve source, audio system not OpenAL")

                // Set offset in seconds.
                AL10.alSourcef(source, AL11.AL_SEC_OFFSET, time.toFloat())
                return id
            }

            override fun generate(args: SoundArgs?, id: Long, time: Double, x: Float, y: Float, z: Float) {
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
                args?.let {
                    AL10.alSourcef(source, AL10.AL_PITCH, it.pitch)
                    AL10.alSourcef(source, AL10.AL_GAIN, it.volume)
                }
            }

            override fun stop(args: SoundArgs?, id: Long) {
                sound?.stop(id)
            }

            override fun hasStarted(args: SoundArgs?, time: Double) =
                0.0 <= time //TODO Figure out a standard.

            override fun hasEnded(args: SoundArgs?, time: Double): Boolean {
                // If looping, never ends.
                args?.let {
                    if (it.looping)
                        return false
                }

                // Get loaded sound.
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
                val durationInSeconds = (sizeInBytes * 8 / (channels * bits)).toFloat() / frequency

                // Check if validliy before time.
                return durationInSeconds < time
            }

        }
}
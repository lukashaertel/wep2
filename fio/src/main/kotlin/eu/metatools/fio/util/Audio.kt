package eu.metatools.fio.util

import com.badlogic.gdx.Audio
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.backends.lwjgl.audio.OpenALAudio
import com.badlogic.gdx.backends.lwjgl.audio.OpenALSound
import com.badlogic.gdx.utils.LongMap

/**
 * Navigates from the object to the explicitly given superclass [T]. Used for declared field access.
 */
private inline fun <reified T> Any.toSuperClass(): Class<*> {
    val target = T::class.java
    var current: Class<*> = this.javaClass

    if (!target.isAssignableFrom(current))
        throw IllegalArgumentException(" ${T::class.simpleName} is not assignable from $this")

    while (current != target) {
        checkNotNull(current.superclass) {
            "Is subclass but no superclass of ${T::class.simpleName} found"
        }
        current = current.superclass
    }

    return current
}

/**
 * For an OpenALSound, retrieves the buffer ID.
 */
fun Sound.bufferId(): Int? {
    // Only work on OpenALSound.
    if (this !is OpenALSound)
        return null

    // Get the bufferID field of the class, resolve it as accessible.
    this.toSuperClass<OpenALSound>().getDeclaredField("bufferID").let {
        try {
            it.isAccessible = true
            @Suppress("unchecked_cast")
            return (it.get(this) as Int)
        } finally {
            it.isAccessible = false
        }
    }
}

/**
 * For an OpenALAudio, retrieves the soundIdToSource field, which for some reason is private. This is used to
 * resolve the source from the sound ID.
 */
fun Audio.sourceFromID(id: Long): Int? {
    // Only work on OpenALAudio.
    if (this !is OpenALAudio)
        return null

    // Get the soundIdToSource field of the class, resolve it as accessible.
    this.toSuperClass<OpenALAudio>().getDeclaredField("soundIdToSource").let {
        try {
            it.isAccessible = true
            @Suppress("unchecked_cast")
            return (it.get(this) as LongMap<Int>)[id]
        } finally {
            it.isAccessible = false
        }
    }
}
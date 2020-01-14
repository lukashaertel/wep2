package eu.metatools.ex.ents.hero

import eu.metatools.ex.data.Dir
import eu.metatools.f2d.drawable.Drawable

/**
 * Sprite set of a [Hero].
 */
interface SpriteSet {
    /**
     * Idle set for the given [dir]ection.
     */
    fun idle(dir: Dir): Drawable<Unit?>

    /**
     * Movement for the given [dir]ection.
     */
    fun move(dir: Dir): Drawable<Unit?>

    /**
     * Drawing weapon for the given [dir]ection.
     */
    fun draw(dir: Dir): Drawable<Unit?>
}
package eu.metatools.f2d

import com.badlogic.gdx.graphics.g2d.SpriteBatch

interface DrawableResource<A> : Resource, Instantiable<A, Drawable>

interface Drawable : Lifetime {
    fun generate(time: Double, receiver: ((SpriteBatch) -> Unit) -> Unit)
}
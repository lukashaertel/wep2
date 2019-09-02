package eu.metatools.wep2.parcel

import eu.metatools.wep2.entity.Context
import eu.metatools.wep2.entity.Entity
import eu.metatools.wep2.util.SimpleMap
import kotlin.reflect.KFunction
import kotlin.reflect.full.primaryConstructor
//
//interface Parcelable {
//    val extraArgs: List<Any?>
//
//    var parcel: Any?
//}
//
//data class IndexParcel<I>(
//    val identity: I,
//    val constructor: KFunction<*>,
//    val extraArgs: List<Any?>,
//    val parcel: Any?
//)
//
//fun <I> parcel(entityMap: SimpleMap<I, out Entity<*, *, I>>) =
//    entityMap.map { (i, e) ->
//        // Assert that the entity is parcelable, do not statically enforce.
//        e as Parcelable
//
//        // Assert that there is a primary constructor, resolve it.
//        val constructor = e::class.primaryConstructor
//            ?: throw IllegalStateException("Entity does not have primary constructor")
//
//        // Return the parcel.
//        IndexParcel(i, constructor, e.extraArgs, e.parcel)
//    }
//
//fun <N, T : Comparable<T>, I> restore(
//    context: Context<N, T, I>,
//    parcel: List<IndexParcel<I>>
//) {
//    // TODO: Nested entities? Fuck that shit.
//
//    // Restore all entries of the parcel.
//    parcel.forEach {
//        // Construct the entity, use context as first argument to the constructor
//        // use the given extra arguments after.
//        val entity = it.constructor.call(restorationContext, *it.extraArgs.toTypedArray())
//
//        // Adjust type information.
//        entity as Parcelable
//
//        // Assign it's parcel value.
//        entity.parcel = it.parcel
//    }
//}
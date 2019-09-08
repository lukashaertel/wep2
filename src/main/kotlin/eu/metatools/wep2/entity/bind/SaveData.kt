package eu.metatools.wep2.entity.bind

import kotlinx.serialization.ContextualSerialization
import kotlinx.serialization.Serializable

@Serializable
data class SaveData<I>(
    val constructors: Map<I, String>,
    val data: Map<String, Map<String, @ContextualSerialization Any?>>
)
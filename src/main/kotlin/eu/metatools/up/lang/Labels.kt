package eu.metatools.up.lang

/**
 * A comparable that has a new label [text] defining it's [toString].
 */
data class LabeledComparable<T : Comparable<T>>(val text: String, val value: T) : Comparable<LabeledComparable<T>> {
    override fun compareTo(other: LabeledComparable<T>) =
        if (this === other) 0 else value.compareTo(other.value)

    override fun toString() =
        text
}

/**
 * Returns a new labeled comparable on the receiver with the given [text].
 */
fun <T : Comparable<T>> T.label(text: String) =
    LabeledComparable(text, this)

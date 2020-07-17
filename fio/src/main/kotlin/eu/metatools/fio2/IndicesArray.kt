package eu.metatools.fio2

import com.badlogic.gdx.utils.BufferUtils

class IndexArray(val short: Boolean, val maxIndices: Int) : BindData {
    override val buffer = BufferUtils.newByteBuffer(maxIndices * if (short) 2 else 4)

    override fun close() {
        BufferUtils.disposeUnsafeByteBuffer(buffer)
    }
}
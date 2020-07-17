//package eu.metatools.fio2
//
//import com.badlogic.gdx.graphics.g2d.TextureRegion
//import com.badlogic.gdx.utils.NumberUtils
//import eu.metatools.fio.data.Col
//import eu.metatools.fio.data.Vec
//
//@Suppress("nothing_to_inline")
//inline operator fun BindVertices.get(i: Int) =
//        buffer.getFloat(i * 4)
//
//@Suppress("nothing_to_inline")
//inline operator fun BindVertices.set(i: Int, value: Float) {
//    buffer.putFloat(i * 4, value)
//}
//
//@Suppress("nothing_to_inline")
//inline fun BindVertices.setVec(i: Int, vec: Vec) {
//    val buffer = buffer
//    val posBefore = buffer.position()
//    buffer.position(i * 4)
//    buffer.putFloat(vec.x)
//    buffer.putFloat(vec.y)
//    buffer.putFloat(vec.z)
//    buffer.position(posBefore)
//}
//
//@Suppress("nothing_to_inline")
//inline fun BindVertices.getVec(i: Int): Vec {
//    val buffer = buffer
//    val posBefore = buffer.position()
//    buffer.position(i * 4)
//    val x = buffer.float
//    val y = buffer.float
//    val z = buffer.float
//    buffer.position(posBefore)
//
//    return Vec(x, y, z)
//}
//
///**
// * Gets the x-component of the [n]th vertex of the [index]th shape.
// */
//@Suppress("nothing_to_inline")
//inline fun DrawsTarget.getX(index: Int, n: Int) =
//        vertices[(index * (if (quad) 4 else 3) + n) * SHAPE_VERTEX_SIZE + 0]
//
///**
// * Sets the x-component of the [n]th vertex of the [index]th shape.
// */
//@Suppress("nothing_to_inline")
//inline fun DrawsTarget.setX(index: Int, n: Int, value: Float) {
//    vertices[(index * (if (quad) 4 else 3) + n) * SHAPE_VERTEX_SIZE + 0] = value
//}
//
///**
// * Gets the y-component of the [n]th vertex of the [index]th shape.
// */
//@Suppress("nothing_to_inline")
//inline fun DrawsTarget.getY(index: Int, n: Int) =
//        vertices[(index * (if (quad) 4 else 3) + n) * SHAPE_VERTEX_SIZE + 1]
//
///**
// * Sets the y-component of the [n]th vertex of the [index]th shape.
// */
//@Suppress("nothing_to_inline")
//inline fun DrawsTarget.setY(index: Int, n: Int, value: Float) {
//    vertices[(index * (if (quad) 4 else 3) + n) * SHAPE_VERTEX_SIZE + 1] = value
//}
//
///**
// * Gets the z-component of the [n]th vertex of the [index]th shape.
// */
//@Suppress("nothing_to_inline")
//inline fun DrawsTarget.getZ(index: Int, n: Int) =
//        vertices[(index * (if (quad) 4 else 3) + n) * SHAPE_VERTEX_SIZE + 2]
//
///**
// * Sets the z-component of the [n]th vertex of the [index]th shape.
// */
//@Suppress("nothing_to_inline")
//inline fun DrawsTarget.setZ(index: Int, n: Int, value: Float) {
//    vertices[(index * (if (quad) 4 else 3) + n) * SHAPE_VERTEX_SIZE + 2] = value
//}
//
///**
// * Gets the position of the [n]th vertex of the [index]th shape.
// */
//@Suppress("nothing_to_inline")
//inline fun DrawsTarget.getPos(index: Int, n: Int) =
//        vertices.getVec((index * (if (quad) 4 else 3) + n) * SHAPE_VERTEX_SIZE)
//
///**
// * Sets the position of the [n]th vertex of the [index]th shape.
// */
//@Suppress("nothing_to_inline")
//inline fun DrawsTarget.setPos(index: Int, n: Int, value: Vec) =
//        vertices.setVec((index * (if (quad) 4 else 3) + n) * SHAPE_VERTEX_SIZE, value)
//
///**
// * Gets the color of the [n]th vertex of the [index]th shape.
// */
//@Suppress("nothing_to_inline")
//inline fun DrawsTarget.getColor(index: Int, n: Int): Col {
//    val value = vertices[(index * (if (quad) 4 else 3) + n) * SHAPE_VERTEX_SIZE + 3]
//    return Col(NumberUtils.floatToIntColor(value))
//}
//
///**
// * Sets the color of the [n]th vertex of the [index]th shape.
// */
//@Suppress("nothing_to_inline")
//inline fun DrawsTarget.setColor(index: Int, n: Int, value: Col) {
//    vertices[(index * (if (quad) 4 else 3) + n) * SHAPE_VERTEX_SIZE + 3] = NumberUtils.intToFloatColor(value.packed)
//}
//
///**
// * Sets all colors of the [index]th shape.
// */
//@Suppress("nothing_to_inline")
//inline fun DrawsTarget.setColors(index: Int, value: Col) {
//    val packed = NumberUtils.intToFloatColor(value.packed)
//    if (quad) {
//        vertices[(index * 4 + 0) * SHAPE_VERTEX_SIZE + 3] = packed
//        vertices[(index * 4 + 1) * SHAPE_VERTEX_SIZE + 3] = packed
//        vertices[(index * 4 + 2) * SHAPE_VERTEX_SIZE + 3] = packed
//        vertices[(index * 4 + 3) * SHAPE_VERTEX_SIZE + 3] = packed
//    } else {
//        vertices[(index * 3 + 0) * SHAPE_VERTEX_SIZE + 3] = packed
//        vertices[(index * 3 + 1) * SHAPE_VERTEX_SIZE + 3] = packed
//        vertices[(index * 3 + 2) * SHAPE_VERTEX_SIZE + 3] = packed
//    }
//}
//
///**
// * Gets the u-coordinate of the [n]th vertex of the [index]th shape.
// */
//@Suppress("nothing_to_inline")
//inline fun DrawsTarget.getU(index: Int, n: Int) =
//        vertices[(index * (if (quad) 4 else 3) + n) * SHAPE_VERTEX_SIZE + 4]
//
///**
// * Sets the u-coordinate of the [n]th vertex of the [index]th shape.
// */
//@Suppress("nothing_to_inline")
//inline fun DrawsTarget.setU(index: Int, n: Int, value: Float) {
//    vertices[(index * (if (quad) 4 else 3) + n) * SHAPE_VERTEX_SIZE + 4] = value
//}
//
///**
// * Gets the v-coordinate of the [n]th vertex of the [index]th shape.
// */
//@Suppress("nothing_to_inline")
//inline fun DrawsTarget.getV(index: Int, n: Int) =
//        vertices[(index * (if (quad) 4 else 3) + n) * SHAPE_VERTEX_SIZE + 5]
//
///**
// * Sets the v-coordinate of the [n]th vertex of the [index]th shape.
// */
//@Suppress("nothing_to_inline")
//inline fun DrawsTarget.setV(index: Int, n: Int, value: Float) {
//    vertices[(index * (if (quad) 4 else 3) + n) * SHAPE_VERTEX_SIZE + 5] = value
//}
//
///**
// * Sets the default UV coordinates for the [index]th shape.
// *
// * Assumes bottom-left to be the first vertex and clockwise rotation. For quads, that's up, right down, and back. For
// * triangles, that is up, down right, and back.
// */
//@Suppress("nothing_to_inline")
//inline fun DrawsTarget.setUV(index: Int, left: Float = 0f, top: Float = 0f, right: Float = 1f, bottom: Float = 1f) {
//    if (quad) {
//        vertices[(index * 4 + 0) * SHAPE_VERTEX_SIZE + 4] = left
//        vertices[(index * 4 + 0) * SHAPE_VERTEX_SIZE + 5] = bottom
//        vertices[(index * 4 + 1) * SHAPE_VERTEX_SIZE + 4] = left
//        vertices[(index * 4 + 1) * SHAPE_VERTEX_SIZE + 5] = top
//        vertices[(index * 4 + 2) * SHAPE_VERTEX_SIZE + 4] = right
//        vertices[(index * 4 + 2) * SHAPE_VERTEX_SIZE + 5] = top
//        vertices[(index * 4 + 3) * SHAPE_VERTEX_SIZE + 4] = right
//        vertices[(index * 4 + 3) * SHAPE_VERTEX_SIZE + 5] = bottom
//    } else {
//        vertices[(index * 3 + 0) * SHAPE_VERTEX_SIZE + 4] = left
//        vertices[(index * 3 + 0) * SHAPE_VERTEX_SIZE + 5] = bottom
//        vertices[(index * 3 + 1) * SHAPE_VERTEX_SIZE + 4] = left
//        vertices[(index * 3 + 1) * SHAPE_VERTEX_SIZE + 5] = top
//        vertices[(index * 3 + 2) * SHAPE_VERTEX_SIZE + 4] = right
//        vertices[(index * 3 + 2) * SHAPE_VERTEX_SIZE + 5] = bottom
//    }
//}
//
///**
// * Sets the default UV coordinates for the [index]th shape.
// *
// * Assumes bottom-left to be the first vertex and clockwise rotation. For quads, that's up, right down, and back. For
// * triangles, that is up, down right, and back.
// */
//@Suppress("nothing_to_inline")
//inline fun DrawsTarget.setUV(index: Int, region: TextureRegion) {
//    // TODO Verify.
//
//    // Get UV coordinates to use with applied flipping.
//    val u = if (region.isFlipX) region.u2 else region.u
//    val u2 = if (region.isFlipX) region.u else region.u2
//    val v = if (region.isFlipY) region.v else region.v2
//    val v2 = if (region.isFlipY) region.v2 else region.v
//
//    // Set accordingly.
//    if (quad) {
//        vertices[(index * 4 + 0) * SHAPE_VERTEX_SIZE + 4] = u
//        vertices[(index * 4 + 0) * SHAPE_VERTEX_SIZE + 5] = v
//        vertices[(index * 4 + 1) * SHAPE_VERTEX_SIZE + 4] = u
//        vertices[(index * 4 + 1) * SHAPE_VERTEX_SIZE + 5] = v2
//        vertices[(index * 4 + 2) * SHAPE_VERTEX_SIZE + 4] = u2
//        vertices[(index * 4 + 2) * SHAPE_VERTEX_SIZE + 5] = v2
//        vertices[(index * 4 + 3) * SHAPE_VERTEX_SIZE + 4] = u2
//        vertices[(index * 4 + 3) * SHAPE_VERTEX_SIZE + 5] = v
//    } else {
//        vertices[(index * 3 + 0) * SHAPE_VERTEX_SIZE + 4] = u
//        vertices[(index * 3 + 0) * SHAPE_VERTEX_SIZE + 5] = v
//        vertices[(index * 3 + 1) * SHAPE_VERTEX_SIZE + 4] = u
//        vertices[(index * 3 + 1) * SHAPE_VERTEX_SIZE + 5] = v2
//        vertices[(index * 3 + 2) * SHAPE_VERTEX_SIZE + 4] = u2
//        vertices[(index * 3 + 2) * SHAPE_VERTEX_SIZE + 5] = v
//    }
//}
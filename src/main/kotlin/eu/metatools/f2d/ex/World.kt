package eu.metatools.f2d.ex

import com.badlogic.gdx.graphics.Color
import eu.metatools.f2d.context.refer
import eu.metatools.f2d.math.Mat
import eu.metatools.f2d.tools.SolidResource
import eu.metatools.f2d.tools.Variation
import eu.metatools.wep2.entity.bind.Restore
import eu.metatools.wep2.tools.Time
import eu.metatools.wep2.track.SI
import eu.metatools.wep2.track.bind.ref
import eu.metatools.wep2.track.bind.refMap
import eu.metatools.wep2.util.first
import eu.metatools.wep2.util.then
import java.io.Serializable

data class XY(val x: Int, val y: Int) : Comparable<XY>, Serializable {
    override fun compareTo(other: XY) =
        first(other) {
            y.compareTo(other.y)
        } then {
            x.compareTo(other.x)
        }

    override fun toString() = "($x, $y)"
}

data class Dimension(val minX: Int, val minY: Int, val maxX: Int, val maxY: Int) {
    val width get() = 1 + maxX - minX

    val height get() = 1 + maxY - minY

    fun include(xy: XY) =
        Dimension(
            minOf(minX, xy.x), minOf(minY, xy.y),
            maxOf(maxX, xy.x), maxOf(maxY, xy.y)
        )
}

var above = true


object Constants {
    val tileWidth = 32
    val tileHeight = 32
}

object Variations {
    val below = Variation(Color(0x2c353e), false)
}

object Resources {
    val resSolid by lazy { frontend.resource("solid") { SolidResource() } }

    val solid by lazy { resSolid.refer() }
}

class World(context: GameContext, restore: Restore?) : GameEntity(context, restore), RootRender {

    private val tiles by refMap<XY, Tile>(restore)

    override fun render(time: Double) {
        if (tiles.isEmpty)
            return

        val dimensions = tiles.keys.fold(Dimension(0, 0, 0, 0), Dimension::include)
        val sx = (Constants.tileWidth * dimensions.width).toFloat()
        val sy = (Constants.tileHeight * dimensions.height).toFloat()
        val tx = (Constants.tileWidth * dimensions.minX).toFloat()
        val ty = (Constants.tileHeight * dimensions.minY).toFloat()

        val field = Mat
            .translation(-0.5f, -0.5f)
            .scale(sx + 1, sy + 1)
            .translate(tx, ty)
            .translate(0.5f, 0.5f)

        if (above) {
            // TODO: Unfuck iteration order problems.
            for ((p, t) in tiles.sortedBy { it.key }) {
                val mat = Mat.translation(
                    (Constants.tileWidth * p.x).toFloat(),
                    (Constants.tileHeight * p.y).toFloat()
                )

                t.render(time, mat)
                t.above?.render(time, mat)
            }
        } else {
            frontend.continuous.submit(Resources.solid, Variations.below, time, field)

            for ((p, t) in tiles)
                t.below?.render(
                    time, Mat.translation(
                        (Constants.tileWidth * p.x).toFloat(),
                        (Constants.tileHeight * p.y).toFloat()
                    )
                )
        }

        // TODO: Render UI.
    }

    override fun evaluate(name: GameName, time: Time, args: Any?): () -> Unit {
        return super.evaluate(name, time, args)
    }
}


abstract class Tile(context: GameContext, restore: Restore?) : GameEntity(context, restore) {
    abstract val source: TileDefinition

    val active get() = modifier?.modifyTile(source) ?: source

    var modifier by ref<Modifier>(restore)

    var above by ref<Structure>(restore)

    var below by ref<Structure>(restore)

    fun update(world: World) {
        val modifier = modifier
        val above = above
        val below = below

        above?.update(world, modifier?.modifyStructure(above.source) ?: above.source)
        below?.update(world, modifier?.modifyStructure(below.source) ?: below.source)
    }

    fun render(time: Double, mat: Mat) {

    }
}

class Modifier(context: GameContext, restore: Restore?) : GameEntity(context, restore) {
    fun modifyTile(definition: TileDefinition): TileDefinition {
        TODO()
    }

    fun modifyStructure(definition: StructureDefinition): StructureDefinition {
        TODO()
    }
}


abstract class Structure(context: GameContext, restore: Restore?) : GameEntity(context, restore) {
    abstract val source: StructureDefinition

    abstract fun update(world: World, definition: StructureDefinition)

    fun render(time: Double, mat: Mat) {

    }
}

interface TileDefinition {

}

interface StructureDefinition
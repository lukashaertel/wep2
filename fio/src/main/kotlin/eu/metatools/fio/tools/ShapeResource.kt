package eu.metatools.fio.tools

import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import eu.metatools.fio.context.Context
import eu.metatools.fio.data.Pt
import eu.metatools.fio.data.Pts
import eu.metatools.fio.drawable.Drawable

object ShapeLine : Drawable<Pair<Pt, Pt>> {
    override fun draw(args: Pair<Pt, Pt>, time: Double, context: Context) {
        val shapes = context.shapes(ShapeRenderer.ShapeType.Line)
        shapes.line(
            args.first.x,
            args.first.y,
            args.second.x,
            args.second.y
        )
    }
}

object ShapeBox : Drawable<Pair<Pt, Pt>> {
    override fun draw(args: Pair<Pt, Pt>, time: Double, context: Context) {
        val shapes = context.shapes(ShapeRenderer.ShapeType.Line)
        shapes.rect(
            args.first.x,
            args.first.y,
            args.second.x,
            args.second.y
        )
    }
}

object ShapeBoxFilled : Drawable<Pair<Pt, Pt>> {
    override fun draw(args: Pair<Pt, Pt>, time: Double, context: Context) {
        val shapes = context.shapes(ShapeRenderer.ShapeType.Filled)
        shapes.rect(
            args.first.x,
            args.first.y,
            args.second.x,
            args.second.y
        )
    }
}

object ShapePoly : Drawable<Pts> {
    override fun draw(args: Pts, time: Double, context: Context) {
        val shapes = context.shapes(ShapeRenderer.ShapeType.Line)
        shapes.polygon(args.values)
    }
}
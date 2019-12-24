package eu.metatools.ex.data

import eu.metatools.ex.math.*
import eu.metatools.f2d.math.RealPt
import eu.metatools.f2d.math.toReal

object PolyTemplates {
    val Block: Poly = polyRect(
        RealPt(
            -0.5f,
            -0.5f
        ), RealPt(0.5f, 0.5f)
    )
    val RampLeft: Poly = listOf(
        RealPt(-0.5f, -0.5f),
        RealPt(0.5f, -0.5f),
        RealPt(-0.5f, 0.5f)
    )
    val RampCapLeft: Poly = listOf(
        RealPt(-0.5f, 1.5f),
        RealPt(0.5f, 0.5f),
        RealPt(0.5f, 0.45f),
        RealPt(-0.5f, 1.45f)
    )

    val RampLeftLong: Poly = listOf(
        RealPt(-0.5f, -0.5f),
        RealPt(1.5f, -0.5f),
        RealPt(-0.5f, 0.5f)
    )

    val RampCapLeftLong: Poly = listOf(
        RealPt(-0.5f, 1.5f),
        RealPt(1.5f, 0.5f),
        RealPt(1.5f, 0.45f),
        RealPt(-0.5f, 1.45f)
    )

    val RampBoundsLeftLong: Poly = listOf(
        RealPt(1.5f, -0.5f),
        RealPt(-0.5f, 0.5f),
        RealPt(-0.5f, 1.45f),
        RealPt(1.5f, 0.45f)
    )

    val RampRightLong: Poly = RampLeftLong.flipX().move(1, 0)

    val RampCapRightLong: Poly = RampCapLeftLong.flipX().move(1, 0)

    val RampBoundsRightLong: Poly = RampBoundsLeftLong.flipX().move(1, 0)

    val RampRight: Poly = RampLeft.flipX()

    val RampCapRight: Poly = RampCapLeft.flipX()

    val TrapezoidRight: Poly = listOf(
        RealPt(0.5f, 0.5f),
        RealPt(0.5f, -0.5f),
        RealPt(0.0f, -.25f),
        RealPt(0.0f, 0.25f)
    )

    val TrapezoidLeft: Poly = TrapezoidRight.flipX()

    val SmallCircle: Poly = polyCircle(
        RealPt.ZERO,
        0.25f.toReal(),
        8
    )

    val Circle = polyCircle(
        RealPt.ZERO,
        0.55f.toReal(),
        16
    )
}
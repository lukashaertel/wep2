package eu.metatools.ex.data

import eu.metatools.ex.math.*
import eu.metatools.f2d.data.RealPt
import eu.metatools.f2d.data.toReal

object PolyTemplates {
    val Block = polyRect(
        RealPt(
            -0.5f,
            -0.5f
        ), RealPt(0.5f, 0.5f)
    )
    val RampLeft = Poly(
        RealPt(-0.5f, -0.5f),
        RealPt(0.5f, -0.5f),
        RealPt(-0.5f, 0.5f)
    )
    val RampCapLeft = Poly(
        RealPt(-0.5f, 1.5f),
        RealPt(0.5f, 0.5f),
        RealPt(0.5f, 0.45f),
        RealPt(-0.5f, 1.45f)
    )

    val RampLeftLong = Poly(
        RealPt(-0.5f, -0.5f),
        RealPt(1.5f, -0.5f),
        RealPt(-0.5f, 0.5f)
    )

    val RampCapLeftLong = Poly(
        RealPt(-0.5f, 1.5f),
        RealPt(1.5f, 0.5f),
        RealPt(1.5f, 0.45f),
        RealPt(-0.5f, 1.45f)
    )

    val RampBoundsLeftLong = Poly(
        RealPt(1.5f, -0.5f),
        RealPt(-0.5f, 0.5f),
        RealPt(-0.5f, 1.45f),
        RealPt(1.5f, 0.45f)
    )

    val RampRightLong = RampLeftLong.flipX().move(1, 0)

    val RampCapRightLong = RampCapLeftLong.flipX().move(1, 0)

    val RampBoundsRightLong = RampBoundsLeftLong.flipX().move(1, 0)

    val RampRight = RampLeft.flipX()

    val RampCapRight = RampCapLeft.flipX()

    val TrapezoidRight = Poly(
        RealPt(0.5f, 0.5f),
        RealPt(0.5f, -0.5f),
        RealPt(0.0f, -.25f),
        RealPt(0.0f, 0.25f)
    )

    val TrapezoidLeft = TrapezoidRight.flipX()

    val SmallCircle = polyCircle(
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
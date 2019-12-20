package eu.metatools.ex.data

import eu.metatools.ex.math.Poly
import eu.metatools.ex.math.flipX
import eu.metatools.ex.math.polyCircle
import eu.metatools.ex.math.polyRect
import eu.metatools.f2d.math.RealPt
import eu.metatools.f2d.math.toReal

enum class PolyTemplates {
    Block {
        override val poly = listOf(
            polyRect(
                RealPt(
                    -0.5f,
                    -0.5f
                ), RealPt(0.5f, 0.5f)
            )
        )
    },
    RampLeft {
        override val poly = listOf(
            listOf(
                RealPt(-0.5f, -0.5f),
                RealPt(0.5f, -0.5f),
                RealPt(-0.5f, 0.5f)
            )
        )
    },
    RampCapLeft {
        override val poly = listOf(
            listOf(
                RealPt(-0.5f, 1.5f),
                RealPt(0.5f, 0.5f),
                RealPt(0.5f, 0.45f),
                RealPt(-0.5f, 1.45f)
            )
        )
    },
    HalfRampLeftFst {
        override val poly = listOf(
            listOf(
                RealPt(-0.5f, -0.5f),
                RealPt(0.5f, -0.5f),
                RealPt(-0.5f, 0.0f)
            )
        )
    },
    HalfRampCapLeftFst {
        override val poly = listOf(
            listOf(
                RealPt(-0.5f, 1.0f),
                RealPt(0.5f, 0.5f),
                RealPt(0.5f, 0.45f),
                RealPt(-0.5f, 0.95f)
            )
        )
    },
    HalfRampLeftSnd {
        override val poly = listOf(
            listOf(
                RealPt(-0.5f, -0.5f),
                RealPt(0.5f, -0.5f),
                RealPt(0.5f, 0.0f),
                RealPt(-0.5f, 0.5f)
            )
        )
    },
    HalfRampCapLeftSnd {
        override val poly = listOf(
            listOf(
                RealPt(-0.5f, 1.5f),
                RealPt(0.5f, 1.0f),
                RealPt(0.5f, 0.95f),
                RealPt(-0.5f, 1.45f)
            )
        )
    },

    HalfRampRightFst {
        override val poly = HalfRampLeftFst.poly.map(Poly::flipX)
    },
    HalfRampCapRightFst {
        override val poly = HalfRampCapLeftFst.poly.map(Poly::flipX)
    },
    HalfRampRightSnd {
        override val poly = HalfRampLeftSnd.poly.map(Poly::flipX)
    },
    HalfRampCapRightSnd {
        override val poly = HalfRampCapLeftSnd.poly.map(Poly::flipX)
    },

    RampRight {
        override val poly = RampLeft.poly.map(Poly::flipX)
    },
    RampCapRight {
        override val poly = RampCapLeft.poly.map(Poly::flipX)
    },
    TrapezoidRight {
        override val poly = listOf(
            listOf(
                RealPt(0.5f, 0.5f),
                RealPt(0.5f, -0.5f),
                RealPt(0.25f, -.25f),
                RealPt(0.25f, 0.25f)
            )
        )
    },
    TrapezoidLeft {
        override val poly = TrapezoidRight.poly.map(Poly::flipX)
    },
    SmallCircle {
        override val poly = listOf(
            polyCircle(
                RealPt.ZERO,
                0.25f.toReal(),
                8
            )
        )
    },
    Circle {
        override val poly = listOf(
            polyCircle(
                RealPt.ZERO,
                0.55f.toReal(),
                16
            )
        )
    }
    ;

    abstract val poly: List<Poly>
}
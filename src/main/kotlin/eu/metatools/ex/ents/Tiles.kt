package eu.metatools.ex.ents

import eu.metatools.ex.Resources
import eu.metatools.ex.add
import eu.metatools.ex.data.PolyTemplates
import eu.metatools.ex.math.Regions
import eu.metatools.ex.math.move
import eu.metatools.ex.remove
import eu.metatools.f2d.context.shift
import eu.metatools.f2d.math.Tri
import eu.metatools.f2d.tools.Static

interface Template {
    fun apply(world: World, x: Int, y: Int, z: Int)

    fun unapply(world: World, x: Int, y: Int, z: Int)
}

enum class Tiles : Template {
    Floor {
        private val visual by lazy {
            Resources.terrain[Static(
                "tile174"
            )].shift(0f, 1f)
        }

        override fun apply(world: World, x: Int, y: Int, z: Int) {
            val at = Tri(x, y, z)
            world.visuals.add(at, visual)
            world.flags[at to "RSP"] = true
            world.flags[at to "floor"] = true
        }

        override fun unapply(world: World, x: Int, y: Int, z: Int) {
            val at = Tri(x, y, z)
            world.visuals.remove(at, visual)
            world.flags.remove(at to "RSP")
            world.flags.remove(at to "floor")
        }
    },
    Wall {
        private val cap by lazy {
            Resources.terrain[Static("tile614")]
        }
        private val fill by lazy {
            Resources.terrain[Static("tile492")]
        }

        override fun apply(world: World, x: Int, y: Int, z: Int) {
            world.visuals.add(Tri(x, y, z + 1), cap)
            world.visuals.add(Tri(x, y, z), fill)
            // TODO: Displacement Z of hulls should be integrated, as well as drawing them.
            world.hull.getOrPut(z, ::Regions).union.add(PolyTemplates.Block.move(x, y + z))
        }

        override fun unapply(world: World, x: Int, y: Int, z: Int) {
            world.visuals.remove(Tri(x, y, z + 1), cap)
            world.visuals.remove(Tri(x, y, z), fill)
            world.hull[z]?.union?.remove(PolyTemplates.Block.move(x, y + z))
        }
    },
    Clip {
        override fun apply(world: World, x: Int, y: Int, z: Int) {
            world.clip.getOrPut(z, ::Regions).union.add(PolyTemplates.Block.move(x, y + z))
        }

        override fun unapply(world: World, x: Int, y: Int, z: Int) {
            world.clip[z]?.union?.remove(PolyTemplates.Block.move(x, y + z))
        }
    },
    StairLeft {
        private val bottomRight by lazy {
            Resources.terrain[Static(
                "tile618"
            )]
        }
        private val bottomLeft by lazy {
            Resources.terrain[Static(
                "tile617"
            )]
        }
        private val topRight by lazy {
            Resources.terrain[Static(
                "tile586"
            )]
        }
        private val topLeft by lazy {
            Resources.terrain[Static(
                "tile585"
            )]
        }

        override fun apply(world: World, x: Int, y: Int, z: Int) {
            world.visuals.add(Tri(x, y, z), bottomLeft)
            world.visuals.add(Tri(x + 1, y, z), bottomRight)
            world.visuals.add(Tri(x, y, z + 1), topLeft)
            world.visuals.add(Tri(x + 1, y, z + 1), topRight)
            world.hull.getOrPut(z, ::Regions).union.apply {
                add(PolyTemplates.RampLeftLong.move(x, y))
                add(PolyTemplates.RampCapLeftLong.move(x, y))
            }
            world.clip.getOrPut(z + 1, ::Regions).apply {
                union.apply {
                    add(PolyTemplates.RampLeftLong.move(x, y))
                    add(PolyTemplates.RampCapLeftLong.move(x, y))
                }
                subtract.apply {
                    add(PolyTemplates.RampBoundsLeftLong.move(x, y))
                    add(PolyTemplates.TrapezoidLeft.move(x + 2, y))
                }
            }

            world.entries.getOrPut(z + 1, ::Regions).union.add(PolyTemplates.TrapezoidRight.move(x + 1, y))
            world.entries.getOrPut(z + 0, ::Regions).union.add(PolyTemplates.TrapezoidLeft.move(x + 2, y))
        }

        override fun unapply(world: World, x: Int, y: Int, z: Int) {
            world.visuals.remove(Tri(x, y, z), bottomLeft)
            world.visuals.remove(Tri(x + 1, y, z), bottomRight)
            world.visuals.remove(Tri(x, y, z + 1), topLeft)
            world.visuals.remove(Tri(x + 1, y, z + 1), topRight)
            world.hull[z]?.union?.apply {
                remove(PolyTemplates.RampLeftLong.move(x, y))
                remove(PolyTemplates.RampCapLeftLong.move(x, y))
            }
            world.clip[z + 1]?.apply {
                union.apply {
                    remove(PolyTemplates.RampLeftLong.move(x, y))
                    remove(PolyTemplates.RampCapLeftLong.move(x, y))
                }
                subtract.apply {
                    remove(PolyTemplates.RampBoundsLeftLong.move(x, y))
                    remove(PolyTemplates.TrapezoidLeft.move(x + 2, y))
                }
            }
            world.entries[z + 1]?.union?.remove(PolyTemplates.TrapezoidRight.move(x + 1, y))
            world.entries[z + 0]?.union?.remove(PolyTemplates.TrapezoidLeft.move(x + 2, y))
        }
    },
    StairRight {
        private val bottomRight by lazy {
            Resources.terrain[Static(
                "tile616"
            )]
        }
        private val bottomLeft by lazy {
            Resources.terrain[Static(
                "tile615"
            )]
        }
        private val topRight by lazy {
            Resources.terrain[Static(
                "tile584"
            )]
        }
        private val topLeft by lazy {
            Resources.terrain[Static(
                "tile583"
            )]
        }

        override fun apply(world: World, x: Int, y: Int, z: Int) {
            world.visuals.add(Tri(x, y, z), bottomLeft)
            world.visuals.add(Tri(x + 1, y, z), bottomRight)
            world.visuals.add(Tri(x, y, z + 1), topLeft)
            world.visuals.add(Tri(x + 1, y, z + 1), topRight)
            world.hull.getOrPut(z, ::Regions).union.apply {
                add(PolyTemplates.RampRightLong.move(x, y))
                add(PolyTemplates.RampCapRightLong.move(x, y))
            }
            world.clip.getOrPut(z + 1, ::Regions).apply {
                union.apply {
                    add(PolyTemplates.RampRightLong.move(x, y))
                    add(PolyTemplates.RampCapRightLong.move(x, y))
                }
                subtract.apply {
                    add(PolyTemplates.RampBoundsRightLong.move(x, y))
                    add(PolyTemplates.TrapezoidRight.move(x - 1, y))
                }
            }

            world.entries.getOrPut(z + 0, ::Regions).union.add(PolyTemplates.TrapezoidRight.move(x - 1, y))
            world.entries.getOrPut(z + 1, ::Regions).union.add(PolyTemplates.TrapezoidLeft.move(x, y))
        }

        override fun unapply(world: World, x: Int, y: Int, z: Int) {
            world.visuals.remove(Tri(x, y, z), bottomLeft)
            world.visuals.remove(Tri(x + 1, y, z), bottomRight)
            world.visuals.remove(Tri(x, y, z + 1), topLeft)
            world.visuals.remove(Tri(x + 1, y, z + 1), topRight)
            world.hull[z]?.union?.apply {
                remove(PolyTemplates.RampRightLong.move(x, y))
                remove(PolyTemplates.RampCapRightLong.move(x, y))
            }
            world.clip[z + 1]?.apply {
                union.apply {
                    remove(PolyTemplates.RampRightLong.move(x, y))
                    remove(PolyTemplates.RampCapRightLong.move(x, y))
                }
                subtract.apply {
                    remove(PolyTemplates.RampBoundsRightLong.move(x, y))
                    remove(PolyTemplates.TrapezoidRight.move(x - 1, y))
                }
            }
            world.entries[z + 1]?.union?.remove(PolyTemplates.TrapezoidRight.move(x - 1, y))
            world.entries[z + 0]?.union?.remove(PolyTemplates.TrapezoidLeft.move(x, y))
        }
    }
}
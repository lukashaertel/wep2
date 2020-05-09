package eu.metatools.sx.ents

import com.badlogic.gdx.graphics.Color
import eu.metatools.fio.data.Mat
import eu.metatools.fio.data.Tri
import eu.metatools.fio.data.Vec
import eu.metatools.fio.data.isNotEmpty
import eu.metatools.sx.SX
import eu.metatools.sx.data.Volume
import eu.metatools.sx.data.merge
import eu.metatools.sx.data.volume
import eu.metatools.sx.process.ProcessOne
import eu.metatools.up.Ent
import eu.metatools.up.Shell
import eu.metatools.up.dsl.set
import eu.metatools.up.dt.Lx
import kotlin.math.nextDown
import kotlin.math.nextUp

data class Flow(val v: Vec, val d: Float) {
    fun isEmpty() = d == 0.0f
    operator fun plus(other: Flow) =
        Flow(v + other.v, d + other.d)
}

fun Flow.isNotEmpty() = !isEmpty()

inline fun laplace(x: Int, y: Int, z: Int, value: Float, f: (Int, Int, Int) -> Float?): Float {
    val a0 = f(x.dec(), y, z) ?: 0f
    val a1 = f(x.inc(), y, z) ?: 0f
    val a2 = f(x, y.dec(), z) ?: 0f
    val a3 = f(x, y.inc(), z) ?: 0f
    val a4 = f(x, y, z.dec()) ?: 0f
    val a5 = f(x, y, z.inc()) ?: 0f

    return a0 + a1 + a2 + a3 + a4 + a5 - value * 6f
}

//inline fun ddx(x: Int, y: Int, z: Int, f: (Int, Int, Int) -> Float?): Float {
//    val a0 = f(x.dec(), y, z) ?: 0f
//    val a1 = f(x.inc(), y, z) ?: 0f
//    return (a1 - a0) * 0.5f
//}
//
//inline fun ddy(x: Int, y: Int, z: Int, f: (Int, Int, Int) -> Float?): Float {
//    val a0 = f(x, y.dec(), z) ?: 0f
//    val a1 = f(x, y.inc(), z) ?: 0f
//    return (a1 - a0) * 0.5f
//}
//
//inline fun ddz(x: Int, y: Int, z: Int, f: (Int, Int, Int) -> Float?): Float {
//    val a0 = f(x, y, z.dec()) ?: 0f
//    val a1 = f(x, y, z.inc()) ?: 0f
//    return (a1 - a0) * 0.5f
//}

inline fun grad(x: Int, y: Int, z: Int, f: (Int, Int, Int) -> Float?): Vec {
    val a0 = f(x.dec(), y, z) ?: 0f
    val a1 = f(x.inc(), y, z) ?: 0f
    val a2 = f(x, y.dec(), z) ?: 0f
    val a3 = f(x, y.inc(), z) ?: 0f
    val a4 = f(x, y, z.dec()) ?: 0f
    val a5 = f(x, y, z.inc()) ?: 0f

    return Vec((a1 - a0) * 0.5f, (a3 - a2) * 0.5f, (a5 - a4) * 0.5f)
}

inline fun div(x: Int, y: Int, z: Int, f: (Int, Int, Int) -> Vec?): Float {
    val a0 = f(x.dec(), y, z)?.x ?: 0f
    val a1 = f(x.inc(), y, z)?.x ?: 0f
    val a2 = f(x, y.dec(), z)?.y ?: 0f
    val a3 = f(x, y.inc(), z)?.y ?: 0f
    val a4 = f(x, y, z.dec())?.z ?: 0f
    val a5 = f(x, y, z.inc())?.z ?: 0f

    return (a1 - a0 + a3 - a2 + a5 - a4) * 0.5f
}

class AddSource(val source: Volume<Vec>, val dt: Float) : ProcessOne<Vec, Vec>() {
    override fun merge(first: Vec, second: Vec) =
        first + second

    override fun computeAt(volume: Volume<Vec>, x: Int, y: Int, z: Int, value: Vec): Vec? {
        val add = source[x, y, z]
        val result = if (add == null) value else value + add * dt
        return result.takeIf(Vec::isNotEmpty)
    }
}

class DiffuseFloat(val viscosity: Float, val dt: Float, val selfBind: Boolean) : ProcessOne<Float, Float>() {
    override fun merge(first: Float, second: Float) =
        first + second

    override fun computeAt(volume: Volume<Float>, x: Int, y: Int, z: Int, value: Float): Float? {
        val laplace = if (selfBind)
            laplace(x, y, z, value) { i, j, k -> volume[i, j, k] ?: value }
        else
            laplace(x, y, z, value) { i, j, k -> volume[i, j, k] ?: 0f }

        return dt * viscosity * laplace
        // TODO: return outside.
    }
}

class DiffuseVec(val viscosity: Float, val dt: Float, val selfBind: Boolean) : ProcessOne<Vec, Vec>() {
    override fun merge(first: Vec, second: Vec) =
        first + second

    override fun computeAt(volume: Volume<Vec>, x: Int, y: Int, z: Int, value: Vec): Vec? {
        val laplaceX = if (selfBind)
            laplace(x, y, z, value.x) { i, j, k -> volume[i, j, k]?.x ?: value.x }
        else
            laplace(x, y, z, value.x) { i, j, k -> volume[i, j, k]?.x ?: 0f }

        val laplaceY = if (selfBind)
            laplace(x, y, z, value.y) { i, j, k -> volume[i, j, k]?.y ?: value.y }
        else
            laplace(x, y, z, value.y) { i, j, k -> volume[i, j, k]?.y ?: 0f }

        val laplaceZ = if (selfBind)
            laplace(x, y, z, value.z) { i, j, k -> volume[i, j, k]?.z ?: value.z }
        else
            laplace(x, y, z, value.z) { i, j, k -> volume[i, j, k]?.z ?: 0f }

        val resultX = dt * viscosity * laplaceX
        val resultY = dt * viscosity * laplaceY
        val resultZ = dt * viscosity * laplaceZ

        return Vec(resultX, resultY, resultZ).takeIf(Vec::isNotEmpty)
        // TODO: return outside.
    }
}

class ProjectVec : ProcessOne<Vec, Vec>() {
    override fun merge(first: Vec, second: Vec) =
        first + second

    override fun computeAt(volume: Volume<Vec>, x: Int, y: Int, z: Int, value: Vec): Vec? {
        val change = grad(x, y, z) { i, j, k ->
            -div(i, j, k, volume::get)
        }

        return value - change
    }
}

class AdvectFloat(val dt: Float, val velocity: Volume<Vec>) : ProcessOne<Float, Float>() {
    override fun merge(first: Float, second: Float) =
        first + second

    override fun computeAt(volume: Volume<Float>, x: Int, y: Int, z: Int, value: Float): Float? {
        val vel = velocity[x, y, z]
        val pos = Vec(x.toFloat(), y.toFloat(), z.toFloat())
        val target = if (vel == null) pos else pos - vel * dt

        val x0 = target.x.nextDown().toInt()
        val x1 = target.x.nextUp().toInt()
        val y0 = target.y.nextDown().toInt()
        val y1 = target.y.nextUp().toInt()
        val z0 = target.z.nextDown().toInt()
        val z1 = target.z.nextUp().toInt()

        val xf = target.x - x0
        val yf = target.y - y0
        val zf = target.z - z0

        val a0 = volume[x0, y0, z0] ?: 0f
        val a1 = volume[x1, y0, z0] ?: 0f
        val a2 = volume[x0, y1, z0] ?: 0f
        val a3 = volume[x1, y1, z0] ?: 0f
        val a4 = volume[x0, y0, z1] ?: 0f
        val a5 = volume[x1, y0, z1] ?: 0f
        val a6 = volume[x0, y1, z1] ?: 0f
        val a7 = volume[x1, y1, z1] ?: 0f

        return ((a0 * (1f - xf) + a1 * xf) * (1f - yf) + (a2 * (1f - xf) + a3 * xf) * yf) * (1f - zf) +
                ((a4 * (1f - xf) + a5 * xf) * (1f - yf) + (a6 * (1f - xf) + a7 * xf) * yf) * zf
    }
}

class AdvectVec(val dt: Float, val velocity: Volume<Vec>) : ProcessOne<Vec, Vec>() {
    override fun merge(first: Vec, second: Vec) =
        first + second

    override fun computeAt(volume: Volume<Vec>, x: Int, y: Int, z: Int, value: Vec): Vec? {
        val vel = velocity[x, y, z]
        val pos = Vec(x.toFloat(), y.toFloat(), z.toFloat())
        val target = if (vel == null) pos else pos - vel * dt

        val x0 = target.x.nextDown().toInt()
        val x1 = target.x.nextUp().toInt()
        val y0 = target.y.nextDown().toInt()
        val y1 = target.y.nextUp().toInt()
        val z0 = target.z.nextDown().toInt()
        val z1 = target.z.nextUp().toInt()

        val xf = target.x - x0
        val yf = target.y - y0
        val zf = target.z - z0

        val a0 = volume[x0, y0, z0] ?: Vec.Zero
        val a1 = volume[x1, y0, z0] ?: Vec.Zero
        val a2 = volume[x0, y1, z0] ?: Vec.Zero
        val a3 = volume[x1, y1, z0] ?: Vec.Zero
        val a4 = volume[x0, y0, z1] ?: Vec.Zero
        val a5 = volume[x1, y0, z1] ?: Vec.Zero
        val a6 = volume[x0, y1, z1] ?: Vec.Zero
        val a7 = volume[x1, y1, z1] ?: Vec.Zero

        return ((a0 * (1f - xf) + a1 * xf) * (1f - yf) + (a2 * (1f - xf) + a3 * xf) * yf) * (1f - zf) +
                ((a4 * (1f - xf) + a5 * xf) * (1f - yf) + (a6 * (1f - xf) + a7 * xf) * yf) * zf
    }
}

class World(
    shell: Shell, id: Lx, val sx: SX
) : Ent(shell, id) {
    val players by set<Player>()

    val hidden by volume<Boolean>()

    val solid by volume<Unit>()

    val velocity by volume<Vec>()

    val density by volume<Float>()

    companion object {
        const val diffusion = 0.1f // TODO

        const val viscosity = 0.1f // TODO


        /**
         * Milliseconds for update.
         */
        const val millis = 100L

        /**
         * Delta-time for the update.
         */
        val dt = millis / 1000f
    }

    private fun mergeFlow(first: Flow?, second: Flow) =
        (first?.plus(second) ?: second).takeIf(Flow::isNotEmpty)

    private fun merge(first: Float?, second: Float) =
        (first?.plus(second) ?: second).takeIf { it != 0f }

    private fun merge(first: Vec?, second: Vec) =
        (first?.plus(second) ?: second).takeIf(Vec::isNotEmpty)


    /**
     * Periodic world update.
     */
    val worldUpdate = repeating(Short.MAX_VALUE, millis, shell::initializedTime) {
        val vel0 = DiffuseVec(viscosity, dt, false)
        val vel1 = ProjectVec()
        val vel2 = AdvectVec(dt, velocity)
        val vel3 = ProjectVec()
        val den0 = DiffuseFloat(diffusion, dt, true)
        val den1 = AdvectFloat(dt, velocity)

        velocity.merge(vel0.compute(velocity), ::merge)
        velocity.merge(vel1.compute(velocity), ::merge)
        velocity.merge(vel2.compute(velocity), ::merge)
        velocity.merge(vel3.compute(velocity), ::merge)

        density.merge(den0.compute(density), ::merge)
        density.merge(den1.compute(density), ::merge)

        // Remove outside of bounds.
        val oob = density[Int.MIN_VALUE..Int.MAX_VALUE, Int.MIN_VALUE..Int.MAX_VALUE, Int.MIN_VALUE..-20]
            .associate { it.first to null }
        density.assign(oob)
    }

    val add = exchange(::doAdd)

    private fun doAdd(coord: Tri) {
        density[coord.x, coord.y, coord.z] = 1f
    }

    /**
     * Renders all actors.
     */
    fun render(time: Double, delta: Double) {
        solid.getAll().forEach { (at, _) ->
            if (hidden[at.x, at.y, at.z] != true) // TODO
                sx.cube(
                    at.copy(z = at.z.inc()) to Tri.Zero, Color.WHITE,
                    Mat.translation(at.x * 4f, at.z * 4f, at.y * 4f).scale(4f, 4f, 4f)
                )
        }
        density.getAll().forEach { (at, value) ->
            if (0.01 < value) {
                sx.cube(
                    at to value,
                    Color(0f, 1f, 1f, 0.4f),
                    Mat.translation(at.x * 4f, at.z * 4f, at.y * 4f)
                        .scale(4f, 4f, 4f)
                        .translate(0f, -0.5f, 0f)
                        .scale(sy = value.toFloat())
                        .translate(0f, 0.5f, 0f)
                )
            }
        }
    }
}


/*
def 1:
vel_step ( N, u, v, u_prev, v_prev, visc, dt );
dens_step ( N, dens, dens_prev, u, v, diff, dt );

def 2;
void vel_step ( int N, float * u, float * v, float * u0, float * v0,float visc, float dt )
u += u0 * dt
v += v0 * dt

-- swap
u0' := u, u' := u0
v0' := v, v' := v0

u' = u0' + dt * visc * laplace(u0')
v' = v0' + dt * visc * laplace(v0')

v0' = -dx(u') - dy(v')
u0' = laplace(v0')
u' -= dx(u0')/2
v' -= dy(u0')/2

-- swap
u":=u0', u0":=u'
v":=v0', v0":=v'

(xx) = locations - dt * (u, v)
(xd, xu) = xx nextDown, xx nextUp
(xf) = (xx) - (xd)
u" = lerp(u0", xd, xu, xf)
v" = lerp(v0", xd, xu, xf)

v0" = -dx(u") - dy(v")
u0" = laplace(v0")
u" -= dx(u0")/2
v" -= dy(u0")/2


def 3:
void dens_step ( int N, float * x, float * x0, float * u, float * v, float diff,float dt )
add_source ( N, x, x0, dt );
x0':=x, x':=x0
diffuse ( N, 0, x', x0', diff, dt );
x"=x0', x0"=x'
advect ( N, 0, x", x0", u, v, dt );

def 4:
void diffuse ( int N, int b, float * x, float * x0, float diff, float dt )
void advect ( int N, int b, float * d, float * d0, float * u, float * v, float dt )
 */

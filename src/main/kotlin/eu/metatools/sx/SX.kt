package eu.metatools.sx

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.esotericsoftware.kryo.Kryo
import eu.metatools.fio.Fio
import eu.metatools.fio.tools.AtlasResource
import eu.metatools.fio.tools.DataResource
import eu.metatools.fio.tools.SolidResource
import eu.metatools.sx.ents.*
import eu.metatools.ugs.BaseGame
import eu.metatools.up.dt.Time
import eu.metatools.up.dt.div
import eu.metatools.up.dt.lx
import eu.metatools.up.lang.Bind
import java.util.*
import kotlin.math.max
import kotlin.math.min


object ANull : DefActor<Double, Unit> {
    override val depends: Query
        get() = QueryNone
    override val deltaZero: Unit
        get() = Unit

    override fun derive(state: Double, depends: SortedSet<Actor<*, *>>) {
    }

    override fun advance(state: Double, derivative: Unit, time: Double, dt: Double): Double {
        return state
    }

    override fun isEdge(state: Double, derivative: Unit): Boolean {
        return false
    }

}

object AD : DefActor<Double, Pair<Double, Double>> {
    override val depends: Query
        get() = QueryWhere { t, s, d -> t is ANull }

    override val deltaZero: Pair<Double, Double>
        get() = 0.0 to 0.0

    override fun derive(state: Double, depends: SortedSet<Actor<*, *>>): Pair<Double, Double> {
        val target = depends.map { it.state as Double }.sum()
        val rate = (target - state) / 10.0
        return (target to rate).also(::println)
    }

    override fun advance(state: Double, derivative: Pair<Double, Double>, time: Double, dt: Double): Double {
        val (target, rate) = derivative
        return if (rate > 0.0)
            min(target, state + dt * rate).also(::println)
        else
            max(target, state + dt * rate).also(::println)
    }

    override fun isEdge(state: Double, derivative: Pair<Double, Double>): Boolean {
        val (target, _) = derivative
        return state == target
    }
}

class SXRes(val fio: Fio) {
    val solid by lazy { fio.use(SolidResource()) }

    val data by lazy { fio.use(DataResource()) }

    val altas by lazy {
        fio.use(AtlasResource { Gdx.files.internal("CTP.atlas") })
    }

}


class SX : BaseGame(-100f, 100f) {
    val res = SXRes(this)

    lateinit var root: World

    override fun configureNet(kryo: Kryo) =
        configureKryo(kryo)

    override fun shellResolve() {
        root = shell.resolve(lx / "root") as? World ?: error("Unsatisfied, root does not exist")
    }

    override fun shellCreate() {
        root = World(shell, lx / "root", this)
            .also(shell.engine::add)

        Actor(shell, lx / "aa", this, 4.0, ANull, null)
            .also(shell.engine::add)
            .let(root.actors::add)
        Actor(shell, lx / "ab", this, 2.0, ANull, null)
            .also(shell.engine::add)
            .let(root.actors::add)

        Actor(shell, lx / "ac", this, 0.0, AD, null)
            .also(shell.engine::add)
            .let(root.actors::add)
    }

    override fun Bind<Time>.inputShell(time: Double, delta: Double) {
    }

    override fun inputRepeating(timeMs: Long) {
        root.worldUpdate(timeMs)
        // Update global time takers.
    }

    override fun outputShell(time: Double, delta: Double) {
        root.render(time, delta)
    }
}

fun main() {
    // Set config values.
    val config = LwjglApplicationConfiguration()
    config.height = config.width

    // Start application.
    LwjglApplication(SX(), config)
}
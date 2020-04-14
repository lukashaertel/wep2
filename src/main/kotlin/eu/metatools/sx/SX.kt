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
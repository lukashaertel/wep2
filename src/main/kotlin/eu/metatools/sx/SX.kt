package eu.metatools.sx

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.esotericsoftware.kryo.Kryo
import eu.metatools.fio.Fio
import eu.metatools.fio.tools.AtlasResource
import eu.metatools.fio.tools.DataResource
import eu.metatools.fio.tools.SolidResource
import eu.metatools.ugs.BaseGame
import eu.metatools.up.Ent
import eu.metatools.up.Shell
import eu.metatools.up.dsl.provideDelegate
import eu.metatools.up.dsl.set
import eu.metatools.up.dt.Lx
import eu.metatools.up.dt.Time
import eu.metatools.up.dt.div
import eu.metatools.up.dt.lx
import eu.metatools.up.lang.Bind

class SXRes(val fio: Fio) {
    val solid by lazy { fio.use(SolidResource()) }

    val data by lazy { fio.use(DataResource()) }

    val altas by lazy {
        fio.use(AtlasResource { Gdx.files.internal("CTP.atlas") })
    }

}


typealias Demand = Map<Any, Int>
typealias Supply = Demand

interface BuildingKind {
    val purposes: Set<Any>
    val supply: Supply
    val height: Int

    val fy: Int
    val by: Int
    val spacing: Int
}

enum class SomeBuildingKinds(
    override val purposes: Set<Any>,
    override val supply: Supply,
    override val height: Int,
    override val fy: Int,
    override val by: Int,
    override val spacing: Int
) : BuildingKind {
    Residential(
        setOf("Residential"),
        mapOf("RU" to 1),
        height = 1,
        fy = 1,
        by = 3,
        spacing = 1
    ),
    MixedUse(
        setOf("Residential", "Commercial"),
        mapOf("RU" to 5, "Shop" to 1),
        height = 2,
        fy = 0,
        by = 0,
        spacing = 0
    ),
    MultiTenant(
        setOf("Residential"),
        mapOf("RU" to 6),
        height = 2,
        fy = 0,
        by = 0,
        spacing = 0
    ),
    ParkingLot(
        setOf("Utility"),
        mapOf("Parking" to 16),
        height = 0,
        fy = 0,
        by = 0,
        spacing = 0
    )
}

class Building(
    shell: Shell, id: Lx, val game: SX,
    inBuildingKind: BuildingKind
) : Ent(shell, id) {
    override val extraArgs = mapOf(
        "inBuildingKind" to inBuildingKind
    )

    var buildingKind by { inBuildingKind }
}

interface ZoneKind<A> {
    fun query(buildings: List<Building>): A

    fun check(data: A, kind: BuildingKind): Pair<Boolean, A>

    fun produce(data: A): Sequence<BuildingKind>

    fun satisfy(demand: Demand): BuildingKind?
}

object SuburbanResidential : ZoneKind<Unit> {
    override fun query(buildings: List<Building>) {
    }

    override fun check(data: Unit, kind: BuildingKind): Pair<Boolean, Unit> {
        if (kind.purposes.singleOrNull() != "Residential") return false to data
        if (kind.height > 1) return false to data
        if (kind.by < 1) return false to data
        if (kind.fy < 1) return false to data
        if (kind.spacing < 1) return false to data

        return true to data
    }

    override fun produce(data: Unit) =
        emptySequence<BuildingKind>()

    override fun satisfy(demand: Demand): BuildingKind? {
        val dRU = demand.getOrDefault("RU", 0)
        val sRU = SomeBuildingKinds.Residential.supply.getOrDefault("RU", 0)
        return if (sRU >= dRU)
            SomeBuildingKinds.Residential
        else
            null
    }
}

object StandardMixedUse : ZoneKind<Int> {
    override fun query(buildings: List<Building>) =
        buildings.asSequence().mapNotNull { it.buildingKind.supply["Parking"] }.sum()

    override fun check(data: Int, kind: BuildingKind): Pair<Boolean, Int> {
        if ("Industrial" in kind.purposes) return false to data
        if (kind.height < 2) return false to data
        if (kind.by > 0) return false to data
        if (kind.fy > 0) return false to data
        if (kind.spacing > 0) return false to data

        return true to data
    }

    override fun produce(data: Int): Sequence<BuildingKind> {
        val parking = SomeBuildingKinds.ParkingLot.supply.getOrDefault("Parking", 0)
        return (0 until (data / parking)).asSequence().map {
            SomeBuildingKinds.ParkingLot
        }
    }

    override fun satisfy(demand: Demand): BuildingKind? {
        TODO("Not yet implemented")
    }

}

class Zone(
    shell: Shell, id: Lx, val game: SX,
    inZoneKind: ZoneKind<*>
) : Ent(shell, id) {
    override val extraArgs = mapOf(
        "inZoneKind" to inZoneKind
    )

    var zoningCode by { inZoneKind }

    val buildings by set<Building>()
}

class Actor(
    shell: Shell, id: Lx, val game: SX
) : Ent(shell, id) {
    val subActors by set<Actor>()
}

class World(shell: Shell, id: Lx, val game: SX) : Ent(shell, id) {
    fun render(time: Double, delta: Double) {

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
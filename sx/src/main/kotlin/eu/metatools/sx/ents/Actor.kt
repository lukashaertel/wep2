package eu.metatools.sx.ents

import com.badlogic.gdx.math.Vector2
import eu.metatools.sx.SX
import eu.metatools.up.Shell
import eu.metatools.up.dsl.prop
import eu.metatools.up.dsl.provideDelegate
import eu.metatools.up.dt.Lx
import eu.metatools.up.list
import kotlin.math.sqrt

/**
 * An [SXEnt] that maintains, plans, and performs [Plan]s.
 * @param shell The hosting shell.
 * @param id The identity.
 * @param sx The output node.
 * @property minimumPressure The minimum pressure before addressing [requirements]
 * @property planMemory The amount of plans to memorize.
 */
abstract class Planner(
    shell: Shell, id: Lx, sx: SX,
    private val minimumPressure: Double,
    private val planMemory: Int
) : SXEnt(shell, id, sx) {
    /**
     * Enumerates the requirements of the entity.
     */
    abstract val requirements: List<Requirement<Planner>>

    /**
     * The currently executing plan.
     */
    var currentPlan by prop<Plan<Planner>?> { null }
        protected set

    /**
     * The last [planMemory] plans that failed.
     */
    var failedPlans by prop { emptyList<Plan<Planner>>() }
        protected set

    /**
     * The last [planMemory] plans that succeeded.
     */
    var succeededPlans by prop { emptyList<Plan<Planner>>() }
        protected set

    /**
     * Acts on the plans.
     */
    fun act(timeElapsed: Double) {
        // Keep remaining time across iterations.
        var available = timeElapsed

        // Run while time is left. Aborted when no new plan found after last one is completed.
        while (available.rgz()) {
            // Work on current plan or find new plan.
            var active = currentPlan ?: requirements
                // Find requirement with highest pressure and combine valid strategies.
                .flatMap { r ->
                    // Compute pressure of requirement.
                    val pressure = r.pressure(this)

                    // Check if no pressure or under threshold.
                    if (pressure == null || pressure < minimumPressure)
                    // Not required, skip.
                        emptyList()
                    else
                    // Required, plan with strategies.
                        r.strategies(this).mapNotNull { s ->
                            s.cost(this)?.div(pressure)?.let { cost -> Triple(r, s, cost) }
                        }
                }
                // Find strategy with highest pressure and lowest cost.
                .sortedByDescending { satisfy ->
                    satisfy.third
                }
                // Plan based on strategy.
                .map { satisfy ->
                    satisfy to satisfy.second.plan(this)
                }
                // Take a solution that is not empty.
                .firstOrNull { satisfy ->
                    satisfy.second.isNotEmpty()
                }
                // Turn into plan.
                ?.let { (satisfy, plan) ->
                    Plan(satisfy.first, satisfy.second, satisfy.third, emptyList(), plan)
                }
            // No requirement or plan found, stop evaluation.
            ?: return

            // While there's time left, act on plan.
            while (available.rgz()) {
                // Perform current step.
                val result = active.current().act(this, available)

                // Subtract time that was consumed.
                available -= result.consumed

                // Check outcome of step.
                if (result.outcome == true && active.isLast()) {
                    // Step successful and last. Push to succeeded plans and deactivate plan. Try to find next plan.
                    succeededPlans = (succeededPlans + active.next()).takeLast(planMemory)
                    currentPlan = null
                    break
                } else if (result.outcome == true) {
                    // Step successful but not last, advance. Try again until there's no step or no time left.
                    active = active.next()
                    currentPlan = active
                } else if (result.outcome == false) {
                    // Step failed. Push to failed and deactivate. Try to find next plan.
                    failedPlans = (failedPlans + active.next()).takeLast(planMemory)
                    currentPlan = null
                    break
                }
            }
        }
    }
}

interface Ongoing<T> {
    fun affect(on: T)
}


data class ActorStats(
    val hunger: Double,
    val fatigue: Double
) {
    override fun toString() = "{hunger=${hunger.roundForDisplay()}, fatigue=${fatigue.roundForDisplay()}}"
}


interface LStep {
    fun performPart(actor: Actor, time: Double): Double

    fun completed(actor: Actor): Boolean
}

data class GetTo(val x: Float, val y: Float) : LStep {
    override fun completed(actor: Actor) = Vector2.dst2(actor.x, actor.y, x, y) < 0.0001

    override fun performPart(actor: Actor, time: Double): Double {
        val l = (actor.speed * time) * (actor.speed * time)
        val d = Vector2(x, y).sub(actor.x, actor.y)
        val s = minOf(l.toFloat(), d.len2())
        d.nor().scl(sqrt(s))

        actor.x += d.x
        actor.y += d.y
        return (s / actor.speed).toDouble()
    }

    override fun toString() = "GetTo(x=${x.roundForDisplay()}, y=${y.roundForDisplay()})"
}

data class EatAt(val id: Lx) : LStep {
    override fun performPart(actor: Actor, time: Double): Double {
        val eatPerSecond = 0.1

        if (actor.shell.resolve(id) !is Dome) return 0.0

        val inHunger = actor.stats.hunger
        actor.stats = actor.stats.copy(hunger = maxOf(0.0, actor.stats.hunger - time * eatPerSecond))
        return ((inHunger - actor.stats.hunger) / eatPerSecond)
    }

    override fun completed(actor: Actor) = actor.stats.hunger < 0.01
}

data class SleepAt(val id: Lx) : LStep {
    override fun performPart(actor: Actor, time: Double): Double {
        val sleepPerSecond = 0.05

        if (actor.shell.resolve(id) !is Dome) return 0.0

        val inHunger = actor.stats.fatigue
        actor.stats = actor.stats.copy(fatigue = maxOf(0.0, actor.stats.fatigue - time * sleepPerSecond))
        return (inHunger - actor.stats.fatigue) / sleepPerSecond
    }

    override fun completed(actor: Actor) = actor.stats.fatigue < 0.01
}

class Actor(
    shell: Shell, id: Lx, sx: SX,
    initX: Float, initY: Float, val speed: Float
) : SXEnt(shell, id, sx), Reakted {
    override val extraArgs = mapOf(
        "initX" to initX,
        "initY" to initY,
        "speed" to speed
    )

    var plan by { emptyList<LStep>() }

    var x by { initX }
    var y by { initY }

    var stats by { ActorStats(0.0, 0.0) }

    fun actOnPlan(dt: Double) {
        if (plan.isEmpty())
            return

        var timeLeftOver = dt

        while (plan.isNotEmpty() && 0.0001 < timeLeftOver) {
            val step = plan.first()
            timeLeftOver -= step.performPart(this, timeLeftOver)
            if (step.completed(this))
                plan = plan.subList(1, plan.size)
        }
    }

    fun planToEat() {
        val dome = shell.list<Dome>().filter { 25.0f < it.radius }.minByOrNull {
            Vector2.dst2(x, y, it.x, it.y)
        } ?: return

        plan = listOf(GetTo(dome.x, dome.y), EatAt(dome.id))
    }

    fun planToSleep() {
        val dome = shell.list<Dome>().filter { it.radius <= 25.0f }.minByOrNull {
            Vector2.dst2(x, y, it.x, it.y)
        } ?: return

        plan = listOf(GetTo(dome.x, dome.y), SleepAt(dome.id))
    }

    fun formPlan() {
        // Do not abort if already acting on desire.
        if (plan.isNotEmpty())
            return

        // Get strongest desire.
        val index = listOf(stats.hunger, stats.fatigue)
            .withIndex()
            .filter { 0.25 < it.value }
            .maxByOrNull { it.value }?.index


        // Act on it.
        when (index) {
            0 -> planToEat()
            1 -> planToSleep()
        }

        if (plan.isNotEmpty())
            println("Actor $this now has plan $plan for desire $index")
    }

    fun updateStats(dt: Double) {
        // Update stat.
        stats = ActorStats(
            hunger = stats.hunger + 0.025 * dt,
            fatigue = stats.fatigue + 0.01 * dt
        )
    }

    fun update(dt: Double) {
        actOnPlan(dt)
        updateStats(dt)
        formPlan()
    }

    override val layer = Layer.Actors

    override fun renderPrimary() {
        val drawable = if (plan.isEmpty()) WorldRes.roundDrawableActor else WorldRes.roundDrawableActorActive
        renderCircle(key = id, x, y, 10.0f, drawable) {
            // Do nothing.
        }
    }

    override fun toString() =
        "[$id, $stats]"

}
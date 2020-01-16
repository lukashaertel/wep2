package eu.metatools.ex.ents.hero

import eu.metatools.f2d.data.Q

/**
 * Stats of a [Hero].
 * @property health The initial and maximum health.
 * @property ammo The ammo capacity.
 * @property baseDamage The base damage before application of factors.
 * @param bowInit The time before bow actually shoots a projectile.
 * @param bowHold The time how long the bow retains the maximum factor.
 * @param bowDegrade The factor degradation per second.
 * @param bowMin The minimum factor.
 * @param projectileSpeed The speed of the projectile.
 * @param speed The movement speed in fields per second.
 * @param hitXP The XP yield when this hero is hit.
 * @param deathXP The XP for killing the hero.
 */
data class Stats(
    val health: Q,
    val ammo: Int,
    val baseDamage: Int,
    val bowInit: Q,
    val bowHold: Q,
    val bowDegrade: Q,
    val bowMin: Q,
    val projectileSpeed:Q,
    val speed: Q,
    val hitXP: Int,
    val deathXP: Int
)
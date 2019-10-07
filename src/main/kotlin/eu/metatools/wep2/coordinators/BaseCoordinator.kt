package eu.metatools.wep2.coordinators

/**
 * Provides the base implementation for registration and publication mechanisms, leaving
 * evaluation abstract.
 */
abstract class BaseCoordinator<N, T : Comparable<T>> : Coordinator<N, T>() {
    /**
     * The registration of listeners.
     */
    private val registration = mutableListOf<(N, T, Any?) -> Unit>()

    override fun register(block: (N, T, Any?) -> Unit): AutoCloseable {
        // Add to registry.
        registration.add(block)

        // Return de-registration.
        return AutoCloseable {
            // Remove from registration so it will not be called anymore.
            registration.remove(block)
        }
    }

    /**
     * Sends the triple to all registrations.
     */
    override fun publish(name: N, time: T, args: Any?) {
        // For all registrations publish delta.
        registration.forEach { it(name, time, args) }
    }
}
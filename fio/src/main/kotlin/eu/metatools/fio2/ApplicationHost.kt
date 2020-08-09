package eu.metatools.fio2

import com.badlogic.gdx.ApplicationListener

/**
 * Application listener pendent that is initialized by an [ApplicationHost] on [ApplicationListener.create].
 */
interface Controller {
    /**
     * Main render method. Invocation frequency will be limited by [LwjglApplicationConfiguration.foregroundFPS] and
     * such.
     */
    fun render()

    /**
     * Disposes the controller's resources.
     */
    fun dispose()

    /**
     * Invoked on window resize. Defaults to [Unit].
     */
    fun resize(width: Int, height: Int) = Unit

    /**
     * Invoked on suspension of the main application.
     */
    fun pause() = Unit

    /**
     * Invoked on resumption of the main application.
     */
    fun resume() = Unit
}

abstract class ApplicationHost : ApplicationListener {
    private lateinit var controller: Controller

    abstract fun createController(): Controller

    protected open fun disposeHost() = Unit

    override fun create() {
        controller = createController()
    }

    override fun resize(width: Int, height: Int) {
        controller.resize(width, height)
    }

    override fun render() {
        controller.render()
    }

    override fun pause() {
        controller.pause()
    }

    override fun resume() {
        controller.resume()
    }

    override fun dispose() {
        try {
            controller.dispose()
        } finally {
            disposeHost()
        }
    }
}

/**
 * Returns an [ApplicationHost] with the [ApplicationHost.createController] method implemented by the parameter.
 */
inline fun host(crossinline createController: () -> Controller) =
        object : ApplicationHost() {
            override fun createController() =
                    createController()
        }
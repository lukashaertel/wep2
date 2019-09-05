package eu.metatools.f2d

import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Matrix4
import eu.metatools.f2d.context.Continuous
import eu.metatools.f2d.context.Once
import eu.metatools.f2d.context.Resource

abstract class F2DListener(val near: Float = 0f, val far: Float = 1f) : ApplicationListener {
    /**
     * All resource registered to the application.
     */
    private val resources = mutableListOf<Resource<*, *>>()

    /**
     * True if creation has happened.
     */
    private var postCreate = false

    /**
     * True if disposal happened.
     */
    private var postDispose = false

    /**
     * Marks a resource as used.
     */
    protected fun <T : Resource<*, *>> use(resource: T): T {
        // Add to resources to be initialized and disposed of.
        resources.add(resource)

        // If used after creation, initialize immediately.
        if (postCreate && !postDispose)
            resource.initialize()

        // Return it if chaining is wanted.
        return resource
    }

    /**
     * The sprite batch to use.
     */
    private lateinit var spriteBatch: SpriteBatch

    /**
     * The one-shot renderer.
     */
    val once = Once()

    /**
     * The continuous renderer.
     */
    val continuous = Continuous()

    /**
     * Gets the current time.
     */
    abstract val time: Double

    override fun render() {
        // Bind the current time.
        val time = time

        // Render to once and continuous.
        render(time)

        // Dispatch generated calls from the one-shot renderer.
        once.dispatch(continuous, time)

        // Clear the screen properly.
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        // Run the sprite batch with the calls generated in the continuous renderer.
        spriteBatch.begin()
        continuous.render(spriteBatch)
        spriteBatch.end()
    }

    /**
     * Renders the graphics at the time.
     */
    abstract fun render(time: Double)

    override fun resize(width: Int, height: Int) {
        // Reset projection.
        spriteBatch.projectionMatrix =
            Matrix4().setToOrtho2D(0f, 0f, width.toFloat(), height.toFloat(), near, far)
    }

    override fun create() {
        // Create main sprite batch.
        spriteBatch = SpriteBatch()
        spriteBatch.enableBlending()
        spriteBatch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        // Initialize proper projection matrix.
        spriteBatch.projectionMatrix =
            Matrix4().setToOrtho2D(0f, 0f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat(), near, far)

        // Initialize all used resources.
        resources.forEach {
            it.initialize()
        }

        // Set post-creation flag.
        postCreate = true
    }

    override fun dispose() {
        // Dispose of all used resources.
        resources.forEach {
            it.dispose()
        }

        // Set post-disposal flag.
        postDispose = true
    }
}
package eu.metatools.f2d

import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import eu.metatools.f2d.context.Continuous
import eu.metatools.f2d.context.Lifecycle
import eu.metatools.f2d.context.Once
import eu.metatools.f2d.math.Mat
import eu.metatools.f2d.math.Vec
import eu.metatools.f2d.util.uniformX
import eu.metatools.f2d.util.uniformY

abstract class F2DListener(val near: Float = 0f, val far: Float = 1f) : ApplicationListener {
    companion object {
        /**
         * The time to use for the first delta.
         */
        private val initialDelta = 0.001
    }

    /**
     * All root lifecycle elements.
     */
    private val roots = mutableListOf<Lifecycle>()

    /**
     * True if creation has happened.
     */
    var postCreate = false
        private set

    /**
     * True if disposal happened.
     */
    var postDispose = false
        private set

    /**
     * Marks an element as used, will be initialized and disposed of in the corresponding methods.
     */
    fun <T : Lifecycle> use(element: T): T {
        // Add to resources to be initialized and disposed of.
        roots.add(element)

        // If used after creation, initialize immediately.
        if (postCreate && !postDispose)
            element.initialize()

        // Return it if chaining is wanted.
        return element
    }

    /**
     * The sprite batch to use.
     */
    private lateinit var spriteBatch: SpriteBatch


    var model = Mat.Id

    var projection = Mat.NaN

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

    /**
     * The time of the last render.
     */
    private var timeOfLast: Double? = null

    override fun render() {
        // Clear the screen properly.
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        // Start drawing with the current matrices.
        continuous.begin(model, projection)

        // Bind the current time, generate delta and set new last time.
        val time = time
        val delta = timeOfLast?.minus(time)?.unaryMinus() ?: initialDelta
        timeOfLast = time

        // Render to once and continuous.
        render(time, delta)

        // Dispatch generated calls from the once to continuous.
        once.send(continuous, time)

        // Render all continuous draws.
        continuous.render(time, spriteBatch)

        // Send all sounds.
        continuous.play(time)

        // Perform capture for primary pointer.
        continuous.collect(time, Gdx.input.uniformX, Gdx.input.uniformY)?.let { (result, intersection) ->
            capture(result, intersection)
        }


        // Finalize batch for this call.
        continuous.end()
    }

    /**
     * Handles captures of the pointer.
     */
    open fun capture(result: Any, intersection: Vec) = Unit

    /**
     * Renders the graphics, sound and captures at the time.
     */
    open fun render(time: Double, delta: Double) = Unit

    override fun resize(width: Int, height: Int) {
        projection = Mat.ortho2D(0f, 0f, width.toFloat(), height.toFloat(), near, far)
    }

    override fun create() {
        // Create main sprite batch.
        spriteBatch = SpriteBatch()
        spriteBatch.enableBlending()
        spriteBatch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        // Initialize proper projection matrix.
        projection = Mat.ortho2D(0f, 0f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat(), near, far)


        // Initialize all used resources.
        roots.forEach {
            it.initialize()
        }

        // Set post-creation flag.
        postCreate = true
    }

    override fun dispose() {
        // Dispose of all used resources.
        roots.forEach {
            it.dispose()
        }

        // Set post-disposal flag.
        postDispose = true
    }
}
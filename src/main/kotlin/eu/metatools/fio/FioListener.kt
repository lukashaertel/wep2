package eu.metatools.fio

import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import eu.metatools.fio.context.StandardContext
import eu.metatools.fio.data.Mat
import eu.metatools.fio.data.Vec
import eu.metatools.fio.immediate.*
import eu.metatools.fio.queued.*
import eu.metatools.fio.resource.Lifecycle
import eu.metatools.fio.util.uniformX
import eu.metatools.fio.util.uniformY

/**
 * Basic application listener dispatching rendering and providing capture/output functionality.
 */
abstract class FioListener(
    val near: Float = 0f,
    val far: Float = 1f,
    val trimExcess: Float = 0.5f
) : ApplicationListener, Fio {
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

    override fun <T : Lifecycle> use(element: T): T {
        // Add to resources to be initialized and disposed of.
        roots.add(element)

        // If used after creation, initialize immediately.
        if (postCreate && !postDispose)
            element.initialize()

        // Return it if chaining is wanted.
        return element
    }

    /**
     * The context to use.
     */
    private val context = StandardContext()

    /**
     * View matrix.
     */
    var view = Mat.ID

    /**
     * The projection matrix, set from rendering.
     */
    private var projection = Mat.NAN

    private val standardCapture = StandardCapture(trimExcess)

    private val standardDraw = StandardDraw(trimExcess)

    private val standardPlay = StandardPlay(trimExcess)

    private val standardQueuedCapture = StandardQueuedCapture(standardCapture)

    private val standardQueuedDraw = StandardQueuedDraw(standardDraw)

    private val standardQueuedPlay = StandardQueuedPlay(standardPlay)

    private val worldCapture = object : TransformedCapture(standardCapture) {
        override val mat: Mat
            get() = view
    }

    private val worldDraw = object : TransformedDraw(standardDraw) {
        override val mat: Mat
            get() = view
    }

    private val worldPlay = object : TransformedPlay(standardPlay) {
        override val mat: Mat
            get() = view
    }

    private val worldQueuedCapture = StandardQueuedCapture(worldCapture)

    private val worldQueuedDraw = StandardQueuedDraw(worldDraw)

    private val worldQueuedPlay = StandardQueuedPlay(worldPlay)

    override val ui: InOut = object : InOut,
        Capture by standardCapture,
        Draw by standardDraw,
        Play by standardPlay,
        QueuedCapture by standardQueuedCapture,
        QueuedDraw by standardQueuedDraw,
        QueuedPlay by standardQueuedPlay {}

    override val world: InOut = object : InOut,
        Capture by worldCapture,
        Draw by worldDraw,
        Play by worldPlay,
        QueuedCapture by worldQueuedCapture,
        QueuedDraw by worldQueuedDraw,
        QueuedPlay by worldQueuedPlay {}

    /**
     * The time of the last render.
     */
    private var timeOfLast: Double? = null

    override fun render() {
        // Clear the screen properly.
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        // Start drawing with the current matrices.
        standardCapture.begin(projection)
        standardDraw.begin(projection)
        standardPlay.begin(projection)

        // Bind the current time, generate delta and set new last time.
        val time = time
        val delta = timeOfLast?.minus(time)?.unaryMinus() ?: initialDelta
        timeOfLast = time

        // Handle all calls to immediate or queued outputs.
        render(time, delta)

        // Submit all UI queue elements.
        standardQueuedCapture.update(time)
        standardQueuedDraw.update(time)
        standardQueuedPlay.update(time)

        // Submit all world queue elements.
        worldQueuedCapture.update(time)
        worldQueuedDraw.update(time)
        worldQueuedPlay.update(time)

        // Commit calls to context.
        standardDraw.render(time, context)

        // Update sounds.
        standardPlay.play(time)

        // Perform capture for primary pointer and handle.
        val (result, intersection) = standardCapture.collect(time, Gdx.input.uniformX, Gdx.input.uniformY)
        capture(result, intersection)

        // Finalize batch for this call.
        standardCapture.end()
        standardDraw.end()
        standardPlay.end()
    }

    /**
     * Handles captures of the pointer.
     */
    open fun capture(result: Any?, intersection: Vec) = Unit

    /**
     * Renders the graphics, sound and captures at the time.
     */
    open fun render(time: Double, delta: Double) = Unit

    override fun resize(width: Int, height: Int) {
        projection = Mat.ortho2D(0f, 0f, width.toFloat(), height.toFloat(), near, far)
    }

    override fun create() {
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

        // Dispose of context.
        context.dispose()

        // Set post-disposal flag.
        postDispose = true
    }
}
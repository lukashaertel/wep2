package eu.metatools.fio

import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import eu.metatools.fio.capturable.Capturable
import eu.metatools.fio.context.StandardContext
import eu.metatools.fio.data.Mat
import eu.metatools.fio.data.Vec
import eu.metatools.fio.drawable.Drawable
import eu.metatools.fio.immediate.StandardCapture
import eu.metatools.fio.immediate.StandardDraw
import eu.metatools.fio.immediate.StandardPlay
import eu.metatools.fio.playable.Playable
import eu.metatools.fio.queued.StandardQueuedCapture
import eu.metatools.fio.queued.StandardQueuedDraw
import eu.metatools.fio.queued.StandardQueuedPlay
import eu.metatools.fio.resource.Lifecycle
import eu.metatools.fio.util.uniformX
import eu.metatools.fio.util.uniformY
import java.util.*

// TODO: Redo entire thing

/**
 * Basic application listener dispatching rendering and providing capture/output functionality.
 * @property radiusLimit The largest object radius. Specified to drop all objects outside of the view frustum.
 */
abstract class Fio(val radiusLimit: Float = 1.0f) : ApplicationListener {
    companion object {
        /**
         * The time to use for the first delta.
         */
        private val initialDelta = 0.001

    }

    private val capture = StandardCapture(radiusLimit)

    private val draw = StandardDraw(radiusLimit)

    private val play = StandardPlay(radiusLimit)

    private val queuedCapture = StandardQueuedCapture(capture)

    private val queuedDraw = StandardQueuedDraw(draw)

    private val queuedPlay = StandardQueuedPlay(play)

    /**
     * Layer entry. Instances are created via constructor methods.
     * @property zNegative True if lower z-values are considered closer.
     * @property zReset True if depth is cleared before the layer is drawn.
     * @property zFaceCull True if faces facing away are culled.
     * @property zSort True if depth comparison function according to [zNegative] is applied.
     */
    inner class Layer(
        val zNegative: Boolean,
        val zReset: Boolean,
        val zFaceCull: Boolean,
        val zSort: Boolean,
        val invertedSorting: Boolean,
        val getProjection: (width: Int, height: Int) -> Mat
    ) : InOut {
        var view: Mat = Mat.ID

        var projection: Mat = Mat.NAN
            private set

        fun updateProjection(width: Int, height: Int) {
            projection = getProjection(width, height)
        }

        override fun <T> submit(subject: Capturable<T>, args: T, result: Any, time: Double, transform: Mat) =
            capture.submit(subject, args, result, time, view * transform)

        override fun <T> submit(subject: Drawable<T>, args: T, time: Double, transform: Mat) =
            draw.submit(subject, args, time, view * transform)

        override fun <T> submit(subject: Playable<T>, args: T, handle: Any, time: Double, transform: Mat) =
            play.submit(subject, args, handle, time, view * transform)

        override fun <T> enqueue(subject: Capturable<T>, args: T, result: Any, coordinates: (Double) -> Mat) =
            queuedCapture.enqueue(subject, args, result) { view * coordinates(it) }

        override fun <T> enqueue(subject: Drawable<T>, args: T, coordinates: (Double) -> Mat) =
            queuedDraw.enqueue(subject, args) { view * coordinates(it) }

        override fun <T> enqueue(subject: Playable<T>, args: T, coordinates: (Double) -> Mat) =
            queuedPlay.enqueue(subject, args) { view * coordinates(it) }

        fun begin() {
            // Reset depth if wanted.
            if (zReset) {
                Gdx.gl.glClearDepthf(if (zNegative) 1f else -1f)
                Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT)
            }

            // Apply sorting if wanted.
            if (zSort) {
                Gdx.gl.glEnable(GL20.GL_DEPTH_TEST)
                Gdx.gl.glDepthFunc(if (zNegative) GL20.GL_LEQUAL else GL20.GL_GEQUAL)
            } else {
                Gdx.gl.glDisable(GL20.GL_DEPTH_TEST)
            }

            // Apply culling if wanted.
            if (zFaceCull) {
                Gdx.gl.glEnable(GL20.GL_CULL_FACE)
                Gdx.gl.glCullFace(if (zNegative) GL20.GL_BACK else GL20.GL_FRONT)
            } else {
                Gdx.gl.glDisable(GL20.GL_CULL_FACE)
            }

            // Start drawing with the current matrices.
            capture.begin(projection)
            draw.begin(projection)
            play.begin(projection)
        }

        fun end() {
            // Submit all UI queue elements.
            queuedCapture.update(time)
            queuedDraw.update(time)
            queuedPlay.update(time)

            // Commit calls to context.
            draw.render(time, zNegative != invertedSorting, context)

            // Update sounds.
            play.play(time)

            // Perform capture for primary pointer and handle.
            val (result, intersection) = capture.collect(time, zNegative, Gdx.input.uniformX, Gdx.input.uniformY)
            capture(this, result, intersection, view.inv * intersection)

            // Finalize batch for this call.
            capture.end()
            draw.end()
            play.end()
        }
    }

    private val layers = TreeMap<Int, Layer>()

    protected fun addLayer(index: Int, layer: Layer): Layer {
        // Put layer.
        val existing = layers.put(index, layer)

        // Assert that it was not assigned before.
        require(existing == null) { "Index $index already has layer $existing" }

        // Return layer itself.
        return layer
    }


    protected fun layerOrthographic(
        zReset: Boolean = false,
        zNear: Float = -100f,
        zFar: Float = 100f,
        invertedSorting: Boolean = true
    ) = Layer(
        zNegative = false,
        zReset = zReset,
        zFaceCull = true,
        zSort = true,
        invertedSorting = invertedSorting
    ) { width, height ->
        Mat.ortho2D(0f, 0f, width.toFloat(), height.toFloat(), zNear, zFar)
    }

    protected fun layerPerspective(
        zReset: Boolean = false,
        zNear: Float = 0.01f,
        zFar: Float = 100f,
        verticalFOV: Float = 60f,
        invertedSorting: Boolean = true
    ) = Layer(
        zNegative = true,
        zReset = zReset,
        zFaceCull = true,
        zSort = true,
        invertedSorting = invertedSorting
    ) { width, height ->
        Mat.perspective(zNear, zFar, verticalFOV, width.toFloat() / height)
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
     * The context to use.
     */
    private val context = StandardContext()

    /**
     * The time of the last render.
     */
    private var timeOfLast: Double? = null

    abstract val time: Double

    override fun render() {
        // Reset clear color and depth.
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClearDepthf(1f)

        // Clear screen.
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        // Bind the current time, generate delta and set new last time.
        val time = time
        val delta = timeOfLast?.minus(time)?.unaryMinus() ?: initialDelta
        timeOfLast = time

        // Delegate to user render.
        render(time, delta)
    }

    /**
     * Handles captures of the pointer.
     * @param layer The layer that originated the capture.
     * @param result The result defined for the capture.
     * @param relative The coordinate in the layer space.
     * @param absolute The coordinate in absolute space (inverted view applied).
     */
    open fun capture(layer: Layer, result: Any?, relative: Vec, absolute: Vec) = Unit

    /**
     * Renders the graphics, sound and captures at the time.
     */
    open fun render(time: Double, delta: Double) = Unit

    override fun resize(width: Int, height: Int) {
        for ((_, layer) in layers)
            layer.updateProjection(width, height)
    }

    override fun create() {
        // Initialize proper projection matrices.
        for ((_, layer) in layers)
            layer.updateProjection(Gdx.graphics.width, Gdx.graphics.height)

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
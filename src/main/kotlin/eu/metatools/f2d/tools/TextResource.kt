package eu.metatools.f2d.tools

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Align
import eu.metatools.f2d.context.*
import java.io.FilenameFilter
import java.util.*
import kotlin.math.roundToInt
import kotlin.math.sqrt

enum class Location {
    Start, Center, End
}

/**
 * Arguments to refer a [TextResource].
 * @property size The display size.
 * @property name The name or null if primary font should be used.
 * @property bold True if bold.
 * @property italic True if italic.
 * @property horizontal The horizontal alignment.
 */
class ReferText(
    val name: String? = null,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val horizontal: Location = Location.Start,
    val vertical: Location = Location.Start
) {
    companion object {
        /**
         * The default value of referring to a texture.
         */
        val DEFAULT = ReferText()
    }
}

/**
 * Font definition set.
 * @property fontFile The file containing the font definition.
 * @property size The pixel size this font is rendered for.
 * @property bold True if rendered in bold.
 * @property italic True if rendered in italic.
 */
data class FontDefinition(
    val fontFile: FileHandle,
    val name: String,
    val size: Int,
    val bold: Boolean,
    val italic: Boolean
)

/**
 * Finds all font definitions in the folder.
 */
fun findDefinitions(folder: FileHandle): List<FontDefinition> {
    // Construct regex and filter.
    val match = Regex("(.+)_([rbi]+)?_(\\d+)\\.fnt")
    val filter = FilenameFilter { _, n -> match.matches(n) }

    // Enumerate the folder.
    return folder.list(filter).mapNotNull { font ->
        match.matchEntire(font.name())?.let { m ->
            // Get parameters from group.
            val name = m.groupValues[1]
            val bold = 'b' in m.groupValues[2]
            val italic = 'i' in m.groupValues[2]
            val size = m.groupValues[3].toInt()

            // Return font definiton.
            FontDefinition(font, name, size, bold, italic)
        }
    }
}

/**
 * A text resource definition on the list of font definitions.
 * @property definitions The font definitions function.
 */
class TextResource(
    val definitions: () -> List<FontDefinition>
) : MemorizingResource<ReferText?, Drawable<String>>() {

    private data class Characteristic(val name: String, val bold: Boolean, val italic: Boolean)

    /**
     * Map from defining characteristics to size to definition map.
     */
    private val fromParams = mutableMapOf<Characteristic, NavigableMap<Int, FontDefinition>>()

    /**
     * Map from font definition to font value.
     */
    private val fromDefinition = mutableMapOf<FontDefinition, BitmapFont>()

    /**
     * Primary font if only loaded with one
     */
    private var primary: String? = null

    private var initialized = false

    override fun initialize() {
        if (!initialized) {
            // Load definitions.
            val definitions = definitions()

            // Add all definitions.
            for (d in definitions) {
                // Determine characteristic.
                val characteristic = Characteristic(d.name, d.bold, d.italic)

                // Add to map, create if not yet present.
                val target = fromParams.getOrPut(characteristic) { TreeMap() }
                target[d.size] = d
            }

            // Determine the primary font.
            primary = definitions.mapTo(hashSetOf()) { it.name }.singleOrNull()

            // Assign initialized.
            initialized = true
        }
    }

    override fun dispose() {
        fromDefinition.values.forEach { it.dispose() }
        fromDefinition.clear()
        fromParams.clear()
        initialized = false
    }

    override fun referNew(argsResource: ReferText?) = object : Drawable<String> {
        /**
         * Resolve active args.
         */
        val activeArgsResource = argsResource ?: ReferText.DEFAULT

        /**
         * Get name, default to primary, but must not be null.
         */
        val name = requireNotNull(activeArgsResource.name ?: primary) {
            "No name given in $argsResource and no primary font present."
        }

        /**
         * Get primary characteristics.
         */
        val characteristic = Characteristic(name, activeArgsResource.bold, activeArgsResource.italic)

        // TODO: With updated model constraints on the continuous renderer, the size computations might change.
        override fun draw(args: String, time: Double, spriteBatch: SpriteBatch) {
            // Get source from parameter.
            val source = requireNotNull(fromParams[characteristic]) {
                "No font in definition for $characteristic"
            }

            // Get planar scaling factor from transformation.
            val fromModel = sqrt(
                spriteBatch.transformMatrix.scaleXSquared + spriteBatch.transformMatrix.scaleYSquared
            )

            // Get font definition to use from next larger definition, or use respective end of the map.
            val use = source.ceilingEntry(fromModel.roundToInt())
                ?: if (fromModel > source.lastKey())
                    source.lastEntry()
                else
                    source.firstEntry()

            // Get bitmap font for that definition.
            val font = fromDefinition.getOrPut(use.value) {
                BitmapFont(use.value.fontFile).also { font ->
                    font.regions.forEach {
                        it.texture.setFilter(
                            Texture.TextureFilter.Linear,
                            Texture.TextureFilter.Linear
                        )
                    }
                }
            }

            // Determine how this instance must be scaled to get back to the original size.
            val factor = 1f / use.key

            // Draw with adjusted matrix, memorize and reset transformation.
            val before = spriteBatch.transformMatrix.cpy()
            spriteBatch.transformMatrix = spriteBatch.transformMatrix
                .scale(factor, factor, 1f)

            // Get alignment from params.
            val align = when (activeArgsResource.horizontal) {
                Location.Start -> Align.left
                Location.Center -> Align.center
                Location.End -> Align.right
            }

            // Get displacement from line count and font height.
            val displace = when (activeArgsResource.vertical) {
                Location.Start -> 0f
                Location.Center -> font.lineHeight * args.lineSequence().count() / 2f
                Location.End -> font.lineHeight * args.lineSequence().count()
            }

            // Transfer color, font uses it's own primary color value.
            font.color = spriteBatch.color
            font.draw(spriteBatch, args, 0f, displace, 1f, align, false)

            // Reset transform.
            spriteBatch.transformMatrix = before
        }
    }
}
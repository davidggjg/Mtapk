package brut.androlib.res.decoder

import brut.androlib.exceptions.AndrolibException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * Drop-in replacement for apktool-lib's own `ResNinePatchStreamDecoder`,
 * built into this app instead of the upstream dependency jar (see
 * app/build.gradle.kts, which strips the original class out at build time).
 *
 * The original decodes a compiled nine-patch PNG into a human-editable
 * source-style .9.png with the stretch-region border drawn in as real
 * pixels, using javax.imageio.ImageIO / java.awt.image.BufferedImage.
 * Neither exists on Android's runtime at all (there is no AWT/ImageIO
 * implementation, only compile-time stub classes in the SDK's android.jar),
 * so calling it throws NoClassDefFoundError - which, being an Error rather
 * than an AndrolibException, isn't caught by ResFileDecoder's per-file
 * fallback and aborts decoding the whole APK.
 *
 * This app only needs to view/edit/re-zip decoded files, not round-trip
 * rebuild them the way `apktool b` would, so a faithful port of the border
 * decoration isn't required: copying the nine-patch PNG bytes through
 * unchanged (matching upstream's own ResRawStreamDecoder used for unknown
 * file types) yields a valid, correct, viewable PNG - just without the
 * baked-in debug border pixels a desktop nine-patch editor would show.
 */
class ResNinePatchStreamDecoder : ResStreamDecoder {

    @Throws(AndrolibException::class)
    override fun decode(input: InputStream, out: OutputStream) {
        try {
            input.copyTo(out)
        } catch (e: IOException) {
            throw AndrolibException("Could not decode raw nine-patch stream.", e)
        }
    }
}

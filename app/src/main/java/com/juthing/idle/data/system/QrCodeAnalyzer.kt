package com.juthing.idle.data.system

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer

/**
 * Decodes QR codes and barcodes from the camera preview using ZXing.
 *
 * ZXing rather than ML Kit: ML Kit pulls in Google Play Services, which would make Idle unusable
 * on a de-Googled phone for no functional gain.
 *
 * The analyser stops reporting after the first successful read, so a code held in front of the
 * camera produces one result rather than a stream of identical ones.
 *
 * @param onDecoded called once, on the analyser thread, with the decoded payload.
 */
class QrCodeAnalyzer(private val onDecoded: (String) -> Unit) : ImageAnalysis.Analyzer {

    private val reader = MultiFormatReader().apply {
        setHints(mapOf(DecodeHintType.TRY_HARDER to true))
    }
    private var decoded = false

    override fun analyze(image: ImageProxy) {
        if (decoded) {
            image.close()
            return
        }

        try {
            // The Y plane alone is the luminance channel, which is all ZXing needs.
            val plane = image.planes[0]
            val data = ByteArray(plane.buffer.remaining()).also(plane.buffer::get)
            val source = PlanarYUVLuminanceSource(
                data,
                plane.rowStride,
                image.height,
                0,
                0,
                image.width,
                image.height,
                false,
            )
            val result = reader.decodeWithState(BinaryBitmap(HybridBinarizer(source)))
            decoded = true
            onDecoded(result.text)
        } catch (_: NotFoundException) {
            // No code in this frame, which is the common case while the user is aiming.
        } finally {
            reader.reset()
            image.close()
        }
    }
}

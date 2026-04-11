/*
 * Copyright (c) 2026 Ryan P. Walsh
 * All rights reserved.
 * This software is proprietary and confidential.
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * This is NOT open source software.
 */

package com.walshtech.receiptwrangler.camera

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import kotlin.math.abs

/**
 * Lightweight, fully local image-quality hints for receipt capture.
 * These heuristics stay intentionally simple so they can run on-device in real time.
 */
class ReceiptFrameAdvisor(private val onHint: (CameraHintState) -> Unit) : ImageAnalysis.Analyzer {
    override fun analyze(image: ImageProxy) {
        val yPlane = image.planes.firstOrNull()
        if (yPlane == null) {
            image.close(); return
        }
        val buffer = yPlane.buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        if (bytes.isEmpty()) { image.close(); return }

        var sum = 0L
        var sumSquares = 0L
        var clippedBright = 0
        var edgeDelta = 0L
        var centerEdgeDelta = 0L
        var centerCount = 0
        val width = image.width.coerceAtLeast(1)
        val height = image.height.coerceAtLeast(1)
        val stride = yPlane.rowStride.coerceAtLeast(width)
        val centerLeft = (width * 0.25f).toInt()
        val centerRight = (width * 0.75f).toInt()
        val centerTop = (height * 0.25f).toInt()
        val centerBottom = (height * 0.75f).toInt()
        val sampleSize = minOf(bytes.size, stride * height)
        for (i in 0 until sampleSize) {
            val v = bytes[i].toInt() and 0xFF
            sum += v
            sumSquares += (v * v)
            if (v > 245) clippedBright++
            if (i > 0) edgeDelta += abs(v - (bytes[i - 1].toInt() and 0xFF))

            val x = i % stride
            val y = i / stride
            if (x in centerLeft until centerRight && y in centerTop until centerBottom) {
                centerCount++
                if (i > 0) centerEdgeDelta += abs(v - (bytes[i - 1].toInt() and 0xFF))
            }
        }
        val avg = sum.toDouble() / sampleSize.toDouble().coerceAtLeast(1.0)
        val variance = (sumSquares.toDouble() / sampleSize.toDouble().coerceAtLeast(1.0)) - (avg * avg)
        val stdDev = kotlin.math.sqrt(variance.coerceAtLeast(0.0))
        val glare = clippedBright.toDouble() / sampleSize.toDouble().coerceAtLeast(1.0)
        val sharpness = edgeDelta.toDouble() / sampleSize.toDouble().coerceAtLeast(1.0)
        val centerSharpness = if (centerCount == 0) 0.0 else centerEdgeDelta.toDouble() / centerCount.toDouble()
        val hint = when {
            glare > 0.18 -> CameraHintState("Reduce glare", "Tilt the receipt to remove hotspots before capture.")
            avg < 55 -> CameraHintState("More light", "Move to brighter light or raise screen-side exposure.")
            stdDev < 18 -> CameraHintState("Low contrast", "Flatten folds and increase contrast before capture.")
            centerSharpness < sharpness * 0.75 -> CameraHintState("Move closer", "Receipt is likely too small in frame for reliable OCR.")
            sharpness < 12 -> CameraHintState("Hold steady", "Pause briefly before capture for cleaner OCR.")
            else -> CameraHintState("Looks good", "Edges and exposure look usable.")
        }
        onHint(hint)
        image.close()
    }
}

data class CameraHintState(val title: String, val detail: String)
